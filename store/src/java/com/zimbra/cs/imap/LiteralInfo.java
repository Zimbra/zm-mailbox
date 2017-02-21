/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import org.apache.mina.filter.codec.ProtocolDecoderException;

public class LiteralInfo {
    int count;
    boolean blocking;

    public static LiteralInfo parse(String line) throws ProtocolDecoderException {
        if (line.endsWith("}")) {
            int i = line.lastIndexOf('{');
            if (i >= 0) {
                LiteralInfo li = new LiteralInfo();
                String s = line.substring(i + 1, line.length() - 1);
                if (s.endsWith("+")) {
                    s = s.substring(0, s.length() - 1);
                } else {
                    li.blocking = true;
                }
                li.count = parseCount(s);
                if (li.count < 0) {
                    throw new NioImapDecoder.TooBigLiteralException(line);
                }
                return li;
            }
        }
        return null;
    }

    public int getCount() { return count; }
    public boolean isBlocking() { return blocking; }
    
    private static int parseCount(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            int d = Character.digit(s.charAt(i), 10);
            if (d == -1) return -1;
            n = n * 10 + d;
            if (n < 0) return -1; // Overflow
        }
        return n;
    }
}
