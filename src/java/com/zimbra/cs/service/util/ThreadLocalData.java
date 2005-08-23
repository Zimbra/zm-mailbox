/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.util;

import java.util.HashMap;
import java.util.Map;


/**
 * Maintains per-thread data, used to keep track of performance statistics and
 * other data that pertains to a single request.  Methods in this class are
 * not thread-safe, since each thread will be synchronously updating its own
 * set of data.
 */
public class ThreadLocalData {

    private static final String TL_DB_TIME = "DbTime";
    private static final String TL_STATEMENT_COUNT = "SqlStatementCount";
    
    private static ThreadLocal sThreadLocal = new ThreadLocal();

    /**
     * Resets all per-thread data.
     *
     */
    public static void reset() {
        Map map = (Map) sThreadLocal.get();
        if (map != null) {
            map.clear();
        }
    }

    /**
     * Returns the total database execution time in milliseconds for the current thread.
     */
    public static int getDbTime() {
        Integer i = (Integer) getThreadLocalMap().get(TL_DB_TIME);
        if (i == null) {
            return 0;
        }
        return i.intValue();
    }
    
    /**
     * Returns the number of SQL statements that have been executed for the current
     * thread.
     */
    public static int getStatementCount() {
        Integer i = (Integer) getThreadLocalMap().get(TL_STATEMENT_COUNT);
        if (i == null) {
            return 0;
        }
        return i.intValue();
    }
    
    /**
     * Increments this thread's database execution time and statement count.
     */
    public static void incrementDbTime(int millis) {
        // Increment database time
        Integer i = (Integer) getThreadLocalMap().get(TL_DB_TIME);
        int previousValue = 0;
        if (i != null) {
            previousValue = i.intValue();
        }
        i = new Integer(previousValue + millis);
        getThreadLocalMap().put(TL_DB_TIME, i);
        
        // Increment statement count
        i = (Integer) getThreadLocalMap().get(TL_STATEMENT_COUNT);
        previousValue = 0;
        if (i != null) {
            previousValue = i.intValue();
        }
        i = new Integer(previousValue + 1);
        getThreadLocalMap().put(TL_STATEMENT_COUNT, i);
    }
    
    private static Map getThreadLocalMap() {
        Map map = (Map) sThreadLocal.get();
        if (map == null) {
            map = new HashMap();
            sThreadLocal.set(map);
        }
        return map;
    }
}
