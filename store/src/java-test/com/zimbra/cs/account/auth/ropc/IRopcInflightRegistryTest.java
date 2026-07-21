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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({IRopcInflightRegistry.class})
@SuppressWarnings("unchecked")
public class IRopcInflightRegistryTest {

    private IRopcInflightRegistry registry = new IRopcInflightRegistry();

    @Test
    public void testKeyGenerationWithNullProtocol() {
        String key = IRopcInflightRegistry.key("testUser", "testAgent", "testProvider", null, "1234312");
        String expectedkey = "testAgent|testUser|testProvider||1234312";
        assertEquals(expectedkey, key);
    }

    @Test
    public void testExecuteSuccessful() {
        Supplier<String> mockTask = mock(Supplier.class);
        when(mockTask.get()).thenReturn("successful_authA_result");

        String result = registry.execute("test_key_1", mockTask, "Fallback_result");
        assertEquals("successful_authA_result", result);

        ConcurrentHashMap<String, CompletableFuture<Object>> inflight = Whitebox.getInternalState(registry, "inflight");
        assertEquals(0, inflight.size());
    }

    @Test
    public void testAdmissionLimitReachedReturnsFallback() {
        Whitebox.setInternalState(registry, "admission", new Semaphore(0));
        Supplier<String> mockTask = mock(Supplier.class);
        when(mockTask.get()).thenReturn("successful_authA_result");

        String result = registry.execute("test_key_1", mockTask, "Fallback_result");
        assertEquals("Fallback_result", result);

        ConcurrentHashMap<String, CompletableFuture<Object>> inflight = Whitebox.getInternalState(registry, "inflight");
        assertEquals(0, inflight.size());
    }

    @Test
    public void testExecuteWIthExistingInflighTask() {
        CompletableFuture existingFuture = new CompletableFuture();
        ConcurrentHashMap<String, CompletableFuture<Object>> inflightMap = new ConcurrentHashMap<>();
        inflightMap.put("duplicate_key", existingFuture);
        Whitebox.setInternalState(registry, "inflight", inflightMap);

        Supplier<String> mockTask = mock(Supplier.class);
        existingFuture.complete("result_from_first_thread");

        String result = registry.execute("duplicate_key", mockTask, "Fallback_result");

        assertEquals("result_from_first_thread", result);
        verify(mockTask, never()).get();
    }

    @Test
    public void testExecuteWithExistingInflighTaskWaitTimeEnds() {
        Whitebox.setInternalState(registry, "maxWaitMillis", 1);
        CompletableFuture existingFuture = new CompletableFuture();
        ConcurrentHashMap<String, CompletableFuture<Object>> inflightMap = new ConcurrentHashMap<>();
        inflightMap.put("duplicate_key", existingFuture);
        Whitebox.setInternalState(registry, "inflight", inflightMap);

        Supplier<String> mockTask = mock(Supplier.class);

        String result = registry.execute("duplicate_key", mockTask, "Fallback_result");

        assertEquals("Fallback_result", result);
        verify(mockTask, never()).get();
    }
}
