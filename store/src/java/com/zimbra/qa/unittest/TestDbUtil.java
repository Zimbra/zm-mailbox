/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.db.Db;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.db.DbPool.DbConnection;
import com.zimbra.cs.db.DbUtil;
import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author bburtin
 */
public class TestDbUtil {
    private static final String USER_NAME = "TestDbUtil-user1";
    private static final Provisioning prov = Provisioning.getInstance();
    private static Server localServer = null;
    private Mailbox mbox = null;

    @BeforeClass
    public static void beforeClass() throws Exception {
        localServer = prov.getLocalServer();
    }

    @Before
    public void setUp() throws Exception {
        Map<String, Object> attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraMailHost, localServer.getServiceHostname());
        TestUtil.createAccount(USER_NAME);
        mbox = TestUtil.getMailbox(USER_NAME);
    }

    @Test
    public void testNormalizeSql() throws Exception {
        String sql = " \t SELECT a, 'b', 1, '', ',', NULL, '\\'' FROM table1\n\nWHERE c IN (1, 2, 3) ";
        String normalized = DbUtil.normalizeSql(sql);
        String expected = "SELECT a, XXX, XXX, XXX, XXX, XXX, XXX FROM tableXXX WHERE c IN (...)";
        assertEquals(expected, normalized);
    }

    @Test
    public void testDatabaseExists() throws Exception {
        Db db = Db.getInstance();
        String dbName = DbMailbox.getDatabaseName(mbox);
        DbConnection conn = DbPool.getConnection();

        assertTrue("Could not find database " + dbName, db.databaseExists(conn, dbName));
        assertFalse("False positive", db.databaseExists(conn, "foobar"));

        DbPool.quietClose(conn);
    }

    @After
    public void tearDown() throws Exception {
        if(TestUtil.accountExists(USER_NAME)) {
            TestUtil.deleteAccount(USER_NAME);
        }
    }
}
