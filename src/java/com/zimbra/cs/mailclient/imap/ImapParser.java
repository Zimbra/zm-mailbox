package com.zimbra.cs.mailclient.imap;

import com.zimbra.cs.mailclient.util.Io;
import com.zimbra.cs.mailclient.ParseException;

import java.io.InputStream;
import java.io.IOException;
import java.io.EOFException;
import java.io.OutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.ArrayList;

/**
 * IMAP protocol parser.
 */
public class ImapParser {
    private InputStream mInputStream;
    private int mNextChar = -1;
    private StringBuilder mBuffer = new StringBuilder();
    private ImapConfig mConfig;

    private static final boolean DEBUG = false;

    public ImapParser(InputStream is, ImapConfig config) {
        mInputStream = is;
        mConfig = config;
    }

    public ImapData readAtom() throws IOException {
        ImapData id = ImapData.text(readChars(Chars.ATOM_CHARS));
        if (DEBUG) pd("readAtom: %s", id);
        return id;
    }

    public ImapData readNString() throws IOException {
        ImapData id = readAString();
        if (id.isText() && !id.isNil()) {
            throw new IOException("Expecting NIL but got: " + id);
        }
        return id;
    }
    
    public long readNZNumber() throws IOException {
        return readNumber(true);
    }

    public long readNumber() throws IOException {
        return readNumber(false);
    }

    private long readNumber(boolean nz) throws IOException {
        String s = readChars(Chars.NUMBER_CHARS);
        long n = 0;
        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            n = n * 10 + (c - '0');
            if (n > 0xffffffffL) {
                throw new ParseException("Number is too big: " + s);
            }
        }
        if (nz && n == 0) {
            throw new ParseException("Expected non-zero number but got " + s);
        }
        if (DEBUG) pd("readNumber: %d", n);
        return n;
    }

    public ImapData readAString() throws IOException {
        ImapData id;
        switch (peekChar()) {
        case '"':
            id = readQuoted();
            break;
        case '{':
            id = readLiteral();
            break;
        default:
            id = ImapData.text(readChars(Chars.ASTRING_CHARS));
        }
        if (DEBUG) pd("readAString: %s", id);
        return id;
    }

    public ImapData readList() throws IOException {
        skipChar('(');
        List<ImapData> list = new ArrayList<ImapData>();
        while (peekChar() != ')') {
            if (list.size() > 0) skipChar(' ');
            if (peekChar() == '(') {
                list.add(readList());
            } else {
                list.add(readAString());
            }
        }
        skipChar(')');
        return ImapData.list(list);
    }

    // Reads a sequence of one or more atoms.
    public ImapData readAtoms() throws IOException {
        List<ImapData> atoms = new ArrayList<ImapData>();
        while (true) {
            atoms.add(readAtom());
            if (!isSpace()) break;
            skipSpace();
        }
        return ImapData.list(atoms);
    }
    
    public void skipAtom(Atom atom) throws IOException {
        Atom a = readAtom().getAtomValue();
        if (a != atom) {
            throw new ParseException(
                "Expected atom '" + atom + "' but got '" + a + '"');
        }
    }

    private ImapData readQuoted() throws IOException {
        skipChar('"');
        mBuffer.setLength(0);
        char c;
        while ((c = readChar()) != '"') {
            switch (c) {
            case '\r': case '\n':
                throw new ParseException(
                    "Unexpected character in quoted string: " + Chars.pp(c));
            case '\\':
                c = readChar();
            }
            mBuffer.append(c);
        }
        return ImapData.quoted(mBuffer.toString());
    }

    private ImapData readLiteral() throws IOException {
        skipChar('{');
        long len = readNumber();
        if (len > Integer.MAX_VALUE) {
            throw new ParseException("Literal size too large: " + len);
        }
        skipChar('}');
        skipCRLF();
        return ImapData.literal(readLiteral((int) len));
    }

    private Literal readLiteral(int len) throws IOException {
        if (len <= mConfig.getMaxLiteralMemSize()) {
            // Cache literal data in memory
            byte[] b = new byte[len];
            Io.readFully(mInputStream, b);
            return new Literal(b);
        }
        // Otherwise, use temporary file for literal data, which will be
        // automatically cleaned up when the ImapResponse is finished.
        File f = File.createTempFile("lit", null, mConfig.getLiteralDataDir());
        f.deleteOnExit();
        OutputStream os = new FileOutputStream(f);
        try {
            Io.copyBytes(mInputStream, os, len);
        } finally {
            os.close();
        }
        return new Literal(f);
    }


    public ImapData readText() throws IOException {
        return ImapData.text(readChars(Chars.TEXT_CHARS));
    }
    
    public ImapData readText(char delim) throws IOException {
        mBuffer.setLength(0);
        while (!isEOF() && Chars.TEXT_CHARS[peek()] && peek() != delim) {
            mBuffer.append(read());
        }
        if (mBuffer.length() == 0) {
            throw new ParseException("Missing character content");
        }
        return ImapData.text(mBuffer.toString());
    }

    private String readChars(boolean[] chars) throws IOException {
        mBuffer.setLength(0);
        while (!isEOF() && chars[peekChar()]) {
            mBuffer.append(readChar());
        }
        if (mBuffer.length() == 0) {
            throw new ParseException("Missing character content");
        }
        return mBuffer.toString();
    }

    public void skipChar(char c) throws IOException {
        if (peekChar() != c) {
            throw new ParseException(
                "Expected character '" + Chars.pp(c) + "' but got '" +
                Chars.pp(peekChar()) + "'");
        }
        read();
    }

    public void skipSpace() throws IOException {
        skipChar(' ');
    }

    public void skipCRLF() throws IOException {
        skipChar('\r');
        skipChar('\n');
    }
    
    public boolean isSpace() throws IOException {
        return peek() == ' ';
    }

    public char readChar() throws IOException {
        int c = read();
        if (c == -1) throw new EOFException();
        return (char) c;
    }

    public char peekChar() throws IOException {
        int c = peek();
        if (c == -1) throw new EOFException();
        return (char) c;
    }
    
    public int read() throws IOException {
        if (mNextChar != -1) {
            int ch = mNextChar;
            mNextChar = -1;
            return ch;
        }
        return mInputStream.read();
    }

    public int peek() throws IOException {
        if (mNextChar == -1) {
            mNextChar = mInputStream.read();
        }
        return mNextChar;
    }

    public boolean isEOF() throws IOException {
        return peek() == -1;
    }

    private static void pd(String fmt, Object... args) {
        System.out.print("[DEBUG] ");
        System.out.printf(fmt, args);
        System.out.println();
    }
}
