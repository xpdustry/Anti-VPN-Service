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

package com.xpdustry.avs.command.list;

import com.xpdustry.avs.service.AntiVpnService;
import com.xpdustry.avs.service.providers.type.AddressProviderReply;
import com.xpdustry.avs.util.Logger;
import com.xpdustry.avs.util.PlayerLogger;
import com.xpdustry.avs.util.Strings;
import com.xpdustry.avs.util.network.AdvancedHttp;

import arc.struct.ObjectMap;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.net.Administration.PlayerInfo;


public class InfoCommand extends com.xpdustry.avs.command.Command {
  private static String publicAddress;
  private static ObjectMap<String, PlayerInfo> playerInfos;
  
  public InfoCommand() { super("info"); }

  @Override
  public void run(String[] args, Logger logger, boolean restrictedMode) { 
    Seq<Helper> queries = new Seq<>();
    String query = String.join(" ", args);
    
    if (args.length == 0) {
      if (logger instanceof PlayerLogger) {
        mindustry.gen.Player player = ((PlayerLogger) logger).player;
        queries.add(new Helper(player.con.address, Vars.netServer.admins.getInfoOptional(player.uuid())));
        
      } else if (publicAddress == null) {
        // Get the public address by using an api and cache the result
        AdvancedHttp.Reply reply = AdvancedHttp.get("http://api.ipify.org");
        if (reply.isError()) {
          logger.err("avs.command.info.public-address-failed");
          return;
        }
        queries.add(new Helper(publicAddress = reply.result, null));
        
      } else queries.add(new Helper(publicAddress, null));

    } else {
      ObjectMap<String, PlayerInfo> infos;
      
      // Try to get access of player infos, else try to get a copy of the list.
      try {
        if (playerInfos == null) 
          playerInfos = arc.util.Reflect.get(Vars.netServer.admins, "playerInfo");   
        infos = playerInfos;
      } catch (RuntimeException e) {
        logger.debug("avs.command.info.player-info-err");
        infos = Vars.netServer.admins.findByName("").toSeq().asMap(i -> i.id);
      }

      // Search name and uuid
      String name = Strings.normalise(query);
      infos.each((id, info) -> {
        if (info.names.contains(n -> Strings.normalise(n).strip().equals(name)) ||
            info.id.equals(query))
          queries.addUnique(new Helper(info.lastIP, info));
      });
      
      // Try with IP (only for console)
      if (queries.isEmpty() && !restrictedMode) {
        try { 
          com.xpdustry.avs.misc.address.AddressValidity.checkIP(query); 
          queries.add(new Helper(com.xpdustry.avs.util.network.Subnet.createInstance(query).toString(), null));
        } catch (Exception ignored) {}
        
        logger.info("avs.command.info.matches", queries.size);
      }
    }
    
    
    if (!queries.isEmpty()) {
      logger.info("avs.command.info.wait");
      queries.each(h -> h.setReply(AntiVpnService.checkAddressOnline(h.address)));
      logger.infoNormal("");
      for (int i=0; i<queries.size; i++) {
        String text = "[" + i + "] " + queries.get(i).toString(logger);
        if (restrictedMode) logger.infoNormal(text);
        else {
          for (String line : text.split("\n"))
            logger.infoNormal(line);
        }
      }
    }
  }
  
  
  private static class Helper {
    final String address;
    final PlayerInfo info;
    AddressProviderReply reply;
    
    Helper(String address, PlayerInfo info) {
      this.address = address;
      this.info = info;
    }
    
    void setReply(AddressProviderReply reply) {
      this.reply = reply;
    }
    
    String toString(Logger logger) {
      String text = "\n";
      
      if (info != null) 
        text = logger.formatKey("avs.command.info.print.head", info.plainLastName(), info.id) + "\n";
      if (reply != null) {
        text += logger.formatKey("avs.command.info.print.address", reply.validity.subnet) + "\n";
        
        if (reply.resultFound()) {
          boolean[] types = Strings.integer2binary(reply.validity.type.toBinary(), 
                                    com.xpdustry.avs.misc.address.AddressType.numberOfTypes);
          Object[] args  = new Object[types.length];
          for (int i=0; i<args.length; i++) args[i] = types[i];
          text += logger.formatKey("avs.command.info.print.security", args) + "\n";
          
          if (reply.validity.infos != null) {
            com.xpdustry.avs.misc.address.AddressInfos infos = reply.validity.infos;
            /* // Avoid to tell the location of an ip, this can be used for tracking.
            text += logger.formatKey("avs.command.info.print.location", infos.location, 
                                     infos.latitude, infos.longitude, infos.locale) + "\n";
            */
            text += logger.formatKey("avs.command.info.print.network", 
                                     infos.network, infos.ISP, infos.ASN) + "\n";
          }
        } else text += logger.formatKey("avs.command.info.print.error", reply.type.toString().toLowerCase().replace('_', ' ')) + "\n";
      } else text += logger.formatKey("avs.command.info.print.address", address) + "\n"
                   + logger.formatKey("avs.command.info.print.error", "no reply") + "\n";
      
      return text;
    }
  }
}
