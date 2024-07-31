/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016, 2024 Synacor, Inc.
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
package com.zimbra.common.util;

import java.net.InetAddress;

import org.junit.Assert;
import org.junit.Test;


public class NetUtilTest {
    @Test
    public void testIsInRangePrivateAddressesIPv4() throws Exception {
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("9.0.0.0"), "10.0.0.0/8"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("9.255.255.255"), "10.0.0.0/8"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("10.0.0.0"), "10.0.0.0/8"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("10.50.50.55"), "10.0.0.0/8"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("10.50.0.255"), "10.0.0.0/8"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("10.50.255.0"), "10.0.0.0/8"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("10.255.255.255"), "10.0.0.0/8"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("11.0.0.0"), "10.0.0.0/8"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("11.255.255.255"), "10.0.0.0/8"));

        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("172.15.255.255"), "172.16.0.0/12"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("172.15.0.0"), "172.16.0.0/12"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("172.16.0.0"), "172.16.0.0/12"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("172.16.50.50"), "172.16.0.0/12"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("172.16.0.255"), "172.16.0.0/12"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("172.16.255.0"), "172.16.0.0/12"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("172.31.255.255"), "172.16.0.0/12"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("172.32.0.0"), "172.16.0.0/12"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("172.32.255.255"), "172.16.0.0/12"));

        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("192.167.0.0"), "192.168.0.0/16"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("192.167.255.255"), "192.168.0.0/16"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("192.168.0.0"), "192.168.0.0/16"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("192.168.1.131"), "192.168.0.0/16"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("192.168.0.255"), "192.168.0.0/16"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("192.168.255.0"), "192.168.0.0/16"));
        Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("192.168.255.255"), "192.168.0.0/16"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("192.169.255.255"), "192.168.0.0/16"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("192.169.0.0"), "192.168.0.0/16"));
    }

    @Test
    public void testIsInRangeTestAddressesIPv4() throws Exception {
        for (int i = 0; i < 256; i++) {
            Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("198.51.100." + i), "198.51.100.0/24"));
        }
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("198.50.100.0"), "198.51.100.0/24"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("198.50.100.255"), "198.51.100.0/24"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("198.52.100.0"), "198.51.100.0/24"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("198.52.100.255"), "198.51.100.0/24"));

        for (int i = 0; i < 256; i++) {
            Assert.assertTrue(NetUtil.isAddressInRange(InetAddress.getByName("203.0.113." + i), "203.0.113.0/24"));
        }
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("203.0.112.0"), "203.0.113.0/24"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("203.0.112.255"), "203.0.113.0/24"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("203.0.114.0"), "203.0.113.0/24"));
        Assert.assertFalse(NetUtil.isAddressInRange(InetAddress.getByName("203.0.114.255"), "203.0.113.0/24"));
    }

    @Test
    public void testIsInRangePrivateAddressesIPv6() throws Exception {
        Assert.assertFalse(NetUtil.isAddressInRange(
            InetAddress.getByName("fedd:0d17:76f7:3e82:0000:0000:0000:0000"), "fddd:0d17:76f7:3e82::/64"));
        Assert.assertFalse(NetUtil.isAddressInRange(
            InetAddress.getByName("fedd:0d17:76f7:3e82:ffff:ffff:ffff:ffff"), "fddd:0d17:76f7:3e82::/64"));
        Assert.assertTrue(NetUtil.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:0000:0000:0000:0000"), "fddd:0d17:76f7:3e82::/64"));
        Assert.assertTrue(NetUtil.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:1111:1234:5678:abcd"), "fddd:0d17:76f7:3e82::/64"));
        Assert.assertTrue(NetUtil.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:ffff:ffff:ffff:ffff"), "fddd:0d17:76f7:3e82::/64"));
        Assert.assertFalse(NetUtil.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e83:0000:0000:0000:0000"), "fddd:0d17:76f7:3e82::/64"));
        Assert.assertFalse(NetUtil.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e83:ffff:ffff:ffff:ffff"), "fddd:0d17:76f7:3e82::/64"));

        Assert.assertFalse(NetUtil.isAddressInRange(
            InetAddress.getByName("fcdd:0d17:76f7:3e82:0000:0000:0000:0000"), "fd00::/8"));
        Assert.assertFalse(NetUtil.isAddressInRange(
            InetAddress.getByName("fcdd:0d17:76f7:3e82:ffff:ffff:ffff:ffff"), "fd00::/8"));
        Assert.assertTrue(NetUtil.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:0000:0000:0000:0000"), "fd00::/8"));
        Assert.assertTrue(NetUtil.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:1111:755f:ffff:0d17"), "fd00::/8"));
        Assert.assertTrue(NetUtil.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:ffff:ffff:ffff:ffff"), "fd00::/8"));
        Assert.assertFalse(NetUtil.isAddressInRange(
            InetAddress.getByName("fedd:0d17:76f7:3e82:0000:0000:0000:0000"), "fd00::/8"));
        Assert.assertFalse(NetUtil.isAddressInRange(
            InetAddress.getByName("fedd:0d17:76f7:3e82:ffff:ffff:ffff:ffff"), "fd00::/8"));
    }

    @Test
    public void testIsInRangeSingleAddressIPv4() throws Exception {
        Assert.assertTrue(NetUtil.isAddressInRange(
            InetAddress.getByName("192.168.1.0"), "192.168.1.0"));
        for (int i = 1; i < 256; i++) {
            Assert.assertTrue(NetUtil.isAddressInRange(
                InetAddress.getByName("192.168.1." + i), "192.168.1." + i));
            Assert.assertFalse(NetUtil.isAddressInRange(
                InetAddress.getByName("192.168.1." + i), "192.168.1.0"));
        }
    }

    @Test
    public void testIsInRangeSingleAddressIPv6() throws Exception {
        Assert.assertTrue(NetUtil.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:0000:0000:ffff:ffff"),
            "fddd:0d17:76f7:3e82:0000:0000:ffff:ffff"));
        Assert.assertTrue(NetUtil.isAddressInRange(
            InetAddress.getByName("fddd:0d17:76f7:3e82:1234:5678:abcd:efff"),
            "fddd:0d17:76f7:3e82:1234:5678:abcd:efff"));
        Assert.assertFalse(NetUtil.isAddressInRange(
            InetAddress.getByName("fddd:0d17:0000:3e82:0000:0000:ffff:ffff"),
            "fddd:0d17:76f7:3e82:0000:0000:ffff:0000"));
    }
}
