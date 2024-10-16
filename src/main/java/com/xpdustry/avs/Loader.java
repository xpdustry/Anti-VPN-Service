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

package com.xpdustry.avs;

import com.xpdustry.avs.misc.AVSConfig;
import com.xpdustry.avs.misc.AVSEvents;
import com.xpdustry.avs.service.AntiVpnService;
import com.xpdustry.avs.service.ServiceManager;
import com.xpdustry.avs.util.DynamicSettings;
import com.xpdustry.avs.util.Logger;
import com.xpdustry.avs.util.VersionChecker;
import com.xpdustry.avs.util.bundle.L10NBundle;

import arc.Events;
import arc.files.Fi;

import mindustry.Vars;
import mindustry.mod.Mods;


public class Loader {
  private static final Logger logger = new Logger();
  private static boolean loaded = false;
  private static Mods.LoadedMod mod;
  
  public static void load(Class<? extends mindustry.mod.Mod> modClass) {
    if (loaded) throw new Mods.ModLoadException("already loaded");
    
    Events.fire(new AVSEvents.AVSLoadingEvent());
    
    mod = Vars.mods.getMod(modClass);
    if (mod == null) throw new Mods.ModLoadException("the specified mod is not loaded");
    
    // First, load settings
    if (loadCheck(() -> loadSettings(Vars.modDirectory.child(mod.meta.name)))) return; 
    // After, the bundles
    if (loadCheck(Loader::loadBundles)) return; 
    // Complete the settings loading
    finishSettingsLoading(); 
    // Check the version
    VersionChecker.checkAndPromptToUpgrade(mod.meta.repo, mod.meta.version); 
    // And init the plugin
    if (loadCheck(Loader::initPlugin)) return; 
    
    loaded = true;
    
    Events.fire(new AVSEvents.AVSLoadedEvent());
  }
  
  /** @return {@code true} if error */
  private static boolean loadCheck(arc.func.Boolp run) {
    boolean isSecurityError = false;
    Throwable error = null;
    try {
      if (run.get()) return false;
      
    } catch (SecurityException e) {
      error = e;
      isSecurityError = true;
    } catch (Throwable e) {
      error = e;
    }
    
    Events.fire(new AVSEvents.AVSLoadingFailedEvent());
    logger.errNormal("");
    logger.errNormal("##################################################");
    if (isSecurityError) {
      logger.err("avs.manager.security-error");
      if (error != null) logger.err("avs.general-error", error.toString());
      
    } else {
      logger.err("avs.loading.failed");
      if (error != null) {
        logger.errNormal("");
        logger.err("avs.general-error", error);
      }
    }
    logger.warnNormal("");
    logger.warn("avs.loading.error");
    logger.errNormal("##################################################");
    logger.errNormal("");
    return true;
  }
  
  public static boolean done() {
    return loaded;
  }
  
  public static boolean loadSettings(Fi workingDirectory) {
    AVSConfig.setWorkingDirectory(workingDirectory);
    AVSConfig.load();
    DynamicSettings.logFile = AVSConfig.subDir(AVSConfig.settingsDirectory.getString())
                                       .child(workingDirectory.name() + ".log");
    DynamicSettings.autosaveSpacing = AVSConfig.autosaveSpacing.getInt();
    return AVSConfig.isLoaded();
  }
  
  /** Because we need to load the bundles before trigger the initial value change listener */
  public static void finishSettingsLoading() {
    AVSConfig.notifyAllValueChanged();
  }
  
  public static boolean initPlugin() {
    if (ServiceManager.registerListeners()) {
      AntiVpnService.load(); 
      DynamicSettings.startAutosave("AVS-Autosave");
      DynamicSettings.waitForAutosave();
      return AntiVpnService.isOperational();
    }
    return false;
  }
  
  public static boolean loadBundles() {
    L10NBundle.load(mod.root.child("bundles"), AVSConfig.defaultLocale.getString());
    logger.debug("avs.loading.custom-bundles");
    Fi bundles = AVSConfig.subDir(AVSConfig.bundlesDirectory.getString());
    bundles.mkdirs();
    L10NBundle.appendBundles(bundles);
    logger.info("avs.loading.bundle-loaded", L10NBundle.bundles.size, L10NBundle.defaultLocale);
    return L10NBundle.isLoaded();
  }
  
  /** Will check the state of plugin's components */
  public static boolean isAllOk() {
    return done() && 
           AVSConfig.isLoaded() &&
           L10NBundle.isLoaded() &&
           ServiceManager.isReady() &&
           AntiVpnService.isOperational() && 
           AntiVpnService.allProviders.allMatch(p -> p.isLoaded()); // <- this one can be unsafe
  }
}
