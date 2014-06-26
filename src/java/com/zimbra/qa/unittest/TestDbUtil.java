/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.cs.db.Db;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author bburtin
 */
public class TestDbUtil extends TestCase
{
    public void testNormalizeSql()
    throws Exception {
        String sql = " \t SELECT a, 'b', 1, '', ',', NULL, '\\'' FROM table1\n\nWHERE c IN (1, 2, 3) ";
        String normalized = DbUtil.normalizeSql(sql);
        String expected = "SELECT a, XXX, XXX, XXX, XXX, XXX, XXX FROM tableXXX WHERE c IN (...)";
        assertEquals(expected, normalized);
    }

    public void testDatabaseExists()
    throws Exception {
        Mailbox mbox = TestUtil.getMailbox("user1");
        Db db = Db.getInstance();
        String dbName = DbMailbox.getDatabaseName(mbox);
        DbConnection conn = DbPool.getConnection();

        assertTrue("Could not find database " + dbName, db.databaseExists(conn, dbName));
        assertFalse("False positive", db.databaseExists(conn, "foobar"));

        DbPool.quietClose(conn);
    }
}
