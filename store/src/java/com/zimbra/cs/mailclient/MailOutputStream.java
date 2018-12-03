/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016, 2018 Synacor, Inc.
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
package com.zimbra.cs.mailclient;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.OutputStream;

import java.nio.charset.StandardCharsets;
import com.zimbra.common.util.Log;

/**
 * An output stream for writing line-oriented mail protocol data.
 */
public class MailOutputStream extends OutputStream {
    private Log log;
    private ByteArrayOutputStream logbuf;
    private boolean privacy = false;

    /** The underlying output stream */
    protected final OutputStream out;

    /**
     * Creates a new {@link MailOutputStream} for the specified underlying output stream.
     *
     * @param out the underlying output stream
     */
    public MailOutputStream(OutputStream out) {
        this.out = out;
    }

    public MailOutputStream(OutputStream out, Log log) {
        this(out);
        this.log = log;
        logbuf = new ByteArrayOutputStream(1024);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (logbuf != null) {
            if (privacy) {
                for (int i = 0; i < len; i++) {
                    logbuf.write('*');
                }
            } else {
                logbuf.write(b, off, len);
            }
        }
        out.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        if (logbuf != null) {
            if (privacy) {
                logbuf.write('*');
            } else {
                logbuf.write(b);
            }
        }
        out.write(b);
    }

    /**
     * Writes the specified ASCII/UTF8 string to the output stream.
     *
     * @param s the string that is to be written
     * @throws IOException if an I/O error occurs
     */
    public void write(String s) throws IOException {
        try {
            byte b[] = s.getBytes(StandardCharsets.UTF_8);
            for (int i = 0; i < b.length; i++) {
                write(b[i]);
            }
        }
        catch (UnsupportedEncodingException e) {
             System.out.println("Unsupported character set: " + e); 
         } 
    }

    /**
     * Writes a line of ASCII characters to the output stream. The line will
     * be terminated CRLF.
     *
     * @param s the line that is to be written
     * @throws IOException if an I/O error occurs
     */
    public void writeLine(String s) throws IOException {
        write(s);
        newLine();
    }

    /**
     * Writes a terminating CRLF to the output stream.
     *
     * @throws IOException if an I/O error occurs
     */
    public void newLine() throws IOException {
        write('\r');
        write('\n');
    }

    @Override
    public void flush() throws IOException {
        out.flush();
    }

    @Override
    public void close() throws IOException {
        out.close();
    }

    /**
     * Mask the trace log.
     *
     * @param value true to start masking, false to stop masking
     */
    public final void setPrivacy(boolean value) {
        privacy = value;
    }

    public final void trace() {
        if (logbuf == null || logbuf.size() == 0) {
            return;
        }
        if (log.isTraceEnabled()) {
            log.trace("C: %s", logbuf.toString().trim());
        }
        logbuf.reset();
    }

}
