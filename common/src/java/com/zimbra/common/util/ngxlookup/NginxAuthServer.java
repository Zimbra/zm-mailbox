/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.common.util.ngxlookup;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.zimbra.common.util.ZimbraLog;

public class NginxAuthServer {
    private String nginxAuthServer;
    private String nginxAuthUser;

    public NginxAuthServer(String authServer, String authPort, String authUser) {
        String format = "%s:%s";
        InetAddress inetAddress;
        try {
            inetAddress = InetAddress.getByName(authServer);
            if (inetAddress instanceof Inet6Address) {
                format = "[%s]:%s";
            }
        } catch (UnknownHostException e) {
            ZimbraLog.misc.error("Failed to recognize address %s", authServer, e);
        }
        this.nginxAuthServer = String.format(format, authServer, authPort);
        this.nginxAuthUser = authUser;
    }

    public String getNginxAuthServer() { return nginxAuthServer; }
    public String getNginxAuthUser() { return nginxAuthUser; }
}
