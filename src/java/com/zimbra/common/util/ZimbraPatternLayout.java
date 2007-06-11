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
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.util;

import java.util.LinkedHashSet;
import java.util.Set;

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

    private Set<String> mMdcKeyOrder;
    
    public ZimbraPatternLayout() {
        this(DEFAULT_CONVERSION_PATTERN);
        setMdcKeyOrder("name,aname,mid,ip");
    }

    public ZimbraPatternLayout(String pattern) {
        super(pattern);
        setMdcKeyOrder("name,aname,mid,ip");
    }

    public PatternParser createPatternParser(String pattern) {
        if (pattern == null) {
            pattern = DEFAULT_CONVERSION_PATTERN;
        }
        return new ZimbraPatternParser(pattern, this);
    }
    
    /**
     * Sets the order of keys printed in the context section.  These keys
     * are printed in the specied order, before any other keys.  Keys not
     * in the context are not printed.
     *   
     * @param order a comma-separated list of MDC keys
     */
    public void setMdcKeyOrder(String order) {
        if (StringUtil.isNullOrEmpty(order)) {
            return;
        }
        mMdcKeyOrder = new LinkedHashSet<String>();
        for (String key : order.split(",")) {
            mMdcKeyOrder.add(key);
        }
    }
    
    Set<String> getMdcKeyOrder() { return mMdcKeyOrder; }

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
