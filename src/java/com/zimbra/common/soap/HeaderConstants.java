/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.soap;

import org.dom4j.QName;

public final class HeaderConstants {
    public static final QName CONTEXT = QName.get("context", ZimbraNamespace.ZIMBRA);
    public static final String E_A          = "a";
    public static final String E_FORMAT     = "format";
    public static final String A_TYPE       = "type";
    public static final String E_AUTH_TOKEN = "authToken";
    public static final String E_ACCOUNT    = "account";
    public static final String A_BY         = "by";
    public static final String A_HOPCOUNT   = "hops";
    public static final String A_MOUNTPOINT = "link";
    public static final String E_NO_QUALIFY = "noqualify";
    public static final String E_NO_NOTIFY  = "nonotify";
    public static final String E_NO_SESSION = "nosession";
    public static final String E_SESSION    = "session";
    public static final String A_ACCOUNT_ID = "acct";
    public static final String A_ID         = "id";
    public static final String A_PROXIED    = "proxy";
    public static final String E_NOTIFY     = "notify";
    public static final String A_NOTIFY     = "notify";
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

    private HeaderConstants() {
    }
}
