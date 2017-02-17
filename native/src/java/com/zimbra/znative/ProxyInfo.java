/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.znative;

import java.util.List;

public class ProxyInfo {
    private final Type type;
    private final String host;
    private final int port;
    private final String user;
    private final String pass;

    public enum Type {
        NONE, AUTO_CONFIG_URL, FTP, HTTP, HTTPS, SOCKS, UNKNOWN
    }

    static {
        Util.loadLibrary();
    }

    public static native boolean isSupported();
    public static native ProxyInfo[] getProxyInfo(String uri);

    ProxyInfo(int type, String host, int port, String user, String pass) {
        this.type = Type.values()[type];
        this.host = host;
        this.port = port;
        this.user = user;
        this.pass = pass;
    }

    public Type getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }

    public String toString() {
        return String.format("{type=%s,host=%s,port=%d,user=%s,pass=%s}",
                             type, host, port, user, pass);
    }
}
