/*
 * This file is part of Anti-VPN-Service (AVS). The plugin securing your server against VPNs.
 *
 * MIT License
 *
 * Copyright (c) 2025 Xpdustry
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

package com.xpdustry.avs.util.bundle;


/**
 * Bundle that contains very important messages, like loading and errors message,
 * in case of default bundle was not loaded.
 */
public class RescueBundle extends Bundle {
  private static final RescueBundle INSTANCE = new RescueBundle();
  public static RescueBundle instance() { return INSTANCE; }
  
  RescueBundle() {
    super(java.util.Locale.ENGLISH, arc.struct.StringMap.of(
      "avs.general-error", "Error: {0}",
      "avs.config.msg.loading", "Loading settings...",
      "avs.config.msg.file", "Loading file: {0}",
      "avs.config.msg.invalid-field", "{0} have an invalid value! Using the default one...", 
      "avs.loading.started", "Anti VPN Service (AVS) is loading...",
      "avs.loading.custom-bundles", "Loading custom bundles...",
      "avs.loading.bundle-loaded", "Loaded {0} locales, default is {1}.",
      "avs.loading.finished", "Loading finished in {0} seconds.",
      "avs.loading.failed", "An error occurred while loading the Anti VPN Service!",
      "avs.loading.security-error", "A security manager is present! Unable to take control of the 'ConnectPacket' listener.",
      "avs.loading.error", "Anti VPN service has been stopped and will not be used due to previous error!",
      "avs.loading.report", "You can report this error at: &lbhttps://github.com/xpdustry/Anti-VPN-Service/issues/new",
      "avs.bundle.loading.started", "Loading bundles in directory: {0}",
      "avs.bundle.loading.list", "Appending {0} bundles.",
      "avs.bundle.loading.file.loaded", "Bundle loaded for locale {0}.",
      "avs.bundle.loading.file.error", "Error while loading bundle file: {0}",
      "avs.bundle.loading.hierarchy.aplying", "Applying bundles hierarchy...",
      "avs.bundle.loading.hierarchy.error", "Error while applying bundles hierarchy!",
      "avs.bundle.loading.done", "Loading done",
      "avs.bundle.warn.bundle-locale.msg1", "Bundles files with no language, in locale code, are not allowed.",
      "avs.bundle.warn.bundle-locale.msg2", "The file name should be: prefix_xx_XX.properties. Where xx and XX are the language and country.",
      "avs.bundle.warn.empty", "The bundle for locale '{0}' is empty.",
      "avs.bundle.not-found", "Unable to find a bundle for locale {0}. Using default one...",
      "avs.bundle.default-not-found", "Unable to a find bundle for the default locale '{0}'. Using machine local...",
      "avs.command.error-detected", "An error was detected during the Anti VPN service startup. Commands have been disabled."
    ));
  }
  
  public void add(String key, String text) {
    properties.put(key, text);
  }
  
  public void remove(String key) {
    properties.remove(key);
  }
}
