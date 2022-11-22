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
     * Port as seen from ZimbraSoapContext in: 
     * zm-mailbox/store/src/java/com/zimbra/cs/service/admin/Auth.java
     * Present in context for SOAP (web-UI and SMTP authentication)
     *
     * To be used with ZimbraCustomAuth extensions to distinguish 
     * between SMTP and web-UI authentication. 
     *
     * Example this is true for an SMTP auth request:
     * if (context.get("soapport").equals(account.getServer().getMtaAuthPort()))
     *
     * type: Integer
     */
    public static final String AC_SOAP_PORT = "soapport";

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

        //for internal use only
        test;
    };
}
