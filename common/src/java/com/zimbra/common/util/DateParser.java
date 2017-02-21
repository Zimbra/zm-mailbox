/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.common.util;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Wrapper around {@link SimpleDateFormat} that allows it to be used in a
 * static context.  <tt>DateParser</tt> automatically handles using
 * <tt>ThreadLocal</tt> to allocate one <tt>SimpleDateFormat</tt> per
 * thread.
 */
public class DateParser {

    private ThreadLocal<SimpleDateFormat> mFormatterHolder = new ThreadLocal<SimpleDateFormat>();
    private String mDatePattern;
    
    public DateParser(String datePattern) {
        mDatePattern = datePattern;
    }
    
    public Date parse(String s) {
        return getFormatter().parse(s, new ParsePosition(0));
    }
    
    public String format(Date date) {
        return getFormatter().format(date);
    }
    
    private SimpleDateFormat getFormatter() {
        SimpleDateFormat formatter = mFormatterHolder.get();
        if (formatter == null) {
            formatter = new SimpleDateFormat(mDatePattern);
            mFormatterHolder.set(formatter);
        }
        return formatter;
    }
}
