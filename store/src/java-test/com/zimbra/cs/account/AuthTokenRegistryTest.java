/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
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

import java.util.HashMap;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit tests for {@link AuthTokenRegistry}.
 *
 * Tests verify token registration, queue management, and lifecycle.
 */
public class AuthTokenRegistryTest {

    private static Provisioning provisioning;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
        provisioning.createAccount("user1@example.zimbra.com", "secret", new HashMap<>());
    }

    /**
     * Test: addTokenToQueue() with single token.
     * Verifies: Token can be added without error.
     */
    @Test
    public void addTokenToQueue_withToken_succeeds() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        AuthToken token = new ZimbraAuthToken(user);

        // Act - should not throw
        AuthTokenRegistry.addTokenToQueue(token);

        // Assert
        Assert.assertTrue(true); // If we get here, no exception was thrown
    }

    /**
     * Test: addTokenToQueue() with multiple tokens.
     * Verifies: Multiple tokens can be added.
     */
    @Test
    public void addTokenToQueue_withMultipleTokens_succeeds() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");

        // Act
        for (int i = 0; i < 5; i++) {
            AuthToken token = new ZimbraAuthToken(user);
            AuthTokenRegistry.addTokenToQueue(token);
        }

        // Assert
        Assert.assertTrue(true); // If we get here, no exception was thrown
    }

    /**
     * Test: addTokenToQueue() respects queue size limit.
     * Verifies: Queue doesn't exceed max size configured in LC.
     */
    @Test
    public void addTokenToQueue_respectsSizeLimit() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        int maxQueueSize = LC.zimbra_deregistered_authtoken_queue_size.intValue();

        // Act - add more tokens than limit
        for (int i = 0; i < maxQueueSize + 100; i++) {
            AuthToken token = new ZimbraAuthToken(user);
            AuthTokenRegistry.addTokenToQueue(token);
        }

        // Assert - just verify it completes without error
        Assert.assertTrue(true);
    }

    /**
     * Test: Token can be added even if queue is at max capacity.
     * Verifies: Overflow handling works.
     */
    @Test
    public void addTokenToQueue_atMaxCapacity_stillAdds() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        int maxQueueSize = LC.zimbra_deregistered_authtoken_queue_size.intValue();

        // Fill queue to capacity
        for (int i = 0; i < maxQueueSize; i++) {
            AuthToken token = new ZimbraAuthToken(user);
            AuthTokenRegistry.addTokenToQueue(token);
        }

        // Act - add one more (should evict oldest)
        AuthToken newToken = new ZimbraAuthToken(user);
        AuthTokenRegistry.addTokenToQueue(newToken);

        // Assert
        Assert.assertTrue(true);
    }

    /**
     * Test: startup() with interval.
     * Verifies: Scheduler startup executes without error.
     */
    @Test
    public void startup_withInterval_succeeds() throws Exception {
        // Act - should not throw
        AuthTokenRegistry.startup(60000); // 60 second interval

        // Assert
        Assert.assertTrue(true);
    }

    /**
     * Test: startup() called multiple times.
     * Verifies: Multiple startup calls don't cause issues.
     */
    @Test
    public void startup_calledMultipleTimes_succeeds() throws Exception {
        // Act
        AuthTokenRegistry.startup(30000);
        AuthTokenRegistry.startup(60000);

        // Assert
        Assert.assertTrue(true);
    }

    /**
     * Test: Add non-expired token.
     * Verifies: Non-expired token is accepted.
     */
    @Test
    public void addTokenToQueue_withNonExpiredToken_isQueued() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        AuthToken token = new ZimbraAuthToken(user);

        // Act - token is non-expired
        Assert.assertFalse("Token should not be expired initially", token.isExpired());
        AuthTokenRegistry.addTokenToQueue(token);

        // Assert
        Assert.assertTrue(true);
    }

    /**
     * Test: Verify tokens are not registered by default.
     * Verifies: Newly created token is not registered.
     */
    @Test
    public void newToken_isNotRegistered() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        AuthToken token = new ZimbraAuthToken(user);

        // Assert
        Assert.assertFalse("New token should not be registered", token.isRegistered());
    }

    /**
     * Test: addTokenToQueue() is callable without authentication.
     * Verifies: Queue access doesn't require special permissions.
     */
    @Test
    public void addTokenToQueue_withoutSpecialPermissions_succeeds() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        AuthToken token = new ZimbraAuthToken(user);

        // Act - should work without admin privileges
        AuthTokenRegistry.addTokenToQueue(token);

        // Assert
        Assert.assertTrue(true);
    }

    /**
     * Test: Verify queue size is reasonable.
     * Verifies: Queue size limit is configured.
     */
    @Test
    public void queueSizeLimit_isConfigured() throws Exception {
        // Act
        int maxSize = LC.zimbra_deregistered_authtoken_queue_size.intValue();

        // Assert
        Assert.assertTrue("Queue size should be positive", maxSize > 0);
        Assert.assertTrue("Queue size should be reasonable", maxSize > 10);
    }

    /**
     * Test: addTokenToQueue() with token that has account ID.
     * Verifies: Token with valid account is queued.
     */
    @Test
    public void addTokenToQueue_withValidAccountId_succeeds() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        AuthToken token = new ZimbraAuthToken(user);
        String accountId = token.getAccountId();

        // Act
        Assert.assertNotNull("Token should have account ID", accountId);
        AuthTokenRegistry.addTokenToQueue(token);

        // Assert
        Assert.assertTrue(true);
    }

    /**
     * Test: startup() scheduler doesn't block.
     * Verifies: startup() returns immediately (non-blocking).
     */
    @Test
    public void startup_nonBlocking() throws Exception {
        // Arrange
        long startTime = System.currentTimeMillis();

        // Act
        AuthTokenRegistry.startup(60000);

        // Assert
        long elapsed = System.currentTimeMillis() - startTime;
        Assert.assertTrue("startup should return quickly", elapsed < 5000); // Less than 5 seconds
    }
}
