/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.mailclient.imap;

import java.util.List;
import java.util.ArrayList;

public final class ImapUtil {
    /*
     * Parses APPENDUID or COPYUID uid sequence set
     */
    public static long[] parseUidSet(String s) {
        if (s.length() == 0) {
            throw new IllegalArgumentException("Empty sequence set");
        }
        List<Long> uids = new ArrayList<Long>();
        for (String part : s.split(",")) {
            String[] range = part.split(":");
            if (range.length == 2) {
                long i = Long.parseLong(range[0]);
                long j = Long.parseLong(range[1]);
                if (i < j) {
                    while (i <= j) uids.add(i++);
                } else {
                    while (j <= i) uids.add(j++);
                }
            } else {
                uids.add(Long.parseLong(part));
            }
        }
        long[] res = new long[uids.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = uids.get(i);
        }
        return res;
    }
}
