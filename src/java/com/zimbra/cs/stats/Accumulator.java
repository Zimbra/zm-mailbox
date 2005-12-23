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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.stats;

import java.util.List;

/**
 * Abstraction for a statistic that increases over time.  A single
 * accumulator might keep track multiple statistics, eg, elapsed
 * time and average time.
 *
 * Required magic in log4j.properties to log statistics.
 * 
 *   # Appender STATS writes to file "stats"
 *   log4j.appender.STATS=org.apache.log4j.FileAppender
 *   log4j.appender.STATS.File=/temp/stats.log
 *   log4j.appender.STATS.Append=false
 *   log4j.appender.STATS.layout=org.apache.log4j.PatternLayout
 *   log4j.appender.STATS.layout.ConversionPattern=%r,%m%n
 *   log4j.additivity.com.zimbra.cs.stats=false
 *   log4j.logger.com.zimbra.cs.stats=DEBUG,STATS
 */
public abstract class Accumulator {

    protected abstract List<String> getColumns();
    protected abstract List getData();
    abstract void reset();

    private String mName;
    
    Accumulator(String name) {
        mName = name;
    }
    
    public String getName() {
        return mName;
    }
}
