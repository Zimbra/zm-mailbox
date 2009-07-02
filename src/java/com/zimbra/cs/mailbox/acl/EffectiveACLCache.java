package com.zimbra.cs.mailbox.acl;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.memcached.MemcachedMap;
import com.zimbra.common.util.memcached.MemcachedSerializer;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.MetadataList;
import com.zimbra.cs.memcached.MemcachedConnector;

public class EffectiveACLCache {
    
    private static EffectiveACLCache sTheInstance = new EffectiveACLCache();
    
    private MemcachedMap<EffectiveACLCacheKey, ACL> mMemcachedLookup;

    EffectiveACLCache() {
        ZimbraMemcachedClient memcachedClient = MemcachedConnector.getClient();
        ACLSerializer serializer = new ACLSerializer();
        mMemcachedLookup = new MemcachedMap<EffectiveACLCacheKey, ACL>(memcachedClient, serializer); 
    }

    private static class ACLSerializer implements MemcachedSerializer<ACL> {
        
        public Object serialize(ACL value) {
            return value.encode().toString();
        }

        public ACL deserialize(Object obj) throws ServiceException {
            MetadataList meta = new MetadataList((String) obj);
            return new ACL(meta);
        }
    }
    
    private ACL get(EffectiveACLCacheKey key) throws ServiceException {
        return mMemcachedLookup.get(key);
    }
    
    private void set(EffectiveACLCacheKey key, ACL data) throws ServiceException {
        mMemcachedLookup.put(key, data);
    }
    
    public static ACL get(String acctId, int folderId) throws ServiceException {
        EffectiveACLCacheKey key = new EffectiveACLCacheKey(acctId, folderId);
        return sTheInstance.get(key);
    }
    
    public static void set(String acctId, int folderId, ACL acl) throws ServiceException {
        EffectiveACLCacheKey key = new EffectiveACLCacheKey(acctId, folderId);
        
        // if no effective ACL, return an empty ACL
        if (acl == null)
            acl = new ACL();
        sTheInstance.set(key, acl);
    }
}
