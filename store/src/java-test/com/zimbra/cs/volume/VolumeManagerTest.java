/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite, Network Edition.
 * Copyright (C) 2026 Zimbra, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.volume;

import com.zimbra.common.service.ServiceException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(PowerMockRunner.class)
@PrepareForTest({VolumeManager.class})
public class VolumeManagerTest {

    private VolumeManager volumeManager;

    @Before
    public void setup() throws Exception {
        volumeManager = PowerMockito.spy(VolumeManager.getInstance());
    }

    @Test
    public void testPureNumericId() throws Exception {
        PowerMockito.doReturn(null)
                .when(volumeManager)
                .getVolume((short) 1);

        volumeManager.getVolume("1");

        Mockito.verify(volumeManager).getVolume((short) 1);
    }

    @Test
    public void testLocatorWithSeparator() throws Exception {
        PowerMockito.doReturn(null)
                .when(volumeManager)
                .getVolume((short) 3);

        volumeManager.getVolume("3@@abc-def");

        Mockito.verify(volumeManager).getVolume((short) 3);
    }

    @Test(expected = ServiceException.class)
    public void testInvalidNumericPrefix() throws Exception {
        volumeManager.getVolume("abc@@123");
    }

    @Test(expected = ServiceException.class)
    public void testEmptyString() throws Exception {
        volumeManager.getVolume("");
    }

    @Test(expected = ServiceException.class)
    public void testNullId() throws Exception {
        volumeManager.getVolume(null);
    }

    @Test(expected = ServiceException.class)
    public void testNonNumericNoSeparator() throws Exception {
        volumeManager.getVolume("foo");
    }

    @Test
    public void testNumberFormatExceptionOutOfRange() {
        String largeNumber = String.valueOf((long) Short.MAX_VALUE + 1);

        try {
            volumeManager.getVolume(largeNumber);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("out of range"));
        }
    }

    @Test
    public void testSeparatorWithOutOfRangeNumber() {
        String largeNumber = String.valueOf((long) Short.MAX_VALUE + 1) + "@@abc";

        try {
            volumeManager.getVolume(largeNumber);
            fail("Expected ServiceException");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("out of range"));
        }
    }
}
