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

import java.io.File;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.activation.DataSource;

import com.zimbra.common.mime.MimePart.PartSource;

public class MimeParserInputStream extends FilterInputStream {
    private MimeParser parser;
    private MimePart.PartSource psource;

    public MimeParserInputStream(InputStream in) {
        super(in);
        parser = new MimeParser();
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


    public MimeParserInputStream setSource(byte[] content) {
        psource = content == null ? null : new PartSource(content);
        return this;
    }

    public MimeParserInputStream setSource(File file) {
        psource = file == null || !file.exists() ? null : new PartSource(file);
        return this;
    }

    public MimeParserInputStream setSource(DataSource ds) {
        psource = ds == null ? null : new PartSource(ds);
        return this;
    }

    public MimePart getPart() {
        return parser.getPart().attachSource(psource);
    }

    <T extends MimeMessage> T insertBodyPart(T mm) {
        mm.setBodyPart(getPart());
        mm.recordEndpoint(parser.getPosition(), parser.getLineNumber());
        mm.attachSource(psource);
        return mm;
    }

    public MimeMessage getMessage(Properties props) {
        MimeMessage mm = new MimeMessage(getPart(), props);
        mm.recordEndpoint(parser.getPosition(), parser.getLineNumber());
        mm.attachSource(psource);
        return mm;
    }
}
