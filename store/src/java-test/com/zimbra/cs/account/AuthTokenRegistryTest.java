/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2016 Synacor, Inc.
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

package com.zimbra.cs.account;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Functional tests for {@link AuthTokenRegistry}, the in-process queue of deregistered
 * auth tokens awaiting broadcast. Tokens are real {@link ZimbraAuthToken} instances built
 * from a real {@link Account} in the in-memory {@link MockProvisioning} harness. The queue
 * is a private static field, read back via reflection to assert add and size-eviction
 * behaviour driven by {@link LC#zimbra_deregistered_authtoken_queue_size}.
 */
public class AuthTokenRegistryTest {

    private Provisioning prov;

    private long savedQueueSize;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @SuppressWarnings("unchecked")
    private ConcurrentLinkedQueue<AuthToken> queue() throws Exception {
        Field f = AuthTokenRegistry.class.getDeclaredField("deregisteredOutAuthTokens");
        f.setAccessible(true);
        return (ConcurrentLinkedQueue<AuthToken>) f.get(null);
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        prov.createAccount("token@example.com", "secret", new HashMap<String, Object>());
        savedQueueSize = LC.zimbra_deregistered_authtoken_queue_size.longValue();
        // Start each test with an empty queue (static state shared across the JVM).
        queue().clear();
    }

    @After
    public void tearDown() throws Exception {
        // Restore shared static state so other suites see the production default.
        LC.zimbra_deregistered_authtoken_queue_size.setDefault(savedQueueSize);
        queue().clear();
    }

    private AuthToken newToken() throws Exception {
        Account account = prov.get(AccountBy.name, "token@example.com");
        return new ZimbraAuthToken(account);
    }

    @Test
    public void addTokenToQueueSingleTokenIsEnqueued() throws Exception {
        // Arrange
        AuthToken token = newToken();

        // Act
        AuthTokenRegistry.addTokenToQueue(token);

        // Assert — the exact token is now the sole queue entry.
        assertEquals(1, queue().size());
        assertTrue("queue must contain the added token", queue().contains(token));
    }

    @Test
    public void addTokenToQueueMultipleTokensAllEnqueuedInOrder() throws Exception {
        // Arrange
        AuthToken first = newToken();
        AuthToken second = newToken();
        AuthToken third = newToken();

        // Act
        AuthTokenRegistry.addTokenToQueue(first);
        AuthTokenRegistry.addTokenToQueue(second);
        AuthTokenRegistry.addTokenToQueue(third);

        // Assert — FIFO ordering preserved, all three present.
        ConcurrentLinkedQueue<AuthToken> q = queue();
        assertEquals(3, q.size());
        assertSame(first, q.poll());
        assertSame(second, q.poll());
        assertSame(third, q.poll());
    }

    private static void assertSame(Object expected, Object actual) {
        assertTrue("expected the same instance", expected == actual);
    }

    @Test
    public void addTokenToQueueOverCapacityEvictsOldestToBound() throws Exception {
        // Arrange — shrink the bound so eviction is reachable without 5000 tokens.
        LC.zimbra_deregistered_authtoken_queue_size.setDefault(2L);
        int limit = LC.zimbra_deregistered_authtoken_queue_size.intValue();
        assertEquals(2, limit);

        AuthToken t1 = newToken();
        AuthToken t2 = newToken();
        AuthToken t3 = newToken();
        AuthToken t4 = newToken();

        // Act — add four; the eviction loop trims while size > limit before each add.
        AuthTokenRegistry.addTokenToQueue(t1);
        AuthTokenRegistry.addTokenToQueue(t2);
        AuthTokenRegistry.addTokenToQueue(t3);
        AuthTokenRegistry.addTokenToQueue(t4);

        // Assert — queue stays close to the bound and the newest token survived,
        // while the oldest token was evicted to make room.
        ConcurrentLinkedQueue<AuthToken> q = queue();
        assertTrue("queue must be bounded near the configured size", q.size() <= limit + 1);
        assertTrue("newest token must remain", q.contains(t4));
        assertFalse("oldest token must have been evicted", q.contains(t1));
    }

    @Test
    public void addTokenToQueueEmptyQueueUnderLimitDoesNotEvict() throws Exception {
        // Arrange — generous bound, single add must not trigger the eviction loop.
        LC.zimbra_deregistered_authtoken_queue_size.setDefault(10L);
        AuthToken token = newToken();

        // Act
        AuthTokenRegistry.addTokenToQueue(token);

        // Assert — token retained, nothing evicted.
        assertEquals(1, queue().size());
        assertTrue(queue().contains(token));
    }

    @Test
    public void startupValidIntervalSchedulesWithoutError() throws Exception {
        // Arrange — a long interval so the scheduled task never actually fires during the test.
        long interval = 60L * 60L * 1000L;

        // Act — scheduling against the shared daemon timer must not throw.
        AuthTokenRegistry.startup(interval);

        // Assert — control returned normally; the registry is still usable afterwards.
        AuthToken token = newToken();
        AuthTokenRegistry.addTokenToQueue(token);
        assertTrue("registry remains functional after startup", queue().contains(token));
    }
}
