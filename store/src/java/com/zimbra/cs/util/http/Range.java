/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.util.http;

import java.util.LinkedList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.util.Pair;

/**
 * Helps with parsing and manipulating HTTP Range headers.
 *
 * @author grzes
 *
 */
public class Range
{
    public static final String HEADER = "Range";

    public List<Pair<Long, Long>> getRanges()
    {
        return ranges;
    }

    private List<Pair<Long, Long>> ranges = new LinkedList<Pair<Long, Long>>();

    private Range()
    {
    }

    /**
     * Creates Range from supplied string. The string needs to contain properly formatted
     * value of Range header, containing one or more numeric ranges, e.g:
     *
     * bytes=0-1000,2000-3000
     * bytes=100-
     * bytes=-500
     *
     * @param header String with the header value
     * @return Range instance or null if header parameter was null
     *
     * @throws RangeException If the supplied string is syntactically invalid; additionally
     * if the range is reversed or contains negative values
     *
     */
    public static Range parse(String header) throws RangeException
    {
        if (header == null) {
            return null;
        }

        // trim leading & trailing whitespaces
        header = header.trim();

        // make sure it starts with "bytes"
        if (!header.startsWith("bytes")) {
            throw new RangeException(HttpServletResponse.SC_BAD_REQUEST);
        }

        // trim bytes and any whitespaces
        header = header.substring(5).trim();

        // make sure the first char is now =
        if (header.isEmpty() || header.charAt(0) != '=') {
            throw new RangeException(HttpServletResponse.SC_BAD_REQUEST);
        }

        // trim = and any whitespaces
        header = header.substring(1).trim();

        java.util.StringTokenizer tokenizer = new java.util.StringTokenizer(header, ",");
        Range range = new Range();

        while (tokenizer.hasMoreElements()) {

            String rangeStr  = tokenizer.nextToken().trim();

            if (rangeStr.isEmpty()) {
                // empty ranges caused by multiple commas are to be tolerated
                continue;
            }

            int dashPos = rangeStr.indexOf('-');

            if (dashPos == -1) {
                // dash must be present in each range spec
                throw new RangeException(HttpServletResponse.SC_BAD_REQUEST);
            }

            Long start = null;
            Long end = null;

            try {
                if (dashPos > 0) {
                    // has start
                    start = Long.parseLong(rangeStr.substring(0, dashPos).trim());
               }

                if ((dashPos + 1) < rangeStr.length()) {
                    // has end
                    end = Long.parseLong(rangeStr.substring(dashPos + 1).trim());

                    if (end < 0 || (start != null && start > end)) {
                        throw new RangeException(HttpServletResponse.SC_BAD_REQUEST);
                    }
                } else if (start == null) {
                    throw new RangeException(HttpServletResponse.SC_BAD_REQUEST);
                }
            } catch (NumberFormatException e) {
                throw new RangeException(HttpServletResponse.SC_BAD_REQUEST);
            }

            range.ranges.add(new Pair<Long, Long>(start, end));
        }

        if (range.ranges.isEmpty()) {
            throw new RangeException(HttpServletResponse.SC_BAD_REQUEST);
        }

        return range;
    }
}
