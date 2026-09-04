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

import java.net.InetAddress;

import com.google.common.net.InetAddresses;

/**
 * Matches an IP address against a single IPv4 or IPv6 CIDR range.
 *
 * A range is written in CIDR notation ({@code 10.0.0.0/8}, {@code 2001:db8::/32}). A bare
 * address with no prefix length is accepted and treated as a single host. Host bits below
 * the prefix length are ignored, so {@code 10.1.2.3/8} is equivalent to {@code 10.0.0.0/8}.
 *
 * Addresses are normalized before comparison: surrounding brackets and any scope id are
 * stripped, and IPv4-mapped IPv6 addresses ({@code ::ffff:10.1.2.3}) are reduced to their
 * IPv4 form so that they match IPv4 ranges. An IPv4 address never matches an IPv6 range,
 * and vice versa.
 *
 * Instances are immutable and safe to share between threads.
 */
public final class CidrMatcher {

    private final byte[] network;
    private final int prefixLength;
    private final String source;

    private CidrMatcher(byte[] network, int prefixLength, String source) {
        this.network = network;
        this.prefixLength = prefixLength;
        this.source = source;
    }

    /**
     * @param cidr a range in CIDR notation, or a bare address meaning a single host
     * @throws IllegalArgumentException if the range cannot be parsed
     */
    public static CidrMatcher parse(String cidr) {
        if (cidr == null) {
            throw new IllegalArgumentException("CIDR range is null");
        }
        String spec = cidr.trim();
        if (spec.isEmpty()) {
            throw new IllegalArgumentException("CIDR range is empty");
        }
        int slash = spec.lastIndexOf('/');
        byte[] network = toBytes(slash < 0 ? spec : spec.substring(0, slash));
        int maxBits = network.length * 8;

        int prefixLength;
        if (slash < 0) {
            prefixLength = maxBits;
        } else {
            String bits = spec.substring(slash + 1).trim();
            try {
                prefixLength = Integer.parseInt(bits);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("invalid prefix length in CIDR range: " + spec);
            }
            if (prefixLength < 0 || prefixLength > maxBits) {
                throw new IllegalArgumentException("prefix length out of range in CIDR range: " + spec);
            }
        }

        maskHostBits(network, prefixLength);
        return new CidrMatcher(network, prefixLength, spec);
    }

    /**
     * @return true if the range can be parsed, without throwing
     */
    public static boolean isValid(String cidr) {
        try {
            parse(cidr);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * @return true if the address falls inside this range; false if it does not, or if it
     *         is null, blank or unparseable
     */
    public boolean matches(String ip) {
        byte[] addr;
        try {
            addr = normalize(toBytes(ip));
        } catch (IllegalArgumentException e) {
            return false;
        }
        return matches(addr);
    }

    /**
     * @return true if the address falls inside this range
     */
    public boolean matches(InetAddress addr) {
        return addr != null && matches(normalize(addr.getAddress()));
    }

    private boolean matches(byte[] addr) {
        // An IPv4 address is never inside an IPv6 range, and vice versa.
        if (addr.length != network.length) {
            return false;
        }
        int wholeBytes = prefixLength / 8;
        for (int i = 0; i < wholeBytes; i++) {
            if (addr[i] != network[i]) {
                return false;
            }
        }
        int remainingBits = prefixLength % 8;
        if (remainingBits != 0) {
            int mask = (0xFF << (8 - remainingBits)) & 0xFF;
            if ((addr[wholeBytes] & mask) != (network[wholeBytes] & mask)) {
                return false;
            }
        }
        return true;
    }

    private static byte[] toBytes(String ip) {
        String addr = ip == null ? "" : ip.trim();
        // The servlet layer can hand back a bracketed IPv6 literal, e.g. [0:0:0:0:0:0:0:1].
        if (addr.length() > 2 && addr.charAt(0) == '[' && addr.charAt(addr.length() - 1) == ']') {
            addr = addr.substring(1, addr.length() - 1);
        }
        int scope = addr.indexOf('%');
        if (scope > 0) {
            addr = addr.substring(0, scope);
        }
        if (addr.isEmpty()) {
            throw new IllegalArgumentException("IP address is empty");
        }
        // Throws IllegalArgumentException on anything that is not a literal address, and
        // never performs a DNS lookup.
        return InetAddresses.forString(addr).getAddress();
    }

    /**
     * Reduces an IPv4-mapped IPv6 address to its four IPv4 bytes so that, for example,
     * ::ffff:10.1.2.3 matches 10.0.0.0/8.
     *
     * The JDK already collapses the textual and byte-array forms to an Inet4Address, so
     * this only bites for an Inet6Address built through the IPv6-specific factory, which
     * retains all sixteen bytes. Deliberately does not touch IPv4-compatible addresses
     * (::a.b.c.d): that form is deprecated, and collapsing it would wrongly rewrite
     * ordinary low-numbered IPv6 addresses such as ::2.
     */
    private static byte[] normalize(byte[] addr) {
        return isIpv4Mapped(addr) ? lowFourBytes(addr) : addr;
    }

    private static boolean isIpv4Mapped(byte[] addr) {
        if (addr.length != 16) {
            return false;
        }
        for (int i = 0; i < 10; i++) {
            if (addr[i] != 0) {
                return false;
            }
        }
        return addr[10] == (byte) 0xFF && addr[11] == (byte) 0xFF;
    }

    private static byte[] lowFourBytes(byte[] addr) {
        return new byte[] { addr[12], addr[13], addr[14], addr[15] };
    }

    private static void maskHostBits(byte[] addr, int prefixLength) {
        int firstZeroByte = prefixLength / 8;
        int remainingBits = prefixLength % 8;
        if (remainingBits != 0) {
            addr[firstZeroByte] &= (byte) ((0xFF << (8 - remainingBits)) & 0xFF);
            firstZeroByte++;
        }
        for (int i = firstZeroByte; i < addr.length; i++) {
            addr[i] = 0;
        }
    }

    public int getPrefixLength() {
        return prefixLength;
    }

    @Override
    public String toString() {
        return source;
    }
}
