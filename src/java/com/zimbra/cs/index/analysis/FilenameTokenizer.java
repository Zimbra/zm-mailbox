/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.Reader;

import org.apache.lucene.analysis.CharTokenizer;

import com.zimbra.cs.index.LuceneIndex;

/**
 * Split by comma, space, CR, LF, dot.
 *
 * @author tim
 * @author ysasaki
 */
public final class FilenameTokenizer extends CharTokenizer {

    public FilenameTokenizer(Reader reader) {
        super(LuceneIndex.VERSION, reader);
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
        return (char) NormalizeTokenFilter.normalize(c);
    }

}
