/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.stats;

import java.util.HashMap;
import java.util.Map;

import com.zimbra.common.stats.RealtimeStatsCallback;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.PermissionCache;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.mailbox.MessageCache;
import com.zimbra.cs.store.BlobInputStream;
import com.zimbra.cs.store.FileDescriptorCache;


public class ServerStatsCallback implements RealtimeStatsCallback {

    public Map<String, Object> getStatData() {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(ZimbraPerf.RTS_MBOX_CACHE_SIZE, ZimbraPerf.getMailboxCacheSize());
        data.put(ZimbraPerf.RTS_MSG_CACHE_SIZE, MessageCache.getSize());
        
        FileDescriptorCache fdc = BlobInputStream.getFileDescriptorCache();
        data.put(ZimbraPerf.RTS_FD_CACHE_SIZE, fdc.getSize());
        data.put(ZimbraPerf.RTS_FD_CACHE_HIT_RATE, fdc.getHitRate());
        
        data.put(ZimbraPerf.RTS_ACL_CACHE_HIT_RATE, PermissionCache.getHitRate());
        
        Provisioning prov = Provisioning.getInstance();
        if (prov instanceof LdapProv) {
            LdapProv ldap = (LdapProv) prov;
            data.put(ZimbraPerf.RTS_ACCOUNT_CACHE_SIZE, ldap.getAccountCacheSize());
            data.put(ZimbraPerf.RTS_ACCOUNT_CACHE_HIT_RATE, ldap.getAccountCacheHitRate());
            data.put(ZimbraPerf.RTS_COS_CACHE_SIZE, ldap.getCosCacheSize());
            data.put(ZimbraPerf.RTS_COS_CACHE_HIT_RATE, ldap.getCosCacheHitRate());
            data.put(ZimbraPerf.RTS_DOMAIN_CACHE_SIZE, ldap.getDomainCacheSize());
            data.put(ZimbraPerf.RTS_DOMAIN_CACHE_HIT_RATE, ldap.getDomainCacheHitRate());
            data.put(ZimbraPerf.RTS_SERVER_CACHE_SIZE, ldap.getServerCacheSize());
            data.put(ZimbraPerf.RTS_SERVER_CACHE_HIT_RATE, ldap.getServerCacheHitRate());
            data.put(ZimbraPerf.RTS_UCSERVICE_CACHE_SIZE, ldap.getUCServiceCacheSize());
            data.put(ZimbraPerf.RTS_UCSERVICE_CACHE_HIT_RATE, ldap.getUCServiceCacheHitRate());
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
