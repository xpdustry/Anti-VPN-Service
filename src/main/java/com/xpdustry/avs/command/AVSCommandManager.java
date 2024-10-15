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

package com.xpdustry.avs.command;

import com.xpdustry.avs.command.list.*;
import com.xpdustry.avs.util.Logger;
import com.xpdustry.avs.util.PlayerLogger;

import arc.util.CommandHandler;

import mindustry.gen.Player;


public class AVSCommandManager {
  private static final Logger logger = new Logger();
  /** 
   * Sub commands of the "avs" command, or "/avs". <br>
   * It's better to do like that, to be able to use bundles,
   * because {@link CommandHandler} doesn't allow to modify the parameters or description. <br>
   * So the main command have static parameters and description in english.
   */
  public static final Command[] subCommands = {
    new ConfigCommand(),
    new ProviderCommand(),
    new ResetCommand(),
    new HelpCommand(),
  };
  
  public static void registerServer(CommandHandler handler) {
    handler.register("avs", "[command] [args...]", "Anti VPN Service command line manager", args -> {
      if (args.length == 0) {
        logger.err("avs.command.invalid-usage");
        return;
      }
      
      for (Command c : subCommands) {
        if (args[0].equals(c.name)) {
          String[] newArgs = args.length > 1 ? args[1].strip().split("\\s+") : new String[0];
          try { c.run(newArgs); }
          catch (Exception e) { logger.err(e); }
          return;
        }
      }
      
      logger.err("avs.command.not-found", args[0]);
    });
  }
  
  public static void registerClient(CommandHandler handler) {
    handler.<Player>register("avs", "[command] [args...]", "Restricted Anti VPN Service manager for admins", (args, player) -> {
      PlayerLogger plogger = new PlayerLogger(player);
      
      if (!player.admin) {
        plogger.err("avs.command.admin-required");
        return;
      } else if (args.length == 0) {
        plogger.err("avs.command.invalid-usage");
        return;
      }
      
      for (Command c : subCommands) {
        if (args[0].equals(c.name)) {
          String[] newArgs = args.length > 1 ? args[1].strip().split("\\s+") : new String[0];
          try { c.run(newArgs, plogger, true); }
          catch (Exception e) { plogger.err("avs.general-error", e.toString()); }
          return;
        }
      }
      
      plogger.err("avs.command.not-found", args[0]);
    });
  }
  
  
}