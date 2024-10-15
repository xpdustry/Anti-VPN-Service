/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2024 Xpdustry
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.xpdustry.avs.service;

import java.util.concurrent.ThreadPoolExecutor;

import com.xpdustry.avs.misc.AVSConfig;
import com.xpdustry.avs.service.providers.type.AddressProviderReply;
import com.xpdustry.avs.util.DynamicSettings;
import com.xpdustry.avs.util.Logger;
import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.bundle.L10NBundlePlayer;

import arc.Core;
import arc.func.Cons2;
import arc.util.Threads;

import mindustry.Vars;
import mindustry.core.Version;
import mindustry.net.NetConnection;
import mindustry.net.Packets.ConnectPacket;
import mindustry.net.Packets.KickReason;


public class ServiceManager {
  private static ThreadPoolExecutor threadPool = 
      (ThreadPoolExecutor) Threads.boundedExecutor("ClientValidator", AVSConfig.connectLimit.getInt());
  private static Logger logger = new Logger();
  private static Cons2<NetConnection, Object> connectPacketServerListener;
  private static boolean isShutdown = false, ready = false;
  
  public static boolean registerListeners() {
    if (ready) return ready;
    setPoolSize();
    
    try { registerIpValidatorListener(); } 
    catch (RuntimeException err) {
      logger.err("avs.manager.security.error1");
      logger.err(err);
      logger.warnNormal("");
      logger.warn("avs.manager.security.error2");
      logger.info("avs.manager.security.error3");
      Threads.await(threadPool);
      DynamicSettings.autosaveThread.interrupt();
      return ready;
    }
    
    registerServerExitListener();
    registerResetEvent();
    
    ready = true;
    return ready;
  }
  
  public boolean isShutdown() {
    return isShutdown;
  }
  
  public boolean isReady() {
    return ready;
  }
  
  /** Set size of thread pool */
  public static void setPoolSize() {
    // This is intended to limit the number of connections at the same time, 
    // It gives a little protection against ddos attacks
    try { threadPool.setMaximumPoolSize(AVSConfig.connectLimit.getInt()); }
    // avoid the case of the value has been manually modified in server settings
    catch (IllegalArgumentException e) {}; 
  }
  
  /** 
   * Takes control of the ConnectPacket listener, to wrap it.
   * And be able to make the client waiting while verifying his IP.
   */
  protected static void registerIpValidatorListener() throws RuntimeException {
    // Get listeners by changing variable as public instead of private
    connectPacketServerListener = 
        arc.util.Reflect.<arc.struct.ObjectMap<Class<?>, Cons2<NetConnection, Object>>>get(Vars.net, "serverListeners")
                                                                                      .get(ConnectPacket.class);

    // Wrap the original listener
    Vars.net.handleServer(ConnectPacket.class, (con, packet) -> {
      if (packet.locale == null) packet.locale = "en"; // Possible?
      
      if (!AntiVpnService.isOperational()) {
        logger.warn("avs.validator.not-operational", con.address);
        kickClient(con, packet, 0, true);
        return;
      }
      
      // For visual in console
      con.uuid = packet.uuid;
      
      // Handle case of multiple connections
      if (con.hasBegunConnecting) {
          con.kick(KickReason.idInUse);
          return;
      }
      
      // Rewrite some part of the initial listener
      if(con.kicked) return;
      if(con.address.startsWith("steam:")) packet.uuid = con.address.substring("steam:".length());
      if(packet.uuid == null || packet.usid == null) {
        con.kick(KickReason.idInUse);
        return;
        
      // After check version of client
      } else if (packet.versionType == null ||
                 ((packet.version == -1 || !packet.versionType.equals(Version.type)) &&
                   Version.build != -1 && !Vars.netServer.admins.allowsCustomClients())) {
        con.kick(!Version.type.equals(packet.versionType) ? KickReason.typeMismatch : KickReason.customClient, 0);
        return;
        
      } else if (packet.version != Version.build && Version.build != -1 && packet.version != -1) {
        con.kick(packet.version > Version.build ? KickReason.serverOutdated : KickReason.clientOutdated, 0);
        return;
      }

      // Also check if the client has been recently kicked
      mindustry.net.Administration.PlayerInfo infos = Vars.netServer.admins.getInfoOptional(con.uuid);
      if(System.currentTimeMillis() < Math.max(infos == null ? 0L : infos.lastKicked, 
                                               Vars.netServer.admins.kickedIPs.get(con.address, 0L))){
        con.kick(KickReason.recentKick);
        return;
      } else Vars.netServer.admins.kickedIPs.remove(con.address);
      
      // And submit a new task to the thread pool, to avoid blocking the server
      try {
        threadPool.submit(() -> {
          try {
            // Check the IP
            AddressProviderReply reply = AntiVpnService.checkAddress(con.address);
      
            if (reply.resultFound()) {
              if (reply.validity.type.isNotValid()) {
                logger.info("avs.validator.ip.blacklisted", con.address, con.uuid);
                // Kick the client without duration to avoid creating an empty account, but still register an kick duration
                kickClient(con, packet, infos == null ? 0 : AVSConfig.clientKickDuration.getInt(), false);
                return;
              }
                
            } else if (AVSConfig.resultRequired.getBool()) {
              logger.warn("avs.validator." + (
                  reply.type == AddressProviderReply.ReplyType.UNAVAILABLE ? "no-available" : 
                  reply.type == AddressProviderReply.ReplyType.NOT_FOUND ? "not-found" :
                  "failed"), 
                con.address, con.uuid);
              
              String message = AVSConfig.errorMessage.get();
              if (!message.isBlank()) con.kick(message, 0L);
              else L10NBundlePlayer.kick(con, packet.locale, "avs.validator.kick.error");
              return;
            }
            
            logger.info("avs.validator.ip.validated", con.address, con.uuid);
            // Now run the original listener on the main thread
            if (connectPacketServerListener != null) 
              Core.app.post(() -> connectPacketServerListener.get(con, packet));             
         
          } catch (Throwable e) {
            logger.warn("avs.validator.failed", con.address, con.uuid);
            logger.err("avs.general-error", e);
            String message = AVSConfig.errorMessage.get();
            if (!message.isBlank()) con.kick(message, 0L);
            else L10NBundlePlayer.kick(con, packet.locale, "avs.validator.kick.error");
          }
        });
        
      } catch (java.util.concurrent.RejectedExecutionException e) {
        // To many connection at same time, kick the player and invite it to retry connection
        logger.warn("avs.validator.busy", con.address, con.uuid);
        kickClient(con, packet, infos == null ? 0 : AVSConfig.clientKickDuration.getInt(), true);
        
      } catch (Throwable e) {
        logger.warn("avs.validator.failed", con.address, con.uuid);
        logger.err("avs.general-error", e);
        kickClient(con, packet, infos == null ? 0 : AVSConfig.clientKickDuration.getInt(), true);
      }
    });
  }
  
  protected static void kickClient(NetConnection con, ConnectPacket packet, int duration, boolean isServerBusy) {
    String message = (isServerBusy ? AVSConfig.serverBusyMessage : AVSConfig.kickMessage).get();
    if (message.isBlank()) 
      L10NBundlePlayer.kick(con, packet.locale, duration, 
                            "avs.validator.kick." + (isServerBusy ? "busy" : "not-valid"), con.address);
    else {
      message = Strings.formatWithColor(L10NBundlePlayer.getDefaultFormatter(), message, "[blue]", "[]", con.address);
      con.kick(message, duration);
    }
    
    if (duration != 0)
      Vars.netServer.admins.kickedIPs.put(con.address, 
        Math.max(Vars.netServer.admins.kickedIPs.get(con.address, 0L), System.currentTimeMillis() + duration * 1000));
  }
  
  /** Register an listener when server is existing, to save settings and cache */
  protected static void registerServerExitListener() {
    Core.app.addListener(new arc.ApplicationListener() {
      @Override
      public void dispose() { shutdownPlugin(); }
    });    
  }
  
  protected static void registerResetEvent() {
    arc.Events.on(com.xpdustry.avs.misc.AVSEvents.AVSResetEvent.class, e -> {
      // Only the reset command have the right
      if (!com.xpdustry.avs.command.list.ResetCommand.isResetConfirmed()) return;
      
      // Register an app listener to delete plugin files after saving
      Core.app.addListener(new arc.ApplicationListener() {
        @Override
        public void dispose() { resetPlugin(); }
      });
      Vars.net.dispose();
      Core.app.exit();
    });
  }
  
  public static void shutdownPlugin() {
    if (isShutdown) return; // do nothing if already shutdown
    isShutdown = true;
    AntiVpnService.stop();
    logger.info("avs.manager.dispose.waiting-for");
    Threads.await(threadPool);
    DynamicSettings.autosaveThread.interrupt();
    logger.info("avs.manager.dispose.saving");
    AntiVpnService.save();
    DynamicSettings.globalAutosave();
    logger.info("avs.manager.dispose.completed");    
  }
  
  /** Reset can only be done by the event */
  private static void resetPlugin() {
    shutdownPlugin();
    logger.infoNormal("");
    logger.info("avs.manager.reset.progress");
    DynamicSettings.autosaveThread.interrupt();
    SettingsCleaner.clearFiles();
    AVSConfig.Field[] folders = {
        AVSConfig.cloudDirectory, AVSConfig.tokensDirectory,
        AVSConfig.cacheDirectory, AVSConfig.settingsDirectory,
        AVSConfig.bundlesDirectory, AVSConfig.pluginDirectory
    };
    for (AVSConfig.Field d : folders) AVSConfig.subDir(d.getString()).delete();
    logger.info("avs.manager.reset.done");
  }
  
  
  /** Inherit from {@link DynamicSettings} to get access of {@link DynamicSettings#files} */
  private static class SettingsCleaner extends DynamicSettings {
    private SettingsCleaner(arc.files.Fi file) { super(file); }
    
    /** Clear values and delete the file of all loaded settings files and clear it */
    private static void clearFiles() {
      files.each(f -> {f.clear(); f.getFile().delete();});
      files.clear();
    }
  }
}