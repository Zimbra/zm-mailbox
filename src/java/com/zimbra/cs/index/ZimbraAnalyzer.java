/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.index;

import java.io.Reader;
import java.io.StringReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import com.zimbra.common.service.ServiceException;

/***
 * Global analyzer wrapper for Zimbra Indexer.
 * <p>
 * You DO NOT need to instantiate multiple copies of this class -- just call
 * ZimbraAnalyzer.getInstance() whenever you need an instance of this class.
 *
 * @since Apr 26, 2004
 * @author tim
 * @author ysasaki
 */
public class ZimbraAnalyzer extends StandardAnalyzer {
    private static final ZimbraAnalyzer sInstance = new ZimbraAnalyzer();

    private static final HashMap<String, Analyzer> sAnalyzerMap =
        new HashMap<String, Analyzer>();

    protected ZimbraAnalyzer() {
        super(LuceneIndex.VERSION);
    }

    /***
     * Extension analyzers.
     * <p>
     * Extension analyzers must call {@link #registerAnalyzer(String, Analyzer)}
     * on startup.
     *
     * @param name
     * @return analyzer
     */
    public static Analyzer getAnalyzer(String name) {
        Analyzer toRet = sAnalyzerMap.get(name);
        if (toRet == null) {
            return getDefaultAnalyzer();
        }
        return toRet;
    }

    /**
     * We maintain a single global instance for our default analyzer, since it
     * is completely thread safe.
     *
     * @return singleton
     */
    public static Analyzer getDefaultAnalyzer() {
        return sInstance;
    }

    /**
     * A custom Lucene Analyzer is registered with this API, usually by a Zimbra
     * Extension.
     * <p>
     * Accounts are configured to use a particular analyzer by setting the
     * "zimbraTextAnalyzer" key in the Account or COS setting.
     *
     * The custom analyzer is assumed to be a stateless single instance
     * (although it can and probably should return a new TokenStream instance
     * from it's APIs)
     *
     * @param name a unique name identifying the Analyzer, it is referenced by
     *  Account or COS settings in LDAP.
     * @param analyzer a Lucene analyzer instance which can be used by accounts
     *  that are so configured.
     * @throws ServiceException
     */
    public static void registerAnalyzer(String name, Analyzer analyzer)
        throws ServiceException {

        if (sAnalyzerMap.containsKey(name)) {
            throw ServiceException.FAILURE("Cannot register analyzer: " +
                    name + " because there is one already registered with that name.",
                    null);
        }

        sAnalyzerMap.put(name, analyzer);
    }

    /**
     * Remove a previously-registered custom Analyzer from the system.
     *
     * @param name
     */
    public static void unregisterAnalyzer(String name) {
        sAnalyzerMap.remove(name);
    }

    public static String getAllTokensConcatenated(String fieldName, String text) {
        Reader reader = new StringReader(text);
        return getAllTokensConcatenated(fieldName, reader);
    }

    public static String getAllTokensConcatenated(String fieldName, Reader reader) {
        StringBuilder toReturn = new StringBuilder();

        TokenStream stream = sInstance.tokenStream(fieldName, reader);
        TermAttribute term = stream.addAttribute(TermAttribute.class);

        try {
            stream.reset();
            while (stream.incrementToken()) {
                toReturn.append(term.term());
                toReturn.append(" ");
            }
            stream.end();
            stream.close();
        } catch (IOException e) {
            e.printStackTrace(); //otherwise eat it
        }

        return toReturn.toString();
    }

    /**
     * "Tokenizer" which returns the entire input as a single token. Used for
     * KEYWORD fields.
     */
    static class DontTokenizer extends CharTokenizer {

        DontTokenizer(Reader input) {
            super(input);
        }

        @Override
        protected boolean isTokenChar(char c) {
            return true;
        }
    }

    // for indexing
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
        return tokenStreamInternal(fieldName, reader, true);
    }

    // for searching
    public TokenStream tokenStreamSearching(String fieldName, Reader reader) {
        return tokenStreamInternal(fieldName, reader, false);
    }

    /*
     * indexing: (bug 44191, see comments for AddressTokenFilter)
     *
     * @param indexing true if we are indexing, false if we are searching
     */
    private TokenStream tokenStreamInternal(String fieldName, Reader reader,
            boolean indexing) {
        if (fieldName.equals(LuceneFields.L_H_MESSAGE_ID)) {
            return new DontTokenizer(reader);
        }

        if (fieldName.equals(LuceneFields.L_FIELD)) {
            return new FieldTokenStream(reader);
        }

        if (fieldName.equals(LuceneFields.L_ATTACHMENTS) ||
                fieldName.equals(LuceneFields.L_MIMETYPE)) {
            return new MimeTypeTokenFilter(CommaSeparatedTokenStream(reader));
        } else if (fieldName.equals(LuceneFields.L_SORT_SIZE)) {
            return new SizeTokenFilter(new NumberTokenStream(reader));
        } else if (fieldName.equals(LuceneFields.L_H_FROM)
                || fieldName.equals(LuceneFields.L_H_TO)
                || fieldName.equals(LuceneFields.L_H_CC)
                || fieldName.equals(LuceneFields.L_H_X_ENV_FROM)
                || fieldName.equals(LuceneFields.L_H_X_ENV_TO)) {
            return new AddressTokenFilter(new AddrCharTokenizer(reader), indexing);
        } else if (fieldName.equals(LuceneFields.L_CONTACT_DATA)) {
            return new ContactDataFilter(new AddrCharTokenizer(reader)); // for bug 48146
        } else if (fieldName.equals(LuceneFields.L_FILENAME)) {
            return new FilenameTokenizer(reader);
        } else {
            return super.tokenStream(fieldName, reader);
        }
    }

    @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader)
        throws IOException {

        return tokenStream(fieldName, reader);
    }

    /**
     * Special Analyzer for structured-data field (see LuceneFields.L_FIELD )
     * <p>
     * "fieldname=Val1 val2 val3\nfieldname2=val2_1 val2_2 val2_3\n" becomes
     * "fieldname:Val1 fieldname:val2 fieldname:val3\nfieldname2:val2_1 fieldname2:val2_2 fieldname2:val2_3"
     */
    static class FieldTokenStream extends Tokenizer {
        protected static final char FIELD_SEPARATOR = ':';
        protected static final char EOL = '\n';

        private int mOffset = 0;
        private String mFieldName = null;
        private List<Token> mValues = new ArrayList<Token>();
        private TermAttribute termAttr = addAttribute(TermAttribute.class);

        FieldTokenStream(Reader reader) {
            super(reader);
        }

        protected String stripFieldName(String fieldName) {
            return fieldName;
        }

        @Override
        public boolean incrementToken() throws IOException {
            while (mValues.isEmpty() ||
                    mFieldName == null || mFieldName.length() == 0) {
                if (!bufferNextLine()) {
                    if (mValues.isEmpty()) {
                        return false;
                    } else {
                        break;
                    }
                }
            }
            Token token = mValues.remove(0);
            termAttr.setTermBuffer(token.term());
            return true;
        }

        protected boolean isWhitespace(char ch) {
            switch (ch) {
                case ' ':
                case '\t':
                case '"': // conflict with query language
                case '\'':
                case ';':
                case ',':
                // case '-': don't remove - b/c of negative numbers!
                case '<':
                case '>':
                case '[':
                case ']':
                case '(':
                case ')':
                case '*': // wildcard conflict w/ query language
                    return true;
            }
            return false;
        }

        /**
         * Strip out punctuation
         *
         * @param val
         * @param ch
         */
        protected void addCharToValue(StringBuilder val, char ch) {
            if (!Character.isISOControl(ch)) {
                val.append(Character.toLowerCase(ch));
            }
        }

        /**
         * Strip out chars we absolutely don't want in the index -- useful just
         * to stop collisions with the query grammar, and stop control chars,
         * etc.
         *
         * @param val
         * @param ch
         */
        protected void addCharToFieldName(StringBuilder val, char ch) {
            if (ch == ':') {
                return;
            }
            if (!Character.isISOControl(ch)) {
                val.append(Character.toLowerCase(ch));
            }
        }

        /**
         * TODO: Try to reuse token instances accommodating to the new
         * TokenStream API.
         *
         * @param curWord word to be added
         * @param wordStart offset into the string (for term position)
         * @param wordEnd offset into the string (for term position)
         */
        protected void addToken(String curWord, int wordStart, int wordEnd) {
            if (mFieldName.length() > 0) {
                if (curWord.length() > 0) {
                    String token = mFieldName + ":" + curWord;
                    mValues.add(new Token(token, wordStart, wordEnd));
                }
            }
        }

        /**
         * 1) fieldName=null
         * 2) get next char
         * 3) if find '='
         *    - save fieldName
         *    - goto 6
         * 3.5) if find EOL
         *    - no values, goto END
         * 4) buffer char in fieldName
         * 5) goto 2
         * 6) curWord = null
         * 7) get next char
         * 8) if find whitespace then
         *    - save fieldName:curWord pair if not empty
         *    - curWord = null
         *    - goto 7
         * 9) if find EOL then
         *    - save fieldName:curWord pair if not empty
         *    - goto END
         * 10) save char in curWord
         * 11) goto 7
         *
         * @return FALSE at EOF for input
         */
        protected boolean bufferNextLine() {
            int c = 0;

            mFieldName = null;
            mValues.clear();

            try {
                // step 1 - 5
                StringBuilder fieldName = new StringBuilder();
                while (mFieldName == null && (c = input.read()) >= 0) {
                    mOffset++;
                    char ch = (char)c;
                    if (ch == FIELD_SEPARATOR) {
                        mFieldName = stripFieldName(fieldName.toString());
                    } else if (ch == '\n') {
                        return true;
                    }
                    addCharToFieldName(fieldName, ch);
                }

                if (c < 0) {
                    return false; // EOL
                }

                assert (mFieldName != null);

                StringBuilder curWord = new StringBuilder();
                int wordStart = mOffset;

                // step 7 - 11
                while (true) {
                    c = input.read();
                    mOffset++;

                    // at EOF?  Finish current word if one exists...
                    if (c < 0) {
                        if (curWord.length() > 0) {
                            addToken(curWord.toString(), wordStart, mOffset);
                        }
                        return false;
                    }

                    char ch = (char) c;

                    // HACKHACKHACK -- treat '-' as whitespace UNLESS it is at the beginning of a word!
                    if (isWhitespace(ch) || (curWord.length() > 0 && ch == '-')) {
                        if (curWord.length() > 0) {
                            addToken(curWord.toString(), wordStart, mOffset);
                            curWord = new StringBuilder();
                        }
                    } else if (ch == EOL) {
                        if (curWord.length() > 0) {
                            addToken(curWord.toString(), wordStart, mOffset);
                        }
                        return true;
                    } else {
                        addCharToValue(curWord, ch);
                    }
                }
                // notreached
            } catch (IOException e) {
                mFieldName = null;
                mValues.clear();
                return false;
            }
        }

    }

    /**
     * numbers separated by ' ' or '\t'
     */
    static class NumberTokenStream extends Tokenizer {
        protected Reader mReader;
        protected int mEndPos = 0;
        private TermAttribute termAttr = addAttribute(TermAttribute.class);
        private OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);

        NumberTokenStream(Reader reader) {
            super(reader);
        }

        @Override
        public boolean incrementToken() throws IOException {
            int startPos = mEndPos;
            StringBuilder buf = new StringBuilder(10);

            while (true) {
                int c = input.read();
                mEndPos++;
                switch (c) {
                    case -1:
                        if (buf.length() == 0) {
                            return false;
                        }
                        // no break!
                    case ' ':
                    case '\t':
                        if (buf.length() != 0) {
                            termAttr.setTermBuffer(buf.toString());
                            offsetAttr.setOffset(startPos, mEndPos - 1);
                            return true;
                        }
                        break;
                    case '0':
                    case '1':
                    case '2':
                    case '3':
                    case '4':
                    case '5':
                    case '6':
                    case '7':
                    case '8':
                    case '9':
                        buf.append((char) c);
                        break;
                    default:
                        // ignore char
                }
            }
        }
    }

    /**
     * NumberTokenStream converted into ascii-sortable (base-36 ascii encoded)
     * numbers.
     */
    static class SizeTokenFilter extends TokenFilter {
        private TermAttribute termAttr = addAttribute(TermAttribute.class);

        SizeTokenFilter(TokenStream in) {
            super(in);
        }

        SizeTokenFilter(TokenFilter in) {
            super(in);
        }

        public static String encodeSize(String size) {
            return size;
        }

        public static String encodeSize(long lsize) {
            return Long.toString(lsize);
        }

        public static long decodeSize(String size) {
            return Long.parseLong(size);
        }

        @Override
        public boolean incrementToken() throws IOException {
            while (input.incrementToken()) {
                String size = encodeSize(termAttr.term());
                if (size == null) {
                   continue;
                }
                termAttr.setTermBuffer(size);
                return true;
            }
            return false;
        }

    }

    /**
     * comma-separated values, typically for content type list
     *
     * @param reader
     * @return TokenStream
     */
    private TokenStream CommaSeparatedTokenStream(Reader reader) {
        return new CharTokenizer(reader) {

            @Override
            protected boolean isTokenChar(char c) {
                return c != ',';
            }

            @Override
            protected char normalize(char c) {
                return Character.toLowerCase(c);
            }
        };
    }

    /**
     * Handles situations where a single string needs to be inserted multiple
     * times into the index -- e.g. "text/plain" gets inserted as "text/plain"
     * and "text" and "plain", "foo@bar.com" as "foo@bar.com" and "foo" and
     * "@bar.com"
     */
    static abstract class MultiTokenFilter extends TokenFilter {
        // returns the next split point
        protected abstract int getNextSplit(String s);

        protected int mMaxSplits = 1;
        protected boolean mIncludeSeparatorChar = false;
        protected boolean mNoLastToken = false;
        Token mCurToken = null;
        protected int mNextSplitPos;
        protected int mNumSplits;
        private TermAttribute termAttr = addAttribute(TermAttribute.class);
        private OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);
        private TypeAttribute typeAttr = addAttribute(TypeAttribute.class);

        MultiTokenFilter(TokenStream in) {
            super(in);
        }

        MultiTokenFilter(TokenFilter in) {
            super(in);
        }

        /**
         * At this point, a token has been extracted from input, and the full
         * token has been returned to the stream. Now we want to return all the
         * "split" forms of the token.
         *
         * On the first call to this API for this token, mNextSplitPos is set to
         * the value of getNextSplit(full_token_text)..., then this API is
         * called repeatedly until mCurToken is cleared.
         */
        public Token nextSplit() {
            if (mNextSplitPos > 0 && mNumSplits < mMaxSplits) {
                // split another piece, save our state, and return...
                mNumSplits++;
                String termText = mCurToken.term();
                String stringToRet = termText.substring(0, mNextSplitPos);

                Token tokenToReturn = new Token(stringToRet,
                        mCurToken.startOffset(),
                        mCurToken.startOffset() + mNextSplitPos,
                        mCurToken.type());

                if (!mIncludeSeparatorChar) {
                    mNextSplitPos++;
                }
                String secondPart = termText.substring(mNextSplitPos);
                if (mNumSplits < mMaxSplits) {
                    mNextSplitPos = getNextSplit(secondPart);
                }

                if (mNoLastToken == true) {
                    mCurToken = null;
                } else {
                    mCurToken = new Token(secondPart,
                            mCurToken.startOffset() + mNextSplitPos,
                            mCurToken.endOffset(), mCurToken.type());
                }

                return tokenToReturn;
            }

            // if we get here, then we've either split as many times as we're
            // allowed, OR we've run out of places to split..

            // no more splitting, just return what's left...
            Token toRet = mCurToken;
            mCurToken = null;
            return toRet;
        }

        @Override
        public boolean incrementToken() throws IOException {
            while (true) {
                if (mCurToken == null) {
                    // Get a new token, and insert the full token (unsplit) into
                    // the index.
                    if (!input.incrementToken()) {
                        return false;
                    }

                    // Does it have any sub-parts that need to be added separately?
                    // If so, then save them as internal state: we'll add them in a bit.
                    String termText = termAttr.term();
                    if (termText.length() <= 1) { // ignore short term text
                        continue;
                    }
                    mNextSplitPos = getNextSplit(termText);
                    if (mNextSplitPos <= 0) {
                        // no sub-tokens
                        return true;
                    }

                    // Now, Insert the full string as a token...we might continue down below
                    // (other parts) if there is more to add...
                    mNumSplits = 0;
                    mCurToken = new Token(termText, offsetAttr.startOffset(),
                            offsetAttr.endOffset(), typeAttr.type());

                    return true;
                } else {
                    // once we get here, we know that the full text has been inserted
                    // once as a single token, now we need to insert all the split tokens
                    Token token = nextSplit();
                    termAttr.setTermBuffer(token.term());
                    offsetAttr.setOffset(token.startOffset(), token.endOffset());
                    typeAttr.setType(token.type());
                    return true;
                }
            }
        }

    }

    /***
     * Email address tokenizer.  For example:
     *   "Tim Brennan" <tim@bar.foo.com>
     * Is tokenized as:
     *    Tim
     *    Brennan
     *    tim
     *    tim@foo.com
     *    @foo.com
     *    foo  -- for bug 30638
     *
     * @author tim
     */
    static class AddressTokenFilter extends MultiTokenFilter {

        /*
         * bug 44191
         *
         * The fix for bug 30638 (change 119098) broke searching fields using this filter,
         * if the search string is a full email address.
         * (see https://bugzilla.zimbra.com/show_bug.cgi?id=42874#c23)
         *
         * Fields using AddressTokenFilter are:
         *     LuceneFields.L_H_FROM
         *     LuceneFields.L_H_TO
         *     LuceneFields.L_H_CC
         *     LuceneFields.L_H_X_ENV_FROM
         *     LuceneFields.L_H_X_ENV_TO
         *
         * This is because an email address, e.g., user1@zimbra.com is tokenized to:
         *
         * pre-change-119098 (i.e. FRANKLIN)
         *     user1@zimbra.com
         *     user1
         *     @zimbra.com
         *
         * post-change-119098 (i.e. in GNR and leter)
         *     user1@zimbra.com
         *     @zimbra
         *     user1
         *     @zimbra.com
         *     zimbra.com
         *
         * bug 44191 is:
         *     The email address is indexed with the sequence of tokens produced by AddressTokenFilter,
         *     and searched with the same sequence of terms using a Lucene PhraseQuery with slop 0
         *     (i.e. exact match).  Using an index built in FRANKLIN, the GNR code cannot find terms
         *     "@zimbra" and "zimbra.com".   The way PhraseQuery (with slop=0) works is that it has
         *     to match all terms, in the same sequence, otherwise no hit.
         *
         *     Reindexing the whole mailbox on all mailboxes will fix the problem, but it is expensive.
         *
         * We put a fix in 6_0_5 (bug 42874) to reindex all contacts.  That fixed a mail filter
         * problem when the filter is "Address in" "{from|to|cc|bcc}" "in" "My Contacts", because the
         * search for that is a "to:{email address}" query.
         *
         * In GNR, we need to be able to find items indexed with the pre-change-119098 way,
         * without having to reindex the entire mailbox.
         *
         * This is the solution for bug 44191:
         * In FRANKLIN: no change, indexing and searching with the pre-change-119098 way.
         *
         * In GNR(and later):
         *   - indexing: use the post-change-119098 way (for bug 30638, so searching by the "main" domain works)
         *   - searching: use the pre-change-119098 way
         *       This is because it just so happens that Terms produced/indexed by the pre-change-119098 code
         *       can always be found by PhraseQuery with sloppiness set to 1, with index built by either
         *       the pre-change-119098 or post-change-119098 way.
         *
         */

        // whether to produce token with the main domain
        // true: post-change-119098
        // false: pre-change-119098
        boolean mWantMainDomain;

        Queue<Token> mSplitStrings;

        AddressTokenFilter(TokenFilter in, boolean wantMainDomain) {
            super(in);
            mIncludeSeparatorChar = true;
            mMaxSplits = 4;
            mWantMainDomain = wantMainDomain;
        }

        AddressTokenFilter(TokenStream in, boolean wantMainDomain) {
            super(in);
            mIncludeSeparatorChar = true;
            mMaxSplits = 4;
            mWantMainDomain = wantMainDomain;
        }

        @Override
        protected int getNextSplit(String s) {
            return s.indexOf("@");
        }

        /**
         * On first call, we have one toplevel 'token' from our parent filter
         * The only interesting case is when there's an @ sign such as:
         *        foo@a.b.c.d
         *
         */
        @Override
        public Token nextSplit() {
            if (mSplitStrings == null) {
                mSplitStrings = new LinkedList<Token>();

                String termText = mCurToken.term();
                // split on the "@"
                String lhs = termText.substring(0, mNextSplitPos);

                // yes, we want to include the @!
                String rhs = termText.substring(mNextSplitPos);

                if (mWantMainDomain) {
                    // now, split the left part on the "."
                    String[] lhsParts = lhs.split("\\.");
                    if (lhsParts.length > 1) {
                        for (String part : lhsParts) {
                            mSplitStrings.add(new Token(part,
                                    mCurToken.startOffset(),
                                    mCurToken.endOffset(),
                                    mCurToken.type()));
                        }
                    }

                    // now, split the right part on the "."
                    String[] rhsParts = rhs.split("\\.");
                    if (rhsParts.length > 1) {
                        // for bug 30638
                        mSplitStrings.add(new Token(
                                rhsParts[rhsParts.length - 2],
                                mCurToken.startOffset(), mCurToken.endOffset(),
                                mCurToken.type()));
                    }

                } else {
                    // now, split the first part on the "."
                    String[] lhsParts = lhs.split("\\.");
                    if (lhsParts.length > 1) {
                        int curOffset = mCurToken.startOffset();
                        for (String part : lhsParts) {
                            mSplitStrings.add(new Token(part,
                                    curOffset, curOffset + part.length(),
                                    mCurToken.type()));

                            curOffset += part.length() + 1;
                        }
                    }
                }

                // the full part to the left of the @
                mSplitStrings.add(new Token(lhs,
                        mCurToken.startOffset(),
                        mCurToken.startOffset() + mNextSplitPos,
                        mCurToken.type()));

                // the full part to the right of the @
                mSplitStrings.add(new Token(rhs,
                        mCurToken.startOffset() + mNextSplitPos,
                        mCurToken.endOffset(), mCurToken.type()));

                if (mWantMainDomain) {
                    // the full part to the right of the @, not including the @
                    // see bug 30638
                    if (rhs.length() > 1) {
                        mSplitStrings.add(new Token(rhs.substring(1),
                                mCurToken.startOffset() + mNextSplitPos,
                                mCurToken.endOffset(), mCurToken.type()));
                    }
                }
            }

            // split another piece, save our state, and return...
            mNumSplits++;
            Token toRet = mSplitStrings.remove();

            if (mSplitStrings.isEmpty()) {
                mSplitStrings = null;
                mCurToken = null;
            }

            return toRet;
        }
    }

    static class FilenameTokenizer extends CharTokenizer {

        FilenameTokenizer(Reader reader) {
            super(reader);
        }

        @Override
        protected boolean isTokenChar(char c) {
            switch (c) {
                case ',':
                case ' ':
                case '\r':
                case '\n':
                case '.':
                    return false;
                default:
                    return true;
            }
        }

        @Override
        protected char normalize(char c) {
            return Character.toLowerCase(c);
        }
    }


    /**
     * Tokenizer for email addresses. Skips & Splits at \r\n<>\",\'
     */
    static class AddrCharTokenizer extends CharTokenizer {

        AddrCharTokenizer(Reader reader) {
            super(reader);
        }

        @Override
        protected boolean isTokenChar(char ch) {
            switch (ch) {
                case ' ':
                case '\r':
                case '\n':
                case '<':
                case '>':
                case '\"':
                case ',':
                case '\'':
                case '(':
                case ')':
                case '[':
                case ']':
                    return false;
            }
            return true;
        }

        @Override
        protected char normalize(char c) {
            return Character.toLowerCase(c);
        }
    }

    /**
     * Swallow '.'. Include '.' in a token only when it is not the only char
     * in the token.
     */
    static class ContactDataFilter extends TokenFilter {
        private TermAttribute termAttr = addAttribute(TermAttribute.class);

        ContactDataFilter(AddrCharTokenizer input) {
            super(input);
        }

        @Override
        public boolean incrementToken() throws IOException {
            while (input.incrementToken()) {
                if (!".".equals(termAttr.term())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * image/jpeg --> "image/jpeg" and "image"
     *
     * @author tim
     */
    static class MimeTypeTokenFilter extends MultiTokenFilter {

        MimeTypeTokenFilter(TokenFilter in) {
            super(in);
            init();
        }

        MimeTypeTokenFilter(TokenStream in) {
            super(in);
            init();
        }

        private void init() {
            mMaxSplits = 1;
            mNoLastToken = true;
        }

        @Override
        protected int getNextSplit(String s) {
            return s.indexOf("/");
        }
    }

}
