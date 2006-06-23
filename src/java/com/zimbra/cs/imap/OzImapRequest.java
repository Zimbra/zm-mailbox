/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.imap;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.codec.binary.Base64;

import com.zimbra.cs.imap.ImapSession.ImapFlag;
import com.zimbra.cs.util.ZimbraLog;

/**
 * NB: Copied from ImapRequest.java on October 20, 2005 - while there
 * are two server impls we have to merge all changes to made to
 * ImapRequest.java since that date, to this file.
 *
 * @author dkarp
 */
class OzImapRequest {
    static final boolean[] ATOM_CHARS     = new boolean[128];
    static final boolean[] ASTRING_CHARS  = new boolean[128];
    static final boolean[] TAG_CHARS      = new boolean[128];
    static final boolean[] PATTERN_CHARS  = new boolean[128];
    static final boolean[] FETCH_CHARS    = new boolean[128];
    static final boolean[] NUMBER_CHARS   = new boolean[128];
    static final boolean[] SEQUENCE_CHARS = new boolean[128];
    static final boolean[] BASE64_CHARS   = new boolean[128];
    static final boolean[] SEARCH_CHARS   = new boolean[128];
    static final boolean[] REGEXP_ESCAPED = new boolean[128];
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
            SEQUENCE_CHARS['*'] = SEQUENCE_CHARS[':'] = SEQUENCE_CHARS[','] = true;
            BASE64_CHARS['+'] = BASE64_CHARS['/'] = BASE64_CHARS['='] = true;

            REGEXP_ESCAPED['('] = REGEXP_ESCAPED[')'] = REGEXP_ESCAPED['.'] = true;
            REGEXP_ESCAPED['['] = REGEXP_ESCAPED[']'] = REGEXP_ESCAPED['|'] = true;
            REGEXP_ESCAPED['^'] = REGEXP_ESCAPED['$'] = REGEXP_ESCAPED['?'] = true;
            REGEXP_ESCAPED['{'] = REGEXP_ESCAPED['}'] = REGEXP_ESCAPED['*'] = true;
            REGEXP_ESCAPED['\\'] = true;
        }

    private ImapSession mSession;
    private List<Object> mParts;
    private String mTag;
    private int mIndex, mOffset;

    OzImapRequest(ImapSession session) {
        mSession = session;
    }

    public OzImapRequest(String tag, List<Object> currentRequestParts, ImapSession session) {
        mSession = session;
        mTag = tag;
        mParts = currentRequestParts;
    }

    void setTag(String tag)  { mTag = tag; }

    boolean eof()  { return peekChar() == -1; }
    int peekChar() {
        if (mIndex >= mParts.size())
            return -1;
        Object part = mParts.get(mIndex);
        if (part instanceof String)
            return (mOffset >= ((String) part).length()) ? -1 : ((String) part).charAt(mOffset);
        else
            return (mOffset >= ((byte[]) part).length) ? -1 : ((byte[]) part)[mOffset];
    }

    void skipSpace() throws ImapParseException { skipChar(' '); }
    void skipChar(char c) throws ImapParseException {
        Object part = mParts.get(mIndex);
        if (part instanceof String && mOffset < ((String) part).length() && ((String) part).charAt(mOffset) == c)
            mOffset++;
        else 
            throw new ImapParseException(mTag, "end of line or wrong character; expected '" + c + '\'');
    }

    void skipNIL() throws ImapParseException {
        if (!readAtom().equals("NIL"))
            throw new ImapParseException(mTag, "did not find expected NIL");
    }

    private String getCurrentLine() throws ImapParseException {
        Object part = mParts.get(mIndex);
        if (!(part instanceof String))
            throw new ImapParseException(mTag, "should not be inside literal");
        return (String) part;
    }

    private byte[] getNextBuffer() throws ImapParseException {
        if ((mIndex + 1) >= mParts.size()) {
            throw new ImapParseException(mTag, "no next literal");
        }
        Object part = mParts.get(mIndex + 1);
        if (!(part instanceof byte[]))
            throw new ImapParseException(mTag, "in string next not literal");
        mIndex += 2;
        mOffset = 0;
        return (byte[]) part;
    }

    private String readContent(boolean[] acceptable) throws ImapParseException {
        String result = readContent(getCurrentLine(), mOffset, mTag, acceptable);
        mOffset += result.length();
        return result;
    }

    private static String readContent(String content, int offset, String tag, boolean[] acceptable) throws ImapParseException {
        int i;
        for (i = offset; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c > 0x7F || !acceptable[c])
                break;
        }
        if (i == offset)
            throw new ImapParseException(tag, "zero-length content");
        return content.substring(offset, i);
    }

    static String readTag(String line) throws ImapParseException { return readContent(line, 0, null, TAG_CHARS); }
    String readTag() throws ImapParseException     { mTag = readContent(TAG_CHARS);  return mTag; }
    String readAtom() throws ImapParseException    { return readContent(ATOM_CHARS).toUpperCase(); }
    String readNumber() throws ImapParseException  { return readContent(NUMBER_CHARS); }

    byte[] readBase64(boolean skipEquals) throws ImapParseException {
        // in some cases, "=" means to just return null and be done with it
        if (skipEquals && peekChar() == '=') {
            skipChar('=');  return null;
        }

        String encoded = readContent(BASE64_CHARS);
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

    private static final int LAST_PUNCT = 0, LAST_DIGIT = 1, LAST_STAR = 2;

    private String validateSequence(String value) throws ImapParseException {
        int i, last = LAST_PUNCT;
        boolean colon = false;
        for (i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c > 0x7F || !SEQUENCE_CHARS[c])
                throw new ImapParseException(mTag, "illegal character '" + c + "' in sequence");
            else if (c == '*') {
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
    String readSequence() throws ImapParseException {
        return validateSequence(readContent(SEQUENCE_CHARS));
    }

    String readQuoted() throws ImapParseException {
        String content = getCurrentLine();
        StringBuffer result = null;

        skipChar('"');
        int backslash = mOffset - 1;
        boolean escaped = false;
        for (int i = mOffset; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c > 0x7F || c == 0x00 || c == '\r' || c == '\n' || (escaped && c != '\\' && c != '"'))
                throw new ImapParseException(mTag, "illegal character '" + c + "' in quoted string");
            else if (!escaped && c == '\\') {
                if (result == null)
                    result = new StringBuffer();
                result.append(content.substring(backslash + 1, i));
                backslash = i;
                escaped = true;
            } else if (!escaped && c == '"') {
                mOffset = i + 1;
                String range = content.substring(backslash + 1, i);
                return (result == null ? range : result.append(range).toString());
            } else
                escaped = false;
        }
        throw new ImapParseException(mTag, "unexpected end of line in quoted string");
    }

    byte[] readLiteral() throws ImapParseException {
        return getNextBuffer();
    }

    byte[] readLiteral8() throws ImapParseException {
        if (peekChar() == '~')
            skipChar('~');
        return readLiteral();
    }

    private String readLiteral(String charset) throws ImapParseException {
        try {
            return new String(readLiteral(), charset);
        } catch (UnsupportedEncodingException e) {
            throw new ImapParseException(mTag, "BADCHARSET", "could not convert string to charset \"" + charset + '"');
        }
    }

    String readAstring() throws ImapParseException {
        return readAstring(null);
    }
    
    String readAstring(String charset) throws ImapParseException {
        return readAstring(charset, ASTRING_CHARS);
    }
    
    private String readAstring(String charset, boolean[] acceptable) throws ImapParseException {
        int c = peekChar();
        if (c == -1)        throw new ImapParseException(mTag, "unexpected end of line");
        else if (c == '{')  return readLiteral(charset != null ? charset : "utf-8");
        else if (c != '"')  return readContent(acceptable);
        else                return readQuoted();
    }

    private String readString(String charset) throws ImapParseException {
        int c = peekChar();
        if (c == -1)        throw new ImapParseException(mTag, "unexpected end of line");
        else if (c == '{')  return readLiteral(charset != null ? charset : "utf-8");
        else                return readQuoted();
    }

    private String readNstring(String charset) throws ImapParseException {
        int c = peekChar();
        if (c == -1)        throw new ImapParseException(mTag, "unexpected end of line");
        else if (c == '{')  return readLiteral(charset != null ? charset : "utf-8");
        else if (c != '"')  { skipNIL();  return null; }
        else                return readQuoted();
    }

    String readFolder() throws ImapParseException {
        return readFolder(false);
    }

    String readEscapedFolder() throws ImapParseException {
        return escapeFolder(readFolder(false), false);
    }

    String readFolderPattern() throws ImapParseException {
        return escapeFolder(readFolder(true), true);
    }

    private String readFolder(boolean isPattern) throws ImapParseException {
        String raw = readAstring(null, isPattern ? PATTERN_CHARS : ASTRING_CHARS);
        if (raw == null || raw.indexOf("&") == -1)
            return raw;
        try {
            return new String(raw.getBytes("US-ASCII"), "imap-utf-7");
        } catch (Throwable t) {
            ZimbraLog.imap.debug("ignoring error while decoding folder name: " + raw, t);
            return raw;
        }
    }

    private String escapeFolder(String unescaped, boolean isPattern) {
        if (unescaped == null)
            return null;
        StringBuffer escaped = new StringBuffer();
        for (int i = 0; i < unescaped.length(); i++) {
            char c = unescaped.charAt(i);
            // 6.3.8: "The character "*" is a wildcard, and matches zero or more characters at this position.
            //         The character "%" is similar to "*", but it does not match a hierarchy delimiter."
            if (isPattern && c == '*')                escaped.append(".*");
            else if (isPattern && c == '%')           escaped.append("[^/]*");
            else if (c > 0x7f || !REGEXP_ESCAPED[c])  escaped.append(c);
            else                                      escaped.append('\\').append(c);
        }
        return escaped.toString().toUpperCase();
    }
    
    List<String> readFlags() throws ImapParseException {
        List<String> tags = new ArrayList<String>();
        String content = getCurrentLine();
        boolean parens = (peekChar() == '(');
        if (parens)
            skipChar('(');
        if (!parens || peekChar() != ')')
            while (mOffset < content.length()) {
                if (peekChar() == '\\') {
                    skipChar('\\');
                    String flagName = '\\' + readAtom();
                    if (mSession != null) {
                        ImapFlag i4flag = mSession.getFlagByName(flagName);
                        if (i4flag == null || !i4flag.mListed)
                            throw new ImapParseException(mTag, "non-storable system tag \"" + flagName + '"');
                        tags.add(flagName);
                    }
                } else
                    tags.add(readAtom());
                if (parens && peekChar() == ')')      break;
                else if (mOffset < content.length())  skipSpace();
            }
        if (parens)
            skipChar(')');
        return tags;
    }

    Date readDate(DateFormat format) throws ImapParseException {
        String dateStr = (peekChar() == '"' ? readQuoted() : readContent(ATOM_CHARS));
        try {
            Date date = format.parse(dateStr);
            if (date.getTime() < 0)
                throw new ImapParseException(mTag, "date out of range");
            return date;
        } catch (java.text.ParseException e) {
            throw new ImapParseException(mTag, "invalid date format");
        }
    }

    Map<String, String> readParameters(boolean nil) throws ImapParseException {
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
        return params;
    }

    int readFetch(List<ImapPartSpecifier> parts) throws ImapParseException {
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
            else if (item.equals("RFC822.SIZE"))    attributes |= ImapHandler.FETCH_RFC822_SIZE;
            else if (item.equals("RFC822.HEADER"))  parts.add(new ImapPartSpecifier(item, "", "HEADER"));
            else if (item.equals("RFC822")) {
                attributes |= ImapHandler.FETCH_MARK_READ;
                parts.add(new ImapPartSpecifier(item, "", ""));
            } else if (item.equals("RFC822.TEXT")) {
                attributes |= ImapHandler.FETCH_MARK_READ;
                parts.add(new ImapPartSpecifier(item, "", "TEXT"));
            } else if (item.equals("BINARY.SIZE")) {
                String sectionPart = "";
                skipChar('[');
                while (peekChar() != ']') {
                    if (!sectionPart.equals("")) {
                        sectionPart += ".";  skipChar('.');
                    }
                    sectionPart += readNumber();
                }
                skipChar(']');
                if (sectionPart.equals(""))
                    attributes |= OzImapConnectionHandler.FETCH_BINARY_SIZE;
                else
                    parts.add(new ImapPartSpecifier(item, sectionPart, ""));
            } else if (item.equals("BODY") || item.equals("BODY.PEEK") || item.equals("BINARY") || item.equals("BINARY.PEEK")) {
                boolean binary = item.startsWith("BINARY");
                if (!item.endsWith(".PEEK"))
                    attributes |= ImapHandler.FETCH_MARK_READ;
                String sectionPart = "", sectionText = "";
                int partialStart = -1, partialCount = -1;
                List<String> headers = null;
                boolean done = false;

                skipChar('[');
                while (Character.isDigit((char) peekChar())) {
                    sectionPart += (sectionPart.equals("") ? "" : ".") + readNumber();
                    if (!(done = (peekChar() != '.')))
                        skipChar('.');
                }
                if (!done && peekChar() != ']') {
                    if (binary)
                        throw new ImapParseException(mTag, "section-text not permitted for BINARY");
                    sectionText = readAtom();
                    if (sectionText.equals("HEADER.FIELDS") || sectionText.equals("HEADER.FIELDS.NOT")) {
                        headers = new ArrayList<String>();
                        skipSpace();  skipChar('(');
                        while (peekChar() != ')') {
                            if (!headers.isEmpty())  skipSpace();
                            headers.add(readAstring().toUpperCase());
                        }
                        if (headers.isEmpty())
                            throw new ImapParseException(mTag, "header-list may not be empty");
                        skipChar(')');
                    } else if (sectionText.equals("MIME")) {
                        if (sectionPart.equals(""))
                            throw new ImapParseException(mTag, "\"MIME\" is not a valid section-spec");
                    } else if (!sectionText.equals("HEADER") && !sectionText.equals("TEXT"))
                        throw new ImapParseException(mTag, "unknown section-text \"" + sectionText + '"');
                }
                skipChar(']');
                if (peekChar() == '<') {
                    try {
                        skipChar('<');  partialStart = Integer.parseInt(readNumber());
                        skipChar('.');  partialCount = Integer.parseInt(readNumber());  skipChar('>');
                    } catch (NumberFormatException e) {
                        throw new ImapParseException(mTag, "invalid partial fetch specifier");
                    }
                }
                ImapPartSpecifier pspec = new ImapPartSpecifier(binary ? "BINARY" : "BODY", sectionPart, sectionText, partialStart, partialCount);
                pspec.setHeaders(headers);
                parts.add(pspec);
            } else
                throw new ImapParseException(mTag, "unknown FETCH attribute \"" + item + '"');
            if (list && peekChar() != ')')  skipSpace();
        } while (list && peekChar() != ')');
        if (list)  skipChar(')');
        return attributes;
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
    private static final Map<String, String> INDEXED_HEADER = new HashMap<String, String>();
        static {
            INDEXED_HEADER.put("CC",      "cc:");
            INDEXED_HEADER.put("FROM",    "from:");
            INDEXED_HEADER.put("SUBJECT", "subject:");
            INDEXED_HEADER.put("TO",      "to:");
        }

    private void readAndQuoteString(StringBuffer sb, String charset) throws ImapParseException {
        String content = readAstring(charset);
        sb.append('"');
        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '\\')      sb.append("\\\"");
            else if (c == '"')  sb.append("\\\\");
            else                sb.append(c);
        }
        sb.append('"');
    }
    private String readAndReformatDate() throws ImapParseException {
        DateFormat format = (mSession == null ? new SimpleDateFormat("dd-MMM-yyyy", Locale.US) : mSession.getDateFormat());
        Date date = readDate(format);
        return mSession.getZimbraDateFormat().format(date);
    }

    private ImapFlag getFlag(String name) {
        return mSession == null ? null : mSession.getFlagByName(name);
    }
    private void insertFlag(String name, StringBuffer sb, TreeMap<Integer, Object> insertions) {
        insertFlag(getFlag(name), sb, insertions);
    }
    private void insertFlag(ImapFlag i4flag, StringBuffer sb, TreeMap<Integer, Object> insertions) {
        insertions.put(sb.length(), i4flag);
    }

    private static final boolean SINGLE_CLAUSE = true, MULTIPLE_CLAUSES = false;
    private static final String SUBCLAUSE = "";

    private StringBuffer readSearchClause(StringBuffer search, TreeMap<Integer, Object> insertions, String charset, boolean single)
    throws ImapParseException {
        boolean first = true;
        int nots = 0;
        do {
            if (!first)  { skipSpace(); }
            int c = peekChar();
            // key will be "" iff we're opening a new subclause...
            String key = (c == '(' ? SUBCLAUSE : readContent(SEARCH_CHARS).toUpperCase());

            if (key.equals("NOT"))  { nots++; first = false; continue; }
            else if ((nots % 2) != 0) {
                if (NEGATED_SEARCH.containsKey(key))  key = NEGATED_SEARCH.get(key);
                else                                  search.append('-');
            }
            nots = 0;

            if (key.equals("ALL"))              search.append("item:all");
            else if (key.equals("ANSWERED"))    search.append("is:replied");
            else if (key.equals("DELETED"))     search.append("tag:\\Deleted");
            else if (key.equals("DRAFT"))       search.append("is:draft");
            else if (key.equals("FLAGGED"))     search.append("is:flagged");
            else if (key.equals("RECENT"))      insertFlag("\\RECENT", search, insertions);
            else if (key.equals("NEW"))         { search.append("(is:read "); insertFlag("\\RECENT", search, insertions);
                                                  search.append(')'); }
            else if (key.equals("OLD"))         { search.append('-'); insertFlag("\\RECENT", search, insertions); }
            else if (key.equals("SEEN"))        search.append("is:read");
            else if (key.equals("UNANSWERED"))  search.append("is:unreplied");
            else if (key.equals("UNDELETED"))   search.append("-tag:\\Deleted");
            else if (key.equals("UNDRAFT"))     search.append("-is:draft");
            else if (key.equals("UNFLAGGED"))   search.append("is:unflagged");
            else if (key.equals("UNSEEN"))      search.append("is:unread");
            // XXX: BCC always returns no results because we don't separately index that field
            else if (key.equals("BCC"))         { skipSpace(); search.append("item:none"); readAstring(charset); }
            else if (key.equals("BEFORE"))      { skipSpace(); search.append("before:").append(readAndReformatDate()); }
            else if (key.equals("BODY"))        { skipSpace(); readAndQuoteString(search, charset); }
            else if (key.equals("CC"))          { skipSpace(); search.append("cc:"); readAndQuoteString(search, charset); }
            else if (key.equals("FROM"))        { skipSpace(); search.append("from:"); readAndQuoteString(search, charset); }
            else if (key.equals("HEADER"))      { skipSpace(); String hdr = readAstring().toUpperCase(), prefix = INDEXED_HEADER.get(hdr);
                                                  if (prefix == null)  throw new ImapParseException(mTag, "unindexed header: " + hdr);
                                                  skipSpace(); search.append(prefix); readAndQuoteString(search, charset); }
            else if (key.equals("KEYWORD"))     { skipSpace(); ImapFlag i4flag = getFlag(readAtom());
                                                  if (i4flag != null && !i4flag.mPositive)   search.append('-');
                                                  if (i4flag != null && !i4flag.mPermanent)  insertFlag(i4flag, search, insertions);
                                                  else  search.append(i4flag == null ? "item:none" : "tag:" + i4flag.mName); }
            else if (key.equals("LARGER"))      { skipSpace(); search.append("larger:").append(readNumber()); }
            else if (key.equals("ON"))          { skipSpace(); search.append("date:").append(readAndReformatDate()); }
            // FIXME: SENTBEFORE, SENTON, and SENTSINCE reference INTERNALDATE, not the Date header
            else if (key.equals("SENTBEFORE"))  { skipSpace(); search.append("before:").append(readAndReformatDate()); }
            else if (key.equals("SENTON"))      { skipSpace(); search.append("date:").append(readAndReformatDate()); }
            else if (key.equals("SENTSINCE"))   { skipSpace(); search.append("after:").append(readAndReformatDate()); }
            else if (key.equals("SINCE"))       { skipSpace(); search.append("after:").append(readAndReformatDate()); }
            else if (key.equals("SMALLER"))     { skipSpace(); search.append("smaller:").append(readNumber()); }
            else if (key.equals("SUBJECT"))     { skipSpace(); search.append("subject:"); readAndQuoteString(search, charset); }
            else if (key.equals("TEXT"))        { skipSpace(); readAndQuoteString(search, charset); }
            else if (key.equals("TO"))          { skipSpace(); search.append("to:"); readAndQuoteString(search, charset); }
            else if (key.equals("UID"))         { skipSpace(); insertions.put(search.length(), '-' + readSequence()); }
            else if (key.equals("UNKEYWORD"))   { skipSpace(); ImapFlag i4flag = getFlag(readAtom());
                                                  if (i4flag != null && i4flag.mPositive)    search.append('-');
                                                  if (i4flag != null && !i4flag.mPermanent)  insertFlag(i4flag, search, insertions);  
                                                  else  search.append(i4flag == null ? "item:all" : "tag:" + i4flag.mName); }
            else if (key.equals(SUBCLAUSE))     { skipChar('(');  readSearchClause(search, insertions, charset, MULTIPLE_CLAUSES);  skipChar(')'); }
            else if (Character.isDigit(key.charAt(0)) || key.charAt(0) == '*')
                insertions.put(search.length(), validateSequence(key));
            else if (key.equals("OR")) {
                search.append("((");      skipSpace();  readSearchClause(search, insertions, charset, SINGLE_CLAUSE);
                search.append(") or (");  skipSpace();  readSearchClause(search, insertions, charset, SINGLE_CLAUSE);
                search.append("))");
            }
            else throw new ImapParseException(mTag, "unknown search tag: " + key);

            search.append(' ');
            first = false;
        } while (peekChar() != -1 && peekChar() != ')' && (nots > 0 || !single));

        if (nots > 0)
            throw new ImapParseException(mTag, "missing search-key after NOT");
        return search;
    }
    String readSearch(TreeMap<Integer, Object> insertions) throws ImapParseException {
        String charset = null;
        StringBuffer search = new StringBuffer();
        int c = peekChar();
        if (c == 'c' || c == 'C') {
            int offset = mOffset, index = mIndex;
            String first = readAtom();
            if (first.equals("CHARSET")) {
                skipSpace();  charset = readAstring();  skipSpace();
                boolean charsetOK = false;
                try {
                    charsetOK = Charset.isSupported(charset);
                } catch (IllegalCharsetNameException icne) { }
                if (!charsetOK)
                    throw new ImapParseException(mTag, "BADCHARSET", "unknown charset: " + charset);
            } else {
                mOffset = offset;  mIndex = index;
            }
        }
        return readSearchClause(search, insertions, charset, MULTIPLE_CLAUSES).toString();
    }
}
