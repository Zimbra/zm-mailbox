/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2010 Zimbra, Inc.
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

package com.zimbra.qa.unittest;

import junit.framework.TestCase;

import com.zimbra.cs.db.Db;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.db.DbPool.Connection;
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
        Connection conn = DbPool.getConnection();
        
        assertTrue("Could not find database " + dbName, db.databaseExists(conn, dbName));
        assertFalse("False positive", db.databaseExists(conn, "foobar"));
        
        DbPool.quietClose(conn);
    }
}
