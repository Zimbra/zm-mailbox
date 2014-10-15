/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for {@link MemcachedSharedDeliveryCoordinator}.
 */
public final class MemcachedSharedDeliveryCoordinatorTest extends AbstractSharedDeliveryCoordinatorTest {

    @BeforeClass
    public static void init() throws Exception {
        LC.zimbra_class_shareddeliverycoordinator.setDefault(MemcachedSharedDeliveryCoordinator.class.getName());
        MailboxTestUtil.initServer(MockStoreManager.class, "", MemcachedOnLocalhostZimbraConfig.class);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Override
    protected void flushCacheBetweenTests() throws Exception {
        Zimbra.getAppContext().getBean(ZimbraMemcachedClient.class).flush();
    }

    protected boolean isExternalCacheAvailableForTest() throws Exception {
        return Zimbra.getAppContext().getBean(ZimbraMemcachedClient.class).isConnected();
    }

    @Test
    public void testFactoryIsLocalConfigAware() throws Exception {
        SharedDeliveryCoordinator sdc = Zimbra.getAppContext().getBean(SharedDeliveryCoordinator.class);
        Assert.assertNotNull(sdc);
        Assert.assertEquals(MemcachedSharedDeliveryCoordinator.class, sdc.getClass());
    }
}
