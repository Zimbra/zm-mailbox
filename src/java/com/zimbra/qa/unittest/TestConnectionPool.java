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
package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.Connection;


public class TestConnectionPool 
extends TestCase {

    /**
     * Confirms that getting and closing connections properly increments
     * and decrements the pool size.  Also confirms that maintenance
     * connections don't affect the connection pool.
     */
    public void testPool()
    throws Exception {
        int initialSize = DbPool.getSize();

        ZimbraLog.test.info("Initial connection pool size: %d", initialSize);
        
        Connection conn1 = DbPool.getConnection();
        assertEquals("After first connection", initialSize + 1, DbPool.getSize());
        
        Connection conn2 = DbPool.getConnection();
        assertEquals("After second connection", initialSize + 2, DbPool.getSize());
        
        Connection maint = DbPool.getMaintenanceConnection();
        assertEquals("After maintenance connection", initialSize + 2, DbPool.getSize());
        
        DbPool.quietClose(conn1);
        assertEquals("After first close", initialSize + 1, DbPool.getSize());
        
        DbPool.quietClose(conn2);
        assertEquals("After second close", initialSize, DbPool.getSize());
        
        DbPool.quietClose(maint);
        assertEquals("After closing maintenance connection", initialSize, DbPool.getSize());
        
        ZimbraLog.test.info("Final connection pool size: %d", DbPool.getSize());
    }
}
