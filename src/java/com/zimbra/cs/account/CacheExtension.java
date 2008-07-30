package com.zimbra.cs.account;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.Provisioning.CacheEntryType;

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
