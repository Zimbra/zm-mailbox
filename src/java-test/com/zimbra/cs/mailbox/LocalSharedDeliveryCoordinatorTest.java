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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.util.Zimbra;

/**
 * Unit test for {@link LocalSharedDeliveryCoordinator}.
 */
public final class LocalSharedDeliveryCoordinatorTest extends AbstractSharedDeliveryCoordinatorTest {

    @BeforeClass
    public static void init() throws Exception {
        LC.zimbra_class_shareddeliverycoordinator.setDefault(LocalSharedDeliveryCoordinator.class.getName());
        AbstractSharedDeliveryCoordinatorTest.init();
    }

    protected boolean isExternalCacheAvailableForTest() {
        return true;
    }

    @Override
    protected void flushCacheBetweenTests() throws Exception {
        SharedDeliveryCoordinator sdc = Zimbra.getAppContext().getBean(SharedDeliveryCoordinator.class);
        ((LocalSharedDeliveryCoordinator)sdc).flush();
    }

    @Test
    public void testFactoryIsLocalConfigAware() throws Exception {
        SharedDeliveryCoordinator sdc = Zimbra.getAppContext().getBean(SharedDeliveryCoordinator.class);
        Assert.assertNotNull(sdc);
        Assert.assertEquals(LocalSharedDeliveryCoordinator.class, sdc.getClass());
    }
}
