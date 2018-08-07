/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.auth;

import java.util.Map;
import java.util.HashMap;

public class AuthContext {
    /*
     * Originating client IP address.
     * Present in context for SOAP, IMAP, POP3, and http basic authentication.
     *
     * type: String
     */
    public static final String AC_ORIGINATING_CLIENT_IP = "ocip";

    /*
     * Remote address as seen by ServletRequest.getRemoteAddr()
     * Present in context for SOAP, IMAP, POP3, and http basic authentication.
     *
     * type: String
     */
    public static final String AC_REMOTE_IP = "remoteip";

    /*
     * Account name passed in to the interface.
     * Present in context for SOAP and http basic authentication.
     *
     * type: String
     */
    public static final String AC_ACCOUNT_NAME_PASSEDIN = "anp";

    /*
     * User agent sending in the auth request.
     *
     * type: String
     */
    public static final String AC_USER_AGENT = "ua";

    /*
     * Whether the auth request came to the admin port and attempting
     * to acquire an admin auth token
     *
     * type: Boolean
     */
    public static final String AC_AS_ADMIN = "asAdmin";

    /*
     *
     */
    public static final String AC_AUTHED_BY_MECH = "authedByMech";

    /*
     * Protocol from which the auth request went in.
     *
     * type: AuthContext.Protocol
     */
    public static final String AC_PROTOCOL = "proto";

    /*
     * Unique device ID, used for identifying trusted mobile devices.
     */
    public static final String AC_DEVICE_ID = "did";

    private static final Map<Protocol, String> defaultUserAgentMap = new HashMap<Protocol, String>();
    static  
    {
        defaultUserAgentMap.put(AuthContext.Protocol.client_certificate, "client_certificate");
        defaultUserAgentMap.put(AuthContext.Protocol.http_basic, "http_basic");
        defaultUserAgentMap.put(AuthContext.Protocol.http_dav, "http_dav");
        defaultUserAgentMap.put(AuthContext.Protocol.im, "im");
        defaultUserAgentMap.put(AuthContext.Protocol.imap, "imap");
        defaultUserAgentMap.put(AuthContext.Protocol.pop3, "pop3");
        defaultUserAgentMap.put(AuthContext.Protocol.soap, "soap");
        defaultUserAgentMap.put(AuthContext.Protocol.spnego, "spnego");
        defaultUserAgentMap.put(AuthContext.Protocol.zsync, "zsync");
        // 'mta' value in case of 'smtp'
        defaultUserAgentMap.put(AuthContext.Protocol.smtp, "mta");    
    }

    private static final Map<String, Protocol> stringProtocolMap = new HashMap<String, Protocol>();
    static  
    {
        stringProtocolMap.put("client_certificate", AuthContext.Protocol.client_certificate);
        stringProtocolMap.put("http_basic", AuthContext.Protocol.http_basic);
        stringProtocolMap.put("http_dav", AuthContext.Protocol.http_dav);
        stringProtocolMap.put("im", AuthContext.Protocol.im);
        stringProtocolMap.put("imap", AuthContext.Protocol.imap);
        stringProtocolMap.put("pop3", AuthContext.Protocol.pop3);
        stringProtocolMap.put("soap", AuthContext.Protocol.soap);
        stringProtocolMap.put("spnego", AuthContext.Protocol.spnego);
        stringProtocolMap.put("zsync", AuthContext.Protocol.zsync);
        stringProtocolMap.put("smtp", AuthContext.Protocol.smtp);
  
    }

    public static String getDefaultUserAgent(AuthContext.Protocol protocol){
        String userAgent = AuthContext.defaultUserAgentMap.get(protocol);
        return userAgent != null ? userAgent : "";
    };

    public static Protocol getProtocol(String protocol){
        AuthContext.Protocol proto = AuthContext.stringProtocolMap.get(protocol);
        return proto;
    };

    public enum Protocol {
        client_certificate,
        http_basic,
        http_dav,
        im,
        imap,
        pop3,
        soap,
        spnego,
        zsync,
        smtp,

        //for internal use only
        test;
    };
}
