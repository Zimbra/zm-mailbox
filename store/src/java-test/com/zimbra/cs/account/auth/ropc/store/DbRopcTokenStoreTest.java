/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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

package com.zimbra.cs.account.auth.ropc.store;

import com.zimbra.common.localconfig.KnownKey;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.db.DbMailbox;
import com.zimbra.cs.db.DbPool;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DbPool.class, LC.class, KnownKey.class, MailboxManager.class, Mailbox.class, DbMailbox.class})
@SuppressStaticInitializationFor({"com.zimbra.common.localconfig.LC", "com.zimbra.cs.mailbox.Mailbox",
        "com.zimbra.cs.db.DbMailbox"})
public class DbRopcTokenStoreTest {
    private DbRopcTokenStore store;

    private DbPool.DbConnection mockConn;

    private PreparedStatement mockStmt;

    private ResultSet mockRs;

    @Before
    public void setUp() throws SQLException, ServiceException {
        store = new DbRopcTokenStore();
        mockConn = PowerMockito.mock(DbPool.DbConnection.class);
        mockStmt = PowerMockito.mock(PreparedStatement.class);
        mockRs = PowerMockito.mock(ResultSet.class);

        MailboxManager mockMailboxManager = PowerMockito.mock((MailboxManager.class));
        Mailbox mockMailbox = PowerMockito.mock(Mailbox.class);

        PowerMockito.mockStatic(MailboxManager.class);
        PowerMockito.when(MailboxManager.getInstance()).thenReturn(mockMailboxManager);
        PowerMockito.when(mockMailboxManager.getMailboxByAccount(any())).thenReturn(mockMailbox);

        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

        PowerMockito.mockStatic(DbPool.class);
        PowerMockito.when(DbPool.getConnection(any())).thenReturn(mockConn);
        PowerMockito.when(DbPool.getConnection()).thenReturn(mockConn);

        PowerMockito.mockStatic(DbMailbox.class);
        PowerMockito.when(DbMailbox.qualifyTableName(any(), anyString())).thenReturn("ropc_token_store");
    }

    @Test
    public void testFindReturnNullREquiredParamterMissing() throws ServiceException {
        assertNull(store.find(null, null, "userAgent", "prvoider", "proto", "deviceid"));
        assertNull(store.find(null, null, "userAgent", null, "proto", "deviceid"));
        assertNull(store.find(null, null, "userAgent", "prvoider", "proto", null));
    }

    @Test
    public void testFindSuccessfullyMapResultSet() throws ServiceException, SQLException {
        when(mockStmt.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true, false);

        when(mockRs.getLong(1)).thenReturn(100L);
        when(mockRs.getString(2)).thenReturn("username");
        when(mockRs.getString(3)).thenReturn("eas");
        when(mockRs.getString(4)).thenReturn("userAgent");
        when(mockRs.getString(5)).thenReturn("deviceId");

        List<IRopcSessionRecord> results = store.find(null, "username", "userAgent", "okta",
                "eas", "deviceId");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(Long.valueOf(100L), results.get(0).getId());
        assertEquals("username", results.get(0).getUsername());

        verify(mockStmt).setString(1, "username");
        verify(mockStmt).setString(2, "okta");
        verify(mockStmt).setString(3, "eas");
        verify(mockStmt).setString(4, "userAgent");
        verify(mockStmt).setString(5, "deviceId");

        DbPool.closeResults(mockRs);
        DbPool.closeStatement(mockStmt);
        DbPool.quietClose(mockConn);
    }

    @Test
    public void testFindByIpSuccessfullyMapResultSet() throws ServiceException, SQLException {
        when(mockStmt.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true, false);

        when(mockRs.getLong(1)).thenReturn(100L);
        when(mockRs.getString(2)).thenReturn("username");
        when(mockRs.getString(3)).thenReturn("eas");
        when(mockRs.getString(4)).thenReturn("userAgent");
        when(mockRs.getString(5)).thenReturn("ip1234");

        List<IRopcSessionRecord> results = store.find(null, "username", "userAgent", "okta",
                "eas", "ip1234");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(Long.valueOf(100L), results.get(0).getId());
        assertEquals("username", results.get(0).getUsername());

        verify(mockStmt).setString(1, "username");
        verify(mockStmt).setString(2, "okta");
        verify(mockStmt).setString(3, "eas");
        verify(mockStmt).setString(4, "userAgent");
        verify(mockStmt).setString(5, "ip1234");

        DbPool.closeResults(mockRs);
        DbPool.closeStatement(mockStmt);
        DbPool.quietClose(mockConn);
    }

    @Test
    public void testupsertWhenIdIsNotNullPerformsupdate() throws SQLException, ServiceException {
        IRopcSessionRecord existingSession = new IRopcSessionRecord.Builder()
                .id(500L)
                .refreshToken("refreshToken1234")
                .idToken("idToken1234")
                .build();

        store.upsert(null, existingSession);

        verify(mockStmt, timeout(1)).executeUpdate();

        verify(mockStmt).setString(1, "refreshToken1234");
        verify(mockStmt).setString(2, "idToken1234");
        verify(mockStmt).setLong(eq(3), anyLong());
        verify(mockStmt).setLong(4, 500L);
    }

    @Test
    public void testDeleteExpiredTokenExecutedSuccessfully() throws SQLException, ServiceException,
            NoSuchFieldException {
        Field lcFiled = LC.class.getField("mfa_idp_hard_reauth_in_days");
        Object mockLcConfig = PowerMockito.mock(lcFiled.getType());
        Whitebox.setInternalState(LC.class, "mfa_idp_hard_reauth_in_days", mockLcConfig);

        ResultSet mockResultSet = PowerMockito.mock(ResultSet.class);
        when(mockStmt.executeQuery()).thenReturn(mockResultSet);
        when(mockResultSet.next()).thenReturn(true, true, true, true, true, false);
        when(mockResultSet.getString("TABLE_SCHEMA")).thenReturn("zimbra");

        when(mockStmt.executeUpdate()).thenReturn(5);

        DbRopcTokenStore.deleteExpiredTokens();
        verify(mockStmt, times(1)).executeQuery();

        verify(mockStmt, times(5)).executeUpdate();

        verify(mockConn, times(1)).commit();

    }

    @Test
    public void testDeleteRecordSuccessfully() throws SQLException, ServiceException {
        when(mockStmt.executeUpdate()).thenReturn(5);
        store.delete(null, 500L, "username", "userAgent", "okta",
                "eas", "deviceId");

        verify(mockStmt, times(1)).executeUpdate();
        verify(mockStmt).setLong(eq(1), anyLong());
        verify(mockConn, times(1)).commit();
    }
}
