/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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
package com.zimbra.cs.mailclient;

import java.io.IOException;
import java.io.OutputStream;

public class MailOutputStream extends OutputStream {
    private final OutputStream os;

    public MailOutputStream(OutputStream os) {
        this.os = os;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        os.write(b, off, len);
    }

    public void write(int b) throws IOException {
        os.write(b);
    }
    
    public void write(String s) throws IOException {
        int len = s.length();
        for (int i = 0; i < len; i++) {
            write(s.charAt(i));
        }
    }

    public void writeLine(String s) throws IOException {
        write(s);
        newLine();
    }

    public void newLine() throws IOException {
        write('\r');
        write('\n');
    }

    public void flush() throws IOException {
        os.flush();
    }

    public void close() throws IOException {
        os.close();
    }
}
