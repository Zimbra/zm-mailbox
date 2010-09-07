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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

public class MimeParserOutputStream extends FilterOutputStream {
    private MimeParser parser;

    public MimeParserOutputStream(OutputStream out) {
        this(out, null);
    }

    public MimeParserOutputStream(OutputStream out, Properties props) {
        super(out);
        parser = new MimeParser(props);
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

    public MimeMessage getMessage() {
        return parser.getMessage();
    }
}
