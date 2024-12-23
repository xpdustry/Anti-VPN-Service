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

package com.xpdustry.avs.misc;

import com.xpdustry.avs.command.AVSCommandManager;
import com.xpdustry.avs.command.Command;
import com.xpdustry.avs.config.AVSConfig;
import com.xpdustry.avs.config.ConfigField;
import com.xpdustry.avs.misc.address.*;
import com.xpdustry.avs.service.providers.ProviderAction;
import com.xpdustry.avs.util.network.Subnet;

import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;


public class JsonSerializer {
  @SuppressWarnings("rawtypes")
  public static void apply(Json json) {
    json.setSerializer(Subnet.class, new Json.Serializer<>() {
      @Override
      public void write(Json json, Subnet object, Class knownType) {
        json.writeValue(object.toString());
      }

      @Override
      public Subnet read(Json json, JsonValue jsonData, Class type) {
        return jsonData.isNull() ? null : Subnet.createInstance(jsonData.asString());
      }
    });
    
    json.setSerializer(AddressInfos.class, new Json.Serializer<>() {
      @Override
      public void write(Json json, AddressInfos object, Class knownType) {
        json.writeObjectStart();
        json.writeValue("ip", object.ip);
        json.writeValue("network", object.network);
        json.writeValue("location", object.location);
        json.writeValue("ISP", object.ISP);
        json.writeValue("ASN", object.ASN);
        json.writeValue("locale", object.locale);
        json.writeValue("longitude", object.longitude);
        json.writeValue("latitude", object.latitude);
        json.writeObjectEnd();
      }

      @Override
      public AddressInfos read(Json json, JsonValue jsonData, Class type) {
        if (jsonData.isNull()) return null;
        AddressInfos infos = new AddressInfos(jsonData.getString("ip"));
        
        infos.network = jsonData.getString("network");
        infos.location = jsonData.getString("location");
        infos.ISP = jsonData.getString("ISP");
        infos.ASN = jsonData.getString("ASN");
        infos.locale = jsonData.getString("locale");
        infos.longitude = jsonData.getFloat("longitude");
        infos.latitude = jsonData.getFloat("latitude");
        
        return infos;
      }
      
    });
    
    json.setSerializer(AddressStats.class, new Json.Serializer<>() {
      @Override
      public void write(Json json, AddressStats object, Class knownType) {
        json.writeObjectStart();
        json.writeValue("kickNumber", object.kickNumber);
        json.writeObjectEnd();
      }

      @Override
      public AddressStats read(Json json, JsonValue jsonData, Class type) {
        AddressStats stats = new AddressStats();
        
        stats.kickNumber = jsonData.getInt("kickNumber");
        
        return stats;
      }
    });
    
    json.setSerializer(AddressType.class, new Json.Serializer<>() {
      @Override
      public void write(Json json, AddressType object, Class knownType) {
        json.writeValue(object.toBinary());
      }

      @Override
      public AddressType read(Json json, JsonValue jsonData, Class type) {
        return AddressType.fromBinary(jsonData.asLong());
      }
    });
    
    json.setSerializer(AddressValidity.class, new Json.Serializer<>() {
      @Override
      public void write(Json json, AddressValidity object, Class knownType) {
        json.writeObjectStart();
        json.writeValue("address", object.subnet);
        if (object.infos != null)
          json.writeValue("infos", object.infos, AddressInfos.class);
        json.writeValue("stats", object.stats);
        json.writeValue("type", object.type);
        json.writeObjectEnd();
      }
      
      @Override
      public AddressValidity read(Json json, JsonValue jsonData, Class type_) {
        Subnet subnet = json.readValue("address", Subnet.class, jsonData);
        if (subnet == null) 
          throw new IllegalArgumentException("malformed AddressValidity object; \"address\" is missing");

        AddressInfos infos = json.readValue("infos", AddressInfos.class, jsonData);
        AddressValidity valid = new AddressValidity(subnet, infos);
        AddressStats stats = json.readValue("stats", AddressStats.class, jsonData);
        AddressType type = json.readValue("type", AddressType.class, jsonData);

        if (stats != null) valid.stats = stats;
        if (type != null) valid.type = type;
        return valid;
      }
    });
    
    json.setSerializer(ProviderAction.class, new Json.Serializer<>() {
      @Override
      public void write(Json json, ProviderAction object, Class knownType) {
        json.writeValue(object.category.name + '.' + object.name);
      }

      @Override
      public ProviderAction read(Json json, JsonValue jsonData, Class type) {
        String name = jsonData.asString();
        int dot = name.indexOf('.');
        if (dot < 1) throw new IllegalArgumentException("malformed ProviderAction value");
        
        String category = name.substring(0, dot);
        String action = name.substring(dot+1);
        return ProviderAction.all.find(a -> a.category.name.equals(category) && a.name.equals(action));
      }
    });
    
    json.setSerializer(ConfigField.class, new Json.Serializer<>() {
      @Override
      public void write(Json json, ConfigField object, Class knownType) {
        json.writeValue(object.name);
      }

      @Override
      public ConfigField read(Json json, JsonValue jsonData, Class type) {
        String name = jsonData.asString();
        return AVSConfig.all.find(f -> f.name.equals(name));
      }
    });
    
    json.setSerializer(Command.class, new Json.Serializer<>() {
      @Override
      public void write(Json json, Command object, Class knownType) {
        json.writeValue(object.name);
      }

      @Override
      public Command read(Json json, JsonValue jsonData, Class type) {
        String name = jsonData.asString();
        return AVSCommandManager.subCommands.find(c -> c.name.equals(name));
      }
    });
  }
}
