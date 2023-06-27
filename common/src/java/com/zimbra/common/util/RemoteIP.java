/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.localconfig.LC;

public class RemoteIP {

    public static final String X_ORIGINATING_IP_HEADER = LC.zimbra_http_originating_ip_header.value();
    public static final String X_ORIGINATING_PORT_HEADER = "X-Forwarded-Port";
    public static final String X_ORIGINATING_PROTOCOL_HEADER = "X-Forwarded-Proto";

    /**
     * IP of the http client, Should be always present.
     */
    private String mClientIP;

    /**
     * port of the http client, Should be always present.
     */
    private int mClientPort;

    /**
     * IP of the originating client. It can be null.
     */
    private String mOrigIP;

    /**
     * Port of the originating client. Can be null.
     */
    private Integer mOrigPort;

    /**
     * It can be the IP of the http client, or in the presence of a real origin IP address http header(header specified
     * in the LC key zimbra_http_originating_ip_header) the IP of the real origin client if the http client is in a
     * trusted network.
     *
     * Should be always present.
     */
    private String mRequestIP;

    /**
     * Port number of the http client.
     */
    private Integer mRequestPort;

    /**
     * Original protocol of the request.
     */
    private String mOrigProto;

    public RemoteIP(HttpServletRequest req, TrustedIPs trustedIPs) {
        mClientIP = req.getRemoteAddr();
        ZimbraLog.mailbox.info("mClientIP  %s", mClientIP);
        mClientPort = req.getRemotePort();
        ZimbraLog.mailbox.info("trustedIPs %s", trustedIPs.toString());
        ZimbraLog.mailbox.info("***X_ORIGINATING_IP_HEADER %s", req.getHeader(X_ORIGINATING_IP_HEADER));
        if (trustedIPs.isIpTrusted(mClientIP)) {
            mOrigIP = req.getHeader(X_ORIGINATING_IP_HEADER);
            String origPort = req.getHeader(X_ORIGINATING_PORT_HEADER);
            if (origPort != null) {
                try {
                    mOrigPort = Integer.parseInt(origPort);
                } catch (NumberFormatException e) {
                    // ignore bad header
                }
            }

            mOrigProto = req.getHeader(X_ORIGINATING_PROTOCOL_HEADER);
        }

        if (mOrigPort != null) {
            mRequestPort = mOrigPort;
        } else {
            mRequestPort = mClientPort;
        }

        if (mOrigIP != null) {
            mRequestIP = mOrigIP;
        } else {
            mRequestIP = mClientIP;
        }
    }

    public String getClientIP() {
        return mClientIP;
    }

    public String getOrigIP() {
        return mOrigIP;
    }

    public String getRequestIP() {
        return mRequestIP;
    }

    public Integer getOrigPort() {
        return mOrigPort;
    }

    public Integer getRequestPort() {
        return mRequestPort;
    }

    public Integer getClientPort() {
        return mClientPort;
    }

    public String getOrigProto() {
        return mOrigProto;
    }

    public void addToLoggingContext() {
        if (mOrigIP != null) {
            ZimbraLog.addOrigIpToContext(mOrigIP);
        }

        if (mOrigPort != null) {
            ZimbraLog.addOrigPortToContext(mOrigPort);
        }

        if (mOrigProto != null) {
            ZimbraLog.addOrigProtoToContext(mOrigProto);
        }

        // don't log client's IP or client's port if original IP/port are present or if client's IP is localhost
        if (!TrustedIPs.isLocalhost(mClientIP)) {
            if (mOrigIP == null) {
                ZimbraLog.addIpToContext(mClientIP);
            }
            if (mOrigPort == null) {
                ZimbraLog.addPortToContext(mClientPort);
            }
        }
    }

    @Override
    public String toString() {
        return String
                .format("RemoteIP [mClientIP=%s, mOrigIP=%s, mRequestIP=%s] : RemotePort [mClientPort=%d, mOrigPort=%d, mRequestPort=%d] mOrigProto=%s",
                        mClientIP, mOrigIP, mRequestIP, mClientPort, mOrigPort, mRequestPort, mOrigProto);
    }

    public static class TrustedIPs {
        private static final String IP_LOCALHOST = "127.0.0.1";
        private static final String IPV6_LOCALHOST = "[0:0:0:0:0:0:0:1]";

        private Set<String> mTrustedIPs = new HashSet<String>();

        public TrustedIPs(String[] ips) {
            if (ips != null) {
                for (String ip : ips) {
                    if (!StringUtil.isNullOrEmpty(ip))
                        mTrustedIPs.add(ip);
                }
            }
        }

        public boolean isIpTrusted(String ip) {
            return isLocalhost(ip) || mTrustedIPs.contains(ip);
        }

        private static boolean isLocalhost(String ip) {
            return IP_LOCALHOST.equals(ip) || IPV6_LOCALHOST.equals(ip);
        }

        @Override
        public String toString() {
            return "TrustedIPs [mTrustedIPs=" + mTrustedIPs + "]";
        }

    }

}
