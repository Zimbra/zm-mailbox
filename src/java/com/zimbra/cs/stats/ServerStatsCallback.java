/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.stats;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.stats.RealtimeStatsCallback;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;
import com.zimbra.cs.mailbox.MessageCache;
import com.zimbra.cs.store.BlobInputStream;


public class ServerStatsCallback implements RealtimeStatsCallback {

    public Map<String, Object> getStatData() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(ZimbraPerf.RTS_MBOX_CACHE_SIZE, ZimbraPerf.getMailboxCacheSize());
        data.put(ZimbraPerf.RTS_MSG_CACHE_SIZE, MessageCache.getSize());
        data.put(ZimbraPerf.RTS_MSG_CACHE_BYTES, MessageCache.getDataSize());
        data.put(ZimbraPerf.RTS_FD_CACHE_SIZE, BlobInputStream.getFileDescriptorCache().getSize());
        
        Provisioning prov = Provisioning.getInstance();
        if (prov instanceof LdapProvisioning) {
            LdapProvisioning ldap = (LdapProvisioning) prov;
            data.put(ZimbraPerf.RTS_ACCOUNT_CACHE_SIZE, ldap.getAccountCacheSize());
            data.put(ZimbraPerf.RTS_ACCOUNT_CACHE_HIT_RATE, ldap.getAccountCacheHitRate());
            data.put(ZimbraPerf.RTS_COS_CACHE_SIZE, ldap.getCosCacheSize());
            data.put(ZimbraPerf.RTS_COS_CACHE_HIT_RATE, ldap.getCosCacheHitRate());
            data.put(ZimbraPerf.RTS_DOMAIN_CACHE_SIZE, ldap.getDomainCacheSize());
            data.put(ZimbraPerf.RTS_DOMAIN_CACHE_HIT_RATE, ldap.getDomainCacheHitRate());
            data.put(ZimbraPerf.RTS_SERVER_CACHE_SIZE, ldap.getServerCacheSize());
            data.put(ZimbraPerf.RTS_SERVER_CACHE_HIT_RATE, ldap.getServerCacheHitRate());
            data.put(ZimbraPerf.RTS_ZIMLET_CACHE_SIZE, ldap.getZimletCacheSize());
            data.put(ZimbraPerf.RTS_ZIMLET_CACHE_HIT_RATE, ldap.getZimletCacheHitRate());
            data.put(ZimbraPerf.RTS_GROUP_CACHE_SIZE, ldap.getGroupCacheSize());
            data.put(ZimbraPerf.RTS_GROUP_CACHE_HIT_RATE, ldap.getGroupCacheHitRate());
            data.put(ZimbraPerf.RTS_XMPP_CACHE_SIZE, ldap.getXMPPCacheSize());
            data.put(ZimbraPerf.RTS_XMPP_CACHE_HIT_RATE, ldap.getXMPPCacheHitRate());
        }
        return data;
    }

}
