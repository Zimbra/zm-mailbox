/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.admin.type.CacheEntryType;

public abstract class CacheExtension {

    private static Map<String, CacheExtension> mHandlers;
    
    /*
     * Register a cache type that can be flushed by the zmprov fc <cache> command.
     * 
     * It should be invoked from the init() method of ZimbraExtension.
     */
    public synchronized static void register(String cacheType, CacheExtension handler) {
        
        if (mHandlers == null)
            mHandlers = new HashMap<String, CacheExtension>();
        else {
            //  make sure the cache is not already registered
            CacheExtension obj = mHandlers.get(cacheType);
            if (obj != null) {
                ZimbraLog.account.warn("cache type " + cacheType + " is already registered, " +
                                       "registering of " + obj.getClass().getCanonicalName() + " is ignored");
                return;
            }    
            
            // make sure the cache type does not clash with one on of the internal cache type
            CacheEntryType cet = null;
            try {
                cet = CacheEntryType.fromString(cacheType);
            } catch (ServiceException e) {
                // this is good
            }
            if (cet != null) {
                ZimbraLog.account.warn("cache type " + cacheType + " is one of the internal cache type, " +
                        "registering of " + obj.getClass().getCanonicalName() + " is ignored");
                return;
            }
        }
        mHandlers.put(cacheType, handler);
    }
    
    public synchronized static CacheExtension getHandler(String cacheType) {
        if (mHandlers == null)
            return null;
        else    
            return mHandlers.get(cacheType);
    }
    
    public abstract void flushCache() throws ServiceException;
    
}
