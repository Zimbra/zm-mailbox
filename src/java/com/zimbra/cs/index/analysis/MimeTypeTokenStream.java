/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.index.analysis;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import com.zimbra.cs.index.LuceneFields;

/**
 * {@code image/jpeg} becomes {@code image/jpeg} and {@code image}
 *
 * @author ysasaki
 */
public final class MimeTypeTokenStream extends TokenStream {
    private static final int MIN_TOKEN_LEN = 3;
    private static final int MAX_TOKEN_LEN = 256;
    private static final int MAX_TOKEN_COUNT = 100;

    private final List<String> tokens = new LinkedList<String>();
    private Iterator<String> itr;
    private final CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);

    public MimeTypeTokenStream() {
        tokens.add(LuceneFields.L_ATTACHMENT_NONE);
    }

    public MimeTypeTokenStream(String src) {
        add(src);
        tokens.add(tokens.isEmpty() ? LuceneFields.L_ATTACHMENT_NONE : LuceneFields.L_ATTACHMENT_ANY);
    }

    public MimeTypeTokenStream(Collection<String> list) {
        for (String src : list) {
            add(src);
        }
        tokens.add(tokens.isEmpty() ? LuceneFields.L_ATTACHMENT_NONE : LuceneFields.L_ATTACHMENT_ANY);
    }

    private void add(String src) {
        if (tokens.size() >= MAX_TOKEN_COUNT) {
            return;
        }
        String token = src.trim();
        if (token.length() < MIN_TOKEN_LEN || token.length() > MAX_TOKEN_LEN) {
            return;
        }
        token = token.toLowerCase();
        tokens.add(token);
        // extract primary of primary/sub
        int delim = token.indexOf('/');
        if (delim > 0) {
            String primary = token.substring(0, delim).trim();
            if (primary.length() >= MIN_TOKEN_LEN) {
                tokens.add(primary);
            }
        }
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (itr.hasNext()) {
            termAttr.setEmpty().append(itr.next());
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
        itr = tokens.iterator();
    }

    @Override
    public void close() {
        tokens.clear();
    }

}
