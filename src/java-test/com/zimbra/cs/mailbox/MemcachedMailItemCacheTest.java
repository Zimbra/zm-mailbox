/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import org.junit.BeforeClass;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.memcached.MemcachedConnector;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for {@link MemcachedMailItemCache}.
 */
public final class MemcachedMailItemCacheTest extends AbstractMailItemCacheTest {

    @Override
    protected MailItemCache constructCache() throws ServiceException {
        MailItemCache cache = new MemcachedMailItemCache();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(cache);
        return cache;
    }

    @BeforeClass
    public static void init() throws Exception {
        Provisioning.getInstance().getLocalServer().addMemcachedClientServerList("localhost");
        Provisioning.getInstance().getLocalServer().setMemcachedClientTimeoutMillis(20);
        Provisioning.getInstance().getLocalServer().setMemcachedClientExpirySeconds(10);
        MemcachedConnector.startup();
        AbstractMailItemCacheTest.init();
    }

    @Override
    protected boolean isExternalCacheAvailableForTest() throws Exception {
        return MemcachedConnector.isConnected();
    }

    @Override
    protected void flushCacheBetweenTests() throws Exception {
        MemcachedConnector.getClient().flush();
    }
}
