/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.ldap.unboundid;

import junit.framework.Assert;
import org.junit.Ignore;

import org.junit.Test;

import com.unboundid.ldap.sdk.FailoverServerSet;
import com.unboundid.ldap.sdk.SingleServerSet;
import com.zimbra.cs.ldap.LdapConnType;
import com.zimbra.cs.ldap.LdapServerConfig.ZimbraLdapConfig;
import com.zimbra.cs.ldap.LdapServerType;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class LdapServerPoolTest {

    @Test
    public void testLDAPICreatePool() throws Exception {
        LdapServerPool pool = new LdapServerPool(new MockLdapiServerConfig(LdapServerType.MASTER));
        SingleServerSet set = (SingleServerSet) pool.serverSet;
        Assert.assertEquals("dummy_host", set.getAddress());
    }

    @Test
    public void testLDAPCreatePool() throws Exception {
         LdapServerPool pool = new LdapServerPool(new MockLdapServerConfig(LdapServerType.MASTER));
         FailoverServerSet set = (FailoverServerSet) pool.serverSet;
         Assert.assertTrue(set.reOrderOnFailover());
    }

    public static class MockLdapiServerConfig extends ZimbraLdapConfig {

        public MockLdapiServerConfig(LdapServerType serverType) {
            super(serverType);
        }

        @Override
        public String getLdapURL() {
            return "ldapi:///";
        }

        @Override
        public LdapConnType getConnType() {
            return LdapConnType.LDAPI;
        }
    }

    public static class MockLdapServerConfig extends ZimbraLdapConfig {

        public MockLdapServerConfig(LdapServerType serverType) {
            super(serverType);
        }

        @Override
        public String getLdapURL() {
            return "ldap://localhost ldap://127.0.0.1";
        }

        @Override
        public LdapConnType getConnType() {
            return LdapConnType.LDAPS;
        }
    }
}

