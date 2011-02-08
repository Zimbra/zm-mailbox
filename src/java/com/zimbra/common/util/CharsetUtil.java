/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import com.zimbra.common.mime.MimeConstants;

public class CharsetUtil {

    // The following 3 charsets are required to be supported in *all* JREs
    public static final Charset US_ASCII   = CharsetUtil.toCharset(MimeConstants.P_CHARSET_ASCII);
    public static final Charset ISO_8859_1 = CharsetUtil.toCharset(MimeConstants.P_CHARSET_LATIN1);
    public static final Charset UTF_8      = CharsetUtil.toCharset(MimeConstants.P_CHARSET_UTF8);

    private static final Charset WINDOWS_1252 = CharsetUtil.toCharset(MimeConstants.P_CHARSET_WINDOWS_1252);
    private static final Charset GB2312       = CharsetUtil.toCharset(MimeConstants.P_CHARSET_GB2312);
    private static final Charset GBK          = CharsetUtil.toCharset(MimeConstants.P_CHARSET_GBK);
    private static final Charset WINDOWS_31J  = CharsetUtil.toCharset(MimeConstants.P_CHARSET_WINDOWS_31J);
    private static final Charset SHIFT_JIS    = CharsetUtil.toCharset(MimeConstants.P_CHARSET_SHIFT_JIS);

    /** Returns a {@link Charset} for the {@code name}, or {@code null} if the
     *  name is invalid or if the named charset is not supported in this JRE. */
    public static Charset toCharset(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }

        // aliases not provided by the JRE's implementations
        if (name.equalsIgnoreCase("cp932") && WINDOWS_31J != null) {
            // "Windows-31J" is supported but "CP932" is not; force the mapping here...
            return WINDOWS_31J;
        }
    
        try {
            return Charset.forName(name.trim());
        } catch (Exception e) {
            return null;
        }
    }

    public static Charset normalizeCharset(String name) throws UnsupportedEncodingException {
        if (name == null || name.isEmpty()) {
            return null;
        }

        Charset charset = normalizeCharset(toCharset(name));
        if (charset == null) {
            throw new UnsupportedEncodingException(name);
        }
        return charset;
    }

    /**
     * Returns a superset of the charset if available.
     *
     * @param charset charset
     * @return a superset of the charset, or the same charset
     */
    public static Charset normalizeCharset(Charset charset) {
        if (charset == null) {
            return charset;
        } else if (WINDOWS_1252 != null && charset.equals(ISO_8859_1)) {
            // windows-1252 is a superset of iso-8859-1 and they're often confused, so use cp1252 in its place
            return WINDOWS_1252;
        } else if (GBK != null && charset.equals(GB2312)) {
            return GBK;
        } else if (WINDOWS_31J != null && charset.equals(SHIFT_JIS)) {
            return WINDOWS_31J;
        } else {
            return charset;
        }
    }

    /** Determines whether {@code data} can be encoded using
     *  {@code requestedCharset}.
     * @param data              the data to be encoded
     * @param requestedCharset  the character set
     * @return {@code requestedCharset} if encoding is supported,
     *         "<tt>utf-8</tt>" if not, or
     *         "<tt>us-ascii</tt>" if data is {@code null}.
     */
    public static String checkCharset(String data, String requestedCharset) {
        if (data == null) {
            return "us-ascii";
        }

        if (requestedCharset != null && !requestedCharset.isEmpty() && !requestedCharset.equalsIgnoreCase("utf-8")) {
            try {
                Charset cset = Charset.forName(requestedCharset);
                if (cset.canEncode() && cset.newEncoder().canEncode(data)) {
                    return requestedCharset;
                }
            } catch (Exception e) { }
        }

        return "utf-8";
    }

    public static Charset checkCharset(String data, Charset requestedCharset) {
        if (data == null) {
            return US_ASCII;
        }

        if (requestedCharset != null && !requestedCharset.equals(UTF_8)) {
            try {
                if (requestedCharset.canEncode() && requestedCharset.newEncoder().canEncode(data)) {
                    return requestedCharset;
                }
            } catch (Exception e) { }
        }

        return UTF_8;
    }
}
