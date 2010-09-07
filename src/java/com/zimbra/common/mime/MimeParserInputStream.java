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
package com.zimbra.common.mime;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MimeParserInputStream extends FilterInputStream {
    private MimeParser parser;

    public MimeParserInputStream(InputStream in) {
        this(in, null);
    }

    public MimeParserInputStream(InputStream in, Properties props) {
        super(in);
        parser = new MimeParser(props);
    }

    @Override public int read() throws IOException {
        int b = super.read();
        if (b != -1) {
            parser.handleByte((byte) b);
        }
        return b;
    }

    @Override public int read(byte[] b, int off, int len) throws IOException {
        int amt = super.read(b, off, len);
        if (amt != -1) {
            parser.handleBytes(b, off, amt);
        }
        return amt;
    }

    @Override public long skip(long n) throws IOException {
        long remaining = n;
        int max = (int) Math.min(n, 32768), read = 0;
        final byte buffer[] = new byte[max];

        while (remaining > 0 && (read = read(buffer, 0, (int) Math.min(remaining, max))) != -1) {
            remaining -= read;
        }
        return n - remaining;
    }

    @Override public void close() throws IOException {
        super.close();
        parser.endParse();
    }

    public MimeMessage getMessage() {
        return parser.getMessage();
    }


    public static void main(String... args) throws IOException {
        java.io.File file = new java.io.File("/Users/dkarp/Documents/messages/unused/undisplayed-generated");
        MimeParserInputStream mpis = new MimeParserInputStream(new java.io.FileInputStream(file));
        try {
            com.zimbra.common.util.ByteUtil.drain(mpis);

            MimeMessage mm = mpis.getMessage();
            mm.setContent(file);
            System.out.println("*** message structure ***");
            MimeMessage.dumpParts(mm);

            MimeParser.checkMessage(mm, file);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mpis.close();
        }
    }
}
