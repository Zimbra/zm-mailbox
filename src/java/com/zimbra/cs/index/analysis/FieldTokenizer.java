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
package com.zimbra.cs.index.analysis;

import java.io.IOException;
import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.zimbra.cs.index.LuceneFields;

/**
 * Special {@link Analyzer} for structured-data field.
 * <p>
 * <ul>
 *  <li>fieldname:Val1 val2 val3
 *  <li>fieldname2:val2_1 val2_2 val2_3
 * </ul>
 * becomes
 * <ul>
 *  <li>fieldname:Val1
 *  <li>fieldname:val2
 *  <li>fieldname:val3
 *  <li>fieldname2:val2_1
 *  <li>fieldname2:val2_2
 *  <li>fieldname2:val2_3
 * </ul>
 *
 * @see LuceneFields#L_FIELD
 * @author tim
 * @author ysasaki
 */
public final class FieldTokenizer extends Tokenizer {

    private static final char FIELD_SEPARATOR = ':';
    private static final char EOL = '\n';

    private int offset = 0;
    private String field;
    private TermAttribute termAttr = addAttribute(TermAttribute.class);
    private OffsetAttribute offsetAttr = addAttribute(OffsetAttribute.class);

    public FieldTokenizer(Reader reader) {
        super(reader);
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();

        while (true) {
            if (field == null) {
                StringBuilder buff = new StringBuilder();
                while (field == null) {
                    int c = input.read();
                    if (c < 0) { // EOF
                        return false;
                    }
                    offset++;
                    switch (c) {
                        case FIELD_SEPARATOR:
                            if (buff.length() > 0) {
                                field = stripFieldName(buff.toString());
                            }
                            break;
                        case EOL: // Reached EOL without any words
                            field = null;
                            break; // back to top
                        default:
                            addCharToFieldName(buff, (char) c);
                            break;
                    }
                }
            }

            StringBuilder word = new StringBuilder();
            int start = offset;

            while (true) {
                int c = input.read();
                offset++;

                if (c < 0) { // EOF
                    if (word.length() > 0) {
                        setAttrs(word.toString(), start, offset);
                        return true;
                    } else {
                        return false;
                    }
                }

                char ch = (char) c;

                // treat '-' as whitespace UNLESS it is at the beginning of a word
                if (isWhitespace(ch) || (ch == '-' && word.length() > 0)) {
                    if (word.length() > 0) {
                        setAttrs(word.toString(), start, offset);
                        return true;
                    }
                } else if (ch == EOL) {
                    if (word.length() > 0) {
                        setAttrs(word.toString(), start, offset);
                        field = null;
                        return true;
                    } else { // Reached EOL without any words
                        field = null;
                        break; // back to top
                    }
                } else {
                    addCharToValue(word, ch);
                }
            }
        }

    }

    private boolean isWhitespace(char ch) {
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
            default:
                return false;
        }
    }

    private String stripFieldName(String fieldName) {
        return fieldName;
    }

    /**
     * Strip out punctuation
     *
     * @param buff string buffer to append the character to
     * @param ch character to append
     */
    private void addCharToValue(StringBuilder buff, char ch) {
        if (!Character.isISOControl(ch)) {
            buff.append(Character.toLowerCase(ch));
        }
    }

    /**
     * Strip out chars we absolutely don't want in the index -- useful just
     * to stop collisions with the query grammar, and stop control chars,
     * etc.
     *
     * @param buff string buffer to append the character to
     * @param ch character to append
     */
    private void addCharToFieldName(StringBuilder buff, char ch) {
        if (ch != ':' && !Character.isISOControl(ch)) {
            buff.append(Character.toLowerCase(ch));
        }
    }

    private void setAttrs(String word, int start, int end) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(word));
        Preconditions.checkState(!Strings.isNullOrEmpty(field));

        termAttr.setTermBuffer(field + ":" + word);
        offsetAttr.setOffset(start, end);
    }

}
