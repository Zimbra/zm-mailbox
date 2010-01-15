/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2010 Zimbra, Inc.
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
package com.zimbra.common.util;

import org.apache.log4j.Category;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.PatternParser;

/**
 * Subclasses Log4J's <tt>PatternLayout</tt> class to add additional support for
 * the <tt>%z</tt> option, which prints the value returned by {@link ZimbraLog#getContextString()}.
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
        Layout layout = new ZimbraPatternLayout("[%z] - %m%n");
        Category cat = Category.getInstance("some.cat");
        cat.addAppender(new ConsoleAppender(layout, ConsoleAppender.SYSTEM_OUT));
        ZimbraLog.addAccountNameToContext("my@account.com");
        ZimbraLog.addMboxToContext(99);
        cat.debug("Hello, log");
        cat.info("Hello again...");
        ZimbraLog.clearContext();
        cat.info("No more context");
    }
}
