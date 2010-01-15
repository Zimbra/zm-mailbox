/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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
