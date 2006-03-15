/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;

public class HttpUtil {

    public enum Browser { IE, FIREFOX, MOZILLA, OPERA, SAFARI, UNKNOWN };

    public static Browser guessBrowser(HttpServletRequest req) {
        String ua = req.getHeader("User-Agent");
        if (ua == null || ua.trim().equals(""))
            return Browser.UNKNOWN;
        else if (ua.indexOf("MSIE") != -1)
            return Browser.IE;
        else if (ua.indexOf("Firefox") != -1)
            return Browser.FIREFOX;
        else if (ua.indexOf("AppleWebKit") != -1)
            return Browser.SAFARI;
        else if (ua.indexOf("Opera") != -1)
            return Browser.OPERA;
        else
            return Browser.UNKNOWN;
    }

    public static String encodeFilename(HttpServletRequest req, String filename) {
        if (StringUtil.isAsciiString(filename))
            return filename;
        return encodeFilename(guessBrowser(req), filename);
    }

    public static String encodeFilename(Browser browser, String filename) {
        if (StringUtil.isAsciiString(filename))
            return filename;
        try {
            if (browser == Browser.IE)
                return URLEncoder.encode(filename, "utf-8");
            else if (browser == Browser.FIREFOX)
                return '"' + MimeUtility.encodeText(filename, "utf-8", "B") + '"';
            else
                return '"' + MimeUtility.encodeText(filename, "utf-8", "B") + '"';
        } catch (UnsupportedEncodingException uee) {
            return filename;
        }
    }
}
