/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util.http;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Represents Content-Range. Helps with parsing and manipulating Content-Range headers.
 *
 * @author grzes
 *
 */

public class ContentRange
{
    public static final String HEADER = "Content-Range";

    private long start;
    private long end;
    private boolean startEndSet;
    private long instanceLength;
    private boolean lengthSet;

    /**
     * Constructs Content-Range without instance length.
     *
     * @param start Start of the range. Must be >= 0.
     * @param end End of the range (inclusive). Must be >= start.
     * @throws RangeException Invalid range
     */
    public ContentRange(long start, long end) throws RangeException
    {
        setStartAndEnd(start, end);
    }

    /**
     * Constructs Content-Range with instance length.
     *
     * @param start Start of the range. Must be >= 0.
     * @param end End of the range (inclusive). Must be >= start.
     * @param instanceLength Instance length. Must be => 0.
     * @throws RangeException Invalid range
     */
    public ContentRange(long start, long end, long instanceLength) throws RangeException
    {
        setStartAndEnd(start, end);
        setInstanceLength(instanceLength);
    }

    /**
     * Constructs Content-Range with instance length only.
     *
     * @param instanceLength Instance length. Must be => 0.
     * @throws RangeException Invalid range
     */
    public ContentRange(long instanceLength) throws RangeException
    {
        setInstanceLength(instanceLength);
    }

    /**
     * Constructs Content-Range without start/end nor instance length
     * set.
     *
     * @param instanceLength Instance length. Must be => 0.
     * @throws RangeException Invalid range
     */
    public ContentRange()
    {
    }

    /**
     * Checks if rnage has start/end set
     *
     * @return True if start/end set, false otherwise
     */
    public boolean hasStartEnd()
    {
        return startEndSet;
    }


    /**
     * Returns start of the range. Undefined if was not set.
     *
     * return Range start
     */
    public long getStart()
    {
        return start;
    }

    /**
     * Returns end of the range. Undefined if was not set.
     *
     * return Range end
     */
    public long getEnd()
    {
        return end;
    }

    /**
     * Checks if range has instance length set
     *
     * return True if instance length set, false otherwise
     */
    public boolean hasInstanceLength()
    {
        return lengthSet;
    }

    /**
     * Returns instance length. Value undefined if
     * the range was not set.
     *
     * return Instance length
     */
    public long getInstanceLength()
    {
        return instanceLength;
    }

    /**
     * Returns (byte) length of the range
     *
     * @return Range length
     */
    public long getRangeLength()
    {
        return end - start + 1;
    }

    /**
     * Checks if the range is complete, i.e. starts at 0
     * and the length is equal to the instance length
     *
     * @return True if range is compete, false otherwise
     */
    public boolean isComplete()
    {
        return hasInstanceLength() && start == 0 && (end + 1) == instanceLength;
    }

    /**
     * Creates ContentRange from supplied HttpServletRequest
     *
     * @param req The request
     *
     * @return ContentRange instance or null if Content-Range header is missing in the request
     *
     * @throws RangeException If invalid values were found
     */
    public static ContentRange parse(HttpServletRequest req) throws RangeException
    {
        return parse(req.getHeader("Content-Range"));
    }

    /**
     * Creates ContentRange from supplied string. The string
     * needs to be in form of "bytes start '-' end '/' instance-length" as per HTTP 1.1 spec.
     * instance-length can be '*'.
     *
     * @param header String with the header value
     * @return ContentRange instance or null if header parameter was null
     * @throws RangeException If invalid values were found
     */
    public static ContentRange parse(String header) throws RangeException
    {
        if (header == null) {
            return null;
        }

        header = header.trim();

        if (!header.startsWith("bytes")) {
            throw new RangeException(HttpServletResponse.SC_BAD_REQUEST);
        }

        header = header.substring(6).trim();

        final int slashPos = header.indexOf('/');

        if (slashPos == -1) {
            throw new RangeException(HttpServletResponse.SC_BAD_REQUEST);
        }

        ContentRange range = new ContentRange();

        try {
            final int dashPos = header.indexOf('-');

            if (dashPos == -1) {
                if (header.charAt(0) != '*') {
                    throw new RangeException(HttpServletResponse.SC_BAD_REQUEST);
                }
            } else {
                long start = Long.parseLong(header.substring(0, dashPos).trim());
                long end = Long.parseLong(header.substring(dashPos + 1, slashPos).trim());

                range.setStartAndEnd(start, end);
            }

            String instanceLengthStr = header.substring(slashPos + 1, header.length()).trim();

            if (!instanceLengthStr.equals("*")) {
                range.setInstanceLength(Long.parseLong(instanceLengthStr));
            }

        } catch (NumberFormatException e) {
            throw new RangeException(HttpServletResponse.SC_BAD_REQUEST);
        }

        return range;
    }

    private void setInstanceLength(long value) throws RangeException
    {
        if (value < 0 || (startEndSet && end >= value)) {
            throw new RangeException(HttpServletResponse.SC_BAD_REQUEST);
        }

        instanceLength = value;
        lengthSet = true;
    }

    private void setStartAndEnd(long start, long end) throws RangeException
    {
        // make sure the values make sense
        if (start < 0 || end < start)
            throw new RangeException(HttpServletResponse.SC_BAD_REQUEST);

        this.start = start;
        this.end = end;
        startEndSet = true;
    }

    /**
     * Returns Content-Range as header's value
     * (i.e. starting from bytes, etc).
     */
    @Override
    public String toString()
    {
        String result = "bytes ";

        if (startEndSet) {
            result += start + "-" + end;
        } else {
            result += "*";
        }

        if (lengthSet) {
            result += "/" + instanceLength;
        } else {
            result += "/*";
        }

        return result;
    }
}
