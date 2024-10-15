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
import com.xpdustry.avs.service.providers.type.AddressProvider;
import com.xpdustry.avs.util.Logger;

import arc.struct.Seq;


public class ProviderCommand extends com.xpdustry.avs.command.Command {
  public static final Seq<AddressProvider> restrictedProviders = new Seq<>();
  
  public ProviderCommand() { super("provider"); }

  @Override
  public void run(String[] args, Logger logger, boolean restrictedMode) {
    logger.warnNormal("coming soon!");
    
    if (args.length == 0) {
      printProviders(restrictedMode ? restrictedProviders : AntiVpnService.allProviders, logger, restrictedMode);
      return;
    }
  }
  
  
  private static void printProviders(Seq<AddressProvider> list, Logger logger, boolean forPlayer) {
    if (list.isEmpty()) {
      logger.warn("avs.command.provider.nothing");
      return;
    }
    
    
  }
}