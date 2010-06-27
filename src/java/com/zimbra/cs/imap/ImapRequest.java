/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

/*
 * Created on Apr 30, 2005
 */
package com.zimbra.cs.imap;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.imap.ImapSearch.AllSearch;
import com.zimbra.cs.imap.ImapSearch.AndOperation;
import com.zimbra.cs.imap.ImapSearch.ContentSearch;
import com.zimbra.cs.imap.ImapSearch.DateSearch;
import com.zimbra.cs.imap.ImapSearch.FlagSearch;
import com.zimbra.cs.imap.ImapSearch.HeaderSearch;
import com.zimbra.cs.imap.ImapSearch.LogicalOperation;
import com.zimbra.cs.imap.ImapSearch.ModifiedSearch;
import com.zimbra.cs.imap.ImapSearch.NotOperation;
import com.zimbra.cs.imap.ImapSearch.OrOperation;
import com.zimbra.cs.imap.ImapSearch.RelativeDateSearch;
import com.zimbra.cs.imap.ImapSearch.SequenceSearch;
import com.zimbra.cs.imap.ImapSearch.SizeSearch;
import org.apache.commons.codec.binary.Base64;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class ImapRequest {
    private static final boolean[] TAG_CHARS      = new boolean[128];
    private static final boolean[] ATOM_CHARS     = new boolean[128];
    private static final boolean[] ASTRING_CHARS  = new boolean[128];
    private static final boolean[] PATTERN_CHARS  = new boolean[128];
    private static final boolean[] FETCH_CHARS    = new boolean[128];
    private static final boolean[] NUMBER_CHARS   = new boolean[128];
    private static final boolean[] SEQUENCE_CHARS = new boolean[128];
    private static final boolean[] BASE64_CHARS   = new boolean[128];
    private static final boolean[] SEARCH_CHARS   = new boolean[128];
    static {
        for (int i = 0x21; i < 0x7F; i++)
            if (i != '(' && i != ')' && i != '{' && i != '%' && i != '*' && i != '"' && i != '\\')
                SEARCH_CHARS[i] = FETCH_CHARS[i] = PATTERN_CHARS[i] = ASTRING_CHARS[i] = ATOM_CHARS[i] = TAG_CHARS[i] = true;
        ATOM_CHARS[']'] = false;
        TAG_CHARS['+']  = false;
        PATTERN_CHARS['%'] = PATTERN_CHARS['*'] = true;
        FETCH_CHARS['['] = false;
        SEARCH_CHARS['*'] = true;

        for (int i = 'a'; i <= 'z'; i++)
            BASE64_CHARS[i] = true;
        for (int i = 'A'; i <= 'Z'; i++)
            BASE64_CHARS[i] = true;
        for (int i = '0'; i <= '9'; i++)
            BASE64_CHARS[i] = NUMBER_CHARS[i] = SEQUENCE_CHARS[i] = true;
        SEQUENCE_CHARS['*'] = SEQUENCE_CHARS[':'] = SEQUENCE_CHARS[','] = SEQUENCE_CHARS['$'] = true;
        BASE64_CHARS['+'] = BASE64_CHARS['/'] = true;
    }

    final ImapHandler mHandler;

    String mTag;
    List<Part> mParts = new ArrayList<Part>();
    int mIndex, mOffset;
    private long mSize;
    private boolean mMaxRequestSizeExceeded;
    private boolean mIsAppend;
    private boolean mIsLogin;

    ImapRequest(ImapHandler handler) {
        mHandler = handler;
    }

    ImapRequest rewind() {
        mIndex = mOffset = 0;
        return this;
    }

    void incrementSize(long increment) {
        mSize += increment;
        if (mSize > getMaxRequestSize()) {
            mMaxRequestSizeExceeded = true;
        }
    }

    long getSize() {
        return mSize;
    }

    int getMaxRequestSize() {
        return mHandler.getConfig().getMaxRequestSize();
    }

    protected abstract class Part {
        abstract int size();
        abstract byte[] getBytes() throws IOException;
        boolean isString()   { return false; }
        boolean isLiteral()  { return false; }
        abstract String getString() throws ImapParseException;
        abstract Literal getLiteral() throws ImapParseException;
        void cleanup()  {}
    }

    private class StringPart extends Part {
        private String str;

        StringPart(String s)  { str = s; }

        @Override int size()                { return str.length(); }
        @Override byte[] getBytes()         { return str.getBytes(); }
        @Override boolean isString()        { return true; }
        @Override String getString()        { return str; }
        @Override public String toString()  { return str; }

        @Override Literal getLiteral() throws ImapParseException {
            throw new ImapParseException(mTag, "not inside literal");
        }
    }

    private class LiteralPart extends Part {
        private Literal lit;

        LiteralPart(Literal l)  { lit = l; }

        @Override int size()                            { return lit.size(); }
        @Override byte[] getBytes() throws IOException  { return lit.getBytes(); }
        @Override boolean isLiteral()                   { return true; }
        @Override Literal getLiteral()                  { return lit; }
        @Override void cleanup()                        { lit.cleanup(); }

        @Override public String getString() throws ImapParseException {
            throw new ImapParseException(mTag, "not inside string");
        }

        @Override public String toString() {
            try {
                return new String(lit.getBytes(), "US-ASCII");
            } catch (IOException e) {
                return "???";
            }
        }
    }

    void addPart(Literal literal) {
        addPart(new LiteralPart(literal));
    }

    void addPart(String line) {
        if (mParts.isEmpty()) {
            String cmd = getCommand(line);
            if ("APPEND".equalsIgnoreCase(cmd)) {
                mIsAppend = true;
            } else if ("LOGIN".equalsIgnoreCase(cmd)) {
                mIsLogin = true;
            }
        }
        addPart(new StringPart(line));
    }
        
    private void addPart(Part part) {
        // Do not add any more parts if we have exceeded the maximum request
        // size. The exception is if this is the first part (request line) so
        // we can recover the tag when sending an error response.
        if (!isMaxRequestSizeExceeded() || mParts.isEmpty()) {
            mParts.add(part);
        }
    }

    void cleanup() {
        for (Part part : mParts)
            part.cleanup();
        mParts.clear();
    }

    protected boolean isAppend() {
        return mIsAppend;
    }

    protected boolean isLogin() {
        return mIsLogin;
    }

    protected String getCommand(String requestLine) {
        int i = requestLine.indexOf(' ') + 1;
        if (i > 0) {
            int j = requestLine.indexOf(' ', i);
            if (j > 0) {
                return requestLine.substring(i, j);
            }
        }
        return null;
    }
    
    String getCurrentLine() throws ImapParseException {
        return mParts.get(mIndex).getString();
    }

    boolean isMaxRequestSizeExceeded() {
        return mMaxRequestSizeExceeded;
    }

    byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Part part : mParts) {
            byte[] content = part.getBytes();
            baos.write(content, 0, content.length);
            if (part.isString())
                baos.write(ImapHandler.LINE_SEPARATOR_BYTES, 0, 2);
        }
        return baos.toByteArray();
    }


    String getTag() {
        if (mTag == null && mIndex == 0 && mOffset == 0 && mParts.size() > 0) {
            try {
                readTag();
            } catch (ImapParseException e) {}
            mIndex = 0;
            mOffset = 0;
        }
        return mTag;
    }

    /* Returns whether the specified IMAP extension is enabled for this session.
     * @see ImapHandler#extensionEnabled(String) */
    boolean extensionEnabled(String extension) {
        return mHandler == null || mHandler.extensionEnabled(extension);
    }

    /* Records the "tag" for the request.  This tag will later be used to
     * indicate that the server has finished processing the request.
     * It may also be used when generating a parse exception. */
    void setTag(String tag)  { mTag = tag; }


    String readContent(boolean[] acceptable) throws ImapParseException {
        return readContent(acceptable, false);
    }

    String readContent(boolean[] acceptable, boolean emptyOK) throws ImapParseException {
        String content = getCurrentLine();
        int i;
        for (i = mOffset; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c > 0x7F || !acceptable[c])
                break;
        }
        if (i == mOffset && !emptyOK)
            throw new ImapParseException(mTag, "zero-length content");

        String result = content.substring(mOffset, i);
        mOffset += result.length();
        return result;
    }


    /* Returns whether the read position is at the very end of the request. */
    boolean eof() {
        return mIndex >= mParts.size() || mOffset >= mParts.get(mIndex).size();
    }

    /* Returns the character at the read position, or -1 if we're at the end
     * of a literal or of a line. */
    int peekChar() throws ImapParseException {
        if (mIndex >= mParts.size())
            return -1;
        String str = mParts.get(mIndex).getString();
        return mOffset < str.length() ? str.charAt(mOffset) : -1;
    }

    String peekATOM() {
        int index = mIndex, offset = mOffset;
        try {
            return readATOM();
        } catch (ImapParseException ipe) {
            return null;
        } finally {
            mIndex = index;  mOffset = offset;
        }
    }

    void skipSpace() throws ImapParseException  { skipChar(' '); }

    void skipChar(char c) throws ImapParseException {
        String str = mParts.get(mIndex).getString();
        if (mOffset >= str.length())
            throw new ImapParseException(mTag, "unexpected end of line; expected '" + c + "'");
        char got = str.charAt(mOffset);
        if (got == c) mOffset++;
        else throw new ImapParseException(mTag, "wrong character; expected '" + c + "' but got '" + got + "'");
    }                                       

    void skipNIL() throws ImapParseException  { skipAtom("NIL"); }

    void skipAtom(String atom) throws ImapParseException {
        if (!readATOM().equals(atom))
            throw new ImapParseException(mTag, "did not find expected " + atom);
    }

    String readAtom() throws ImapParseException  { return readContent(ATOM_CHARS); }
    String readATOM() throws ImapParseException  { return readContent(ATOM_CHARS).toUpperCase(); }

    String readQuoted() throws ImapParseException {
        String content = getCurrentLine();
        StringBuffer result = null;

        skipChar('"');
        int backslash = mOffset - 1;
        boolean escaped = false;
        for (int i = mOffset; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c > 0x7F || c == 0x00 || c == '\r' || c == '\n' || (escaped && c != '\\' && c != '"')) {
                throw new ImapParseException(mTag, "illegal character '" + c + "' in quoted string");
            } else if (!escaped && c == '\\') {
                if (result == null)
                    result = new StringBuffer();
                result.append(content.substring(backslash + 1, i));
                backslash = i;
                escaped = true;
            } else if (!escaped && c == '"') {
                mOffset = i + 1;
                String range = content.substring(backslash + 1, i);
                return (result == null ? range : result.append(range).toString());
            } else {
                escaped = false;
            }
        }
        throw new ImapParseException(mTag, "unexpected end of line in quoted string");
    }

    abstract Literal readLiteral() throws IOException, ImapParseException;

    private String readLiteral(String charset) throws IOException, ImapParseException {
        try {
            return new String(readLiteral().getBytes(), charset);
        } catch (UnsupportedEncodingException e) {
            throw new ImapParseException(mTag, "BADCHARSET", "could not convert string to charset \"" + charset + '"', true);
        }
    }

    Literal readLiteral8() throws IOException, ImapParseException {
        if (peekChar() == '~' && extensionEnabled("BINARY"))
            skipChar('~');
        return readLiteral();
    }

    String readAstring() throws IOException, ImapParseException {
        return readAstring(null);
    }

    String readAstring(String charset) throws IOException, ImapParseException {
        return readAstring(charset, ASTRING_CHARS);
    }

    private String readAstring(String charset, boolean[] acceptable) throws IOException, ImapParseException {
        int c = peekChar();
        if (c == -1)        throw new ImapParseException(mTag, "unexpected end of line");
        else if (c == '{')  return readLiteral(charset != null ? charset : "utf-8");
        else if (c != '"')  return readContent(acceptable);
        else                return readQuoted();
    }

    private String readAquoted() throws ImapParseException {
        int c = peekChar();
        if (c == -1)        throw new ImapParseException(mTag, "unexpected end of line");
        else if (c != '"')  return readContent(ASTRING_CHARS);
        else                return readQuoted();
    }

    private String readString(String charset) throws IOException, ImapParseException {
        int c = peekChar();
        if (c == -1)        throw new ImapParseException(mTag, "unexpected end of line");
        else if (c == '{')  return readLiteral(charset != null ? charset : "utf-8");
        else                return readQuoted();
    }

    private String readNstring(String charset) throws IOException, ImapParseException {
        int c = peekChar();
        if (c == -1)        throw new ImapParseException(mTag, "unexpected end of line");
        else if (c == '{')  return readLiteral(charset != null ? charset : "utf-8");
        else if (c != '"')  { skipNIL();  return null; }
        else                return readQuoted();
    }


    String readTag() throws ImapParseException   { return mTag = readContent(TAG_CHARS); }


    static final boolean NONZERO = false, ZERO_OK = true;

    String readNumber() throws ImapParseException  { return readNumber(ZERO_OK); }

    String readNumber(boolean zeroOK) throws ImapParseException {
        String number = readContent(NUMBER_CHARS);
        if (number.startsWith("0") && (!zeroOK || number.length() > 1))
            throw new ImapParseException(mTag, "invalid number: " + number);
        return number;
    }

    int parseInteger(String number) throws ImapParseException {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException nfe) {
            throw new ImapParseException(mTag, "number out of range: " + number);
        }
    }

    long parseLong(String number) throws ImapParseException {
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException nfe) {
            throw new ImapParseException(mTag, "number out of range: " + number);
        }
    }


    byte[] readBase64(boolean skipEquals) throws ImapParseException {
        // in some cases, "=" means to just return null and be done with it
        if (skipEquals && peekChar() == '=') {
            skipChar('=');  return null;
        }

        String encoded = readContent(BASE64_CHARS, true);
        int padding = (4 - (encoded.length() % 4)) % 4;
        if (padding == 3)
            throw new ImapParseException(mTag, "invalid base64-encoded content");
        while (padding-- > 0) {
            skipChar('=');  encoded += "=";
        }
        try {
            return new Base64().decode(encoded.getBytes("us-ascii"));
        } catch (UnsupportedEncodingException e) {
            throw new ImapParseException(mTag, "invalid base64-encoded content");
        }
    }


    String readSequence(boolean specialsOK) throws ImapParseException {
        return validateSequence(readContent(SEQUENCE_CHARS), specialsOK);
    }

    String readSequence() throws ImapParseException {
        return validateSequence(readContent(SEQUENCE_CHARS), true);
    }

    private static final int LAST_PUNCT = 0, LAST_DIGIT = 1, LAST_STAR = 2;

    private String validateSequence(String value, boolean specialsOK) throws ImapParseException {
        // "$" is OK per RFC 5182 [SEARCHRES]
        if (value.equals("$") && specialsOK && extensionEnabled("SEARCHRES"))
            return value;

        int i, last = LAST_PUNCT;
        boolean colon = false;
        for (i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c > 0x7F || c == '$' || !SEQUENCE_CHARS[c] || (c == '*' && !specialsOK)) {
                throw new ImapParseException(mTag, "illegal character '" + c + "' in sequence");
            } else if (c == '*') {
                if (last == LAST_DIGIT)  throw new ImapParseException(mTag, "malformed sequence");
                last = LAST_STAR;
            } else if (c == ',') {
                if (last == LAST_PUNCT)  throw new ImapParseException(mTag, "malformed sequence");
                last = LAST_PUNCT;  colon = false;
            } else if (c == ':') {
                if (colon || last == LAST_PUNCT)  throw new ImapParseException(mTag, "malformed sequence");
                last = LAST_PUNCT;  colon = true;
            } else {
                if (last == LAST_STAR || c == '0' && last == LAST_PUNCT)
                    throw new ImapParseException(mTag, "malformed sequence");
                last = LAST_DIGIT;
            }
        }
        if (last == LAST_PUNCT)
            throw new ImapParseException(mTag, "malformed sequence");
        return value;
    }


    String readFolder() throws IOException, ImapParseException {
        return readFolder(false);
    }

    String readFolderPattern() throws IOException, ImapParseException {
        return readFolder(true);
    }

    private String readFolder(boolean isPattern) throws IOException, ImapParseException {
        String raw = readAstring(null, isPattern ? PATTERN_CHARS : ASTRING_CHARS);
        if (raw == null || raw.indexOf("&") == -1)
            return raw;
        try {
            return ImapPath.FOLDER_ENCODING_CHARSET.decode(ByteBuffer.wrap(raw.getBytes("US-ASCII"))).toString();
        } catch (Exception e) {
            ZimbraLog.imap.debug("ignoring error while decoding folder name: " + raw, e);
            return raw;
        }
    }


    private static Set<String> SYSTEM_FLAGS = new HashSet<String>(Arrays.asList("ANSWERED", "FLAGGED", "DELETED", "SEEN", "DRAFT"));

    List<String> readFlags() throws ImapParseException {
        List<String> tags = new ArrayList<String>();
        String content = getCurrentLine();
        boolean parens = (peekChar() == '(');
        if (parens)
            skipChar('(');
        else if (mOffset == content.length())
            throw new ImapParseException(mTag, "missing flag list");

        if (!parens || peekChar() != ')') {
            while (mOffset < content.length()) {
                if (!tags.isEmpty())
                    skipSpace();

                if (peekChar() == '\\') {
                    skipChar('\\');
                    String name = readAtom();
                    if (!SYSTEM_FLAGS.contains(name.toUpperCase()))
                        throw new ImapParseException(mTag, "invalid flag: \\" + name);
                    tags.add('\\' + name);
                } else {
                    tags.add(readAtom());
                }

                if (parens && peekChar() == ')')
                    break;
            }
        }

        if (parens)
            skipChar(')');
        return tags;
    }


    private Date readDate() throws ImapParseException {
        return readDate(false, false);
    }

    Date readDate(boolean datetime, boolean checkRange) throws ImapParseException {
        String dateStr = (peekChar() == '"' ? readQuoted() : readAtom());
        if (dateStr.length() < (datetime ? 26 : 10))
            throw new ImapParseException(mTag, "invalid date format");

        Calendar cal = new GregorianCalendar();
        cal.clear();

        int pos = 0, count;
        if (datetime && dateStr.charAt(0) == ' ')
            pos++;
        count = 2 - pos - (datetime || dateStr.charAt(1) != '-' ? 0 : 1);
        validateDigits(dateStr, pos, count, cal, Calendar.DAY_OF_MONTH);  pos += count;
        validateChar(dateStr, pos, '-');  pos++;
        validateMonth(dateStr, pos, cal);  pos += 3;
        validateChar(dateStr, pos, '-');  pos++;
        validateDigits(dateStr, pos, 4, cal, Calendar.YEAR);  pos += 4;

        if (datetime) {
            validateChar(dateStr, pos, ' ');  pos++;
            validateDigits(dateStr, pos, 2, cal, Calendar.HOUR);  pos += 2;
            validateChar(dateStr, pos, ':');  pos++;
            validateDigits(dateStr, pos, 2, cal, Calendar.MINUTE);  pos += 2;
            validateChar(dateStr, pos, ':');  pos++;
            validateDigits(dateStr, pos, 2, cal, Calendar.SECOND);  pos += 2;
            validateChar(dateStr, pos, ' ');  pos++;
            boolean zonesign = dateStr.charAt(pos) == '+';
            validateChar(dateStr, pos, zonesign ? '+' : '-');  pos++;
            int zonehrs = validateDigits(dateStr, pos, 2, cal, -1);  pos += 2;
            int zonemins = validateDigits(dateStr, pos, 2, cal, -1);  pos += 2;
            cal.set(Calendar.ZONE_OFFSET, (zonesign ? 1 : -1) * (60 * zonehrs + zonemins) * 60000);
            cal.set(Calendar.DST_OFFSET, 0);
        }

        if (pos != dateStr.length())
            throw new ImapParseException(mTag, "excess characters at end of date string");

        Date date = cal.getTime();
        if (checkRange && date.getTime() < 0)
            throw new ImapParseException(mTag, "date out of range");
        return date;
    }

    private int validateDigits(String str, int pos, int count, Calendar cal, int field) throws ImapParseException {
        if (str.length() < pos + count)
            throw new ImapParseException(mTag, "unexpected end of date string");

        int value = 0;
        for (int i = 0; i < count; i++) {
            char c = str.charAt(pos + i);
            if (c < '0' || c > '9')
                throw new ImapParseException(mTag, "invalid digit in date string");
            value = value * 10 + (c - '0');
        }

        if (field >= 0)
            cal.set(field, value);
        return value;
    }

    private void validateChar(String str, int pos, char c) throws ImapParseException {
        if (str.length() < pos + 1)
            throw new ImapParseException(mTag, "unexpected end of date string");
        if (str.charAt(pos) != c)
            throw new ImapParseException(mTag, "unexpected character in date string");
    }

    private static final Map<String, Integer> MONTH_NUMBER = new HashMap<String, Integer>(16);
        static {
            String[] names = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
            for (int i = 0; i < names.length; i++)
                MONTH_NUMBER.put(names[i], i);
        }

    private void validateMonth(String str, int pos, Calendar cal) throws ImapParseException {
        Integer month = MONTH_NUMBER.get(str.substring(pos, pos + 3).toUpperCase());
        if (month == null)
            throw new ImapParseException(mTag, "invalid month string");
        cal.set(Calendar.MONTH, month);
    }


    Map<String, String> readParameters(boolean nil) throws IOException, ImapParseException {
        if (peekChar() != '(') {
            if (!nil)
                throw new ImapParseException(mTag, "did not find expected '('");
            skipNIL();  return null;
        }

        Map<String, String> params = new HashMap<String, String>();
        skipChar('(');
        do {
            String name = readString("utf-8");
            skipSpace();
            params.put(name, readNstring("utf-8"));
            if (peekChar() == ')')  break;
            skipSpace();
        } while (true);
        skipChar(')');
        return params;
    }


    int readFetch(List<ImapPartSpecifier> parts) throws IOException, ImapParseException {
        boolean list = peekChar() == '(';
        int attributes = 0;
        if (list)  skipChar('(');
        do {
            String item = readContent(FETCH_CHARS).toUpperCase();
            if (!list && item.equals("ALL"))        attributes = ImapHandler.FETCH_ALL;
            else if (!list && item.equals("FULL"))  attributes = ImapHandler.FETCH_FULL;
            else if (!list && item.equals("FAST"))  attributes = ImapHandler.FETCH_FAST;
            else if (item.equals("BODY") && peekChar() != '[')  attributes |= ImapHandler.FETCH_BODY;
            else if (item.equals("BODYSTRUCTURE"))  attributes |= ImapHandler.FETCH_BODYSTRUCTURE;
            else if (item.equals("ENVELOPE"))       attributes |= ImapHandler.FETCH_ENVELOPE;
            else if (item.equals("FLAGS"))          attributes |= ImapHandler.FETCH_FLAGS;
            else if (item.equals("INTERNALDATE"))   attributes |= ImapHandler.FETCH_INTERNALDATE;
            else if (item.equals("UID"))            attributes |= ImapHandler.FETCH_UID;
            else if (item.equals("MODSEQ") && extensionEnabled("CONDSTORE"))  attributes |= ImapHandler.FETCH_MODSEQ;
            else if (item.equals("RFC822.SIZE"))    attributes |= ImapHandler.FETCH_RFC822_SIZE;
            else if (item.equals("RFC822.HEADER"))  parts.add(new ImapPartSpecifier(item, "", "HEADER"));
            else if (item.equals("RFC822")) {
                attributes |= ImapHandler.FETCH_MARK_READ;
                parts.add(new ImapPartSpecifier(item, "", ""));
            } else if (item.equals("RFC822.TEXT")) {
                attributes |= ImapHandler.FETCH_MARK_READ;
                parts.add(new ImapPartSpecifier(item, "", "TEXT"));
            } else if (item.equals("BINARY.SIZE") && extensionEnabled("BINARY")) {
                String sectionPart = "";
                skipChar('[');
                while (peekChar() != ']') {
                    if (!sectionPart.equals("")) {
                        sectionPart += ".";  skipChar('.');
                    }
                    sectionPart += readNumber(NONZERO);
                }
                skipChar(']');
                if (sectionPart.equals(""))
                    attributes |= ImapHandler.FETCH_BINARY_SIZE;
                else
                    parts.add(new ImapPartSpecifier(item, sectionPart, ""));
            } else if (item.equals("BODY") || item.equals("BODY.PEEK") || ((item.equals("BINARY") || item.equals("BINARY.PEEK")) && extensionEnabled("BINARY"))) {
                if (!item.endsWith(".PEEK"))
                    attributes |= ImapHandler.FETCH_MARK_READ;
                boolean binary = item.startsWith("BINARY");
                skipChar('[');
                ImapPartSpecifier pspec = readPartSpecifier(binary, true);
                skipChar(']');
                if (peekChar() == '<') {
                    try {
                        skipChar('<');  int partialStart = Integer.parseInt(readNumber());
                        skipChar('.');  int partialCount = Integer.parseInt(readNumber(NONZERO));  skipChar('>');
                        pspec.setPartial(partialStart, partialCount);
                    } catch (NumberFormatException e) {
                        throw new ImapParseException(mTag, "invalid partial fetch specifier");
                    }
                }
                parts.add(pspec);
            } else {
                throw new ImapParseException(mTag, "unknown FETCH attribute \"" + item + '"');
            }
            if (list && peekChar() != ')')  skipSpace();
        } while (list && peekChar() != ')');
        if (list)  skipChar(')');
        return attributes;
    }

    ImapPartSpecifier readPartSpecifier(boolean binary, boolean literals) throws ImapParseException, IOException {
        String sectionPart = "", sectionText = "";
        List<String> headers = null;
        boolean done = false;

        while (Character.isDigit((char) peekChar())) {
            sectionPart += (sectionPart.equals("") ? "" : ".") + readNumber(NONZERO);
            if (!(done = (peekChar() != '.')))
                skipChar('.');
        }
        if (!done && peekChar() != ']') {
            if (binary)
                throw new ImapParseException(mTag, "section-text not permitted for BINARY");
            sectionText = readATOM();
            if (sectionText.equals("HEADER.FIELDS") || sectionText.equals("HEADER.FIELDS.NOT")) {
                headers = new ArrayList<String>();
                skipSpace();  skipChar('(');
                while (peekChar() != ')') {
                    if (!headers.isEmpty())  skipSpace();
                    headers.add((literals ? readAstring() : readAquoted()).toUpperCase());
                }
                if (headers.isEmpty())
                    throw new ImapParseException(mTag, "header-list may not be empty");
                skipChar(')');
            } else if (sectionText.equals("MIME")) {
                if (sectionPart.equals(""))
                    throw new ImapParseException(mTag, "\"MIME\" is not a valid section-spec");
            } else if (!sectionText.equals("HEADER") && !sectionText.equals("TEXT")) {
                throw new ImapParseException(mTag, "unknown section-text \"" + sectionText + '"');
            }
        }
        ImapPartSpecifier pspec = new ImapPartSpecifier(binary ? "BINARY" : "BODY", sectionPart, sectionText);
        pspec.setHeaders(headers);
        return pspec;
    }


    private static final Map<String, String> NEGATED_SEARCH = new HashMap<String, String>();
        static {
            NEGATED_SEARCH.put("ANSWERED",   "UNANSWERED");
            NEGATED_SEARCH.put("DELETED",    "UNDELETED");
            NEGATED_SEARCH.put("DRAFT",      "UNDRAFT");
            NEGATED_SEARCH.put("FLAGGED",    "UNFLAGGED");
            NEGATED_SEARCH.put("KEYWORD",    "UNKEYWORD");
            NEGATED_SEARCH.put("RECENT",     "OLD");
            NEGATED_SEARCH.put("OLD",        "RECENT");
            NEGATED_SEARCH.put("SEEN",       "UNSEEN");
            NEGATED_SEARCH.put("UNANSWERED", "ANSWERED");
            NEGATED_SEARCH.put("UNDELETED",  "DELETED");
            NEGATED_SEARCH.put("UNDRAFT",    "DRAFT");
            NEGATED_SEARCH.put("UNFLAGGED",  "FLAGGED");
            NEGATED_SEARCH.put("UNKEYWORD",  "KEYWORD");
            NEGATED_SEARCH.put("UNSEEN",     "SEEN");
        }

    private static final boolean SINGLE_CLAUSE = true, MULTIPLE_CLAUSES = false;
    private static final String SUBCLAUSE = "";

    private ImapSearch readSearchClause(String charset, boolean single, LogicalOperation parent)
    throws IOException, ImapParseException {
        boolean first = true;
        int nots = 0;
        do {
            if (!first)  { skipSpace(); }
            int c = peekChar();
            // key will be "" iff we're opening a new subclause...
            String key = (c == '(' ? SUBCLAUSE : readContent(SEARCH_CHARS).toUpperCase());

            LogicalOperation target = parent;
            if (key.equals("NOT")) {
                nots++;  first = false;  continue;
            } else if ((nots % 2) != 0) {
                if (NEGATED_SEARCH.containsKey(key))  key = NEGATED_SEARCH.get(key);
                else                                  parent.addChild(target = new NotOperation());
            }
            nots = 0;

            ImapSearch child;
            if (key.equals("ALL"))              child = new AllSearch();
            else if (key.equals("ANSWERED"))    child = new FlagSearch("\\Answered");
            else if (key.equals("DELETED"))     child = new FlagSearch("\\Deleted");
            else if (key.equals("DRAFT"))       child = new FlagSearch("\\Draft");
            else if (key.equals("FLAGGED"))     child = new FlagSearch("\\Flagged");
            else if (key.equals("RECENT"))      child = new FlagSearch("\\Recent");
            else if (key.equals("NEW"))         child = new AndOperation(new FlagSearch("\\Recent"), new NotOperation(new FlagSearch("\\Seen")));
            else if (key.equals("OLD"))         child = new NotOperation(new FlagSearch("\\Recent"));
            else if (key.equals("SEEN"))        child = new FlagSearch("\\Seen");
            else if (key.equals("UNANSWERED"))  child = new NotOperation(new FlagSearch("\\Answered"));
            else if (key.equals("UNDELETED"))   child = new NotOperation(new FlagSearch("\\Deleted"));
            else if (key.equals("UNDRAFT"))     child = new NotOperation(new FlagSearch("\\Draft"));
            else if (key.equals("UNFLAGGED"))   child = new NotOperation(new FlagSearch("\\Flagged"));
            else if (key.equals("UNSEEN"))      child = new NotOperation(new FlagSearch("\\Seen"));
            else if (key.equals("BCC"))         { skipSpace(); child = new HeaderSearch(HeaderSearch.Header.BCC, readAstring(charset)); }
            else if (key.equals("BEFORE"))      { skipSpace(); child = new DateSearch(DateSearch.Relation.before, readDate()); }
            else if (key.equals("BODY"))        { skipSpace(); child = new ContentSearch(readAstring(charset)); }
            else if (key.equals("CC"))          { skipSpace(); child = new HeaderSearch(HeaderSearch.Header.CC, readAstring(charset)); }
            else if (key.equals("FROM"))        { skipSpace(); child = new HeaderSearch(HeaderSearch.Header.FROM, readAstring(charset)); }
            else if (key.equals("HEADER"))      { skipSpace(); HeaderSearch.Header relation = HeaderSearch.Header.parse(readAstring());
                                                  skipSpace(); child = new HeaderSearch(relation, readAstring(charset)); }
            else if (key.equals("KEYWORD"))     { skipSpace(); child = new FlagSearch(readAtom()); }
            else if (key.equals("LARGER"))      { skipSpace(); child = new SizeSearch(SizeSearch.Relation.larger, parseLong(readNumber())); }
            else if (key.equals("MODSEQ") && extensionEnabled("CONDSTORE"))  { skipSpace();
                                                  if (peekChar() == '"') { readFolder(); skipSpace(); readATOM(); skipSpace(); }
                                                  child = new ModifiedSearch(parseInteger(readNumber(ZERO_OK))); }
            else if (key.equals("ON"))          { skipSpace(); child = new DateSearch(DateSearch.Relation.date, readDate()); }
            else if (key.equals("OLDER") && extensionEnabled("WITHIN"))  { skipSpace(); child = new RelativeDateSearch(DateSearch.Relation.before, parseInteger(readNumber())); }
            // FIXME: SENTBEFORE, SENTON, and SENTSINCE reference INTERNALDATE, not the Date header
            else if (key.equals("SENTBEFORE"))  { skipSpace(); child = new DateSearch(DateSearch.Relation.before, readDate()); }
            else if (key.equals("SENTON"))      { skipSpace(); child = new DateSearch(DateSearch.Relation.date, readDate()); }
            else if (key.equals("SENTSINCE"))   { skipSpace(); child = new DateSearch(DateSearch.Relation.after, readDate()); }
            else if (key.equals("SINCE"))       { skipSpace(); child = new DateSearch(DateSearch.Relation.after, readDate()); }
            else if (key.equals("SMALLER"))     { skipSpace(); child = new SizeSearch(SizeSearch.Relation.smaller, parseLong(readNumber())); }
            else if (key.equals("SUBJECT"))     { skipSpace(); child = new HeaderSearch(HeaderSearch.Header.SUBJECT, readAstring(charset)); }
            else if (key.equals("TEXT"))        { skipSpace(); child = new ContentSearch(readAstring(charset)); }
            else if (key.equals("TO"))          { skipSpace(); child = new HeaderSearch(HeaderSearch.Header.TO, readAstring(charset)); }
            else if (key.equals("UID"))         { skipSpace(); child = new SequenceSearch(mTag, readSequence(), true); }
            else if (key.equals("UNKEYWORD"))   { skipSpace(); child = new NotOperation(new FlagSearch(readAtom())); }
            else if (key.equals("YOUNGER") && extensionEnabled("WITHIN"))  { skipSpace(); child = new RelativeDateSearch(DateSearch.Relation.after, parseInteger(readNumber())); }
            else if (key.equals(SUBCLAUSE))     { skipChar('(');  child = readSearchClause(charset, MULTIPLE_CLAUSES, new AndOperation());  skipChar(')'); }
            else if (Character.isDigit(key.charAt(0)) || key.charAt(0) == '*' || key.charAt(0) == '$')
                child = new SequenceSearch(mTag, validateSequence(key, true), false);
            else if (key.equals("OR")) {
                skipSpace(); child = readSearchClause(charset, SINGLE_CLAUSE, new OrOperation());
                skipSpace(); readSearchClause(charset, SINGLE_CLAUSE, (LogicalOperation) child);
            }
            else throw new ImapParseException(mTag, "unknown search tag: " + key);

            target.addChild(child);
            first = false;
        } while (peekChar() != -1 && peekChar() != ')' && (nots > 0 || !single));

        if (nots > 0)
            throw new ImapParseException(mTag, "missing search-key after NOT");
        return parent;
    }

    ImapSearch readSearch(String charset) throws IOException, ImapParseException {
        return readSearchClause(charset, MULTIPLE_CLAUSES, new AndOperation());
    }

    String readCharset() throws IOException, ImapParseException {
        String charset = readAstring();
        try {
            if (Charset.isSupported(charset))
                return charset;
        } catch (Exception e) { }
        throw new ImapParseException(mTag, "BADCHARSET", "unknown charset: " + charset.replace('\r', ' ').replace('\n', ' '), true);
    }
}
