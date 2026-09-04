/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import org.junit.Assert;
import org.junit.Test;

public class CidrMatcherTest {

    @Test
    public void ipv4RangeMatchesInsideAndRejectsOutside() {
        CidrMatcher m = CidrMatcher.parse("10.0.0.0/8");
        Assert.assertTrue(m.matches("10.0.0.0"));
        Assert.assertTrue(m.matches("10.1.2.3"));
        Assert.assertTrue(m.matches("10.255.255.255"));
        Assert.assertFalse(m.matches("11.0.0.0"));
        Assert.assertFalse(m.matches("9.255.255.255"));
        Assert.assertFalse(m.matches("192.168.1.1"));
    }

    @Test
    public void ipv4RangeHonoursNonByteAlignedPrefix() {
        CidrMatcher m = CidrMatcher.parse("192.168.1.0/28");
        Assert.assertTrue(m.matches("192.168.1.0"));
        Assert.assertTrue(m.matches("192.168.1.15"));
        Assert.assertFalse(m.matches("192.168.1.16"));

        CidrMatcher odd = CidrMatcher.parse("172.16.0.0/12");
        Assert.assertTrue(odd.matches("172.16.0.1"));
        Assert.assertTrue(odd.matches("172.31.255.254"));
        Assert.assertFalse(odd.matches("172.32.0.1"));
        Assert.assertFalse(odd.matches("172.15.255.254"));
    }

    @Test
    public void hostBitsBelowPrefixAreIgnored() {
        // 10.1.2.3/8 is the same range as 10.0.0.0/8
        CidrMatcher m = CidrMatcher.parse("10.1.2.3/8");
        Assert.assertTrue(m.matches("10.9.9.9"));
        Assert.assertFalse(m.matches("11.1.2.3"));
    }

    @Test
    public void slashZeroMatchesEverythingInFamily() {
        CidrMatcher v4 = CidrMatcher.parse("0.0.0.0/0");
        Assert.assertTrue(v4.matches("1.2.3.4"));
        Assert.assertTrue(v4.matches("255.255.255.255"));
        // Still family-scoped: an IPv6 address is not inside an IPv4 range.
        Assert.assertFalse(v4.matches("2001:db8::1"));
    }

    @Test
    public void bareAddressIsTreatedAsSingleHost() {
        CidrMatcher m = CidrMatcher.parse("192.168.1.50");
        Assert.assertEquals(32, m.getPrefixLength());
        Assert.assertTrue(m.matches("192.168.1.50"));
        Assert.assertFalse(m.matches("192.168.1.51"));

        CidrMatcher v6 = CidrMatcher.parse("2001:db8::1");
        Assert.assertEquals(128, v6.getPrefixLength());
        Assert.assertTrue(v6.matches("2001:db8::1"));
        Assert.assertFalse(v6.matches("2001:db8::2"));
    }

    @Test
    public void ipv6RangeMatchesInsideAndRejectsOutside() {
        CidrMatcher m = CidrMatcher.parse("2001:db8::/32");
        Assert.assertTrue(m.matches("2001:db8::1"));
        Assert.assertTrue(m.matches("2001:db8:ffff:ffff::abcd"));
        Assert.assertFalse(m.matches("2001:db9::1"));
        Assert.assertFalse(m.matches("fe80::1"));
    }

    @Test
    public void familiesDoNotCrossMatch() {
        Assert.assertFalse(CidrMatcher.parse("10.0.0.0/8").matches("2001:db8::1"));
        Assert.assertFalse(CidrMatcher.parse("2001:db8::/32").matches("10.1.2.3"));
        // ::/0 must not swallow IPv4 addresses.
        Assert.assertFalse(CidrMatcher.parse("::/0").matches("10.1.2.3"));
    }

    @Test
    public void ipv4MappedIpv6AddressMatchesIpv4Range() {
        CidrMatcher m = CidrMatcher.parse("10.0.0.0/8");
        Assert.assertTrue(m.matches("::ffff:10.1.2.3"));
        Assert.assertTrue(m.matches("::ffff:0a01:0203"));
        Assert.assertFalse(m.matches("::ffff:11.1.2.3"));
    }

    @Test
    public void ipv4MappedRangeCollapsesToNativeIpv4() {
        // The JDK parses ::ffff:a.b.c.d as an Inet4Address, so the range is four bytes wide
        // and the prefix must be expressed in IPv4 terms.
        CidrMatcher m = CidrMatcher.parse("::ffff:10.0.0.0/8");
        Assert.assertEquals(8, m.getPrefixLength());
        Assert.assertTrue(m.matches("10.1.2.3"));
        Assert.assertFalse(m.matches("11.1.2.3"));
        // An IPv6-scale prefix on a mapped address is therefore out of range.
        Assert.assertFalse(CidrMatcher.isValid("::ffff:10.0.0.0/104"));
    }

    @Test
    public void ipv6InstanceHoldingMappedAddressMatchesIpv4Range() throws Exception {
        // Inet6Address.getByAddress keeps all sixteen bytes, unlike InetAddress.getByAddress.
        byte[] mapped = new byte[16];
        mapped[10] = (byte) 0xFF;
        mapped[11] = (byte) 0xFF;
        mapped[12] = 10;
        mapped[13] = 1;
        mapped[14] = 2;
        mapped[15] = 3;
        java.net.InetAddress v6 = java.net.Inet6Address.getByAddress(null, mapped, 0);
        Assert.assertEquals(16, v6.getAddress().length);
        Assert.assertTrue(CidrMatcher.parse("10.0.0.0/8").matches(v6));
        Assert.assertFalse(CidrMatcher.parse("11.0.0.0/8").matches(v6));
    }

    @Test
    public void ipv4CompatibleAddressIsNotCollapsed() {
        // ::2 is an ordinary IPv6 address and must not be read as 0.0.0.2.
        Assert.assertFalse(CidrMatcher.parse("0.0.0.0/8").matches("::2"));
        Assert.assertTrue(CidrMatcher.parse("::/64").matches("::2"));
    }

    @Test
    public void bracketedAndScopedAddressesAreAccepted() {
        // RemoteIP reports IPv6 localhost in bracketed form.
        Assert.assertTrue(CidrMatcher.parse("::1").matches("[0:0:0:0:0:0:0:1]"));
        Assert.assertTrue(CidrMatcher.parse("[::1]").matches("::1"));
        Assert.assertTrue(CidrMatcher.parse("fe80::/10").matches("fe80::1%eth0"));
    }

    @Test
    public void loopbackIsOnlyMatchedWhenConfigured() {
        Assert.assertTrue(CidrMatcher.parse("127.0.0.0/8").matches("127.0.0.1"));
        Assert.assertFalse(CidrMatcher.parse("10.0.0.0/8").matches("127.0.0.1"));
    }

    @Test
    public void unparseableAddressesDoNotMatch() {
        CidrMatcher m = CidrMatcher.parse("10.0.0.0/8");
        Assert.assertFalse(m.matches((String) null));
        Assert.assertFalse(m.matches((java.net.InetAddress) null));
        Assert.assertFalse(m.matches(""));
        Assert.assertFalse(m.matches("   "));
        Assert.assertFalse(m.matches("not-an-ip"));
        Assert.assertFalse(m.matches("10.0.0.256"));
        Assert.assertFalse(m.matches("10.0.0"));
        // Must never resolve a hostname.
        Assert.assertFalse(m.matches("localhost"));
    }

    @Test
    public void surroundingWhitespaceIsTolerated() {
        CidrMatcher m = CidrMatcher.parse("  10.0.0.0/8  ");
        Assert.assertTrue(m.matches(" 10.1.2.3 "));
    }

    @Test
    public void invalidRangesAreRejected() {
        String[] invalid = {
                null, "", "   ", "not-an-ip", "10.0.0.0/",  "10.0.0.0/x",
                "10.0.0.0/33", "10.0.0.0/-1", "2001:db8::/129", "10.0.0.256/8",
                "10.0.0.0/8/8", "/8", "::ffff:10.0.0.0/104",
        };
        for (String spec : invalid) {
            Assert.assertFalse("should be rejected: " + spec, CidrMatcher.isValid(spec));
        }
    }

    @Test
    public void validRangesAreAccepted() {
        String[] valid = {
                "10.0.0.0/8", "192.168.1.0/24", "0.0.0.0/0", "255.255.255.255/32",
                "192.168.1.50", "2001:db8::/32", "::1", "::/0", "fe80::/10",
                "::ffff:10.0.0.0/8",
        };
        for (String spec : valid) {
            Assert.assertTrue("should be accepted: " + spec, CidrMatcher.isValid(spec));
        }
    }

    @Test
    public void parseThrowsOnPrefixOutOfRange() {
        try {
            CidrMatcher.parse("10.0.0.0/33");
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue("unexpected message: " + e.getMessage(),
                    e.getMessage().contains("prefix length out of range"));
        }
    }

    @Test
    public void parseThrowsOnGarbage() {
        try {
            CidrMatcher.parse("nonsense/8");
            Assert.fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Guava rejects the address half before the prefix is considered.
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void parseThrowsOnNullAndEmpty() {
        try {
            CidrMatcher.parse(null);
            Assert.fail("expected IllegalArgumentException for null");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("null"));
        }
        try {
            CidrMatcher.parse("   ");
            Assert.fail("expected IllegalArgumentException for blank");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("empty"));
        }
    }

    @Test
    public void toStringReturnsTheConfiguredSpec() {
        Assert.assertEquals("10.0.0.0/8", CidrMatcher.parse(" 10.0.0.0/8 ").toString());
    }
}
