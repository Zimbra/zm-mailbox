/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.qa.unittest.server;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.zimbra.client.ZDataSource;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZImapDataSource;
import com.zimbra.client.ZMailbox;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.ldap.LdapConstants;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.ScheduledTask;
import com.zimbra.qa.unittest.TestUtil;
import com.zimbra.soap.type.DataSource.ConnectionType;
public class TestDataSourceServer {

    @Rule
    public TestName testInfo = new TestName();

    protected static String USER_NAME = null;
    protected String testId;
    
    @Before
    public void setUp() throws Exception {
        testId = String.format("%s-%s-%d", this.getClass().getSimpleName(), testInfo.getMethodName(), (int)Math.abs(Math.random()*100));
        USER_NAME = String.format("%s-user", testId).toLowerCase();
        cleanUp();
        TestUtil.createAccount(USER_NAME);
    }

    @Test
    public void testCustomDS() throws Exception {
        String DSName = "testCustomDS";
        String importClassName = "some.data.source.ClassName";
        ArrayList<String> attrs = new ArrayList<String>();
        String stringAttr = "somestringattr";
        String jsonAttr = "{\"someVar\":\"someValue\"}";
        String colonAttr = "someProp:someVal";
        attrs.add(stringAttr);
        attrs.add(jsonAttr);
        attrs.add(colonAttr);
        String refreshToken = "refresh-token";
        String refreshTokenUrl = "https://this.is.where.you/?refresh=the&token";
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        ZDataSource zds = new ZDataSource(DSName, importClassName, attrs).setRefreshToken(refreshToken).setRefreshTokenURL(refreshTokenUrl);
        String dsId = zmbox.createDataSource(zds);
        assertNotNull("should get an id for new DataSource", dsId);
        assertFalse("DataSource id should not be empty", dsId.isEmpty());
        ZDataSource ds = TestUtil.getDataSource(zmbox, DSName);
        assertNotNull("should retrieve a non-null DataSource", ds);
        assertEquals("Data source name should be " + DSName, ds.getName(), DSName);
        assertNotNull("new DataSource should have an import class", ds.getImportClass());
        assertEquals("expecting import class: " + importClassName, importClassName, ds.getImportClass());
        assertNotNull("new DataSource should have a refresh token", ds.getRefreshToken());
        assertEquals("expecting refresh token: " + refreshToken, refreshToken, ds.getRefreshToken());
        assertNotNull("new DataSource should have a refresh token URL", ds.getRefreshTokenUrl());
        assertEquals("expecting refresh token URL: " + refreshTokenUrl, refreshTokenUrl, ds.getRefreshTokenUrl());
        assertNotNull("new DataSource should have attributes", ds.getAttributes());
        assertFalse("new DataSource attributes should not be empty", ds.getAttributes().isEmpty());
        assertTrue("expecting to find attribute with value " + stringAttr, ds.getAttributes().contains(stringAttr));
        assertTrue("expecting to find attribute with value " + jsonAttr, ds.getAttributes().contains(jsonAttr));
        assertTrue("expecting to find attribute with value " + colonAttr, ds.getAttributes().contains(colonAttr));
    }

    @Test
    public void testScheduling() throws Exception {
        ZMailbox zmbox = TestUtil.getZMailbox(USER_NAME);
        ZFolder folder = TestUtil.createFolder(zmbox, "/testScheduling");
        Provisioning prov = Provisioning.getInstance();
        Server server = prov.getLocalServer();
        int port = server.getImapBindPort();
        ZImapDataSource zds = new ZImapDataSource("testScheduling", true, "localhost", port,
            "user2", "test123", folder.getId(), ConnectionType.cleartext);
        String dsId = zmbox.createDataSource(zds);
        
        // Test scheduling based on polling interval. 
        Mailbox mbox = TestUtil.getMailbox(USER_NAME);
        String attrName = Provisioning.A_zimbraDataSourcePollingInterval;
        String imapAttrName = Provisioning.A_zimbraDataSourceImapPollingInterval;
        TestUtil.setDataSourceAttr(USER_NAME, zds.getName(), attrName, "0");
        checkSchedule(mbox, dsId, null);
        
        TestUtil.setDataSourceAttr(USER_NAME, zds.getName(), attrName, "10m");
        checkSchedule(mbox, dsId, 600000);

        TestUtil.setAccountAttr(USER_NAME, imapAttrName, "");
        TestUtil.setDataSourceAttr(USER_NAME, zds.getName(), attrName, "");
        checkSchedule(mbox, dsId, null);
        
        TestUtil.setAccountAttr(USER_NAME, imapAttrName, "5m");
        checkSchedule(mbox, dsId, 300000);
        
        TestUtil.setDataSourceAttr(USER_NAME, zds.getName(), attrName, "0");
        checkSchedule(mbox, dsId, null);
        
        // Bug 44502: test changing polling interval from 0 to unset when
        // interval is set on the account.
        TestUtil.setDataSourceAttr(USER_NAME, zds.getName(), attrName, "");
        checkSchedule(mbox, dsId, 300000);
        
        TestUtil.setDataSourceAttr(USER_NAME, zds.getName(), Provisioning.A_zimbraDataSourceEnabled, LdapConstants.LDAP_FALSE);
        checkSchedule(mbox, dsId, null);
    }
    
    private void checkSchedule(Mailbox mbox, String dataSourceId, Integer intervalMillis)
    throws Exception {
        ScheduledTask task = DataSourceManager.getTask(mbox, dataSourceId);
        if (intervalMillis == null) {
            assertNull(task);
        } else {
            assertEquals(intervalMillis.longValue(), task.getIntervalMillis());
        }
    }
    
    @After
    public void tearDown() throws Exception {
        cleanUp();
    }
    
    private void cleanUp() throws Exception {
        if (USER_NAME != null) {
            TestUtil.deleteAccountIfExists(USER_NAME);
        }
    }
}
