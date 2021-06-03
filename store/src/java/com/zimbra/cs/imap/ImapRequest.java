/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
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
import com.zimbra.soap.admin.type.CacheEntrySelector;
import com.zimbra.soap.admin.type.CacheEntrySelector.CacheEntryBy;
import com.zimbra.soap.admin.type.CacheEntryType;

/**
 * @since Apr 30, 2005
 */
public abstract class ImapRequest {
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
        for (int i = 0x21; i < 0x7F; i++) {
            if (i != '(' && i != ')' && i != '{' && i != '%' && i != '*' && i != '"' && i != '\\') {
                SEARCH_CHARS[i] = FETCH_CHARS[i] = PATTERN_CHARS[i] = ASTRING_CHARS[i] = ATOM_CHARS[i] = TAG_CHARS[i] = true;
            }
        }
        ATOM_CHARS[']'] = false;
        TAG_CHARS['+']  = false;
        PATTERN_CHARS['%'] = PATTERN_CHARS['*'] = true;
        FETCH_CHARS['['] = false;
        SEARCH_CHARS['*'] = true;

        for (int i = 'a'; i <= 'z'; i++) {
            BASE64_CHARS[i] = true;
        }
        for (int i = 'A'; i <= 'Z'; i++) {
            BASE64_CHARS[i] = true;
        }
        for (int i = '0'; i <= '9'; i++) {
            BASE64_CHARS[i] = NUMBER_CHARS[i] = SEQUENCE_CHARS[i] = true;
        }
        SEQUENCE_CHARS['*'] = SEQUENCE_CHARS[':'] = SEQUENCE_CHARS[','] = SEQUENCE_CHARS['$'] = true;
        BASE64_CHARS['+'] = BASE64_CHARS['/'] = true;
    }
    private static final int LAST_PUNCT = 0;
    private static final int LAST_DIGIT = 1;
    private static final int LAST_STAR = 2;
    private static final Set<String> SYSTEM_FLAGS = ImmutableSet.of("ANSWERED", "FLAGGED", "DELETED", "SEEN", "DRAFT");
    private static final Map<String, String> NEGATED_SEARCH = ImmutableMap.<String, String>builder()
            .put("ANSWERED", "UNANSWERED")
            .put("DELETED", "UNDELETED")
            .put("DRAFT", "UNDRAFT")
            .put("FLAGGED", "UNFLAGGED")
            .put("KEYWORD", "UNKEYWORD")
            .put("RECENT", "OLD")
            .put("OLD", "RECENT")
            .put("SEEN", "UNSEEN")
            .put("UNANSWERED", "ANSWERED")
            .put("UNDELETED", "DELETED")
            .put("UNDRAFT", "DRAFT")
            .put("UNFLAGGED", "FLAGGED")
            .put("UNKEYWORD", "KEYWORD")
            .put("UNSEEN", "SEEN")
            .build();
    private static final boolean SINGLE_CLAUSE = true;
    private static final boolean MULTIPLE_CLAUSES = false;
    private static final String SUBCLAUSE = "";
    protected static final boolean NONZERO = false;
    protected static final boolean ZERO_OK = true;

    protected final ImapHandler mHandler;
    protected String tag;
    protected final List<Part> parts = new ArrayList<Part>();
    protected int index;
    protected int offset;
    private boolean isAppend;
    private boolean isLogin;
    private final int maxNestingInSearchRequest;

    protected ImapRequest(ImapHandler handler) {
        mHandler = handler;
        maxNestingInSearchRequest = LC.imap_max_nesting_in_search_request.intValue();
    }

    protected ImapRequest rewind() {
        index = offset = 0;
        return this;
    }

    protected abstract class Part {
        protected abstract int size();
        protected abstract byte[] getBytes() throws IOException;
        protected abstract String getString() throws ImapParseException;
        protected abstract Literal getLiteral() throws ImapParseException;

        protected boolean isString() {
            return false;
        }

        protected boolean isLiteral() {
            return false;
        }

        protected void cleanup() {
        }
    }

    private final class StringPart extends Part {
        private final String str;

        StringPart(String s)  {
            str = s;
        }

        @Override
        protected int size() {
            return str.length();
        }

        @Override
        protected byte[] getBytes() {
            return str.getBytes();
        }

        @Override
        protected boolean isString() {
            return true;
        }

        @Override
        protected String getString() {
            return str;
        }

        @Override
        public String toString() {
            return str;
        }

        @Override
        protected Literal getLiteral() throws ImapParseException {
            throw new ImapParseException(tag, "not inside literal");
        }
    }

    private final class LiteralPart extends Part {
        private final Literal lit;

        LiteralPart(Literal l)  {
            lit = l;
        }

        @Override
        protected int size() {
            return lit.size();
        }

        @Override
        protected byte[] getBytes() throws IOException {
            return lit.getBytes();
        }

        @Override
        protected boolean isLiteral() {
            return true;
        }

        @Override
        protected Literal getLiteral() {
            return lit;
        }

        @Override
        protected void cleanup() {
            lit.cleanup();
        }

        @Override
        public String getString() throws ImapParseException {
            throw new ImapParseException(tag, "not inside string");
        }

        @Override
        public String toString() {
            try {
                return new String(lit.getBytes(), Charsets.US_ASCII);
            } catch (IOException e) {
                return "???";
            }
        }
    }

    protected void addPart(Literal literal) {
        addPart(new LiteralPart(literal));
    }

    protected void addPart(String line) {
        if (parts.isEmpty()) {
            String cmd = getCommand(line);
            if ("APPEND".equalsIgnoreCase(cmd)) {
                isAppend = true;
            } else if ("LOGIN".equalsIgnoreCase(cmd)) {
                isLogin = true;
            }
        }
        addPart(new StringPart(line));
    }

    protected void addPart(Part part) {
        parts.add(part);
    }

    protected void cleanup() {
        for (Part part : parts) {
            part.cleanup();
        }
        parts.clear();
    }

    protected boolean isAppend() {
        return isAppend;
    }

    protected boolean isLogin() {
        return isLogin;
    }

    public static String getCommand(String requestLine) {
        int i = requestLine.indexOf(' ') + 1;
        if (i > 0) {
            int j = requestLine.indexOf(' ', i);
            if (j > 0) {
                return requestLine.substring(i, j);
            }
        }
        return null;
    }

    protected String getCurrentLine() throws ImapParseException {
        return parts.get(index).getString();
    }

    protected byte[] toByteArray() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Part part : parts) {
            byte[] content = part.getBytes();
            baos.write(content, 0, content.length);
            if (part.isString()) {
                baos.write(ImapHandler.LINE_SEPARATOR_BYTES, 0, 2);
            }
        }
        return baos.toByteArray();
    }


    protected String getTag() {
        if (tag == null && index == 0 && offset == 0 && parts.size() > 0) {
            try {
                readTag();
            } catch (ImapParseException ignore) {
            }
            index = 0;
            offset = 0;
        }
        return tag;
    }

    /**
     * Returns whether the specified IMAP extension is enabled for this session.
     *
     * @see ImapHandler#extensionEnabled(String)
     */
    protected boolean extensionEnabled(String extension) {
        return mHandler == null || mHandler.extensionEnabled(extension);
    }

    /**
     * Records the "tag" for the request. This tag will later be used to indicate that the server has finished
     * processing the request. It may also be used when generating a parse exception.
     */
    protected void setTag(String value) {
        tag = value;
    }

    protected String readContent(boolean[] acceptable) throws ImapParseException {
        return readContent(acceptable, false);
    }

    protected String readContent(boolean[] acceptable, boolean emptyOK) throws ImapParseException {
        String content = getCurrentLine();
        int i;
        for (i = offset; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c > 0x7F || !acceptable[c]) {
                break;
            }
        }
        if (i == offset && !emptyOK) {
            throw new ImapParseException(tag, "zero-length content");
        }
        String result = content.substring(offset, i);
        offset += result.length();
        return result;
    }


    /**
     * Returns whether the read position is at the very end of the request.
     */
    protected boolean eof() {
        return index >= parts.size() || offset >= parts.get(index).size();
    }

    /**
     * Returns the character at the read position, or -1 if we're at the end of a literal or of a line.
     */
    protected int peekChar() throws ImapParseException {
        if (index >= parts.size()) {
            return -1;
        }
        String str = parts.get(index).getString();
        return offset < str.length() ? str.charAt(offset) : -1;
    }

    protected String peekATOM() {
        int i = index;
        int o = offset;
        try {
            return readATOM();
        } catch (ImapParseException ipe) {
            return null;
        } finally {
            index = i;
            offset = o;
        }
    }

    protected void skipSpace() throws ImapParseException {
        skipChar(' ');
    }

    protected void skipChar(char c) throws ImapParseException {
        if (index >= parts.size()) {
            throw new ImapParseException(tag, "unexpected end of line; expected '" + c + "'");
        }
        String str = parts.get(index).getString();
        if (offset >= str.length()) {
            throw new ImapParseException(tag, "unexpected end of line; expected '" + c + "'");
        }
        char got = str.charAt(offset);
        if (got == c) {
            offset++;
        } else {
            throw new ImapParseException(tag, "wrong character; expected '" + c + "' but got '" + got + "'");
        }
    }

    protected void skipNIL() throws ImapParseException {
        skipAtom("NIL");
    }

    protected void skipAtom(String atom) throws ImapParseException {
        if (!readATOM().equals(atom)) {
            throw new ImapParseException(tag, "did not find expected " + atom);
        }
    }

    protected String readAtom() throws ImapParseException {
        return readContent(ATOM_CHARS);
    }

    protected String readATOM() throws ImapParseException {
        return readContent(ATOM_CHARS).toUpperCase();
    }

    protected String readQuoted(Charset charset) throws ImapParseException {
        String result = readQuoted();
        if (charset == null || Charsets.ISO_8859_1.equals(charset) || Charsets.US_ASCII.equals(charset)) {
            return result;
        } else {
            return new String(result.getBytes(Charsets.ISO_8859_1), charset);
        }
    }

    protected String readQuoted() throws ImapParseException {
        String content = getCurrentLine();
        StringBuilder result = null;

        skipChar('"');
        int backslash = offset - 1;
        boolean escaped = false;
        for (int i = offset; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c > 0x7F || c == 0x00 || c == '\r' || c == '\n' || (escaped && c != '\\' && c != '"')) {
                throw new ImapParseException(tag, "illegal character '" + c + "' in quoted string");
            } else if (!escaped && c == '\\') {
                if (result == null) {
                    result = new StringBuilder();
                }
                result.append(content.substring(backslash + 1, i));
                backslash = i;
                escaped = true;
            } else if (!escaped && c == '"') {
                offset = i + 1;
                String range = content.substring(backslash + 1, i);
                return (result == null ? range : result.append(range).toString());
            } else {
                escaped = false;
            }
        }
        throw new ImapParseException(tag, "unexpected end of line in quoted string");
    }

    protected abstract Literal readLiteral() throws IOException, ImapParseException;

    private String readLiteral(Charset charset) throws IOException, ImapParseException {
        return new String(readLiteral().getBytes(), charset);
    }

    protected Literal readLiteral8() throws IOException, ImapParseException {
        if (peekChar() == '~' && extensionEnabled("BINARY")) {
            skipChar('~');
        }
        return readLiteral();
    }

    protected String readAstring() throws IOException, ImapParseException {
        return readAstring(null);
    }

    protected String readAstring(Charset charset) throws IOException, ImapParseException {
        return readAstring(charset, ASTRING_CHARS);
    }

    private String readAstring(Charset charset, boolean[] acceptable) throws IOException, ImapParseException {
        int c = peekChar();
        if (c == -1) {
            throw new ImapParseException(tag, "unexpected end of line");
        } else if (c == '{') {
            return readLiteral(charset != null ? charset : Charsets.UTF_8);
        } else if (c != '"') {
            return readContent(acceptable);
        } else {
            return readQuoted(charset);
        }
    }

    private String readAquoted() throws ImapParseException {
        int c = peekChar();
        if (c == -1) {
            throw new ImapParseException(tag, "unexpected end of line");
        } else if (c != '"') {
            return readContent(ASTRING_CHARS);
        } else {
            return readQuoted();
        }
    }

    private String readString(Charset charset) throws IOException, ImapParseException {
        int c = peekChar();
        if (c == -1) {
            throw new ImapParseException(tag, "unexpected end of line");
        } else if (c == '{') {
            return readLiteral(charset != null ? charset : Charsets.UTF_8);
        } else {
            return readQuoted(charset);
        }
    }

    private String readNstring(Charset charset) throws IOException, ImapParseException {
        int c = peekChar();
        if (c == -1) {
            throw new ImapParseException(tag, "unexpected end of line");
        } else if (c == '{') {
            return readLiteral(charset != null ? charset : Charsets.UTF_8);
        } else if (c != '"') {
            skipNIL();
            return null;
        } else {
            return readQuoted(charset);
        }
    }

    protected String readTag() throws ImapParseException {
        return tag = readContent(TAG_CHARS);
    }

    public static String parseTag(String src) throws ImapParseException {
        int i;
        for (i = 0; i < src.length(); i++) {
            char c = src.charAt(i);
            if (c > 0x7F || !TAG_CHARS[c]) {
                break;
            }
        }
        if (i > 0) {
            return src.substring(0, i);
        } else {
            throw new ImapParseException();
        }
    }

    protected String readNumber() throws ImapParseException {
        return readNumber(ZERO_OK);
    }

    protected String readNumber(boolean zeroOK) throws ImapParseException {
        String number = readContent(NUMBER_CHARS);
        if (number.startsWith("0") && (!zeroOK || number.length() > 1)) {
            throw new ImapParseException(tag, "invalid number: " + number);
        }
        return number;
    }

    protected int parseInteger(String number) throws ImapParseException {
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException nfe) {
            throw new ImapParseException(tag, "number out of range: " + number);
        }
    }

    protected long parseLong(String number) throws ImapParseException {
        try {
            return Long.parseLong(number);
        } catch (NumberFormatException nfe) {
            throw new ImapParseException(tag, "number out of range: " + number);
        }
    }

    protected byte[] readBase64(boolean skipEquals) throws ImapParseException {
        // in some cases, "=" means to just return null and be done with it
        if (skipEquals && peekChar() == '=') {
            skipChar('=');
            return null;
        }

        String encoded = readContent(BASE64_CHARS, true);
        int padding = (4 - (encoded.length() % 4)) % 4;
        if (padding == 3) {
            throw new ImapParseException(tag, "invalid base64-encoded content");
        }
        while (padding-- > 0) {
            skipChar('=');
            encoded += "=";
        }
        return new Base64().decode(encoded.getBytes(Charsets.US_ASCII));
    }

    protected String readSequence(boolean specialsOK) throws ImapParseException {
        return validateSequence(readContent(SEQUENCE_CHARS), specialsOK);
    }

    protected String readSequence() throws ImapParseException {
        return validateSequence(readContent(SEQUENCE_CHARS), true);
    }

    private CacheEntryType readCacheEntryType() throws IOException, ImapParseException {
        String cacheTypeStr = readAstring(Charsets.UTF_8);
        try {
            CacheEntryType cacheType = CacheEntryType.fromString(cacheTypeStr);
            if (!ImapHandler.IMAP_CACHE_TYPES.contains(cacheType)) {
                ZimbraLog.imap.debug("skipping flushing cache type %s", cacheType);
                return null;
            } else {
                return cacheType;
            }
        } catch (ServiceException e) {
            throw new ImapParseException(tag, "invalid cache type: " + cacheTypeStr);
        }
    }

    protected List<CacheEntryType> readCacheEntryTypes() throws IOException, ImapParseException {
        if (peekChar() != '(') {
            CacheEntryType type = readCacheEntryType();
            if (type != null) {
                return Arrays.asList(new CacheEntryType[] { type } );
            } else {
                return Collections.emptyList();
            }
        }
        skipChar('(');
        List<CacheEntryType> cacheTypes = new ArrayList<CacheEntryType>();
        if (peekChar() != ')') {
            do {
                CacheEntryType cacheType = readCacheEntryType();
                if (cacheType != null) {
                    cacheTypes.add(cacheType);
                }
                if (peekChar() == ')') {
                    break;
                }
                skipSpace();
            } while (true);
        skipChar(')');
        return cacheTypes;
        } else {
            throw new ImapParseException(tag, "must specify a cache type");
        }
    }

    protected List<CacheEntrySelector> readCacheEntries() throws IOException, ImapParseException {
        if (eof()) {
            return null;
        }
        skipSpace();
        if (peekChar() != '(') {
            throw new ImapParseException(tag, "did not find expected '('");
        }
        List<CacheEntrySelector> cacheEntries = new ArrayList<CacheEntrySelector>();
        skipChar('(');
        if (peekChar() != ')') {
            do {
                try {
                    CacheEntryBy cacheBy = CacheEntryBy.fromString(readAstring(Charsets.UTF_8));
                    skipSpace();
                    CacheEntrySelector cacheEntry = new CacheEntrySelector(cacheBy, readAstring(Charsets.UTF_8));
                    cacheEntries.add(cacheEntry);
                } catch (ServiceException e) {
                    throw new ImapParseException(tag, e.getMessage());
                }
                if (peekChar() == ')') {
                    break;
                }
                skipSpace();
            } while (true);
        }
        skipChar(')');
        return cacheEntries;
    }

    private String validateSequence(String value, boolean specialsOK) throws ImapParseException {
        // "$" is OK per RFC 5182 [SEARCHRES]
        if ("$".equals(value) && specialsOK && extensionEnabled("SEARCHRES")) {
            return value;
        }
        int i;
        int last = LAST_PUNCT;
        boolean colon = false;
        for (i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c > 0x7F || c == '$' || !SEQUENCE_CHARS[c] || (c == '*' && !specialsOK)) {
                throw new ImapParseException(tag, "illegal character '" + c + "' in sequence");
            } else if (c == '*') {
                if (last == LAST_DIGIT) {
                    throw new ImapParseException(tag, "malformed sequence");
                }
                last = LAST_STAR;
            } else if (c == ',') {
                if (last == LAST_PUNCT) {
                    throw new ImapParseException(tag, "malformed sequence");
                }
                last = LAST_PUNCT;
                colon = false;
            } else if (c == ':') {
                if (colon || last == LAST_PUNCT) {
                    throw new ImapParseException(tag, "malformed sequence");
                }
                last = LAST_PUNCT;
                colon = true;
            } else {
                if (last == LAST_STAR || c == '0' && last == LAST_PUNCT) {
                    throw new ImapParseException(tag, "malformed sequence");
                }
                last = LAST_DIGIT;
            }
        }
        if (last == LAST_PUNCT) {
            throw new ImapParseException(tag, "malformed sequence");
        }
        return value;
    }


    protected String readFolder() throws IOException, ImapParseException {
        return readFolder(false);
    }

    protected String readFolderPattern() throws IOException, ImapParseException {
        return readFolder(true);
    }

    private String readFolder(boolean isPattern) throws IOException, ImapParseException {
        String raw = readAstring(null, isPattern ? PATTERN_CHARS : ASTRING_CHARS);
        if (raw == null || raw.indexOf("&") == -1)
            return raw;
        try {
            return ImapPath.FOLDER_ENCODING_CHARSET.decode(ByteBuffer.wrap(raw.getBytes(Charsets.US_ASCII))).toString();
        } catch (Exception e) {
            ZimbraLog.imap.debug("ignoring error while decoding folder name: %s", raw, e);
            return raw;
        }
    }

    protected List<String> readFlags() throws ImapParseException {
        List<String> tags = new ArrayList<String>();
        String content = getCurrentLine();
        boolean parens = (peekChar() == '(');
        if (parens) {
            skipChar('(');
        } else if (offset == content.length()) {
            throw new ImapParseException(tag, "missing flag list");
        }
        if (!parens || peekChar() != ')') {
            while (offset < content.length()) {
                if (!tags.isEmpty()) {
                    skipSpace();
                }
                if (peekChar() == '\\') {
                    skipChar('\\');
                    String name = readAtom();
                    if (!SYSTEM_FLAGS.contains(name.toUpperCase())) {
                        throw new ImapParseException(tag, "invalid flag: \\" + name);
                    }
                    tags.add('\\' + name);
                } else {
                    tags.add(readAtom());
                }

                if (parens && peekChar() == ')') {
                    break;
                }
            }
        }
        if (parens) {
            skipChar(')');
        }
        return tags;
    }


    private Date readDate() throws ImapParseException {
        return readDate(false, false);
    }

    protected Date readDate(boolean datetime, boolean checkRange) throws ImapParseException {
        String dateStr = (peekChar() == '"' ? readQuoted() : readAtom());
        if (dateStr.length() < (datetime ? 24 : 10)) {
            throw new ImapParseException(tag, "invalid date format: '" + dateStr + "'");
        }

        Date date = null;

        Calendar cal = new GregorianCalendar();
        cal.clear();

        String pattern = "dd-MMM-yyyy";

        if (datetime) {
            pattern = "dd-MMM-yyyy HH:mm:ss Z";
        }

        SimpleDateFormat fmt = new SimpleDateFormat(pattern, Locale.US);

        try {
            date = fmt.parse(dateStr);
        }
        catch (Exception e) {
            throw new ImapParseException(tag, "invalid date format: '" + dateStr + "'");
        }

        cal.setTime(date);

        date = cal.getTime();
        if (checkRange && date.getTime() < 0) {
            throw new ImapParseException(tag, "date out of range");
        }
        return date;
    }

    protected Map<String, String> readParameters(boolean nil) throws IOException, ImapParseException {
        if (peekChar() != '(') {
            if (!nil) {
                throw new ImapParseException(tag, "did not find expected '('");
            }
            skipNIL();  return null;
        }

        Map<String, String> params = new HashMap<String, String>();
        skipChar('(');
        if (peekChar() != ')') {//skip over empty parameters
            do {
                String name = readString(Charsets.UTF_8);
                skipSpace();
                params.put(name, readNstring(Charsets.UTF_8));
                if (peekChar() == ')') {
                    break;
                }
                skipSpace();
            } while (true);
        }
        skipChar(')');
        return params;
    }

    protected int readFetch(List<ImapPartSpecifier> parts) throws IOException, ImapParseException {
        boolean list = peekChar() == '(';
        int attributes = 0;
        if (list)  skipChar('(');
        do {
            String item = readContent(FETCH_CHARS).toUpperCase();
            if (!list && item.equals("ALL")) {
                attributes = ImapHandler.FETCH_ALL;
            } else if (!list && item.equals("FULL")) {
                attributes = ImapHandler.FETCH_FULL;
            } else if (!list && item.equals("FAST")) {
                attributes = ImapHandler.FETCH_FAST;
            } else if (item.equals("BODY") && peekChar() != '[') {
                attributes |= ImapHandler.FETCH_BODY;
            } else if (item.equals("BODYSTRUCTURE")) {
                attributes |= ImapHandler.FETCH_BODYSTRUCTURE;
            } else if (item.equals("ENVELOPE")) {
                attributes |= ImapHandler.FETCH_ENVELOPE;
            } else if (item.equals("FLAGS")) {
                attributes |= ImapHandler.FETCH_FLAGS;
            } else if (item.equals("INTERNALDATE")) {
                attributes |= ImapHandler.FETCH_INTERNALDATE;
            } else if (item.equals("UID")) {
                attributes |= ImapHandler.FETCH_UID;
            } else if (item.equals("MODSEQ") && extensionEnabled("CONDSTORE")) {
                attributes |= ImapHandler.FETCH_MODSEQ;
            } else if (item.equals("RFC822.SIZE")) {
                attributes |= ImapHandler.FETCH_RFC822_SIZE;
            } else if (item.equals("RFC822.HEADER")) {
                parts.add(new ImapPartSpecifier(item, "", "HEADER"));
            } else if (item.equals("RFC822")) {
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
                if (sectionPart.isEmpty()) {
                    attributes |= ImapHandler.FETCH_BINARY_SIZE;
                } else {
                    parts.add(new ImapPartSpecifier(item, sectionPart, ""));
                }
            } else if (item.equals("BODY") || item.equals("BODY.PEEK") ||
                    ((item.equals("BINARY") || item.equals("BINARY.PEEK")) && extensionEnabled("BINARY"))) {
                if (!item.endsWith(".PEEK")) {
                    attributes |= ImapHandler.FETCH_MARK_READ;
                }
                boolean binary = item.startsWith("BINARY");
                skipChar('[');
                ImapPartSpecifier pspec = readPartSpecifier(binary, true);
                skipChar(']');
                if (peekChar() == '<') {
                    try {
                        skipChar('<');
                        int partialStart = Integer.parseInt(readNumber());
                        skipChar('.');
                        int partialCount = Integer.parseInt(readNumber(NONZERO));
                        skipChar('>');
                        pspec.setPartial(partialStart, partialCount);
                    } catch (NumberFormatException e) {
                        throw new ImapParseException(tag, "invalid partial fetch specifier");
                    }
                }
                parts.add(pspec);
            } else {
                throw new ImapParseException(tag, "unknown FETCH attribute \"" + item + '"');
            }
            if (list && peekChar() != ')') {
                skipSpace();
            }
        } while (list && peekChar() != ')');
        if (list) {
            skipChar(')');
        }
        return attributes;
    }

    protected ImapPartSpecifier readPartSpecifier(boolean binary, boolean literals)
            throws ImapParseException, IOException {
        String sectionPart = "";
        String sectionText = "";
        List<String> headers = null;
        boolean done = false;

        while (Character.isDigit((char) peekChar())) {
            sectionPart += (sectionPart.equals("") ? "" : ".") + readNumber(NONZERO);
            if (!(done = (peekChar() != '.'))) {
                skipChar('.');
            }
        }
        if (!done && peekChar() != ']') {
            if (binary) {
                throw new ImapParseException(tag, "section-text not permitted for BINARY");
            }
            sectionText = readATOM();
            if (sectionText.equals("HEADER.FIELDS") || sectionText.equals("HEADER.FIELDS.NOT")) {
                headers = new ArrayList<String>();
                skipSpace();  skipChar('(');
                while (peekChar() != ')') {
                    if (!headers.isEmpty()) {
                        skipSpace();
                    }
                    headers.add((literals ? readAstring() : readAquoted()).toUpperCase());
                }
                if (headers.isEmpty()) {
                    throw new ImapParseException(tag, "header-list may not be empty");
                }
                skipChar(')');
            } else if (sectionText.equals("MIME")) {
                if (sectionPart.isEmpty()) {
                    throw new ImapParseException(tag, "\"MIME\" is not a valid section-spec");
                }
            } else if (!sectionText.equals("HEADER") && !sectionText.equals("TEXT")) {
                throw new ImapParseException(tag, "unknown section-text \"" + sectionText + '"');
            }
        }
        ImapPartSpecifier pspec = new ImapPartSpecifier(binary ? "BINARY" : "BODY", sectionPart, sectionText);
        pspec.setHeaders(headers);
        return pspec;
    }

    private ImapSearch readSearchClause(Charset charset, boolean single, LogicalOperation parent, int depth)
            throws IOException, ImapParseException {
        depth++;
        if (depth > maxNestingInSearchRequest) {
            ZimbraLog.imap.debug("search nesting too deep (depth=%s) Max allowed=%s", depth, maxNestingInSearchRequest);
            throw new ImapSearchTooComplexException(tag, "Search query too complex");
        }
        boolean first = true;
        int nots = 0;
        do {
            if (!first) {
                skipSpace();
            }
            int c = peekChar();
            // key will be "" iff we're opening a new subclause...
            String key = (c == '(' ? SUBCLAUSE : readContent(SEARCH_CHARS).toUpperCase());

            LogicalOperation target = parent;
            if (key.equals("NOT")) {
                nots++;  first = false;  continue;
            } else if ((nots % 2) != 0) {
                if (NEGATED_SEARCH.containsKey(key)) {
                    key = NEGATED_SEARCH.get(key);
                } else {
                    parent.addChild(target = new NotOperation());
                }
            }
            nots = 0;

            ImapSearch child;
            if (key.equals("ALL")) {
                child = new AllSearch();
            } else if (key.equals("ANSWERED")) {
                child = new FlagSearch("\\Answered");
            } else if (key.equals("DELETED")) {
                child = new FlagSearch("\\Deleted");
            } else if (key.equals("DRAFT")) {
                child = new FlagSearch("\\Draft");
            } else if (key.equals("FLAGGED")) {
                child = new FlagSearch("\\Flagged");
            } else if (key.equals("RECENT")) {
                child = new FlagSearch("\\Recent");
            } else if (key.equals("NEW")) {
                child = new AndOperation(new FlagSearch("\\Recent"), new NotOperation(new FlagSearch("\\Seen")));
            } else if (key.equals("OLD")) {
                child = new NotOperation(new FlagSearch("\\Recent"));
            } else if (key.equals("SEEN")) {
                child = new FlagSearch("\\Seen");
            } else if (key.equals("UNANSWERED")) {
                child = new NotOperation(new FlagSearch("\\Answered"));
            } else if (key.equals("UNDELETED")) {
                child = new NotOperation(new FlagSearch("\\Deleted"));
            } else if (key.equals("UNDRAFT")) {
                child = new NotOperation(new FlagSearch("\\Draft"));
            } else if (key.equals("UNFLAGGED")) {
                child = new NotOperation(new FlagSearch("\\Flagged"));
            } else if (key.equals("UNSEEN")) {
                child = new NotOperation(new FlagSearch("\\Seen"));
            } else if (key.equals("BCC")) {
                skipSpace(); child = new HeaderSearch(HeaderSearch.Header.BCC, readAstring(charset));
            } else if (key.equals("BEFORE")) {
                skipSpace(); child = new DateSearch(DateSearch.Relation.before, readDate());
            } else if (key.equals("BODY")) {
                skipSpace(); child = new ContentSearch(readAstring(charset));
            } else if (key.equals("CC")) {
                skipSpace();
                child = new HeaderSearch(HeaderSearch.Header.CC, readAstring(charset));
            } else if (key.equals("FROM")) {
                skipSpace();
                child = new HeaderSearch(HeaderSearch.Header.FROM, readAstring(charset));
            } else if (key.equals("HEADER")) {
                skipSpace();
                HeaderSearch.Header relation = HeaderSearch.Header.parse(readAstring());
                skipSpace();
                child = new HeaderSearch(relation, readAstring(charset));
            } else if (key.equals("KEYWORD")) {
                skipSpace();
                child = new FlagSearch(readAtom());
            } else if (key.equals("LARGER")) {
                skipSpace();
                child = new SizeSearch(SizeSearch.Relation.larger, parseLong(readNumber()));
            } else if (key.equals("MODSEQ") && extensionEnabled("CONDSTORE")) {
                skipSpace();
                if (peekChar() == '"') {
                    readFolder();
                    skipSpace();
                    readATOM();
                    skipSpace();
                }
                child = new ModifiedSearch(parseInteger(readNumber(ZERO_OK)));
            } else if (key.equals("ON")) {
                skipSpace();
                child = new DateSearch(DateSearch.Relation.date, readDate());
            } else if (key.equals("OLDER") && extensionEnabled("WITHIN")) {
                skipSpace();
                child = new RelativeDateSearch(DateSearch.Relation.before, parseInteger(readNumber()));
            } else if (key.equals("SENTBEFORE")) {
                // FIXME: SENTBEFORE, SENTON, and SENTSINCE reference INTERNALDATE, not the Date header
                skipSpace();
                child = new DateSearch(DateSearch.Relation.before, readDate());
            } else if (key.equals("SENTON")) {
                skipSpace();
                child = new DateSearch(DateSearch.Relation.date, readDate());
            } else if (key.equals("SENTSINCE")) {
                skipSpace();
                child = new DateSearch(DateSearch.Relation.after, readDate());
            } else if (key.equals("SINCE")) {
                skipSpace();
                child = new DateSearch(DateSearch.Relation.after, readDate());
            } else if (key.equals("SMALLER")) {
                skipSpace();
                child = new SizeSearch(SizeSearch.Relation.smaller, parseLong(readNumber()));
            } else if (key.equals("SUBJECT")) {
                skipSpace();
                child = new HeaderSearch(HeaderSearch.Header.SUBJECT, readAstring(charset));
            } else if (key.equals("TEXT")) {
                skipSpace();
                child = new ContentSearch(readAstring(charset));
            } else if (key.equals("TO")) {
                skipSpace();
                child = new HeaderSearch(HeaderSearch.Header.TO, readAstring(charset));
            } else if (key.equals("UID")) {
                skipSpace();
                child = new SequenceSearch(tag, readSequence(), true);
            } else if (key.equals("UNKEYWORD")) {
                skipSpace();
                child = new NotOperation(new FlagSearch(readAtom()));
            } else if (key.equals("YOUNGER") && extensionEnabled("WITHIN")) {
                skipSpace();
                child = new RelativeDateSearch(DateSearch.Relation.after, parseInteger(readNumber()));
            } else if (key.equals(SUBCLAUSE)) {
                skipChar('(');
                child = readSearchClause(charset, MULTIPLE_CLAUSES, new AndOperation(), depth);
                skipChar(')');
            } else if (Character.isDigit(key.charAt(0)) || key.charAt(0) == '*' || key.charAt(0) == '$') {
                child = new SequenceSearch(tag, validateSequence(key, true), false);
            } else if (key.equals("OR")) {
                skipSpace();
                child = readSearchClause(charset, SINGLE_CLAUSE, new OrOperation(), depth);
                skipSpace();
                readSearchClause(charset, SINGLE_CLAUSE, (LogicalOperation) child, depth);
            } else {
                throw new ImapParseException(tag, "unknown search tag: " + key);
            }
            target.addChild(child);
            first = false;
        } while (peekChar() != -1 && peekChar() != ')' && (nots > 0 || !single));

        if (nots > 0) {
            throw new ImapParseException(tag, "missing search-key after NOT");
        }
        depth--;
        return parent;
    }

    protected ImapSearch readSearch(Charset charset) throws IOException, ImapParseException {
        return readSearchClause(charset, MULTIPLE_CLAUSES, new AndOperation(), 0);
    }

    protected Charset readCharset() throws IOException, ImapParseException {
        String charset = readAstring();
        try {
            return Charset.forName(charset);
        } catch (Exception e) {
        }
        throw new ImapParseException(tag, "BADCHARSET", "unknown charset: " +
                charset.replace('\r', ' ').replace('\n', ' '), true);
    }
}
