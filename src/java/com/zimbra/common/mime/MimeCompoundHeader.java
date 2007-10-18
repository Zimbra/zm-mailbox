/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.mime;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.BitSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.mail.internet.MimeUtility;

import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.QCodec;
import org.apache.commons.codec.net.URLCodec;

public class MimeCompoundHeader {
    private enum RFC2231State { PARAM, CONTINUED, EXTENDED, EQUALS, CHARSET, LANG, VALUE, QVALUE, SLOP, COMMENT };

    private String mValue;
    private Map<String, String> mParams = new LinkedHashMap<String, String>();
    private boolean use2231Encoding;

    public MimeCompoundHeader(String header)  { this(header, false); }
    public MimeCompoundHeader(String header, boolean use2231) {
        use2231Encoding = use2231;
        if (header == null)
            return;
        header = header.trim();

        RFC2231Data rfc2231 = new RFC2231Data();
        boolean escaped = false;

        for (int i = 0, count = header.length(); i < count; i++) {
            char c = header.charAt(i);
            if (rfc2231.state == RFC2231State.SLOP) {
                if (c == ';' || c == '\n' || c == '\r') {
                    rfc2231.setState(RFC2231State.PARAM);
                } else if (c == '(') {
                    escaped = false;  rfc2231.comment++;
                    rfc2231.setState(RFC2231State.COMMENT);
                }
            } else if (rfc2231.state == RFC2231State.QVALUE) {
                if (!escaped && c == '\\') {
                    escaped = true;
                } else if (!escaped && c == '"') {
                    rfc2231.saveParameter(mParams);
                    rfc2231.setState(RFC2231State.SLOP);
                } else if (c != '\n' && c != '\r') {
                    rfc2231.addValueChar(c);  escaped = false;
                }
            } else if (c == '\r' || c == '\n') {
                if (!mParams.isEmpty() || rfc2231.value.length() > 0) {
                    rfc2231.saveParameter(mParams);
                    rfc2231.setState(RFC2231State.PARAM);
                }
                // otherwise, it's just folding and we can effectively just ignore the CR/LF
            } else if (rfc2231.state == RFC2231State.PARAM) {
                if (c == '=') {
                    rfc2231.setState(RFC2231State.EQUALS);
                } else if (c == '*') {
                    rfc2231.setState(RFC2231State.EXTENDED);
                } else if (c == '(' && rfc2231.key.length() == 0) {
                    escaped = false;  rfc2231.comment++;
                    rfc2231.setState(RFC2231State.COMMENT);
                } else if (c == ';') {
                    rfc2231.saveParameter(mParams);
                    rfc2231.setState(RFC2231State.PARAM);
                } else if (c > 0x20 && c < 0xFF && !TSPECIALS[c]) {
                    rfc2231.addKeyChar(c);
                }
            } else if (rfc2231.state == RFC2231State.VALUE) {
                if (c != ';' && c != ' ' && c != '\t') {
                    rfc2231.addValueChar(c);
                } else {
                    rfc2231.saveParameter(mParams);
                    rfc2231.setState(c == ';' ? RFC2231State.PARAM : RFC2231State.SLOP);
                }
            } else if (rfc2231.state == RFC2231State.EQUALS) {
                if (c == ';') {
                    rfc2231.saveParameter(mParams);
                    rfc2231.setState(RFC2231State.PARAM);
                } else if (c == '"') {
                    escaped = false;
                    rfc2231.setState(RFC2231State.QVALUE);
                } else if (c != ' ' && c != '\t') {
                    rfc2231.addValueChar(c);
                    rfc2231.setState(RFC2231State.VALUE);
                }
            } else if (rfc2231.state == RFC2231State.EXTENDED) {
                if (c >= '0' && c <= '9') {
                    if (c != '0')
                        rfc2231.setContinued();
                    rfc2231.setState(RFC2231State.CONTINUED);
                } else if (c == '=') {
                    rfc2231.setEncoded();
                    rfc2231.setState(rfc2231.continued ? RFC2231State.VALUE : RFC2231State.CHARSET);
                }
            } else if (rfc2231.state == RFC2231State.CONTINUED) {
                if (c == '=')
                    rfc2231.setState(RFC2231State.EQUALS);
                else if (c == '*')
                    rfc2231.setState(RFC2231State.EXTENDED);
                else if (c >= '0' && c <= '9')
                    rfc2231.setContinued();
            } else if (rfc2231.state == RFC2231State.CHARSET) {
                if (c == '\'')
                    rfc2231.setState(RFC2231State.LANG);
                else
                    rfc2231.addCharsetChar(c);
            } else if (rfc2231.state == RFC2231State.LANG) {
                if (c == '\'')
                    rfc2231.setState(RFC2231State.VALUE);
            } else if (rfc2231.state == RFC2231State.COMMENT) {
                if (escaped)
                    escaped = false;
                else if (c == '\\')
                    escaped = true;
                else if (c == '(')
                    rfc2231.comment++;
                else if (c == ')' && --rfc2231.comment == 0)
                    rfc2231.setState(rfc2231.precomment);
            }
        }

        rfc2231.saveParameter(mParams);
        mValue = mParams.remove(null);
    }

    public MimeCompoundHeader(MimeCompoundHeader mch) {
        mValue = mch.mValue;
        mParams.putAll(mch.mParams);
        use2231Encoding = mch.use2231Encoding;
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
            } else if (use2231Encoding) {
                try {
                    sb.append(param.getKey()).append("*=utf-8''").append(new URLEncoder().encode(value));
                } catch (EncoderException e) { }
            } else {
                try {
                    sb.append(param.getKey()).append("=\"").append(new QEncoder().encode(value, "utf-8")).append('"');
                } catch (EncoderException e) { }
            }

            if (position + sb.length() > LINE_WRAP_LENGTH) {
                line.append("\r\n\t");  position = 8;
            } else {
                line.append(' ');  position++;
            }
            line.append(sb);  position += sb.length();
        }
        return line.append("\r\n").toString();
    }

    // special subclass of URLCodec that replaces ' ' with "%20" rather than with "+"
    private static class URLEncoder extends URLCodec {
        private static final BitSet WWW_URL = (BitSet) WWW_FORM_URL.clone();
            static { WWW_URL.clear(' '); }
        public byte[] encode(byte[] bytes)  { return encodeUrl(WWW_URL, bytes); }
    }
    private static class QEncoder extends QCodec {
        private QEncoder()  { super();  setEncodeBlanks(true); }
    }

    private class RFC2231Data {
        RFC2231State state = RFC2231State.EQUALS;
        StringBuilder key = null;
        StringBuilder value = new StringBuilder();
        boolean continued = false;
        private boolean encoded = false;
        private StringBuilder charset = null;
        int comment = 0;
        RFC2231State precomment;
    
        void setState(RFC2231State newstate) {
            if (newstate == RFC2231State.COMMENT && state != RFC2231State.COMMENT)
                precomment = state;
            state = newstate;
            if (newstate == RFC2231State.PARAM) {
                key = new StringBuilder();  value = new StringBuilder();
                continued = false;  encoded = false;
            }
        }

        void setContinued()  { continued = true; }
        void setEncoded() {
            encoded = true;
            if (!continued)
                charset = new StringBuilder();
        }
    
        void addCharsetChar(char c)  { charset.append(c); }
        void addKeyChar(char c)      { key.append(c); }
        void addValueChar(char c)    { value.append(c); }
    
        void saveParameter(Map<String, String> attrs) {
            if (value == null)
                return;
            String pname = key == null ? null : key.toString().toLowerCase();
            String pvalue = value.toString();
            if ("".equals(pname) && "".equals(pvalue))
                return;
            if (encoded) {
                if (charset.length() == 0)
                    charset.append("us-ascii");
                try {
                    pvalue = URLDecoder.decode(pvalue, charset.toString());
                } catch (UnsupportedEncodingException uee) { 
                    System.out.println(uee);
                }
            } else if (pvalue.lastIndexOf("=?") > 0 || pvalue.indexOf("?=") < pvalue.length() - 2) {
                try {
                    pvalue = MimeUtility.decodeText(pvalue);
                } catch (Exception e) { }
            } else if (pvalue.startsWith("=?") && pvalue.endsWith("?=")) {
                try {
                    pvalue = MimeUtility.decodeWord(pvalue);
                } catch (Exception e) { }
            }
            String existing = continued ? attrs.get(pname) : null;
            attrs.put(pname, existing == null ? pvalue : existing + pvalue);
            key = null;  value = null;
        }
    }

    /** Printable ASCII characters that must be quoted in parameter values and avoided in parameter names. */
    private static final boolean[] TSPECIALS = new boolean[128];
        static {
            TSPECIALS['('] = TSPECIALS[')'] = TSPECIALS['<'] = TSPECIALS['>']  = true;
            TSPECIALS[','] = TSPECIALS[';'] = TSPECIALS[':'] = TSPECIALS['\\'] = true;
            TSPECIALS['/'] = TSPECIALS['['] = TSPECIALS[']'] = TSPECIALS['?']  = true;
            TSPECIALS['@'] = TSPECIALS['"'] = TSPECIALS['='] = TSPECIALS[' ']  = true;
        }


    public static void main(String[] args) {
        MimeCompoundHeader mch;
        mch = new ContentType("text/plain; charset=US-ASCII;\r\n\tFormat=Flowed   DelSp=Yes\r\n");
        System.out.println(mch.toString("Content-Type"));

        mch = new ContentDisposition("   \n  INline;\n filename=\"=?utf-8?Q?=E3=82=BD=E3=83=AB=E3=83=86=E3=82=A3=E3=83=AC=E3=82=A4.rtf?=\"\n  \n ", false);
        System.out.println(mch.toString("Content-Disposition"));
        mch = new ContentDisposition("   \n  gropp;\n filename*=UTF-8''%E3%82%BD%E3%83%AB%E3%83%86%E3%82%A3%E3%83%AC%E3%82%A4.rtf\n  \n ", true);
        System.out.println(mch.toString("Content-Disposition"));
        mch = new ContentDisposition(mch.toString());
        System.out.println(mch.toString("Content-Disposition"));

        mch = new ContentType("application/x-stuff; title*0*=us-ascii'en'This%20is%20even%20more%20; title*1*=%2A%2A%2Afun%2A%2A%2A%20; title*2=\"isn't it!\"\n");
        System.out.println(mch.toString("Content-Type"));
        mch = new ContentType("multipart/MIXED; charset=us-ascii;\n foo=\n  boundary=\"---\" \n");
        System.out.println(mch.toString("Content-Type"));
        mch = new ContentType("message/external-body; access-type=URL;\n URL*0=\"ftp://\";\n URL*1=\"cs.utk.edu/pub/moore/bulk-mailer/bulk-mailer.tar\"\n");
        System.out.println(mch.toString("Content-Type"));
        mch = new ContentType("application/x-stuff;\n\ttitle*=us-ascii'en-us'This%20is%20%2A%2A%2Afun%2A%2A%2A");
        System.out.println(mch.toString("Content-Type"));
        mch = new ContentType("application/pdf;\n    x-unix-mode=0644;\n    name=Zimbra on Mac OS X success story.pdf");
        System.out.println(mch.toString("Content-Type"));
        mch = new ContentType("c; name=TriplePlay_Converged_Network_v5.pdf;\n x-mac-creator=70727677; x-mac-type=50444620");
        System.out.println(mch.toString("Content-Type"));
        mch = new ContentType("text;\n pflaum;=foo; name=\"spam\\\"bag\\\\wall\" \n\t((plain; text=missing); (pissed=off); where=myrtle);;a=b;c;=d;\n (a)foo=bar");
        System.out.println(mch.toString("Content-Type"));
        mch = new ContentType((String) null);
        System.out.println(mch.toString("Content-Type"));

        mch = new ContentDisposition("attachment; filename*0*=ISO-8859-1''BASE%20INICIAL%20CAMPANHA%20PROVIS%C3O%20ABAIXO; filename*1*=%20DE%20ZERO%2009_10_06%20SUCHY.xls");
        System.out.println(mch.toString("Content-Disposition"));
        mch = new ContentDisposition("attachment;\n filename=\"=?iso-8859-1?Q?BASE_INICIAL_CAMPANHA_PROVIS=C3O_ABAIXO_DE_ZERO_09=5F10=5F?=\n =?iso-8859-1?Q?06_SUCHY=2Exls?=\"");
        System.out.println(mch.toString("Content-Disposition"));
    }
}
