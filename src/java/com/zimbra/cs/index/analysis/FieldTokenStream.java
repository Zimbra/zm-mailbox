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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.NumericUtils;

import com.google.common.base.Strings;
import com.zimbra.cs.index.LuceneFields;

/**
 * {@link TokenStream} for structured-data field.
 * <p>
 * {@code name:Val1 val2 val3} gets tokenized to {@code name:val1}, {@code name:val2}, {@code name:val3}. If the field
 * only consists of a single integer value, it produces an extra token of which name is appended by '#' to distinguish
 * from text search and the integer value gets encoded by Lucene's {@link NumericUtils}, so that it is also searchable
 * by numeric range query. Note that numeric fields are still tokenized as text too for wildcard search.
 *
 * @see LuceneFields#L_FIELD
 * @author tim
 * @author ysasaki
 */
public final class FieldTokenStream extends TokenStream {

    private static final Pattern NUMERIC_VALUE_REGEX = Pattern.compile("-?\\d+");

    private List<String> tokens = new LinkedList<String>();
    private Iterator<String> iterator;
    private TermAttribute termAttr = addAttribute(TermAttribute.class);

    public FieldTokenStream() {
    }

    public FieldTokenStream(String name, String value) {
        add(name, value);
    }

    public void add(String name, String value) {
        if (Strings.isNullOrEmpty(name) || Strings.isNullOrEmpty(value)) {
            return;
        }

        name = normalizeName(name);

        if (NUMERIC_VALUE_REGEX.matcher(value).matches()) {
            try {
                tokens.add(name + "#:" + NumericUtils.intToPrefixCoded(Integer.parseInt(value)));
            } catch (NumberFormatException ignore) { // pass through
            }
        }

        if (value.equals("*")) { // wildcard alone
            tokens.add(name + ":*");
            return;
        }

        StringBuilder word = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            // treat '-' as whitespace UNLESS it is at the beginning of a word
            if (isWhitespace(c) || (c == '-' && word.length() > 0)) {
                if (word.length() > 0) {
                    tokens.add(name + ':' + word.toString());
                    word.setLength(0);
                }
            } else if (!Character.isISOControl(c)) {
                word.append(Character.toLowerCase(c));
            }
        }

        if (word.length() > 0) {
            tokens.add(name + ':' + word.toString());
        }
    }

    @Override
    public boolean incrementToken() throws IOException {
        clearAttributes();
        if (iterator == null) {
            iterator = tokens.iterator();
        }

        if (iterator.hasNext()) {
            termAttr.setTermBuffer(iterator.next());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
        iterator = null;
    }

    @Override
    public void close() {
        tokens.clear();
    }

    private boolean isWhitespace(char ch) {
        switch (ch) {
        case ' ':
        case '\r':
        case '\n':
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

    /**
     * Strip control characters and ':', make it all lower-case.
     *
     * @param name raw field name
     * @return normalized field name
     */
    private String normalizeName(String name) {
        StringBuilder result = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isISOControl(c) && c != ':') {
                result.append(Character.toLowerCase(c));
            }
        }
        return result.toString();
    }

}
