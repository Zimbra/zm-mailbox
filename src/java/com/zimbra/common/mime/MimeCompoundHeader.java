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

import javax.mail.internet.MimeUtility;

import org.apache.commons.codec.net.URLCodec;

import com.zimbra.common.util.StringUtil;

public class MimeCompoundHeader {
    private enum RFC2231State { PARAM, CONTINUED, EXTENDED, EQUALS, CHARSET, LANG, VALUE, QVALUE, SLOP, COMMENT };

    private String mValue;
    private Map<String, String> mParams = new LinkedHashMap<String, String>();
    private boolean mUse2231Encoding;
    private String mCharset;

    public MimeCompoundHeader(String header)                   { this(header, null, false); }
    public MimeCompoundHeader(String header, boolean use2231)  { this(header, null, use2231); }
    public MimeCompoundHeader(String header, String charset)   { this(header, charset, false); }
    public MimeCompoundHeader(String header, String charset, boolean use2231) {
        mUse2231Encoding = use2231;
        setCharset(charset);
        if (header == null)
            return;

        RFC2231Data rfc2231 = new RFC2231Data();
        boolean escaped = false;

        for (int i = 0, count = header.length(); i < count; i++) {
            char c = header.charAt(i);
            // ignore folding, even where it's not actually permitted
            if ((c == '\r' || c == '\n') && rfc2231.state != RFC2231State.VALUE && rfc2231.state != RFC2231State.SLOP) {
                escaped = false;
                continue;
            }

            switch (rfc2231.state) {
                case QVALUE:
                    if (!escaped && c == '\\') {
                        escaped = true;
                    } else if (!escaped && c == '"') {
                        rfc2231.saveParameter(mParams);
                        rfc2231.setState(RFC2231State.SLOP);
                    } else {
                        rfc2231.addValueChar(c);  escaped = false;
                    }
                    break;

                case PARAM:
                    if (c == '=') {
                        rfc2231.setState(RFC2231State.EQUALS);
                    } else if (c == '*') {
                        rfc2231.setState(RFC2231State.EXTENDED);
                    } else if (c == '(') {
                        escaped = false;  rfc2231.comment++;
                        rfc2231.setState(RFC2231State.COMMENT);
                    } else if (c == ';') {
                        rfc2231.saveParameter(mParams);
                        rfc2231.setState(RFC2231State.PARAM);
                    } else if (c > 0x20 && c < 0x7F && !TSPECIALS[c]) {
                        rfc2231.addKeyChar(c);
                    }
                    break;

                case VALUE:
                    if (c == ';' || c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                        rfc2231.saveParameter(mParams);
                        rfc2231.setState(c == ' ' || c == '\t' ? RFC2231State.SLOP : RFC2231State.PARAM);
                    } else if (c == '(') {
                        escaped = false;  rfc2231.comment++;
                        rfc2231.setState(RFC2231State.COMMENT);
                    } else {
                        rfc2231.addValueChar(c);
                    }
                    break;

                case EQUALS:
                    if (c == ';') {
                        rfc2231.saveParameter(mParams);
                        rfc2231.setState(RFC2231State.PARAM);
                    } else if (c == '"') {
                        escaped = false;
                        rfc2231.setState(RFC2231State.QVALUE);
                    } else if (c == '(') {
                        escaped = false;  rfc2231.comment++;
                        rfc2231.setState(RFC2231State.COMMENT);
                    } else if (c != ' ' && c != '\t') {
                        rfc2231.addValueChar(c);
                        rfc2231.setState(RFC2231State.VALUE);
                    }
                    break;

                case EXTENDED:
                    if (c >= '0' && c <= '9') {
                        rfc2231.continued = c - '0';
                        rfc2231.setState(RFC2231State.CONTINUED);
                    } else if (c == '=') {
                        rfc2231.setEncoded();
                        rfc2231.setState(rfc2231.continued > 0 ? RFC2231State.VALUE : RFC2231State.CHARSET);
                    }
                    break;

                case CONTINUED:
                    if (c == '=')
                        rfc2231.setState(RFC2231State.EQUALS);
                    else if (c == '*')
                        rfc2231.setState(RFC2231State.EXTENDED);
                    else if (c >= '0' && c <= '9')
                        rfc2231.continued = rfc2231.continued * 10 + c - '0';
                    break;

                case CHARSET:
                    if (c == '\'')
                        rfc2231.setState(RFC2231State.LANG);
                    else
                        rfc2231.addCharsetChar(c);
                    break;

                case LANG:
                    if (c == '\'')
                        rfc2231.setState(RFC2231State.VALUE);
                    break;

                case COMMENT:
                    if (escaped)
                        escaped = false;
                    else if (c == '\\')
                        escaped = true;
                    else if (c == '(')
                        rfc2231.comment++;
                    else if (c == ')' && --rfc2231.comment == 0)
                        rfc2231.setState(rfc2231.precomment);
                    break;

                case SLOP:
                    if (c == ';' || c == '\r' || c == '\n') {
                        rfc2231.setState(RFC2231State.PARAM);
                    } else if (c == '(') {
                        escaped = false;  rfc2231.comment++;
                        rfc2231.setState(RFC2231State.COMMENT);
                    }
                    break;
            }
        }

        rfc2231.saveParameter(mParams);
        rfc2231.assembleContinuations(mParams);
        mValue = mParams.remove(null);
    }

    public MimeCompoundHeader(MimeCompoundHeader mch) {
        mValue = mch.mValue;
        mParams.putAll(mch.mParams);
        mUse2231Encoding = mch.mUse2231Encoding;
    }


    public String getValue()                          { return mValue; }
    public MimeCompoundHeader setValue(String value)  { mValue = value;  return this; }

    public boolean containsParameter(String name)  { return mParams.containsKey(name); }
    public String getParameter(String name)        { return mParams.get(name); }
    public MimeCompoundHeader setParameter(String name, String value) {
        // massage the parameter name intil it's valid
        if (name != null) {
            name = name.trim();
            for (int i = 0; i < name.length(); i++) {
                char c = name.charAt(i);
                if (c <= 0x20 || c >= 0xFF || TSPECIALS[c])
                    name = name.substring(0, i) + name.substring(i-- + 1);
            }
        }

        if (value == null)
            mParams.remove(name);
        else
            mParams.put(name, value);
        return this;
    }

    public Iterator<Map.Entry<String, String>> getParameterIterator() {
        return mParams.entrySet().iterator();
    }


    public MimeCompoundHeader setCharset(String charset) {
        mCharset = charset == null || charset.trim().equals("") ? null : charset;
        return this;
    }

    public MimeCompoundHeader setUse2231Encoding(boolean use2231) {
        mUse2231Encoding = use2231;
        return this;
    }

    /** Printable ASCII characters that must be quoted in parameter values and avoided in parameter names. */
    private static final boolean[] TSPECIALS = new boolean[128];
        static {
            TSPECIALS['('] = TSPECIALS[')']  = TSPECIALS[','] = true;
            TSPECIALS['<'] = TSPECIALS['>']  = TSPECIALS['?'] = true;
            TSPECIALS['['] = TSPECIALS[']']  = TSPECIALS['='] = true;
            TSPECIALS[':'] = TSPECIALS[';']  = TSPECIALS['"'] = true;
            TSPECIALS['/'] = TSPECIALS['\\'] = TSPECIALS['@'] = true;
            TSPECIALS[' '] = TSPECIALS['\t'] = true;
        }


    private static final int LINE_WRAP_LENGTH = 76;

    @Override public String toString()         { return toString(null, 0); }
    public String toString(String hdrName)     { return toString(hdrName, 0); }
    public String toString(int leadingOffset)  { return toString(null, leadingOffset); }
    private String toString(String hdrName, int leadingOffset) {
        StringBuilder line = new StringBuilder();
        if (hdrName != null && !hdrName.trim().equals(""))
            line.append(hdrName.trim()).append(": ");
        if (mValue != null)
            line.append(mValue);

        int position = line.length() + leadingOffset;
        for (Map.Entry<String,String> param : mParams.entrySet()) {
            String key = param.getKey(), value = param.getValue();
            if (key == null || key.equals("") || value == null)
                continue;
            line.append(';');  position++;

            boolean quoted = false, nonascii = false;
            for (int i = 0, max = value.length(); i < max; i++) {
                char c = value.charAt(i);
                if (c >= 0x7F || (c < 0x20 && c != '\t')) {
                    nonascii = true;  break;
                } else if (TSPECIALS[c]) {
                    quoted = true;
                }
            }

            String charset = nonascii ? StringUtil.checkCharset(value, mCharset) : "us-ascii";

            StringBuilder sb = new StringBuilder();
            if (!nonascii) {
                if (quoted || value.length() == 0) {
                    sb.append(param.getKey()).append("=\"");
                    for (int i = 0, max = value.length(); i < max; i++) {
                        char c = value.charAt(i);
                        if (c == '"' || c == '\\')  sb.append('\\');
                        sb.append(c);
                    }
                    sb.append('"');
                } else {
                    sb.append(param.getKey()).append('=').append(value);
                }
            } else if (mUse2231Encoding) {
                try {
                    String encoded = new URLEncoder().encode(value, charset);
                    sb.append(param.getKey()).append("*=").append(charset).append("''").append(encoded);
                } catch (UnsupportedEncodingException e) { }
            } else {
                sb.append(param.getKey()).append("=\"").append(MimeHeader.EncodedWord.encode(value, charset)).append('"');
            }

            if (position + sb.length() > LINE_WRAP_LENGTH) {
                line.append("\r\n\t");  position = 8;
            } else {
                line.append(' ');  position++;
            }
            line.append(sb);  position += sb.length();
        }
        return line.toString();
    }

    // special subclass of URLCodec that replaces ' ' with "%20" rather than with "+"
    private static class URLEncoder extends URLCodec {
        URLEncoder()  { }
        private static final BitSet WWW_URL = (BitSet) WWW_FORM_URL.clone();
            static { WWW_URL.clear(' '); }
        @Override public byte[] encode(byte[] bytes)  { return encodeUrl(WWW_URL, bytes); }
    }

    private static class RFC2231Data {
        RFC2231State state = RFC2231State.EQUALS;
        RFC2231State precomment;
        StringBuilder key = null;
        StringBuilder value = new StringBuilder();
        int comment = 0;
        int continued = -1;
        private boolean encoded = false;
        private StringBuilder charset = null;
        private Map<String, Map<Integer, ParameterContinuation>> partials;

        private static class ParameterContinuation {
            String charset;  boolean encoded;  String value;
            ParameterContinuation(String c, boolean e, String v) { charset = c;  encoded = e;  value = v; }
        }

        RFC2231Data()  { }

        void setState(RFC2231State newstate) {
            if (newstate == RFC2231State.COMMENT && state != RFC2231State.COMMENT)
                precomment = state;
            state = newstate;
        }

        void setEncoded() {
            encoded = true;
            if (continued <= 0)
                charset = new StringBuilder();
        }
    
        void addCharsetChar(char c)  { charset.append(c); }
        void addKeyChar(char c)      { key.append(c); }
        void addValueChar(char c)    { value.append(c); }

        void reset() {
            key = new StringBuilder();  value = new StringBuilder();
            continued = -1;  encoded = false;
        }
    
        void saveParameter(Map<String, String> attrs) {
            try {
                if (value == null)
                    return;
                String pname = key == null ? null : key.toString().toLowerCase();
                String pvalue = value.toString();
                if ("".equals(pname))
                    return;
    
                if (continued >= 0) {
                    // in order to handle out-of-order parts, store all partials in a hash until we're done
                    if (partials == null)
                        partials = new HashMap<String, Map<Integer, ParameterContinuation>>(3);
                    Map<Integer, ParameterContinuation> parts = partials.get(pname);
                    if (parts == null)
                        partials.put(pname, parts = new TreeMap<Integer, ParameterContinuation>());
                    parts.put(continued, new ParameterContinuation(charset == null || charset.length() == 0 ? "us-ascii" : charset.toString(), encoded, value.toString()));
                    attrs.put(pname, null);
                } else {
                    if (encoded) {
                        if (charset.length() == 0)
                            charset.append("us-ascii");
                        try {
                            pvalue = URLDecoder.decode(pvalue, charset.toString());
                        } catch (UnsupportedEncodingException uee) { 
                            System.out.println(uee);
                        }
                    } else if (pvalue.length() >= 8) {
                        int firstEnd;
                        if (pvalue.lastIndexOf("=?") > 0 || ((firstEnd = pvalue.indexOf("?=", 6)) >= 6 && firstEnd < pvalue.length() - 2)) {
                            try {
                                pvalue = MimeUtility.decodeText(pvalue);
                            } catch (Exception e) { }
                        } else if (pvalue.startsWith("=?") && pvalue.endsWith("?=")) {
                            try {
                                pvalue = MimeUtility.decodeWord(pvalue);
                            } catch (Exception e) { }
                        }
                    }
                    attrs.put(pname, pvalue);
                }
            } finally {
                reset();
            }
        }

        void assembleContinuations(Map<String, String> attrs) {
            if (partials == null)
                return;

            for (Map.Entry<String, Map<Integer, ParameterContinuation>> entry : partials.entrySet()) {
                String pname = entry.getKey();
                if (attrs.get(pname) != null)
                    continue;

                Map<Integer, ParameterContinuation> parts = entry.getValue();
                ParameterContinuation first = parts.get(0);
                String paramCharset = first == null ? "us-ascii" : first.charset;

                StringBuilder raw = null, assembled = new StringBuilder();
                for (Iterator<ParameterContinuation> it = parts.values().iterator(); it.hasNext(); ) {
                    ParameterContinuation partial = it.next();
                    if (partial.encoded) {
                        // we need to concatenate consecutive encoded parts because they don't necessarily break on a character boundary
                        if (raw == null)
                            raw = new StringBuilder();
                        raw.append(partial.value);
                        // fall through if the *last* partial was encoded
                        if (it.hasNext())
                            continue;
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
                    if (!partial.encoded)
                        assembled.append(partial.value);
                }
                attrs.put(pname, assembled.toString());
            }
        }
    }


    private static void testParser(boolean isContentType, String[] test) {
        String description = test[0], raw = test[1], value = test[2];
        MimeCompoundHeader mch = isContentType ? new ContentType(raw) : new ContentDisposition(raw);

        boolean fail = false;
        if (!value.equals(mch.getValue()))
            fail = true;
        else if (test.length - 3 != mch.mParams.size() * 2)
            fail = true;
        else
            for (int i = 3; i < test.length; i += 2)
                if (!test[i+1].equals(mch.mParams.get(test[i])))
                    fail = true;

        if (fail) {
            System.out.println("failed " + (isContentType ? "Content-Type" : "Content-Disposition") + " test: " + description);
            System.out.println("  raw:      {" + (raw == null ? "null" : raw.trim()) + '}');
            System.out.print("  expected: |" + value + '|');
            for (int i = 3; i < test.length; i += 2)
                System.out.print(", " + test[i] + "=|" + test[i+1] + '|');
            System.out.println();
            System.out.print("  actual:   |" + mch.mValue + '|');
            for (Map.Entry<String, String> param : mch.mParams.entrySet())
                System.out.print(", " + param.getKey() + "=|" + param.getValue() + '|');
            System.out.println();
        }
    }

    public static void main(String[] args) {
        String[][] ctypeTests = new String[][] {
            { "missing semicolon between params, standard line breaks",
                "text/plain; charset=US-ASCII;\r\n\tFormat=Flowed   DelSp=Yes\r\n",
                "text/plain", "charset", "US-ASCII", "format", "Flowed" },
            { "mixed encoded and non-encoded continuations",
                "application/x-stuff; title*0*=us-ascii'en'This%20is%20even%20more%20; title*1*=%2A%2A%2Afun%2A%2A%2A%20; title*2=\"isn't it!\"\n",
                "application/x-stuff", "title", "This is even more ***fun*** isn't it!" },
            { "downcasing value, implicit end-of-value at eol",
                "multipart/MIXED; charset=us-ascii;\n foo=\n  boundary=\"---\" \n",
                "multipart/mixed", "charset", "us-ascii", "foo", "boundary=\"---\"" },
            { "non-encoded continuation",
                "message/external-body; access-type=URL;\n URL*0=\"ftp://\";\n URL*1=\"cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar\"\n",
                "message/external-body", "access-type", "URL", "url", "ftp://cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar" },
            { "encoded param value",
                "application/x-stuff;\n\ttitle*=us-ascii'en-us'This%20is%20%2A%2A%2Afun%2A%2A%2A",
                "application/x-stuff", "title", "This is ***fun***" },
            { "missing quotes around param value",
                "application/pdf;\n    x-unix-mode=0644;\n    name=Zimbra on Mac OS X success story.pdf",
                "application/pdf", "x-unix-mode", "0644", "name", "Zimbra" },
            { "invalid value",
                "c; name=TriplePlay_Converged_Network_v5.pdf;\n x-mac-creator=70727677; x-mac-type=50444620",
                "application/octet-stream", "name", "TriplePlay_Converged_Network_v5.pdf", "x-mac-creator", "70727677", "x-mac-type", "50444620" },
            { "'text' as value, backslashes in quoted-string, missing equals, missing param name, blank param, comments before param name, nested comments",
                "text;\n pflaum;=foo; name=\"spam\\\"bag\\\\wall\" \n\t((plain; text=missing); (pissed=off); where=myrtle);;a=b;c;=d;\n (a)foo=bar",
                "text/plain", "pflaum", "", "name", "spam\"bag\\wall", "a", "b", "c", "", "foo", "bar" },
            { "null input",
                null,
                "text/plain" },
            { "comments before and after value, param name, equals, and param value",
                " (morg) text/plain(whoppity)  ;(heep)(hop(hoo)) format(ig)=(nore)\"floo\"  (kell) \n (perm) \n\t(eeble) zoom (ig) = (nore)whop (mm)",
                "text/plain", "format", "floo", "zoom", "whop" },
            { "unquoted encoded-words, bad encoded-words in non-2231 values",
                "text/plain; filename==?us-ascii?q?boo_bah.pdf?=; note=\"   ?==?\"; bloop=\"=?x-unknown?a?text?=\" ",
                "text/plain", "filename", "boo bah.pdf", "note", "   ?==?", "bloop", "=?x-unknown?a?text?=" },
        };

        for (String[] test : ctypeTests)
            testParser(true, test);


        String[][] cdispTests = new String[][] {
            { "content-insensitive value, leading spaces, old-style RFC 2047 param values",
                "   \n  INline;\n filename=\"=?utf-8?Q?=E3=82=BD=E3=83=AB=E3=83=86=E3=82=A3=E3=83=AC=E3=82=A4.rtf?=\"\n  \n ",
                "inline", "filename", "\u30bd\u30eb\u30c6\u30a3\u30ec\u30a4.rtf" },
            { "default value, leading spaces, RFC 2231 encoding",
                "   \n  gropp;\n filename*=UTF-8''%E3%82%BD%E3%83%AB%E3%83%86%E3%82%A3%E3%83%AC%E3%82%A4.rtf\n  \n ",
                "attachment", "filename", "\u30bd\u30eb\u30c6\u30a3\u30ec\u30a4.rtf" },
            { "encoded continuations",
                "attachment; filename*0*=ISO-8859-1''BASE%20INICIAL%20CAMPANHA%20PROVIS%C3O%20ABAIXO; filename*1*=%20DE%20ZERO%2009_10_06%20SUCHY.xls",
                "attachment", "filename", "BASE INICIAL CAMPANHA PROVIS\u00c3O ABAIXO DE ZERO 09_10_06 SUCHY.xls" },
            { "joined 2047 encoded-words",
                "attachment;\n filename=\"=?iso-8859-1?Q?BASE_INICIAL_CAMPANHA_PROVIS=C3O_ABAIXO_DE_ZERO_09=5F10=5F?=\n =?iso-8859-1?Q?06_SUCHY=2Exls?=\"",
                "attachment", "filename", "BASE INICIAL CAMPANHA PROVIS\u00c3O ABAIXO DE ZERO 09_10_06 SUCHY.xls" },
            { "misordered continuations, continuations overriding standard value",
                "inline;\n filename=\"1565 =?ISO-8859-1?Q?ST=C5ENDE_CAD_Netic_SKI=2Epdf?=\";\n filename*1*=%20%4E%65%74%69%63%20%53%4B%49%2E%70%64%66;\n filename*0*=ISO-8859-1''%31%35%36%35%20%53%54%C5%45%4E%44%45%20%43%41%44",
                "inline", "filename", "1565 ST\u00c5ENDE CAD Netic SKI.pdf" },
            { "misordered continuations, continuations overriding standard value",
                "inline;\n filename*1*=%20%4E%65%74%69%63%20%53%4B%49%2E%70%64%66;\n filename*0*=ISO-8859-1''%31%35%36%35%20%53%54%C5%45%4E%44%45%20%43%41%44\n filename=\"1565 =?ISO-8859-1?Q?H=C5MBURGER=2Epdf?=\"",
                "inline", "filename", "1565 H\u00c5MBURGER.pdf" },
            { "leading CFWS, missing semicolon after value, missing semicolon after quoted-string, trailing comment",
                "  \n inline\n foo=\"bar\"\n baz=whop\n (as)\n",
                "inline", "foo", "bar", "baz", "whop" },
            { "missing charset on first continuation",
                "attachment; foo*0=big; foo*1*=%20dog",
                "attachment", "foo", "big dog" },
            { "missing first continuation, out-of-order continuations",
                "attachment; foo*2*=%20dog; foo*1=big",
                "attachment", "foo", "big dog" },
            { "charset on subsequent continuation, out-of-order continuations",
                "attachment; foo*2*=%20dog; foo*1=iso-8859-1'en'big",
                "attachment", "foo", "iso-8859-1'en'big dog" },
            { "encoded continuation split across partials",
                "inline;\n filename*0*=ISO-2022-JP''%1B%24%42%24%33%24%73%24%4B%24%41%24%4F%21%22%40;\n filename*1*=%24%33%26%21%2A%1B%28%42%2E%70%64%66",
                "inline", "filename", "\u3053\u3093\u306b\u3061\u306f\u3001\u4e16\u754c\uff01.pdf" },
        };

        for (String[] test : cdispTests)
            testParser(false, test);

        // FIXME: add tests for serialization (2231 or not, various charsets, quoting, folding, etc.)
    }
}
