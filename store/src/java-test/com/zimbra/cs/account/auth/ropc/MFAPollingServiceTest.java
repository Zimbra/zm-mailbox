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

package com.zimbra.cs.account.auth.ropc;

import com.zimbra.common.service.ServiceException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MFAPollingService.class, MFAChallenge.class, IRopcHandler.class})
public class MFAPollingServiceTest {

    private MFAPollingService mfaPollingService = MFAPollingService.getInstance();

    private ScheduledExecutorService testExecutor;

    private IRopcHandler mockProvider;

    private MFAChallenge mockChallenge;

    @Before
    public void setUp() {

        testExecutor = Executors.newScheduledThreadPool(1);

        Whitebox.setInternalState(mfaPollingService, "scheduler", testExecutor);
        Whitebox.setInternalState(mfaPollingService, "admission", new Semaphore(10));
        Whitebox.setInternalState(mfaPollingService, "active", new ConcurrentHashMap<>());

        mockProvider = mock(IRopcHandler.class);
        mockChallenge = mock(MFAChallenge.class);

        when(mockChallenge.getDedupeKey()).thenReturn("test-dedupe-key");
    }

    @After
    public void teatDown() {
        if (testExecutor != null && !testExecutor.isShutdown()) {
            testExecutor.shutdown();
        }
    }

    @Test
    public void testAwaitWithNullProviderReturnsError() {
        MFAPollResult result = mfaPollingService.await(null, mockChallenge, 1000L, 5000L);
        assertEquals(MFAPollResult.ERROR, result);
    }

    @Test
    public void testAwaitWithNullPChallengeReturnsError() {
        MFAPollResult result = mfaPollingService.await(mockProvider, null, 1000L, 5000L);
        assertEquals(MFAPollResult.ERROR, result);
    }

    @Test
    public void testAwaitAdmissionLimitReachedReturnsError() throws ServiceException {
        Whitebox.setInternalState(mfaPollingService, "admission", new Semaphore(0));
        MFAPollResult result = mfaPollingService.await(mockProvider, mockChallenge, 1000L, 5000L);
        assertEquals(MFAPollResult.ERROR, result);
        verify(mockProvider, never()).pollChallenge(any(MFAChallenge.class));
    }

    @Test
    public void testAwaitSuccessfulPoll() throws ServiceException {
        when(mockProvider.pollChallenge((mockChallenge))).thenReturn(MFAPollResult.SUCCESS);

        MFAPollResult result = mfaPollingService.await(mockProvider, mockChallenge, 10L, 2000L);
        assertEquals(MFAPollResult.SUCCESS, result);
        ConcurrentHashMap<?, ?> active = Whitebox.getInternalState(mfaPollingService, "active");
        assertEquals(0, active.size());
    }

    @Test
    public void testAwaitPollExpireViaDeadline() throws ServiceException {
        when(mockProvider.pollChallenge((mockChallenge))).thenReturn(MFAPollResult.WAITING);

        MFAPollResult result = mfaPollingService.await(mockProvider, mockChallenge, 10L, 50L);
        assertEquals(MFAPollResult.EXPIRED, result);
        ConcurrentHashMap<?, ?> active = Whitebox.getInternalState(mfaPollingService, "active");
        assertEquals(0, active.size());
    }

    @Test
    public void testAwaitProviderThrowsExceptionREturnError() throws ServiceException {
        when(mockProvider.pollChallenge((mockChallenge))).thenThrow(new RuntimeException("IDP connection issue"));

        MFAPollResult result = mfaPollingService.await(mockProvider, mockChallenge, 10L, 50L);
        assertEquals(MFAPollResult.ERROR, result);
        ConcurrentHashMap<?, ?> active = Whitebox.getInternalState(mfaPollingService, "active");
        assertEquals(0, active.size());
    }
}
