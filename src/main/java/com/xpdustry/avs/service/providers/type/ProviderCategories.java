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

package com.xpdustry.avs.service.providers.type;

import com.xpdustry.avs.misc.address.AddressValidity;
import com.xpdustry.avs.util.network.Subnet;

import arc.struct.Seq;


public class ProviderCategories {
  public static interface Basic {
    /** Internal name of provider. Must be lower case with '-' as word separator */
    public String name();
    /** The name that should be used for printing */
    public String displayName();
    
    /** Load the provider content */
    public boolean load();
    /** Reload the provider content */
    public boolean reload();
    /** Save the provider content */
    public boolean save();
    
    /** @return if the provider successful loaded */
    public boolean isLoaded();
    /** @return if the provider is enabled or not */
    public boolean isEnabled();
    /** @return if the provider can be used now */
    public boolean isProviderAvailable();
    
    /** Enable the provider */
    public void enable();
    /** Disable the provider */
    public void disable();
    
    /**
     * Try to find informations about the address
     * 
     * @apiNote the address format (IPv4/IPv6) should be validated before
     * @return the provider reply (nerver {@code null})
     */
    public AddressProviderReply checkAddress(String address);
  }
  
  public static interface Cacheable extends Basic {
    public AddressValidity get(String subnet);
    public AddressValidity get(Subnet subnet);
    /** @return addresses/subnets that matches the address */
    public Seq<AddressValidity> matches(String address);
    
    /** @return the cache. (must be a copy for safety) */
    public Seq<AddressValidity> getCache();
    /** 
     * @return only the addresses/subnets in the cache.
     *         Not {@link AddressValidity} that includes informations about an address.
     */
    public Seq<Subnet> list();
    /** @return the size of the cache */
    public int cacheSize();
  }
  
  public static interface Cloudable extends Basic {
    /** Clear the cache and download again the list */
    public boolean refresh();
    
    /** The type of address ranges that correspond the downloaded list */
    public CloudDownloadedProvider.ProviderType providerType();
  }

  public static interface Editable extends Basic {
    /** Add an address to the list */
    public boolean add(AddressValidity address);
    /** Remove an address from the list */
    public boolean remove(AddressValidity address);
    /** Clear the list */
    public void clear();
  }
  
  public static interface Cleanable extends Basic {
    /**
     * Schedule the auto cleanup every X minutes, and only triggered when the list isn't empty
     * @param spacing in minutes
     * @apiNote use 0 to disable the auto cleanup
     */
    public void scheduleCleanup(float spacing);
  }
  
  public static interface Onlinable extends Basic {
    /** Define if the service can be trusted if it's saying that the address is valid */
    public boolean isTrusted();
    
    public String getURL();
    
    /** Address check cooldown before service reuse */
    public int unavailableCooldown();
    /** Remove the service from waiting list */
    public void makeAvailable();
    
    /** Number of loaded tokens */
    public int loadedTokens();
    public Seq<String> getTokens();
    /** Tokens that are in waiting list with the cooldown */
    public arc.struct.ObjectMap<String, Integer> waitingTokens();
    
    /** @return if the service absolutely needs tokens to work */
    public boolean tokensNeeded();
    /** @return if the service can work with or without tokens */
    public boolean canUseTokens();
    /** @return if the service will use tokens if it can */
    public boolean willUseTokens();
    
    /** @return whether the token has been added */
    public boolean addToken(String token);
    /** @return whether the token has been removed */
    public boolean removeToken(String token);
    /** @return whether the token unavailability has been reset */
    public boolean makeTokenAvailable(String token);
  }
}