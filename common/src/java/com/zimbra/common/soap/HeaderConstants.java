/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.soap;

import org.dom4j.QName;

public final class HeaderConstants {
    public static final QName CONTEXT = QName.get("context", ZimbraNamespace.ZIMBRA);
    public static final String E_A          = "a";
    public static final String E_FORMAT     = "format";
    public static final String A_TYPE       = "type";
    public static final String E_ACCOUNT    = "account";
    public static final String E_AUTH_TOKEN = "authToken";
    public static final String E_JWT_TOKEN = "jwtToken";
    public static final String E_JWT_SALT = "jwtSalt";
    public static final String E_AUTH_TOKEN_CONTROL = "authTokenControl";
    public static final String A_VOID_ON_EXPIRED = "voidOnExpired";
    public static final String A_BY         = "by";
    public static final String A_HOPCOUNT   = "hops";
    public static final String A_MOUNTPOINT = "link";
    public static final String E_NO_QUALIFY = "noqualify";
    public static final String E_NO_NOTIFY  = "nonotify";
    public static final String E_NO_SESSION = "nosession";
    public static final String E_SESSION    = "session";
    public static final String E_MODS = "mods";

    @Deprecated
    public static final String E_SESSION_ID    = "sessionId";
    public static final String A_ACCOUNT_ID = "acct";
    public static final String A_ID         = "id";
    public static final String A_PROXIED    = "proxy";
    public static final String E_NOTIFY     = "notify";
    public static final String A_NOTIFY     = "notify";
    public static final String E_REFRESH    = "refresh";
    public static final String A_SEQNO      = "seq";
    public static final String E_CHANGE     = "change";
    public static final String A_CHANGE_ID  = "token";
    public static final String E_TARGET_SERVER = "targetServer";
    public static final String E_USER_AGENT = "userAgent";
    public static final String E_VIA = "via";
    public static final String A_N          = "n";
    public static final String A_NAME       = "name";
    public static final String A_VERSION    = "version";
    public static final String E_CONTEXT    = "context";
    public static final String BY_NAME = "name";
    public static final String BY_ID   = "id";
    public static final String TYPE_XML        = "xml";
    public static final String TYPE_JAVASCRIPT = "js";
    public static final String CHANGE_MODIFIED = "mod";
    public static final String CHANGE_CREATED  = "new";
    public static final String SESSION_MAIL  = "mail";
    public static final String SESSION_ADMIN = "admin";
    public static final String E_CSRFTOKEN = "csrfToken";
    public static final String E_SOAP_ID = "soapId";
    public static final String A_WAITSET_ID = "wsId";

    // sieve constants
    public static final String INDEX = ":index";
    public static final String LAST = ":last";
    public static final String NEW_NAME = ":newname";
    public static final String NEW_VALUE = ":newvalue";
    public static final String I_ASCII_NUMERIC = "i;ascii-numeric";
    public static final String COUNT = ":count";
    public static final String VALUE = ":value";

    public static final String GT_OP = "gt";
    public static final String GE_OP = "ge";
    public static final String LT_OP = "lt";
    public static final String LE_OP = "le";
    public static final String EQ_OP = "eq";
    public static final String NE_OP = "ne";

    // http request headers
    public static final String HTTP_HEADER_ORIG_USER_AGENT = "Original-User-Agent";

    private HeaderConstants() {
    }
}
