/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

import org.apache.logging.log4j.core.Layout;

/**
 * Subclasses Log4J's <tt>PatternLayout</tt> class to add additional support for
 * the <tt>%z</tt> option, which prints the value returned by {@link ZimbraLog#getContextString()}.
 *   
 * @author bburtin
 */
public class ZimbraPatternLayout extends PatternLayout   {

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
    
    /*
     * public static void main(String[] args) { Layout layout = new
     * ZimbraPatternLayout("[%z] - %m%n"); Category cat =
     * Category.getInstance("some.cat"); cat.addAppender(new ConsoleAppender(layout,
     * ConsoleAppender.SYSTEM_OUT));
     * ZimbraLog.addAccountNameToContext("my@account.com");
     * ZimbraLog.addMboxToContext(99); cat.debug("Hello, log");
     * cat.info("Hello again..."); ZimbraLog.clearContext();
     * cat.info("No more context"); }
     */
}
