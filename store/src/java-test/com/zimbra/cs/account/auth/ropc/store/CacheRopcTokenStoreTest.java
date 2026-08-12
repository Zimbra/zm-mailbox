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
import com.zimbra.cs.account.Account;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.core.classloader.annotations.SuppressStaticInitializationFor;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LC.class, KnownKey.class, Account.class})
@SuppressStaticInitializationFor("com.zimbra.common.localconfig.LC")
public class CacheRopcTokenStoreTest {
    private CacheRopcTokenStore store;

    @Mock
    private Account mockAccount;

    @Before
    public void setup() throws NoSuchFieldException {
        Field lcFiled = LC.class.getField("mfa_idp_hard_reauth_in_days");
        Object mockLcConfig = PowerMockito.mock(lcFiled.getType());
        Whitebox.setInternalState(LC.class, "mfa_idp_hard_reauth_in_days", mockLcConfig);

        mockAccount = Whitebox.newInstance(Account.class);

        store = new CacheRopcTokenStore();
    }

    @Test
    public void testUpsertNewSession() throws ServiceException {
        IRopcSessionRecord session = new IRopcSessionRecord.Builder()
                .username("testUser")
                .userAgent("TestAgent")
                .protocol("eas")
                .provider("okta")
                .deviceId("deviceid123")
                .build();

        store.upsert(null, session);

        assertNotNull("ID should been generate and assigned", session.getId());

        List<IRopcSessionRecord> results = store.find(null, "testUser", "TestAgent", "okta", "eas", "deviceid123");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(session.getId(), results.get(0).getId());
    }

    @Test(expected = ServiceException.class)
    public void testUpsertNullSessionThrowsException() throws ServiceException {
        store.upsert(null, null);
    }

    @Test
    public void testUpsertExistingSessionUpdateRecord() throws ServiceException {
        IRopcSessionRecord session = new IRopcSessionRecord.Builder()
                .username("testUser")
                .userAgent("TestAgent")
                .protocol("eas")
                .provider("okta")
                .deviceId("deviceid123")
                .refreshToken("oldToken")
                .build();

        store.upsert(null, session);
        Long generatedId = session.getId();

        session.setRefreshToken("newToken");
        store.upsert(null, session);

        List<IRopcSessionRecord> results = store.find(null, "testUser", "TestAgent", "okta", "eas", "deviceid123");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(generatedId, results.get(0).getId());
        assertEquals("newToken", results.get(0).getRefreshToken());
    }

    @Test
    public void testUpdateDeviceIdMoveSessionToNewKey() throws ServiceException {
        IRopcSessionRecord session = new IRopcSessionRecord.Builder()
                .username("testUser")
                .userAgent("TestAgent")
                .protocol("eas")
                .provider("okta")
                .ip("ip123")
                .build();

        store.upsert(null, session);
        Long generatedId = session.getId();

        List<IRopcSessionRecord> ipResults = store.findByIp(null, "testUser", "TestAgent", "okta", "eas", "ip123");

        assertNotNull(ipResults);
        assertEquals(1, ipResults.size());

        IRopcSessionRecord newsession = new IRopcSessionRecord.Builder()
                .id(generatedId)
                .username("testUser")
                .userAgent("TestAgent")
                .protocol("eas")
                .provider("okta")
                .ip("ip123")
                .deviceId("device123")
                .build();
        store.updateDeviceId(null, newsession);

        List<IRopcSessionRecord> newDeviceResults = store.find(null, "testUser", "TestAgent",
                "okta", "eas", "device123");

        assertNotNull(newDeviceResults);
        assertEquals(1, newDeviceResults.size());
        assertEquals(generatedId, newDeviceResults.get(0).getId());
        assertEquals("device123", newDeviceResults.get(0).getDeviceId());
    }

    @Test
    public void testUpdateDeviceIdNUllChecksRetuenEarly() throws ServiceException {
        store.updateDeviceId(null, null);
        IRopcSessionRecord emptySession = new IRopcSessionRecord.Builder()
                .build();
        store.updateDeviceId(null, emptySession);
    }

    @Test
    public void testDeleteRemoveSessionFromCache() throws ServiceException {

        IRopcSessionRecord session = new IRopcSessionRecord.Builder()
                .username("testUser")
                .userAgent("TestAgent")
                .protocol("eas")
                .provider("okta")
                .deviceId("device123")
                .build();

        store.upsert(null, session);
        assertNotNull(store.find(null, "testUser", "TestAgent", "okta", "eas", "device123"));

        store.delete(null, session.getId(), "testUser", "TestAgent", "okta", "eas", "device123");

        assertNull(store.find(null, "testUser", "TestAgent", "okta", "eas", "device123"));;
    }

    @Test
    public void testDeleteNullIdDoesNothing() throws ServiceException {
        store.delete(null, null, "testUser", "TestAgent", "okta", "eas", "device123");
    }

    @Test
    public void testFindByDeviceIdAndUsernameSuccessfully() throws ServiceException {
        PowerMockito.stub(PowerMockito.method(Account.class, "getName")).toReturn("testUser");

        IRopcSessionRecord session = new IRopcSessionRecord.Builder()
                .username("testUser")
                .userAgent("TestAgent")
                .protocol("eas")
                .provider("okta")
                .deviceId("device123")
                .build();

        store.upsert(null, session);

        List<IRopcSessionRecord> results = store.findByDeviceIdAndUsername(mockAccount, "device123");

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("testUser", results.get(0).getUsername());
        assertEquals("device123", results.get(0).getDeviceId());
    }

    @Test
    public void testFindByDeviceIdAndUsernameNullCheckReturnEarly() throws ServiceException {
        PowerMockito.stub(PowerMockito.method(Account.class, "getName")).toReturn("testUser");
        assertNull(store.findByDeviceIdAndUsername(null, "device123"));
        assertNull(store.findByDeviceIdAndUsername(mockAccount, null));
        assertNull(store.findByDeviceIdAndUsername(null, null));
    }

    @Test
    public void testDeleteByDeviceIdAndUsernameRemoveSessionFromCache() throws ServiceException {
        PowerMockito.stub(PowerMockito.method(Account.class, "getName")).toReturn("testUser");

        IRopcSessionRecord targetSession = new IRopcSessionRecord.Builder()
                .username("testUser")
                .userAgent("TestAgent")
                .protocol("eas")
                .provider("okta")
                .deviceId("device123")
                .build();

        store.upsert(null, targetSession);

        IRopcSessionRecord unrelatedSession = new IRopcSessionRecord.Builder()
                .username("testUser")
                .userAgent("SafeAgent")
                .protocol("eas")
                .provider("okta")
                .deviceId("device999")
                .build();

        store.upsert(null, unrelatedSession);

        assertNotNull(store.findByDeviceIdAndUsername(mockAccount, "device123"));
        assertNotNull(store.findByDeviceIdAndUsername(mockAccount, "device999"));

        store.deleteByDeviceIdAndUsername(mockAccount, "device123");
        assertNull(store.findByDeviceIdAndUsername(mockAccount, "device123"));

        List<IRopcSessionRecord> remianingResults = store.findByDeviceIdAndUsername(mockAccount, "device999");
        assertNotNull(remianingResults);
        assertEquals(1, remianingResults.size());
        assertEquals("device999", remianingResults.get(0).getDeviceId());
    }

    @Test
    public void testDeleteByDeviceIdAndUsernameRemoveNullCheckDoNothing() throws ServiceException {
        PowerMockito.stub(PowerMockito.method(Account.class, "getName")).toReturn("testUser");

        IRopcSessionRecord session = new IRopcSessionRecord.Builder()
                .username("testUser")
                .userAgent("TestAgent")
                .protocol("eas")
                .provider("okta")
                .deviceId("device123")
                .build();

        store.upsert(null, session);

        store.deleteByDeviceIdAndUsername(null, "device123");
        store.deleteByDeviceIdAndUsername(mockAccount, null);

        assertNotNull(store.findByDeviceIdAndUsername(mockAccount, "device123"));
    }
}
