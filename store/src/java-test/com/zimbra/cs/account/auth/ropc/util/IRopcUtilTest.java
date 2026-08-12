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

package com.zimbra.cs.account.auth.ropc.util;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.auth.PasswordUtil;
import com.zimbra.cs.account.auth.ropc.IRopcCredCache;
import com.zimbra.cs.account.auth.ropc.store.IRopcSessionRecord;
import com.zimbra.cs.account.auth.ropc.store.IRopcTokenStore;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.CONVERT_TO_MILLI;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.PROVIDER;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.REQUEST_PARAM_CLIENT_ID;
import static com.zimbra.cs.account.auth.ropc.IRopcConstants.TOKEN_ENDPOINT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PasswordUtil.class, PasswordUtil.SSHA512.class, IRopcTokenStore.class, IRopcSessionRecord.class,
        IRopcCredCache.class, Account.class})
public class IRopcUtilTest {

    private IRopcTokenStore mockStore;

    private IRopcSessionRecord mockRecord;

    @Mock
    private Account mockAccount;

    @Before
    public void setUp() {
        mockStore = PowerMockito.mock(IRopcTokenStore.class);
        mockRecord = PowerMockito.mock(IRopcSessionRecord.class);
        mockAccount = Whitebox.newInstance(Account.class);
    }

    @Test
    public void testParseToMillisValidNumber() {
        Optional<Long> result = IRopcUtil.parseToMillis("5");
        assertTrue(result.isPresent());
        assertEquals(Long.valueOf(5L * CONVERT_TO_MILLI), result.get());
    }

    @Test
    public void testParseToMillisWithSpace() {
        Optional<Long> result = IRopcUtil.parseToMillis(" 5 ");
        assertTrue(result.isPresent());
        assertEquals(Long.valueOf(5L * CONVERT_TO_MILLI), result.get());
    }

    @Test
    public void testParseToMillisNullInput() {
        Optional<Long> result = IRopcUtil.parseToMillis(null);
        assertFalse(result.isPresent());
    }

    @Test
    public void testParseToMillisEmptyOrBlank() {
        Optional<Long> result = IRopcUtil.parseToMillis("");
        assertFalse(result.isPresent());
        Optional<Long> result1 = IRopcUtil.parseToMillis("  ");
        assertFalse(result1.isPresent());
    }

    @Test
    public void testParseToMillisInvalidFormat() {
        Optional<Long> result = IRopcUtil.parseToMillis("abc");
        assertFalse(result.isPresent());
    }

    @Test
    public void testExtractConfigValidArgs() throws ServiceException {
        List<String> args = Arrays.asList("token_endpoint=https://okta.com", "client_id=12321", "provider=okta");
        Map<String, String> result = IRopcUtil.extractConfigsFromArgs(args);

        assertEquals(3, result.size());
        assertEquals("https://okta.com", result.get(TOKEN_ENDPOINT));
        assertEquals("12321", result.get(REQUEST_PARAM_CLIENT_ID));
        assertEquals("okta", result.get(PROVIDER));
    }

    @Test
    public void testExtractConfigWithExtraEqualsSign() throws ServiceException {
        List<String> args = Collections.singletonList("token_endpoint=abc=def");
        Map<String, String> result = IRopcUtil.extractConfigsFromArgs(args);

        assertEquals(1, result.size());
        assertEquals("abc=def", result.get(TOKEN_ENDPOINT));
    }

    @Test
    public void testExtractConfigIgnoresInvalidFormat() throws ServiceException {
        List<String> args = Arrays.asList("token_endpoint=https://okta.com", "invalidformat", "=missingkey", null);
        Map<String, String> result = IRopcUtil.extractConfigsFromArgs(args);

        assertEquals(1, result.size());
        assertEquals("https://okta.com", result.get(TOKEN_ENDPOINT));
    }

    @Test
    public void testExtractConfigNullOrEmptyList() throws ServiceException {
        assertTrue(IRopcUtil.extractConfigsFromArgs(null).isEmpty());
        assertTrue(IRopcUtil.extractConfigsFromArgs(Collections.emptyList()).isEmpty());
    }

    @Test
    public void testFindInStoreFoundByDeviceIdPasswordMatches() throws ServiceException {
        String password = "password";
        String hash = "hashedPassword";
        when(mockRecord.getPasswordHash()).thenReturn(hash);
        when(mockStore.find(any(), anyString(), anyString(), anyString(), anyString(), anyString())).
                thenReturn(Collections.singletonList(mockRecord));

        PowerMockito.mockStatic(PasswordUtil.SSHA512.class);
        PowerMockito.when(PasswordUtil.SSHA512.verifySSHA512(hash, password)).thenReturn(true);
        IRopcSessionRecord result = IRopcUtil.findInStore(null, "user", "userAgent", "provider", "proto",
                "Deviceid", "ip", password, mockStore);
        assertNotNull(result);
        assertEquals(mockRecord, result);

        verify(mockStore, never()).findByIp(any(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void testFindInStoreFallBackToIpPasswordMatches() throws ServiceException {
        String password = "password";
        String hash = "hashedPassword";
        when(mockRecord.getPasswordHash()).thenReturn(hash);
        when(mockStore.find(any(), anyString(), anyString(), anyString(), anyString(), anyString())).
                thenReturn(Collections.emptyList());

        when(mockStore.findByIp(any(), anyString(), anyString(), anyString(), anyString(), anyString())).
                thenReturn(Collections.singletonList(mockRecord));

        PowerMockito.mockStatic(PasswordUtil.SSHA512.class);
        PowerMockito.when(PasswordUtil.SSHA512.verifySSHA512(hash, password)).thenReturn(true);
        IRopcSessionRecord result = IRopcUtil.findInStore(null, "user", "userAgent", "provider", "proto",
                "Deviceid", "ip", password, mockStore);
        assertNotNull(result);
        assertEquals(mockRecord, result);
    }

    @Test
    public void testFindInStorePasswordMismatch() throws ServiceException {
        String password = "wrongPassword";
        String hash = "hashedPassword";
        when(mockRecord.getPasswordHash()).thenReturn(hash);
        when(mockStore.find(any(), anyString(), anyString(), anyString(), anyString(), anyString())).
                thenReturn(Collections.emptyList());

        when(mockStore.findByIp(any(), anyString(), anyString(), anyString(), anyString(), anyString())).
                thenReturn(Collections.singletonList(mockRecord));

        PowerMockito.mockStatic(PasswordUtil.SSHA512.class);
        PowerMockito.when(PasswordUtil.SSHA512.verifySSHA512(hash, password)).thenReturn(false);
        IRopcSessionRecord result = IRopcUtil.findInStore(null, "user", "userAgent", "provider", "proto",
                "Deviceid", "ip", password, mockStore);
        assertNull(result);
    }

    @Test
    public void testFindInStoreNoRecordFoundInStore() throws ServiceException {
        when(mockStore.find(any(), anyString(), anyString(), anyString(), anyString(), anyString())).
                thenReturn(Collections.emptyList());

        when(mockStore.findByIp(any(), anyString(), anyString(), anyString(), anyString(), anyString())).
                thenReturn(Collections.emptyList());
        IRopcSessionRecord result = IRopcUtil.findInStore(null, "user", "userAgent", "provider", "proto",
                "Deviceid", "ip", "password", mockStore);
        assertNull(result);
    }

    @Test
    public void testRemoveDataForDeviceSuccessfullyClearCacheAndDb() throws ServiceException {
        PowerMockito.stub(PowerMockito.method(Account.class, "getName")).toReturn("testUser");

        Whitebox.setInternalState(IRopcCredCache.class, "STORE", mockStore);
        PowerMockito.mockStatic(IRopcCredCache.class);

        when(mockRecord.getUsername()).thenReturn("testUser");
        when(mockRecord.getUserAgent()).thenReturn("TestAgent");
        when(mockRecord.getProtocol()).thenReturn("eas");
        when(mockRecord.getProvider()).thenReturn("okta");
        when(mockRecord.getIp()).thenReturn("22.22.22.22");
        when(mockRecord.getDeviceId()).thenReturn("device123");

        when(mockStore.findByDeviceIdAndUsername(mockAccount, "device123")).
                thenReturn(Collections.singletonList(mockRecord));
        IRopcUtil.removeDataForDevice(mockAccount, "device123");
        PowerMockito.verifyStatic(times(1));
        IRopcCredCache.invalidate("testUser", "TestAgent", "eas", "okta", "22.22.22.22", "device123");

        verify(mockStore, times(1)).deleteByDeviceIdAndUsername(mockAccount, "device123");
    }

    @Test
    public void testRemoveDataForDeviceDoesNothingIfNoCandidateFound() throws ServiceException {
        PowerMockito.stub(PowerMockito.method(Account.class, "getName")).toReturn("testUser");
        Whitebox.setInternalState(IRopcCredCache.class, "STORE", mockStore);
        PowerMockito.mockStatic(IRopcCredCache.class);

        when(mockStore.findByDeviceIdAndUsername(mockAccount, "device123")).thenReturn(Collections.emptyList());
        IRopcUtil.removeDataForDevice(mockAccount, "device123");

        PowerMockito.verifyStatic(never());
        IRopcCredCache.invalidate(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(mockStore, never()).deleteByDeviceIdAndUsername(mockAccount, "device123");
    }

    @Test
    public void testRemoveDataForDeviceReturnEarlyOnNullParameter() throws ServiceException {
        PowerMockito.stub(PowerMockito.method(Account.class, "getName")).toReturn("testUser");
        Whitebox.setInternalState(IRopcCredCache.class, "STORE", mockStore);
        PowerMockito.mockStatic(IRopcCredCache.class);

        IRopcUtil.removeDataForDevice(null, "device123");
        IRopcUtil.removeDataForDevice(mockAccount, null);

        verify(mockStore, never()).findByDeviceIdAndUsername(any(Account.class), anyString());
        verify(mockStore, never()).deleteByDeviceIdAndUsername(any(Account.class), anyString());
    }

    @Test
    public void testRemoveDataForDeviceSafelyCatchesDatabaseExceptions() throws ServiceException {
        PowerMockito.stub(PowerMockito.method(Account.class, "getName")).toReturn("testUser");
        Whitebox.setInternalState(IRopcCredCache.class, "STORE", mockStore);
        PowerMockito.mockStatic(IRopcCredCache.class);
        when(mockStore.findByDeviceIdAndUsername(mockAccount, "device123")).
                thenThrow(new RuntimeException("Db failure"));

        IRopcUtil.removeDataForDevice(mockAccount, "device123");

        PowerMockito.verifyStatic(never());
        IRopcCredCache.invalidate(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
        verify(mockStore, never()).deleteByDeviceIdAndUsername(any(Account.class), anyString());
    }

}
