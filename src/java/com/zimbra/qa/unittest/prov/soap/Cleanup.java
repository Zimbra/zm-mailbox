package com.zimbra.qa.unittest.prov.soap;

import com.zimbra.cs.account.Provisioning.CacheEntryType;
import com.zimbra.cs.account.soap.SoapProvisioning;

public class Cleanup {
    static void deleteAll(String... domainNames) throws Exception {
        com.zimbra.qa.unittest.prov.ldap.Cleanup.deleteAll(domainNames);
        
        SoapProvisioning prov = SoapProvisioning.getAdminInstance();
        prov.flushCache(
                CacheEntryType.account.name() + "," +
                CacheEntryType.group.name() + "," +
                CacheEntryType.config.name() + "," +
                CacheEntryType.globalgrant.name() + "," +
                CacheEntryType.cos.name() + "," +
                CacheEntryType.domain.name() + "," +
                CacheEntryType.mime.name() + "," +
                CacheEntryType.server.name() + "," +
                CacheEntryType.zimlet.name(), 
                null, true);
    }
}
