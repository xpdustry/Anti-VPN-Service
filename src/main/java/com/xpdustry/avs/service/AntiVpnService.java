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

import com.xpdustry.avs.misc.AVSConfig;
import com.xpdustry.avs.misc.address.AddressValidity;
import com.xpdustry.avs.service.providers.custom.*;
import com.xpdustry.avs.service.providers.local.*;
import com.xpdustry.avs.service.providers.online.*;
import com.xpdustry.avs.service.providers.type.*;
import com.xpdustry.avs.util.Logger;

import arc.struct.Seq;


public class AntiVpnService {
  /**
   * Reversed providers are checked at first. <br>
   * And as his name suggests, the check is valid when the address is found. (like the whitelist)
   * 
   * @apiNote These providers should return null only if the address is not found.
   */
  public static final Seq<AddressProvider> reversedProviders = Seq.with(
      new Whitelist()
  );
  
  /** After, the user editable providers. (and the cache for online providers) */  
  public static final Seq<EditableAddressProvider> customProviders = Seq.with(
      new Blacklist(),
      // Add also the provider used as a cache for online services
      OnlineServiceProvider.cacheProvider
  );
  
  /** Next we have local lists, downloaded from providers and saved in cache. */
  public static final Seq<CloudDownloadedProvider> localProviders = Seq.with(
      new AmazonWebServices(),
      new Azure(),
      new GithubActions(),
      new GoogleCloud(),
      new OracleCloud()
  );
  /**
   * And the online Services API. <br>
   * Must be used when local providers founds nothing. <br><br>
   * This list is in rotation, so when a service is down or token limit is reach, 
   * this will use the next service. <br>
   * Or a random one, if the option was enabled.
   */
  public static final Seq<OnlineServiceProvider> onlineProviders = Seq.with(
      new VpnApiService()
  );
  
  /** Field to store all providers */
  public static final Seq<AddressProvider> allProviders = Seq.withArrays(
      reversedProviders, customProviders, localProviders, onlineProviders
  );
  
  /** Logger without a topic is by default the main plugin topic. */
  public static final Logger logger = new Logger();
  private static boolean operational = false, loaded = false;

  public static AddressProviderReply checkAddress(String ip) {
    if (!isOperational()) return null;
    
    AddressValidity.checkIP(ip);
    AddressProviderReply result;

    //TODO: fire an event
    
    for (AddressProvider p : reversedProviders) {
      result = p.checkAddress(ip);
      if (result.resultFound()) {
        result.type = AddressProviderReply.ReplyType.NOT_FOUND;
        return result;
      }
    }

    //TODO: make an option to check providers until one say it's not valid
    
    result = checkAddressImpl(ip, customProviders);
    if (result != null && result.resultFound()) return result;
    result = checkAddressImpl(ip, localProviders);
    if (result != null && result.resultFound()) return result;
    Seq<OnlineServiceProvider> p = onlineProviders;
    if (AVSConfig.randomOnlineProviders.getBool()&& 
        onlineProviders.size > 1)
      p = onlineProviders.copy().shuffle();
    result = checkAddressImpl(ip, p);
    
    // Prevents all online services from being unavailable
    
    
    //TODO: fire an event
    
    return result;
  }
  
  protected static AddressProviderReply checkAddressImpl(String ip, Seq<? extends AddressProvider> providers) {
    for (AddressProvider p : providers) {
      if (!isOperational()) return null;
      AddressProviderReply result = p.checkAddress(ip);
      
      if (result.resultFound()) {
        if (!result.validity.type.isNotValid() && p instanceof OnlineServiceProvider && 
            !((OnlineServiceProvider) p).isTrusted())
          continue;
        return result;
      }
    }      

    return null;
  }
  
  public static boolean isOperational() {
    return operational && loaded;
  }
  
  public static void stop() {
    operational = false;
  }
  
  public static void load() {
    if (loaded) return;
    stop();
    
    logger.info("avs.service.loading");
    allProviders.each(p -> p.load());
    //TODO: fire an event

    // Count the total
    int loadedRanges = 0, localProviders = 0, loadedTokens = 0, onlineProviders = 0;
    
    for (int i=0; i<allProviders.size; i++) {
      AddressProvider p = allProviders.get(i);
      
      if (p instanceof CachedAddressProvider) {
        CachedAddressProvider pc = (CachedAddressProvider) p;
        loadedRanges += pc.cacheSize();
        if (pc.isLoaded()) localProviders++;
      }
      
      if (p instanceof OnlineServiceProvider) {
        OnlineServiceProvider po = (OnlineServiceProvider) p;
        loadedTokens += po.loadedTokens();
        if (po.isLoaded()) onlineProviders++;
      }
    }
    
    logger.infoNormal("");
    logger.info("avs.service.loaded-local", loadedRanges, localProviders);
    logger.info("avs.service.loaded-online", loadedTokens, onlineProviders);
    logger.infoNormal("");
    
    loaded = true;
    operational = true;
    
    //TODO: fire an event
  }
  
  public static void save() {
    if (loaded) {
      allProviders.each(p -> p.save());
      //TODO: fire an event
    } else logger.err("avs.service.cannot-save");
  }
}
