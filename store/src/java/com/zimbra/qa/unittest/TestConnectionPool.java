/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;


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

        DbConnection conn1 = DbPool.getConnection();
        assertEquals("After first connection", initialSize + 1, DbPool.getSize());

        DbConnection conn2 = DbPool.getConnection();
        assertEquals("After second connection", initialSize + 2, DbPool.getSize());

        DbConnection maint = DbPool.getMaintenanceConnection();
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
