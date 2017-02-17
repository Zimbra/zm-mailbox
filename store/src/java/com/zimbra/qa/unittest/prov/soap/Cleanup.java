/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest.prov.soap;

import com.zimbra.cs.account.soap.SoapProvisioning;
import com.zimbra.cs.ldap.unboundid.InMemoryLdapServer;
import com.zimbra.soap.admin.type.CacheEntryType;

public class Cleanup {
    static void deleteAll(String... domainNames) throws Exception {

        if (InMemoryLdapServer.isOn()) {
            return;
        }

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
                CacheEntryType.alwaysOnCluster.name() + "," +
                CacheEntryType.zimlet.name(),
                null, true);
    }
}
