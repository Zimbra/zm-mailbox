/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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
package com.zimbra.cs.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.zimbra.common.account.ZAttrProvisioning.IPMode;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;


public class IPUtil {

    /**
     * get the IP address of the host name according to current IP mode.
     *
     * for ipv4 mode, the first ipv4 address will be used.
     * for ipv6 mode, the first ipv6 address will be used.
     * for both mode, try to return the first available ipv4. If no ipv4 available, use the first available ipv6.
     */
    public static InetAddress getIPByIPMode(Provisioning prov, String hostname) throws ServiceException {
        String localhost = LC.get("zimbra_server_hostname");
        IPMode mode = prov.getServerByName(localhost).getIPMode();
        InetAddress[] ips;
        try {
            ips = InetAddress.getAllByName(hostname);
        } catch (UnknownHostException e) {
            throw ServiceException.FAILURE("Failed enumerating local ip addresses", e);
        }
        if (mode == IPMode.ipv4) {
            for (InetAddress ip: ips) {
                if (ip instanceof Inet4Address) {
                    return ip;
                }
            }
            throw ServiceException.FAILURE("Can't find available IPv4 address for upstream " + hostname + " whose IP mode is IPv4 only", null);
        } else if (mode == IPMode.ipv6) {
            for (InetAddress ip: ips) {
                if (ip instanceof Inet6Address) {
                    return ip;
                }
            }
            throw ServiceException.FAILURE("Can't find available IPv6 address for upstream " + hostname + " whose IP mode is IPv6 only", null);
        } else {
            for (InetAddress ip: ips) {
                if (ip instanceof Inet4Address) {
                    return ip;
                }
            }
            return ips[0];
        }
    }
}

