/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
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
