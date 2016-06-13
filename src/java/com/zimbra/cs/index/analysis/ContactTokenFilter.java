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

import java.io.IOException;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Swallow dots, but include dots in a token only when it is not the only char
 * in the token.
 */
public final class ContactTokenFilter extends TokenFilter {
    private CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);

    public ContactTokenFilter(AddrCharTokenizer input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        while (input.incrementToken()) {
            if (termAttr.length() == 1 && termAttr.charAt(0) == '.') {
                continue; // swallow dot
            } else {
                return true;
            }
        }
        return false;
    }

}
