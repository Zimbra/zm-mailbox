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

/**
 * A <code>Counter</code> that supports <code>start()</code>
 * and <code>stop()</code> methods for conveniently timing events.
 * Lots the count, total and average times by default.
 * 
 * @author bburtin
 */
public class StopWatch
extends Counter {
    
    public static StopWatch getInstance(String name) {
        StopWatch ta = (StopWatch)getInstance(name, StopWatch.class);
        ta.setShowCount(true);
        ta.setShowAverage(true);
        ta.setUnits("ms");
        return ta;
    }

    public long start() {
        return System.currentTimeMillis();
    }
    
    public void stop(long startTime) {
        increment(System.currentTimeMillis() - startTime);
    }
}