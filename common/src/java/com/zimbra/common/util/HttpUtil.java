/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016, 2018 Synacor, Inc.
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.internet.MimeUtility;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.net.URLCodec;

import com.google.common.base.Charsets;
import com.google.common.primitives.Bytes;

public final class HttpUtil {

    public enum Browser {
        // Browser types
        // NOTE: please make sure to update getBrowser/getMajorVersion
        // if you add a new type.
        IE("MSIE"),
        FIREFOX("Firefox"),
        MOZILLA("Mozilla"),
        OPERA("Opera"),
        SAFARI("AppleWebKit"),
        APPLE_ICAL("iCal"),
        CHROME("Chrome"),
        UNKNOWN("");
        /**
         * The string used to determine which browser this
         * is in the user agent
         */
        private final String userAgentStr;
        Browser(String userAgentStr) {
            this.userAgentStr = userAgentStr;
        };

        String getUserAgentStr() {
            return userAgentStr;
        }

        /**
         * Gets the index of this browser in the string if its present, -1 if not
         * @param str The string to check, usually the full user-agent
         * @return The index (like String.indexof) if present, -1 if not
         */
        int indexOf(String str) {
            if (StringUtil.isNullOrEmpty(str)) {
                return -1;
            }
            return str.indexOf(userAgentStr);
        }

        private static class Version {
            private String mVersion;
            private int mMajor = -1;
            private int mMinor = 0;

            public Version(String v) {
                if (v == null || v.length() == 0) return;

                // so parseInt doesn't choke on something like 1.9pre
                v = v.replaceAll("[^0-9\\.].*", "");
                int d1 = v.indexOf('.');
                if (d1 != -1) {
                    mMajor = parseInt(v.substring(0, d1), -1);
                    int d2 = v.indexOf('.', d1+1);
                    if (d2 != -1)
                        mMinor = parseInt(v.substring(d1+1, d2), 0);
                    else
                        mMinor = parseInt(v.substring(d1+1), 0);
                } else {
                    mMajor = parseInt(v, -1);
                }
                mVersion = mMajor + "." + mMinor;
            }

            private int parseInt(String s, int defaultVal) {
                try {
                    return Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    return defaultVal;
                }
            }

            public String getVersion() { return mVersion; }
            public int getMajor() { return mMajor; }
            public int getMinor() { return mMinor; }
        }

        static boolean isModernIE (String ua) {
            boolean isModernIE = false;
            ua = ua.toLowerCase();
            int index = 0;
            Version tridentVersion = null;
            Version mozVersion = null;
            if ((index = ua.indexOf("trident/")) != -1) {
                tridentVersion = new Version(ua.substring(index + 8));
            }

            if ((index = ua.indexOf("rv:")) != -1){
                mozVersion = new Version(ua.substring(index + 3));
            }

            if (tridentVersion != null && mozVersion != null) {
                isModernIE = tridentVersion.getMajor() >= 7 && mozVersion.getMajor() >= 11;
            }

            return isModernIE;
        }

        /**
         * call this method only if isModernIE()=true;
         * @param ua
         * @return
         */
        static int getModernIEVersion(String ua) {
            ua = ua.toLowerCase();
            Version mozVersion;
            int index = ua.indexOf("rv:");
            if (index != -1) {
                mozVersion = new Version(ua.substring(index + 3));
                return mozVersion.getMajor();
            } else {
                return index;
            }
        }

        /**
         * Guesses the browser type based on the user agent string
         * @param ua The user agent of the browser to check
         * @return
         */
        static Browser guessBrowser(String ua) {
            if (ua == null || ua.trim().equals(""))
                return Browser.UNKNOWN;
            if (Browser.IE.indexOf(ua) != -1 || isModernIE(ua))
                return Browser.IE;
            if (Browser.FIREFOX.indexOf(ua) != -1)
                return Browser.FIREFOX;
            if (Browser.CHROME.indexOf(ua) != -1)
                // Note: Needs to happen before safari detection as
                // it will be detected as the same thing
                return Browser.CHROME;
            if (Browser.SAFARI.indexOf(ua) != -1)
                return Browser.SAFARI;
            if (Browser.OPERA.indexOf(ua) != -1)
                // Note: Needs to be detected after firefox and IE
                // as it has user agents that attempt to be IE and firefox.
               return Browser.OPERA;
            if (Browser.APPLE_ICAL.indexOf(ua) != -1)
                return Browser.APPLE_ICAL;
            return Browser.UNKNOWN;
        }

        /**
         * Gets the major version of the browser, mostly used to determine
         * when bugs were fixed in the browser to change our workaround strategy
         * @param ua The user agent from the browser
         * @return The version if it can be found. -1 if not
         */
        int getMajorVersion(String ua) {
            if (this == UNKNOWN) {
                return -1;
            }
            // Note, this assumes that the major will be separated frome
            // the minor versions by a '.'. So far everything supported does.
            int start = 0;
            int version = 0;
            switch (this) {
            case IE:
                if (isModernIE(ua)) {
                    return getModernIEVersion(ua);
                }
                // example: MSIE 10.6;
                start = indexOf(ua) + IE.userAgentStr.length() +1;
                break;
            case FIREFOX:
                // example : Firefox/10.0a4
                start = indexOf(ua) + FIREFOX.userAgentStr.length() + 1;
                break;
            case CHROME:
                // example: Chrome/19.0.1042.0
                start = indexOf(ua) + CHROME.userAgentStr.length() + 1;
                break;
            case SAFARI:
                // example: AppleWebKit/533.21.1 (KHTML, like Gecko) Version/5.0.5
                start = indexOf(ua) + SAFARI.userAgentStr.length() +1 ;
                version = ua.indexOf("Version") + "Version".length();
                if (version < "Version".length()) {
                    // Webkit doesn't use 'version' in versions 1 or 2.
                    return 2;
                }
                start = version + 1;
                break;
            case OPERA:
                // Opera does a bunch of silly things with its user agent
                // depending on whether or not its attempting to fake IE or Firefox
                // We're going to ignore those for this check.. As they should get
                // detected as IE or Firefox when they are setup this way
                start = indexOf(ua) + OPERA.userAgentStr.length() + 1;
                version = ua.indexOf("Version") + "Version".length();
                // if the version is found, we'll use it
                // otherwise it'll just be Opera/<ver> for versions older than 10
                if (version >= "Version".length()) {
                    start = version +1;
                }
                break;
            case APPLE_ICAL:
                // example :iCal/4.0
                start = indexOf(ua) + APPLE_ICAL.userAgentStr.length() +1 ;
                break;

            }
            // If we get down to here, with a < 0 index, we know something bad happened
            // so we'll just return -1
            if (start < 0 || start >= ua.length()) {
                return -1;
            }

            // find the end of the substring.. should be a '.' for all versions
            int end = ua.indexOf(".", start);
            String verStr = null;
            if (end > start) {
                verStr = ua.substring(start, end);
            } else {
                verStr = ua.substring(start);
            }

            try {
                return Integer.parseInt(verStr);
            } catch (NumberFormatException nfe) {
                return -1;
            }
        }


      };

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
        return Browser.guessBrowser(ua);
    }
    /**
     * Used to determine the browser version from the user agent string
     * @param browser
     * @param ua
     * @return
     */
    public static int guessBrowserMajorVersion(String ua) {
       Browser browser = Browser.guessBrowser(ua);
       return browser.getMajorVersion(ua);
    }

    @Deprecated
    public static String encodeFilename(HttpServletRequest req, String filename) {
        if (StringUtil.isAsciiString(filename) && filename.indexOf('"') == -1) {
            return '"' + StringUtil.sanitizeFilename(filename) + '"';
        }
        return encodeFilename(guessBrowser(req), filename);
    }

    /**
     * Used to encode filenames to be safe for browser headers
     *
     * Note: Due to browsers finally starting to implement RFC 5987 its better
     * to use the createContentDisposition() method below so it can
     * setup the filename= or filename*= correctly based on the browser
     *
     * @param browser
     * @param filename
     * @return
     */
    @Deprecated
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
                case CHROME:
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

    /**
     * Creates the content disposition value for a given filename.
     * @param request
     * @param disposition
     * @param filename
     * @return
     */
    public static String createContentDisposition(HttpServletRequest request, String disposition, String filename) {
        StringBuffer result = new StringBuffer(disposition);
        result.append("; ");

        // First, check to see if we're just dealing with a straight ascii filename
        // If that's the case nothing else needs to be done
        if (StringUtil.isAsciiString(filename) && filename.indexOf('"') == -1) {
            result.append("filename=")
                  .append("\"")
                  .append(filename)
                  .append("\"");
            return result.toString();
        }

        // Now here's where it gets fun. Some browsers don't do well with any
        // encoding -- see <http://stackoverflow.com/questions/1361604> for a
        // list. Most browsers support RFC 5987 for encoding non-ascii
        // filenames.
        String pathInfo = request.getPathInfo();
        String ua = request.getHeader("User-Agent");
        Browser browser = guessBrowser(ua);

        int majorVer = browser.getMajorVersion(ua);

        if (pathInfo != null && pathInfo.endsWith(filename)) {
            // If we have a path info that matches our filename, we'll leave out the
            // filename= part of the header and let the browser use that
        } else if (browser == Browser.IE && majorVer < 9) {
            result.append("filename=")
                  .append(new String(URLCodec.encodeUrl(IE_URL_SAFE, filename.getBytes(Charsets.UTF_8)),
                                     Charsets.ISO_8859_1));
        } else if (browser == Browser.SAFARI && majorVer < 6 ||
                   browser == Browser.CHROME && majorVer < 11) {
            // force it over to Latin 1 and '?' any of the characters we don't know.
            result.append("filename=")
                 .append(new String(filename.getBytes(Charsets.ISO_8859_1), Charsets.ISO_8859_1));
        } else {
            // use RFC 5987
            result.append("filename*=") // note: the *= is not a typo
                  .append("UTF-8''")
                  // encode it just like IE
                  .append(new String(URLCodec.encodeUrl(IE_URL_SAFE, filename.getBytes(Charsets.UTF_8)),
                          Charsets.ISO_8859_1));
        }


        return result.toString();
    }

    /** Strips any userinfo (username/password) data from the passed-in URL
     *  and returns the result. */
    public static String sanitizeURL(String url) {
        if (url != null && url.indexOf('@') != -1) {
            try {
                URL urlTemp = new URL(url);
                if (urlTemp.getUserInfo() != null) {
                    // UserInfo format username:password@
                    //  change to username:
                    String userInfo  = urlTemp.getUserInfo();
                    int index = url.indexOf(userInfo);
                    String newUserInfo = userInfo;
                    if (index != -1) {
                        newUserInfo = url.substring(index, (index + userInfo.length() + 1));
                    }
                    String sanitizedUrl = url.replace(newUserInfo, "");
                    ZimbraLog.misc.debug("Sanitized url: %s" , sanitizedUrl);
                    return sanitizedUrl;
                }
            } catch (MalformedURLException urie) { }
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

    public static String urlEscapeIncludingSlash(String str) {
        String escaped = urlEscape(str);
        return escaped.replaceAll("/", "%2F");
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

    /**
     * Returns the decoded fragments of an url.
     * @param uri
     * @return
     */

    public static String[] getPathFragments(URI uri) {
        List<String> fragments = new ArrayList<String>();
        String[] encodedFragments = uri.getRawPath().split("/");
        for (String encodedFragment : encodedFragments) {
            if (!encodedFragment.isEmpty()) {
                fragments.add(urlUnescape(encodedFragment));
            }
        }
        return fragments.toArray(new String[0]);
    }

    /**
     * Make the uri from decoded fragments
     * @param fragments
     * @param leadingSlash
     * @param tralingSlash
     * @return
     */
    public static URI getUriFromFragments(String[] fragments, String queryString, boolean leadingSlash, boolean tralingSlash) {
        StringBuilder sb = new StringBuilder();
        if (leadingSlash) {
            sb.append('/');
        }
        for (int i = 0; i < fragments.length-1; i++) {
            sb.append(urlEscapeIncludingSlash(fragments[i])).append("/");
        }
        sb.append(urlEscapeIncludingSlash(fragments[fragments.length-1]));
        if (tralingSlash) {
            sb.append('/');
        }
        if (queryString != null) {
            sb.append('?').append(queryString);
        }
        return URI.create(sb.toString());
    }

    public static void main(String[] args) {
        System.out.println(getURIParams((String) null));
        System.out.println(getURIParams("foo=bar"));
        System.out.println(getURIParams("foo=bar&baz&ben=wak"));
        System.out.println(getURIParams("foo=bar&%45t%4E=%33%20%6eford"));
    }
}
