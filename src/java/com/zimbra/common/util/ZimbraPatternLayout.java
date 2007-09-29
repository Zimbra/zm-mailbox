/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import org.apache.log4j.Category;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.MDC;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.PatternParser;

/**
 * Subclasses Log4J's <tt>PatternLayout</tt> class to add additional support for
 * the <tt>%X</tt> option.  If <tt>%X</tt> is specified without braces, all keys
 * and values in the {@link MDC} are logged.
 *   
 * @author bburtin
 */
public class ZimbraPatternLayout extends PatternLayout {

    public ZimbraPatternLayout() {
        this(DEFAULT_CONVERSION_PATTERN);
    }

    public ZimbraPatternLayout(String pattern) {
        super(pattern);
    }

    public PatternParser createPatternParser(String pattern) {
        if (pattern == null) {
            pattern = DEFAULT_CONVERSION_PATTERN;
        }
        return new ZimbraPatternParser(pattern, this);
    }
    
    public static void main(String[] args) {
        Layout layout = new ZimbraPatternLayout("[%X] - %m%n");
        Category cat = Category.getInstance("some.cat");
        cat.addAppender(new ConsoleAppender(layout, ConsoleAppender.SYSTEM_OUT));
        MDC.put("one", "1");
        MDC.put("two", "2");
        cat.debug("Hello, log");
        cat.info("Hello again...");    
    }
}
