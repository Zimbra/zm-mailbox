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
package com.zimbra.common.mime;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.codec.net.URLCodec;

import com.zimbra.common.mime.HeaderUtils.ByteBuilder;
import com.zimbra.common.util.StringUtil;

public class MimeCompoundHeader extends MimeHeader {
    private boolean mUse2231Encoding;
    private String mPrimaryValue;
    private Map<String, String> mParams = new LinkedHashMap<String, String>();

    public MimeCompoundHeader(String name, String value) {
        this(name, value, false);
    }

    public MimeCompoundHeader(String name, String value, boolean use2231) {
        super(name, value == null ? null : value.getBytes());
        mUse2231Encoding = use2231;
        parse();
    }

    MimeCompoundHeader(MimeHeader header) {
        super(header);
        parse();
    }

    MimeCompoundHeader(String name, byte[] content, int start) {
        super(name, content, start);
        parse();
    }

    public MimeCompoundHeader(MimeCompoundHeader mch) {
        super(mch);
        mUse2231Encoding = mch.mUse2231Encoding;
        mPrimaryValue    = mch.mPrimaryValue;
        mParams.putAll(mch.mParams);
    }

    private void parse() {
        if (mContent == null) {
            return;
        }

        RFC2231Data rfc2231 = new RFC2231Data();
        boolean escaped = false;

        for (int i = mValueStart, count = mContent.length; i < count; i++) {
            byte b = mContent[i];

            // ignore folding, even where it's not actually permitted
            if ((b == '\r' || b == '\n') && rfc2231.state != RFC2231State.VALUE && rfc2231.state != RFC2231State.SLOP) {
                escaped = false;
                continue;
            }

            switch (rfc2231.state) {
                case QVALUE:
                    if (!escaped && b == '\\') {
                        escaped = true;
                    } else if (!escaped && b == '"') {
                        rfc2231.saveParameter(mParams);
                        rfc2231.setState(RFC2231State.SLOP);
                    } else {
                        rfc2231.addValueByte(b);
                        escaped = false;
                    }
                    break;

                case PARAM:
                    if (b == '=') {
                        rfc2231.setState(RFC2231State.EQUALS);
                    } else if (b == '*') {
                        rfc2231.setState(RFC2231State.EXTENDED);
                    } else if (b == '(') {
                        escaped = false;
                        rfc2231.comment++;
                        rfc2231.setState(RFC2231State.COMMENT);
                    } else if (b == ';') {
                        rfc2231.saveParameter(mParams);
                        rfc2231.setState(RFC2231State.PARAM);
                    } else if (b > 0x20 && b < 0x7F && !TSPECIALS[b]) {
                        rfc2231.addKeyByte(b);
                    }
                    break;

                case VALUE:
                    if (b == ';' || b == ' ' || b == '\t' || b == '\r' || b == '\n') {
                        rfc2231.saveParameter(mParams);
                        rfc2231.setState(b == ' ' || b == '\t' ? RFC2231State.SLOP : RFC2231State.PARAM);
                    } else if (b == '(') {
                        escaped = false;
                        rfc2231.comment++;
                        rfc2231.setState(RFC2231State.COMMENT);
                    } else {
                        rfc2231.addValueByte(b);
                    }
                    break;

                case EQUALS:
                    if (b == ';') {
                        rfc2231.saveParameter(mParams);
                        rfc2231.setState(RFC2231State.PARAM);
                    } else if (b == '"') {
                        escaped = false;
                        rfc2231.setState(RFC2231State.QVALUE);
                    } else if (b == '(') {
                        escaped = false;
                        rfc2231.comment++;
                        rfc2231.setState(RFC2231State.COMMENT);
                    } else if (b != ' ' && b != '\t') {
                        rfc2231.addValueByte(b);
                        rfc2231.setState(RFC2231State.VALUE);
                    }
                    break;

                case EXTENDED:
                    if (b >= '0' && b <= '9') {
                        rfc2231.continued = b - '0';
                        rfc2231.setState(RFC2231State.CONTINUED);
                    } else if (b == '=') {
                        rfc2231.setEncoded();
                        rfc2231.setState(rfc2231.continued > 0 ? RFC2231State.VALUE : RFC2231State.CHARSET);
                    }
                    break;

                case CONTINUED:
                    if (b == '=') {
                        rfc2231.setState(RFC2231State.EQUALS);
                    } else if (b == '*') {
                        rfc2231.setState(RFC2231State.EXTENDED);
                    } else if (b >= '0' && b <= '9') {
                        rfc2231.continued = rfc2231.continued * 10 + b - '0';
                    }
                    break;

                case CHARSET:
                    if (b == '\'') {
                        rfc2231.setState(RFC2231State.LANG);
                    } else {
                        rfc2231.addCharsetByte(b);
                    }
                    break;

                case LANG:
                    if (b == '\'') {
                        rfc2231.setState(RFC2231State.VALUE);
                    }
                    break;

                case COMMENT:
                    if (escaped) {
                        escaped = false;
                    } else if (b == '\\') {
                        escaped = true;
                    } else if (b == '(') {
                        rfc2231.comment++;
                    } else if (b == ')' && --rfc2231.comment == 0) {
                        rfc2231.setState(rfc2231.precomment);
                    }
                    break;

                case SLOP:
                    if (b == ';' || b == '\r' || b == '\n') {
                        rfc2231.setState(RFC2231State.PARAM);
                    } else if (b == '(') {
                        escaped = false;
                        rfc2231.comment++;
                        rfc2231.setState(RFC2231State.COMMENT);
                    }
                    break;
            }
        }

        rfc2231.saveParameter(mParams);
        rfc2231.assembleContinuations(mParams);
        mPrimaryValue = mParams.remove(null);
    }

    public String getPrimaryValue() {
        return mPrimaryValue;
    }

    public MimeCompoundHeader setPrimaryValue(String value) {
        if (value != null && !value.equals(mPrimaryValue)) {
            markDirty();
            mPrimaryValue = value;
        }
        return this;
    }

    public int getParameterCount() {
        return mParams.size();
    }

    public boolean containsParameter(String name) {
        return getParameter(name) != null;
    }

    public String getParameter(String name) {
        if (name != null) {
            // RFC 2045 2: "All media type values, subtype values, and parameter names
            //              as defined are case-insensitive."
            for (Map.Entry<String, String> param : mParams.entrySet()) {
                if (name.equalsIgnoreCase(param.getKey()))
                    return param.getValue();
            }
        }
        return null;
    }

    public MimeCompoundHeader setParameter(String name, String value) {
        markDirty();

        // massage the parameter name until it's valid
        String key = name;
        if (name != null) {
            StringBuilder sb = new StringBuilder(name.length());
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c > 0x20 && c < 0xFF && !TSPECIALS[c]) {
                    sb.append(c);
                }
            }
            key = sb.toString();
        }

        if (key != null && !key.isEmpty()) {
            // RFC 2045 2: "All media type values, subtype values, and parameter names
            //              as defined are case-insensitive."
            for (Iterator<String> it = mParams.keySet().iterator(); it.hasNext(); ) {
                if (key.equalsIgnoreCase(it.next())) {
                    it.remove();
                }
            }
            if (value != null) {
                mParams.put(key, value);
            }
        }
        return this;
    }

    public Iterator<Map.Entry<String, String>> parameterIterator() {
        return mParams.entrySet().iterator();
    }

    public MimeCompoundHeader setCharset(String charset) {
        mCharset = charset == null || charset.trim().isEmpty() ? null : charset;
        return this;
    }

    public MimeCompoundHeader setUse2231Encoding(boolean use2231) {
        mUse2231Encoding = use2231;
        return this;
    }

    public MimeCompoundHeader cleanup() {
        markDirty();
        return this;
    }

    /** Printable ASCII characters that must be quoted in parameter values
     *  and avoided in parameter names. */
    private static final boolean[] TSPECIALS = new boolean[128];
    static {
        TSPECIALS['('] = TSPECIALS[')'] = TSPECIALS[','] = true;
        TSPECIALS['<'] = TSPECIALS['>'] = TSPECIALS['?'] = true;
        TSPECIALS['['] = TSPECIALS[']'] = TSPECIALS['='] = true;
        TSPECIALS[':'] = TSPECIALS[';'] = TSPECIALS['"'] = true;
        TSPECIALS['/'] = TSPECIALS['\\'] = TSPECIALS['@'] = true;
        TSPECIALS[' '] = TSPECIALS['\t'] = true;
    }

    private static final int LINE_WRAP_LENGTH = 76;

    @Override protected void reserialize() {
        if (mContent != null) {
            return;
        }

        ByteBuilder line = new ByteBuilder();
        line.append(mName.getBytes()).append(':').append(' ');
        mValueStart = line.length();

        if (mPrimaryValue != null) {
            line.append(mPrimaryValue);
        }

        int position = line.length();
        for (Map.Entry<String, String> param : mParams.entrySet()) {
            String key = param.getKey(), value = param.getValue();
            if (value == null || key == null || key.isEmpty()) {
                continue;
            }
            line.append(';');
            position++;

            boolean quoted = false, nonascii = false;
            for (int i = 0, max = value.length(); i < max; i++) {
                char c = value.charAt(i);
                if (c >= 0x7F || c < 0x20 && c != '\t') {
                    nonascii = true;
                    break;
                } else if (TSPECIALS[c]) {
                    quoted = true;
                }
            }

            String charset = nonascii ? StringUtil.checkCharset(value, mCharset) : "us-ascii";

            ByteBuilder bb = new ByteBuilder();
            if (!nonascii) {
                if (quoted || value.isEmpty()) {
                    bb.append(param.getKey()).append('=').append('"');
                    for (int i = 0, max = value.length(); i < max; i++) {
                        char c = value.charAt(i);
                        if (c == '"' || c == '\\') {
                            bb.append('\\');
                        }
                        bb.append(c);
                    }
                    bb.append('"');
                } else {
                    bb.append(param.getKey()).append('=').append(value);
                }
            } else if (mUse2231Encoding) {
                try {
                    String encoded = new URLEncoder().encode(value, charset);
                    bb.append(param.getKey()).append('*').append('=').append(charset).append('\'').append('\'').append(encoded);
                } catch (UnsupportedEncodingException e) {
                }
            } else {
                bb.append(param.getKey()).append('=').append('"').append(MimeHeader.EncodedWord.encode(value, charset)).append('"');
            }

            if (position + bb.length() > LINE_WRAP_LENGTH) {
                line.append('\r').append('\n').append('\t');
                position = 8;
            } else {
                line.append(' ');
                position++;
            }
            line.append(bb);
            position += bb.length();
        }

        mContent = line.append('\r').append('\n').toByteArray();
    }

    // special subclass of URLCodec that replaces ' ' with "%20" rather than with "+"
    private static class URLEncoder extends URLCodec {
        URLEncoder() {}

        private static final BitSet WWW_URL = (BitSet) WWW_FORM_URL.clone();
        static {
            WWW_URL.clear(' ');
        }

        @Override public byte[] encode(byte[] bytes) {
            return encodeUrl(WWW_URL, bytes);
        }
    }

    private enum RFC2231State {
        PARAM, CONTINUED, EXTENDED, EQUALS, CHARSET, LANG, VALUE, QVALUE, SLOP, COMMENT
    };

    private static class RFC2231Data {
        RFC2231State state = RFC2231State.EQUALS;
        RFC2231State precomment;
        ByteBuilder key = null;
        ByteBuilder value = new ByteBuilder();
        int comment = 0;
        int continued = -1;
        private boolean encoded = false;
        private ByteBuilder charset = null;
        private Map<String, Map<Integer, ParameterContinuation>> partials;

        private static class ParameterContinuation {
            String charset;
            boolean encoded;
            String value;

            ParameterContinuation(String c, boolean e, String v) {
                charset = c;  encoded = e;  value = v;
            }
        }

        RFC2231Data() {}

        void setState(RFC2231State newstate) {
            if (newstate == RFC2231State.COMMENT && state != RFC2231State.COMMENT) {
                precomment = state;
            }
            state = newstate;
        }

        void setEncoded() {
            encoded = true;
            if (continued <= 0) {
                charset = new ByteBuilder();
            }
        }

        void addCharsetByte(byte b) {
            charset.append(b);
        }

        void addKeyByte(byte b) {
            key.append(b);
        }

        void addValueByte(byte b) {
            value.append(b);
        }

        void reset() {
            key = new ByteBuilder();
            value = new ByteBuilder();
            continued = -1;
            encoded = false;
        }

        void saveParameter(Map<String, String> attrs) {
            try {
                if (value == null) {
                    return;
                }
                String pname = key == null ? null : key.toString().toLowerCase();
                String pvalue = value.toString();
                if ("".equals(pname)) {
                    return;
                }

                if (continued >= 0) {
                    // in order to handle out-of-order parts, store all partials in a hash until we're done
                    if (partials == null) {
                        partials = new HashMap<String, Map<Integer, ParameterContinuation>>(3);
                    }
                    Map<Integer, ParameterContinuation> parts = partials.get(pname);
                    if (parts == null) {
                        partials.put(pname, parts = new TreeMap<Integer, ParameterContinuation>());
                    }
                    parts.put(continued, new ParameterContinuation(charset == null || charset.isEmpty() ? "us-ascii" : charset.toString(), encoded, value.toString()));
                    attrs.put(pname, null);
                } else {
                    if (encoded) {
                        try {
                            pvalue = URLDecoder.decode(pvalue, charset.isEmpty() ? "us-ascii" : charset.toString());
                        } catch (UnsupportedEncodingException uee) {
                            System.out.println(uee);
                        }
                    } else if (pvalue.length() >= 8 && pvalue.indexOf("=?") >= 0 && pvalue.indexOf("?=") > 0) {
                        pvalue = decode(pvalue);
                    }
                    attrs.put(pname, pvalue);
                }
            } finally {
                reset();
            }
        }

        void assembleContinuations(Map<String, String> attrs) {
            if (partials == null) {
                return;
            }

            for (Map.Entry<String, Map<Integer, ParameterContinuation>> entry : partials.entrySet()) {
                String pname = entry.getKey();
                if (attrs.get(pname) != null) {
                    continue;
                }

                Map<Integer, ParameterContinuation> parts = entry.getValue();
                ParameterContinuation first = parts.get(0);
                String paramCharset = first == null ? "us-ascii" : first.charset;

                StringBuilder raw = null, assembled = new StringBuilder();
                for (Iterator<ParameterContinuation> it = parts.values().iterator(); it.hasNext();) {
                    ParameterContinuation partial = it.next();
                    if (partial.encoded) {
                        // we need to concatenate consecutive encoded parts because they don't necessarily break on a character boundary
                        if (raw == null) {
                            raw = new StringBuilder();
                        }
                        raw.append(partial.value);
                        // fall through if the *last* partial was encoded
                        if (it.hasNext()) {
                            continue;
                        }
                    }
                    if (raw != null) {
                        // if we're here, we've reached the end of consecutive encoded parts and can decode them
                        try {
                            assembled.append(URLDecoder.decode(raw.toString(), paramCharset));
                        } catch (UnsupportedEncodingException uee) {
                            assembled.append(raw.toString());
                        }
                        raw = null;
                    }
                    // if this wasn't an encoded partial, append it
                    if (!partial.encoded) {
                        assembled.append(partial.value);
                    }
                }
                attrs.put(pname, assembled.toString());
            }
        }
    }
}
