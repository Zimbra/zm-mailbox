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

import com.zimbra.common.util.ZimbraLog;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DbRopcTokenStore.class, ZimbraLog.class})
public class IRopcTokenCleanupSchedulerTest {

    @Before
    public void setUp() {
        Whitebox.setInternalState(IRopcTokenCleanupScheduler.class, "started", false);
    }

    @Test
    public void testStartInitializesSuccessfully() {
        assertFalse(IRopcTokenCleanupScheduler.isStarted());
        IRopcTokenCleanupScheduler.start();
        assertTrue(IRopcTokenCleanupScheduler.isStarted());
    }

    @Test
    public void testStartIWhneAlreadyStartedDoesNothing() {
        Whitebox.setInternalState(IRopcTokenCleanupScheduler.class, "started", true);
        IRopcTokenCleanupScheduler.start();

        // it remains true and does not crash
        assertTrue(IRopcTokenCleanupScheduler.isStarted());
    }

    @Test
    public void testCleanExpiredTokenCallsDbLayer() throws Exception {
        PowerMockito.mockStatic(DbRopcTokenStore.class);
        Whitebox.invokeMethod(IRopcTokenCleanupScheduler.class, "cleanExpiredTokens");

        PowerMockito.verifyStatic(times(1));
        DbRopcTokenStore.deleteExpiredTokens();
    }
}
