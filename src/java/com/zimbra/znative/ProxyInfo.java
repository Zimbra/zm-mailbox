/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 VMware, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
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
