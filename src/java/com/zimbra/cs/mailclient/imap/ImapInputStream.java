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
package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.util.Io;
import com.zimbra.cs.mailclient.util.Ascii;
import com.zimbra.cs.mailclient.ParseException;
import com.zimbra.cs.mailclient.MailInputStream;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.EOFException;

/**
 * An input stream for reading IMAP response data.
 */
public final class ImapInputStream extends MailInputStream {
    private ImapConfig config;

    private static final boolean DEBUG = false;

    public ImapInputStream(InputStream is, ImapConfig config) {
        super(is);
        this.config = config;
    }

    public Atom readAtom() throws IOException {
        Atom atom = new Atom(readChars(Chars.ATOM_CHARS));
        if (DEBUG) pd("readAtom: %s", atom);
        return atom;
    }

    public Atom readFlag() throws IOException {
        sbuf.setLength(0);
        if (peek() == '\\') {
            sbuf.append((char) read());
            if (peek() == '*') {
                return new Atom(sbuf.append((char) read()).toString());
            }
        }
        return new Atom(readChars(sbuf, Chars.ATOM_CHARS).toString());
    }
    
    public String readString() throws IOException {
        return readStringData().toString();
    }
    
    public ImapData readStringData() throws IOException {
        ImapData as = readAStringData();
        if (!as.isString()) {
            throw new ParseException("Expected STRING but got " + as);
        }
        return as;
    }

    public String readNString() throws IOException {
        ImapData ns = readNStringData();
        return ns.isNil() ? null : ns.toString();
    }
    
    public ImapData readNStringData() throws IOException {
        ImapData as = readAStringData();
        if (!as.isNString()) {
            throw new ParseException("Expected NIL or STRING, but got: " + as);
        }
        return as.isNil() ? null : as;
    }

    public String readAString() throws IOException {
        return readAStringData().toString();
    }
    
    public ImapData readAStringData() throws IOException {
        ImapData as;
        switch (peekChar()) {
        case '"':
            as = readQuoted();
            break;
        case '{':
            as = readLiteral();
            break;
        default:
            as = new Atom(readChars(Chars.ASTRING_CHARS));
        }
        if (DEBUG) pd("readAString: %s (%s)", as, as.getType());
        return as;
    }

    public boolean isNumber() throws IOException {
        return !isEOF() && Chars.isNumber(peekChar());
    }
    
    public long readNZNumber() throws IOException {
        return readNumber(true);
    }

    public long readNumber() throws IOException {
        return readNumber(false);
    }

    private long readNumber(boolean nz) throws IOException {
        String s = readChars(Chars.NUMBER_CHARS);
        long n = Chars.getNumber(s);
        if (n == -1) {
            throw new ParseException("Invalid number format: " + s);
        }
        if (nz && n == 0) {
            throw new ParseException("Expected non-zero number but got " + s);
        }
        if (DEBUG) pd("readNumber: %d", n);
        return n;
    }

    public Quoted readQuoted() throws IOException {
        skipChar('"');
        sbuf.setLength(0);
        int c;
        while ((c = read()) != '"') {
            switch (c) {
            case '\r': case '\n':
                throw new ParseException(
                    "Unexpected end of line while reading QUOTED string");
            case -1:
                throw new EOFException(
                    "Unexpected end of stream while reading QUOTED string");
            case '\\':
                c = readChar();
            }
            sbuf.append((char) c);
        }
        return new Quoted(sbuf.toString());
    }

    private Literal readLiteral() throws IOException {
        skipChar('{');
        long len = readNumber();
        if (len > Integer.MAX_VALUE) {
            throw new ParseException("Literal size too large: " + len);
        }
        if (DEBUG) pd("readLiteral: size = %d", len);
        skipChar('}');
        skipCRLF();
        return readLiteral((int) len);
    }

    private Literal readLiteral(int len) throws IOException {
        if (len <= config.getMaxLiteralMemSize()) {
            // Cache literal data in memory
            byte[] b = new byte[len];
            Io.readFully(in, b);
            return new Literal(b);
        }
        // Otherwise, use temporary file for literal data, which will be
        // automatically cleaned up when the ImapResponse is finished.
        File f = File.createTempFile("lit", null, config.getLiteralDataDir());
        f.deleteOnExit();
        OutputStream os = new FileOutputStream(f);
        try {
            Io.copyBytes(in, os, len);
        } finally {
            os.close();
        }
        return new Literal(f);
    }


    public String readText() throws IOException {
        return readChars(Chars.TEXT_CHARS);
    }
    
    public String readText(char delim) throws IOException {
        sbuf.setLength(0);
        char c;
        while ((c = peekChar()) != delim) {
            if (!Chars.isText(c)) {
                throw new ParseException(
                    "Unexpected character '" + Ascii.pp((byte) c) +
                    "' while reading TEXT string");
            }
            sbuf.append((char) read());
        }
        return sbuf.toString();
    }

    public String readChars(boolean[] chars) throws IOException {
        sbuf.setLength(0);
        return readChars(sbuf, chars).toString();
    }

    public StringBuilder readChars(StringBuilder sb, boolean[] chars)
            throws IOException {
        while (chars[peekChar()]) {
            sb.append((char) read());
        }
        return sb;
    }

    public void skipChar(char expectedChar) throws IOException {
        char c = readChar();
        if (c != expectedChar) {
            throw new ParseException(
                "Unexpected character '" + Ascii.pp((byte) c) +
                "' (expecting '" + Ascii.pp((byte) expectedChar) + "')");
        }
    }

    public void skipNil() throws IOException {
        Atom a = readAtom();
        if (!a.isNil()) {
            throw new ParseException("Expecting NIL but got " + a);
        }
    }
    
    public void skipCRLF() throws IOException {
        skipChar('\r');
        skipChar('\n');
    }

    /**
     * If next character in stream matches specified character then read it
     * and return true. Otherwise, return false.
     * 
     * @param c the expected character
     * @return true if next character matches, false otherwise
     * @throws IOException an an I/O error occurs
     */
    public boolean match(char c) throws IOException {
        if (peek() == c) {
            read();
            return true;
        }
        return false;
    }

    private static void pd(String fmt, Object... args) {
        System.out.print("[DEBUG] ");
        System.out.printf(fmt, args);
        System.out.println();
    }
}
