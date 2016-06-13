/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2013, 2014, 2016 Synacor, Inc.
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

    public MimeParserOutputStream setSource(MimePart.InputStreamSource iss) {
        psource = iss == null ? null : new PartSource(iss);
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
