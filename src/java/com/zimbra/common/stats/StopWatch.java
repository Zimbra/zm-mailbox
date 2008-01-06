/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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

package com.zimbra.common.stats;

/**
 * A <code>Counter</code> that supports <code>start()</code>
 * and <code>stop()</code> methods for conveniently timing events.
 * By default, the count and average times are logged and the
 * total is not.
 * 
 * @author bburtin
 */
public class StopWatch
extends Counter {
    
    public StopWatch(String name) {
        super(name, "ms");
        setShowCount(true);
        setShowAverage(true);
        setShowTotal(false);
    }

    public long start() {
        return System.currentTimeMillis();
    }
    
    public void stop(long startTime) {
        increment(System.currentTimeMillis() - startTime);
    }
}