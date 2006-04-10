/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * Part of the Zimbra Collaboration Suite Server.
 *
 * The Original Code is Copyright (C) Jive Software. Used with permission
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):
 *
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.im.xmpp.util;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;

/**
 * A formatter for formatting byte sizes. For example, formatting 12345 byes results in
 * "12.1 K" and 1234567 results in "1.18 MB".
 *
 * @author Bill Lynch
 */
public class ByteFormat extends Format {

    /**
     * Formats a long which represent a number of bytes.
     */
    public String format(long bytes) {
        return format(new Long(bytes));
    }

    /**
     * Formats a long which represent a number of kilobytes.
     */
    public String formatKB(long kilobytes) {
        return format(new Long(kilobytes * 1024));
    }

    /**
     * Format the given object (must be a Long).
     *
     * @param obj assumed to be the number of bytes as a Long.
     * @param buf the StringBuffer to append to.
     * @param pos
     * @return A formatted string representing the given bytes in more human-readable form.
     */
    public StringBuffer format(Object obj, StringBuffer buf, FieldPosition pos) {
        if (obj instanceof Long) {
            long numBytes = ((Long)obj).longValue();
            if (numBytes < 1024 * 1024) {
                DecimalFormat formatter = new DecimalFormat("#,##0.0");
                buf.append(formatter.format((double)numBytes / 1024.0)).append(" K");
            }
            else {
                DecimalFormat formatter = new DecimalFormat("#,##0.0");
                buf.append(formatter.format((double)numBytes / (1024.0 * 1024.0))).append(" MB");
            }
        }
        return buf;
    }

    /**
     * In this implementation, returns null always.
     *
     * @param source
     * @param pos
     * @return returns null in this implementation.
     */
    public Object parseObject(String source, ParsePosition pos) {
        return null;
    }
}