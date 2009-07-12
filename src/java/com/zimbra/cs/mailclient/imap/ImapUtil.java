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
