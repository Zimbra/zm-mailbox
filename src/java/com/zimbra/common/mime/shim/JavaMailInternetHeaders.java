/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.common.mime.shim;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;

import com.google.common.collect.ImmutableSet;

public class JavaMailInternetHeaders extends InternetHeaders implements JavaMailShim {
    private static final boolean ZPARSER = JavaMailMimeMessage.ZPARSER;

    com.zimbra.common.mime.MimeHeaderBlock zheaders;
    private String defaultCharset;

    JavaMailInternetHeaders(com.zimbra.common.mime.MimeHeaderBlock headers) {
        this(headers, null);
    }

    JavaMailInternetHeaders(com.zimbra.common.mime.MimeHeaderBlock headers, String charset) {
        zheaders = headers;
        defaultCharset = charset;
    }

    public JavaMailInternetHeaders() {
        super();
        if (ZPARSER) {
            zheaders = new com.zimbra.common.mime.MimeHeaderBlock(false);
        }
    }

    public JavaMailInternetHeaders(InputStream is) throws MessagingException {
        super();
        if (ZPARSER) {
            try {
                zheaders = new com.zimbra.common.mime.MimeHeaderBlock(is);
            } catch (IOException ioe) {
                throw new MessagingException("error reading InternetHeaders", ioe);
            }
        } else {
            this.headers = new ArrayList<InternetHeader>(40); 
            load(is);
        }
    }

    @SuppressWarnings("unchecked")
    JavaMailInternetHeaders(InternetHeaders jmheaders) {
        this();
        for (Enumeration<InternetHeader> en = jmheaders.getAllHeaders(); en.hasMoreElements(); ) {
            InternetHeader jmheader = en.nextElement();
            addHeader(jmheader.getName(), jmheader.getValue());
        }
    }


    com.zimbra.common.mime.MimeHeaderBlock getZimbraMimeHeaderBlock() {
        return zheaders;
    }

    @Override public void load(InputStream is) throws MessagingException {
        if (ZPARSER) {
            try {
                zheaders.appendAll(new com.zimbra.common.mime.MimeHeaderBlock(is));
            } catch (IOException ioe) {
                throw new MessagingException("error reading header block", ioe);
            }
        } else {
            super.load(is);
        }
    }

    @Override public String[] getHeader(String name) {
        if (ZPARSER) {
            List<com.zimbra.common.mime.MimeHeader> matches = zheaders.getAll(name);
            if (matches == null || matches.isEmpty()) {
                return null;
            } else {
                int i = 0;
                String[] values = new String[matches.size()];
                for (com.zimbra.common.mime.MimeHeader header : matches) {
                    values[i++] = header.getEncodedValue(defaultCharset);
                }
                return values;
            }
        } else {
            return super.getHeader(name);
        }
    }

    @Override public String getHeader(String name, String delimiter) {
        if (ZPARSER) {
            if (delimiter == null) {
                String[] values = getHeader(name);
                return values == null || values.length == 0 ? null : values[0];
            } else {
                // the superclass method calls our getHeader(String)
                return super.getHeader(name, delimiter);
            }
        } else {
            return super.getHeader(name, delimiter);
        }
    }

    @Override public void setHeader(String name, String value) {
        if (ZPARSER) {
            zheaders.setHeader(name, value.getBytes());
        } else {
            super.setHeader(name, value);
        }
    }

    private static final Set<String> RESENT_HEADERS = ImmutableSet.of(
            "resent-date", "resent-from", "resent-sender", "resent-to", "resent-cc", "resent-bcc", "resent-message-id"
    );

    @SuppressWarnings("unchecked")
    @Override public void addHeader(String name, String value) {
        if (ZPARSER) {
            zheaders.addHeader(name, value.getBytes());
        } else {
            if (name != null && RESENT_HEADERS.contains(name.toLowerCase())) {
                headers.add(0, new InternetHeader(name, value));
            } else {
                super.addHeader(name, value);
            }
        }
    }

    @Override public void removeHeader(String name) {
        if (ZPARSER) {
            zheaders.setHeader(name, (String) null);
        } else {
            super.removeHeader(name);
        }
    }

    private static final String[] NO_HEADERS = new String[0];

    private Enumeration<Header> enumerateHeaders(boolean match, String[] names) {
        if (names == null) {
            names = NO_HEADERS;
        }
        List<InternetHeader> jmheaders = new ArrayList<InternetHeader>();
        for (com.zimbra.common.mime.MimeHeader header : zheaders) {
            int i = 0;
            for ( ; i < names.length; i++) {
                if (header.getName().equalsIgnoreCase(names[i])) {
                    break;
                }
            }
            if (match == (i != names.length)) {
                jmheaders.add(new InternetHeader(header.getName(), header.getValue(defaultCharset)));
            }
        }
        return new IteratorEnumeration<Header>(jmheaders);
    }

    @SuppressWarnings("unchecked")
    @Override public Enumeration<Header> getAllHeaders() {
        if (ZPARSER) {
            return enumerateHeaders(false, NO_HEADERS);
        } else {
            return super.getAllHeaders();
        }
    }

    @SuppressWarnings("unchecked")
    @Override public Enumeration<Header> getMatchingHeaders(String[] names) {
        if (ZPARSER) {
            return enumerateHeaders(true, names);
        } else {
            return super.getMatchingHeaders(names);
        }
    }

    @SuppressWarnings("unchecked")
    @Override public Enumeration<Header> getNonMatchingHeaders(String[] names) {
        if (ZPARSER) {
            return enumerateHeaders(false, names);
        } else {
            return super.getNonMatchingHeaders(names);
        }
    }

    @Override public void addHeaderLine(String line) {
        if (ZPARSER) {
            if (line == null || line.isEmpty()) {
                return;
            } else if (line.charAt(0) == ' ' || line.charAt(0) == '\t') {
                throw new UnsupportedOperationException("adding continuation lines not yet supported");
            } else {
                byte contents[] = line.getBytes(), b;
                int colon, start, end, wsp;
                for (colon = 0; colon < contents.length; colon++) {
                    if (contents[colon] == ':') {
                        break;
                    }
                }
                if (colon == contents.length || colon == 0) {
                    return;
                }
                String name = new String(contents, 0, colon).trim();
                if (name.isEmpty()) {
                    return;
                }
                for (start = colon + 1, wsp = 0; start < contents.length; start++) {
                    if ((b = contents[start]) != '\n' && b != '\r' && ((b != ' ' && b != '\t') || ++wsp >= 2)) {
                        break;
                    }
                }
                for (end = contents.length - 1; end > start; end--) {
                    if ((b = contents[end]) != '\r' && b != '\n') {
                        break;
                    }
                }
                byte[] bvalue = new byte[end - start + 1];
                System.arraycopy(contents, start, bvalue, 0, end - start + 1);
                zheaders.appendHeader(name, bvalue);
            }
        } else {
            super.addHeaderLine(line);
        }
    }

    private Enumeration<String> enumerateHeaderLines(boolean match, String[] names) {
        List<String> jmheaders = new ArrayList<String>();
        for (com.zimbra.common.mime.MimeHeader header : zheaders) {
            int i = 0;
            for ( ; i < names.length; i++) {
                if (header.getName().equalsIgnoreCase(names[i])) {
                    break;
                }
            }
            if (match == (i != names.length)) {
                jmheaders.add(new String(header.getRawHeader()).trim());
            }
        }
        return new IteratorEnumeration<String>(jmheaders);
    }

    @SuppressWarnings("unchecked")
    @Override public Enumeration<String> getAllHeaderLines() {
        if (ZPARSER) {
            return enumerateHeaderLines(false, NO_HEADERS);
        } else {
            return super.getAllHeaderLines();
        }
    }

    @SuppressWarnings("unchecked")
    @Override public Enumeration<String> getMatchingHeaderLines(String[] names) {
        if (ZPARSER) {
            return enumerateHeaderLines(true, names);
        } else {
            return super.getMatchingHeaderLines(names);
        }
    }

    @SuppressWarnings("unchecked")
    @Override public Enumeration<String> getNonMatchingHeaderLines(String[] names) {
        if (ZPARSER) {
            return enumerateHeaderLines(false, names);
        } else {
            return super.getNonMatchingHeaderLines(names);
        }
    }


    static class IteratorEnumeration<T> implements Enumeration<T> {
        private final Iterator<? extends T> mIterator;

        IteratorEnumeration(Iterator<? extends T> iterator) {
            mIterator = iterator;
        }

        IteratorEnumeration(Iterable<? extends T> iterable) {
            mIterator = iterable.iterator();
        }

        @Override public boolean hasMoreElements() {
            return mIterator.hasNext();
        }

        @Override public T nextElement() {
            return mIterator.next();
        }
    }

    public static void main(String... args) {
        JavaMailInternetHeaders jmheaders = new JavaMailInternetHeaders();
        jmheaders.addHeaderLine("Subject: foo");
        jmheaders.addHeaderLine("From: Prue Loo <prue@example.com>\r\n");
        jmheaders.addHeaderLine("\r\n");
        jmheaders.addHeader("X-Mailer", "mailbot 3000");
        jmheaders.setHeader("Subject", "floo");

        Enumeration<String> elines = jmheaders.getAllHeaderLines();
        while (elines.hasMoreElements()) {
            System.out.println(elines.nextElement());
        }

//        Enumeration<InternetHeader> eheaders = jmheaders.getAllHeaders();
//        while (eheaders.hasMoreElements()) {
//            System.out.println(eheaders.nextElement().getValue());
//        }
    }
}
