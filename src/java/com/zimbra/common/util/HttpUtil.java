/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.common.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.httpclient.HttpURL;
import org.apache.commons.httpclient.HttpsURL;

import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;

public final class HttpUtil {

    public enum Browser { IE, FIREFOX, MOZILLA, OPERA, SAFARI, APPLE_ICAL, UNKNOWN };

    // Encode ' ' to '%20' instead of '+' because IE doesn't decode '+' to ' '.
    private static final BitSet IE_URL_SAFE = new BitSet(256);
    static {
        for (int i = 'a'; i <= 'z'; i++) {
            IE_URL_SAFE.set(i);
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            IE_URL_SAFE.set(i);
        }
        for (int i = '0'; i <= '9'; i++) {
            IE_URL_SAFE.set(i);
        }
        // special except for space (0x20).
        IE_URL_SAFE.set('-');
        IE_URL_SAFE.set('_');
        IE_URL_SAFE.set('.');
        IE_URL_SAFE.set('*');
    }

    public static Browser guessBrowser(HttpServletRequest req) {
        String ua = req.getHeader("User-Agent");
        return guessBrowser(ua);
    }

    /**
     *
     * @param ua User-Agent string
     * @return
     */
    public static Browser guessBrowser(String ua) {
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
        else if (ua.indexOf("iCal") != -1)
            return Browser.APPLE_ICAL;
        else
            return Browser.UNKNOWN;
    }

    public static String encodeFilename(HttpServletRequest req, String filename) {
        if (StringUtil.isAsciiString(filename) && filename.indexOf('"') == -1) {
            return '"' + StringUtil.sanitizeFilename(filename) + '"';
        }
        return encodeFilename(guessBrowser(req), filename);
    }

    public static String encodeFilename(Browser browser, String filename) {
        filename = StringUtil.sanitizeFilename(filename);
        if (StringUtil.isAsciiString(filename) && filename.indexOf('"') == -1) {
            return '"' + filename + '"';
        }
        try {
            switch (browser) {
                case IE:
                    return new String(URLCodec.encodeUrl(IE_URL_SAFE, filename.getBytes(Charsets.UTF_8)),
                            Charsets.ISO_8859_1);
                case SAFARI:
                    // Safari doesn't support any encoding. The only solution is
                    // to let Safari use the path-info in URL by returning no
                    // filename here.
                    return "";
                case FIREFOX:
                default:
                    return '"' + MimeUtility.encodeText(filename, "utf-8", "B") + '"';
            }
        } catch (UnsupportedEncodingException uee) {
            return filename;
        }
    }

    /** Strips any userinfo (username/password) data from the passed-in URL
     *  and returns the result. */
    public static String sanitizeURL(String url) {
        if (url != null && url.indexOf('@') != -1) {
            try {
                HttpURL httpurl = (url.indexOf("https:") == 0) ? new HttpsURL(url) : new HttpURL(url);
                if (httpurl.getPassword() != null) {
                    httpurl.setPassword("");
                    return httpurl.toString();
                }
            } catch (org.apache.commons.httpclient.URIException urie) { }
        }
        return url;
    }

    /** Returns the full URL (including query string) associated with the
     *  given <code>HttpServletRequest</code>. */
    public static String getFullRequestURL(HttpServletRequest req) {
        if (req == null)
            return null;

        String uri = encodePath(req.getRequestURI()), qs = req.getQueryString();
        if (qs != null)
            uri += '?' + qs;
        return uri;
    }

    public static Map<String, String> getURIParams(HttpServletRequest req) {
        return getURIParams(req.getQueryString());
    }

    public static Map<String, String> getURIParams(String queryString) {
        Map<String, String> params = new HashMap<String, String>();
        if (queryString == null || queryString.trim().equals(""))
            return params;

        for (String pair : queryString.split("&")) {
            String[] keyVal = pair.split("=");
            // URI query string is always encoded with application/x-www-form-urlencoded,
            // so use URLDecoder.decode() which converts '+' to ' ' 
            try {
                String value = keyVal.length > 1 ? URLDecoder.decode(keyVal[1], "UTF-8") : "";
                params.put(URLDecoder.decode(keyVal[0], "UTF-8"), value);
            } catch (UnsupportedEncodingException e) {                
            }            
        }
        return params;
    }

    /**
     * URL-encodes the given URL path.
     *
     * @return the encoded path, or the original path if it
     * is malformed
     */
    public static String encodePath(String path) {
        String encoded = path;
        try {
            URI uri = new URI(null, null, path, null);
            encoded = uri.toASCIIString();
        } catch (URISyntaxException e) {
            // ignore and just return the orig path
        }
        return encoded;
    }

    /**
     * bug 32207
     *
     * The apache reverse proxy is re-writing the Host header to be the MBS IP.  It sets
     * the original request hostname in the X-Forwarded-Host header.  To work around it,
     * we first check for X-Forwarded-Host and then fallback to Host.
     *
     * @param req
     * @return the original request hostname
     */
    public static String getVirtualHost(HttpServletRequest req) {
        String virtualHost = req.getHeader("X-Forwarded-Host");
        if (virtualHost != null)
            return virtualHost;
        else
            return req.getServerName();
    }

    private static final Map<Character,String> sUrlEscapeMap = new HashMap<Character,String>();

    static {
        sUrlEscapeMap.put(' ', "%20");
        sUrlEscapeMap.put('"', "%22");
        sUrlEscapeMap.put('#', "%23");
        sUrlEscapeMap.put('%', "%25");
        sUrlEscapeMap.put('&', "%26");
        sUrlEscapeMap.put('<', "%3C");
        sUrlEscapeMap.put('>', "%3E");
        sUrlEscapeMap.put('?', "%3F");
        sUrlEscapeMap.put('[', "%5B");
        sUrlEscapeMap.put('\\', "%5C");
        sUrlEscapeMap.put(']', "%5D");
        sUrlEscapeMap.put('^', "%5E");
        sUrlEscapeMap.put('`', "%60");
        sUrlEscapeMap.put('{', "%7B");
        sUrlEscapeMap.put('|', "%7C");
        sUrlEscapeMap.put('}', "%7D");
        sUrlEscapeMap.put('+', "%2B");
    }

    /**
     * urlEscape method will encode '?' and '&', so make sure
     * the passed in String does not have query string in it.
     * Or call urlEscape on each segment and append query
     * String afterwards. Deviates slightly from RFC 3986 to
     * include "<" and ">"
     *
     * from RFC 3986:
     *
     * pchar       = unreserved / pct-encoded / sub-delims / ":" / "@"
     *
     * sub-delims  = "!" / "$" / "&" / "'" / "(" / ")"
     *                   / "*" / "+" / "," / ";" / "="
     *
     * unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
     *
     */
    public static String urlEscape(String str) {
        // rfc 2396 url escape.
        StringBuilder buf = null;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            String escaped = null;
            if (c < 0x7F)
                escaped = sUrlEscapeMap.get(c);

            if (escaped != null || c >= 0x7F) {
                if (buf == null) {
                    buf = new StringBuilder();
                    buf.append(str.substring(0, i));
                }
                if (escaped != null)
                    buf.append(escaped);
                else {
                    try {
                        byte[] raw = Character.valueOf(c).toString().getBytes("UTF-8");
                        for (byte b : raw) {
                            int unsignedB = b & 0xFF;  // byte is signed
                            buf.append("%").append(Integer.toHexString(unsignedB).toUpperCase());
                        }
                    } catch (IOException e) {
                        buf.append(c);
                    }
                }
            } else if (buf != null) {
                buf.append(c);
            }
        }
        if (buf != null)
            return buf.toString();
        return str;
    }

    /**
     * The main difference between java.net.URLDecoder.decode()
     * and this method is the handling of "+" sign.  URLDecoder
     * will turn + into ' ' (space), and not suitable for
     * decoding URL used in HTTP request.
     */
    public static String urlUnescape(String escaped) {
        // all the encoded byte groups should be converted to
        // string together
        ArrayList<Byte> segment = new ArrayList<Byte>();
        StringBuilder buf = null;
        for (int i = 0; i < escaped.length(); i++) {
            char c = escaped.charAt(i);
            if (c == '%' && i < (escaped.length() - 2)) {
                String bytes = escaped.substring(i+1, i+3);
                try {
                    // java Byte type is signed with range of -0x80 to 0x7F.
                    // it cannot convert segment of encoded UTF-8 string
                    // with high bit set.  we'll parse using Integer
                    // then cast the result back to signed Byte.
                    // e.g. 0xED becomes 237 as Integer, then -19 as Byte
                    int b = Integer.parseInt(bytes, 16);
                    segment.add(Byte.valueOf((byte)b));
                } catch (NumberFormatException e) {
                }
                if (buf == null) {
                    buf = new StringBuilder(escaped.substring(0, i));
                }
                i += 2;
                // append to the buffer if we are at the end of the string,
                // or if this is the last encoded character in the segment.
                if (i + 1 == escaped.length() || escaped.charAt(i + 1) != '%') {
                    try {
                        buf.append(new String(Bytes.toArray(segment), "UTF-8"));
                    } catch (UnsupportedEncodingException e) {
                    }
                    segment.clear();
                }
                continue;
            }
            if (buf != null) {
                buf.append(c);
            }
        }
        if (buf != null)
            return buf.toString();
        return escaped;
    }

    public static void main(String[] args) {
        System.out.println(getURIParams((String) null));
        System.out.println(getURIParams("foo=bar"));
        System.out.println(getURIParams("foo=bar&baz&ben=wak"));
        System.out.println(getURIParams("foo=bar&%45t%4E=%33%20%6eford"));
    }
}
