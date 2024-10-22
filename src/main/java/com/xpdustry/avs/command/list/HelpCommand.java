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

import com.xpdustry.avs.command.AVSCommandManager;
import com.xpdustry.avs.command.Command;
import com.xpdustry.avs.util.Logger;
import com.xpdustry.avs.util.Strings;

import arc.struct.Seq;


public class HelpCommand extends com.xpdustry.avs.command.Command {
  public HelpCommand() { super("help"); }

  @Override
  public void run(String[] args, Logger logger, boolean restrictedMode) {
    if (args.length == 0) {
      printCommands(restrictedMode ? AVSCommandManager.restrictedCommands : AVSCommandManager.subCommands, 
                    logger, restrictedMode);
      return;
    }
    
    Command command = AVSCommandManager.subCommands.find(c -> c.name.equals(args[0]));
    
    if (command == null) {
      logger.err("avs.command.not-found", args[0]);
      return;
    } else if (restrictedMode && !AVSCommandManager.restrictedCommands.contains(command)) {
      logger.err("avs.command.restricted");
      return;
    }
    
    command.printHelp(logger);
  }
  
  private static void printCommands(Seq<Command> list, Logger logger, boolean forPlayer) {
    if (list.isEmpty()) {
      logger.warn("avs.command.nothing");
      return;
    }
    
    String format = logger.getKey("avs.command.help.format." + (forPlayer ? "player" : "server"));
    
    if (forPlayer) {
      StringBuilder builder = new StringBuilder(logger.getKey("avs.command.help.list") + "\n");
      list.each(c -> {
        String params = c.getArgs(logger);
        builder.append(Strings.format(format, c.name, params.isEmpty() ? "" : " "+params, c.getDesc(logger)) + "\n");
      });
      logger.infoNormal(builder.toString());
      
    } else {
      logger.info("avs.command.help.list");
      list.each(c -> {
        String params = c.getArgs(logger);
        logger.infoNormal(Strings.format(format, c.name, params.isEmpty() ? "" : " "+params, c.getDesc(logger)));
      });
    }
  }
}
