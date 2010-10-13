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
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import javax.activation.DataSource;

import com.zimbra.common.mime.MimePart.PartSource;

public class MimeParserOutputStream extends FilterOutputStream {
    private MimeParser parser;
    private MimePart.PartSource psource;

    public MimeParserOutputStream(OutputStream out) {
        super(out);
        parser = new MimeParser();
    }

    @Override public void write(int b) throws IOException {
        super.write(b);
        if (b != -1) {
            parser.handleByte((byte) b);
        }
    }

    // don't override write(byte[], int, int), since the superclass method
    //   just invokes write(int) and that'd trickle down into double calls
    //   to parser.handleByte()

    @Override public void close() throws IOException {
        super.close();
        parser.endParse();
    }


    public MimeParserOutputStream setSource(byte[] content) {
        psource = content == null ? null : new PartSource(content);
        return this;
    }

    public MimeParserOutputStream setSource(File file) {
        psource = file == null || !file.exists() ? null : new PartSource(file);
        return this;
    }

    public MimeParserOutputStream setSource(DataSource ds) {
        psource = ds == null ? null : new PartSource(ds);
        return this;
    }

    public MimePart getPart() {
        return parser.getPart().attachSource(psource);
    }

    public MimeMessage getMessage(Properties props) {
        MimeMessage mm = new MimeMessage(getPart(), props);
        mm.recordEndpoint(parser.getPosition(), parser.getLineNumber());
        mm.attachSource(psource);
        return mm;
    }
}
