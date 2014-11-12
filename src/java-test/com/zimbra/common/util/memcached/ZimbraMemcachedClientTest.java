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
package com.zimbra.common.util.memcached;

import java.io.IOException;

import junit.framework.Assert;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link ZimbraMemcachedClient}.
 */
public class ZimbraMemcachedClientTest {
    ZimbraMemcachedClient zimbraMemcachedClient = new ZimbraMemcachedClient();

    public ZimbraMemcachedClientTest() throws Exception {
        String[] serverList = {"localhost"};
        boolean useBinaryProtocol = false;
        String hashAlgorithm = null;
        int expirySeconds = 10;
        long timeoutMillis = 500;
        zimbraMemcachedClient.connect(serverList, useBinaryProtocol, hashAlgorithm, expirySeconds, timeoutMillis);
        Thread.sleep(timeoutMillis);
    }

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(zimbraMemcachedClient.isConnected());
        zimbraMemcachedClient.flush();
    }


    @Test
    public void testIncrNonExistent() throws IOException {
        String key = RandomStringUtils.randomAscii(10);
        Long value = zimbraMemcachedClient.incr(key);
        Assert.assertEquals(-1L, value.longValue());
    }

    @Test
    public void testIncr() throws IOException {
        String key = RandomStringUtils.randomAscii(10);
        boolean waitForAck = true;
        Assert.assertTrue(zimbraMemcachedClient.put(key, "11", waitForAck));
        Long value_ = zimbraMemcachedClient.incr(key);
        Assert.assertNotNull(value_);
        Assert.assertEquals(12L, value_.longValue());
    }
}
