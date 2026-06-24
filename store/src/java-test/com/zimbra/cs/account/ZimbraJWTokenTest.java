/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link ZimbraJWToken} built from a real {@link Account}
 * via the in-memory harness. Covers the constructor lifetime computation,
 * the property accessors, expiry logic, usage propagation, JWT generation, and
 * the validation failure path.
 */
public class ZimbraJWTokenTest {

    private Account account;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("jwt@example.com", "secret", new HashMap<String, Object>());
        account = prov.get(AccountBy.name, "jwt@example.com");
    }

    @Test
    public void constructorDefaultLifetimeComputesFutureExpiry() {
        // Arrange
        long before = System.currentTimeMillis();

        // Act
        ZimbraJWToken token = new ZimbraJWToken(account);

        // Assert — default lifetime is positive, so the token expires in the future
        assertTrue("expiry must be after creation time", token.getExpires() > before);
        assertFalse("a freshly created token must not be expired", token.isExpired());
    }

    @Test
    public void getAccountIdBuiltFromAccountMatchesAccountId() {
        // Arrange
        ZimbraJWToken token = new ZimbraJWToken(account);

        // Act / Assert
        assertEquals(account.getId(), token.getAccountId());
    }

    @Test
    public void constructorExplicitExpiresUsesProvidedExpiry() {
        // Arrange
        long expires = System.currentTimeMillis() + 999999L;

        // Act
        ZimbraJWToken token = new ZimbraJWToken(account, expires);

        // Assert
        assertEquals(expires, token.getExpires());
        assertFalse(token.isExpired());
    }

    @Test
    public void isExpiredPastExpiryReturnsTrue() {
        // Arrange — expiry already in the past
        ZimbraJWToken token = new ZimbraJWToken(account, System.currentTimeMillis() - 10000L);

        // Act / Assert
        assertTrue(token.isExpired());
    }

    @Test
    public void isRegisteredAnyJwtReturnsTrue() {
        // Arrange
        ZimbraJWToken token = new ZimbraJWToken(account);

        // Act / Assert — JWTs are always reported as registered
        assertTrue(token.isRegistered());
    }

    @Test
    public void isAdminNonAdminAccountReturnsFalse() {
        // Arrange
        ZimbraJWToken token = new ZimbraJWToken(account);

        // Act / Assert
        assertFalse(token.isAdmin());
        assertFalse(token.isDomainAdmin());
        assertFalse(token.isDelegatedAdmin());
    }

    @Test
    public void isZimbraUserInternalAccountReturnsTrue() {
        // Arrange
        ZimbraJWToken token = new ZimbraJWToken(account);

        // Act / Assert
        assertTrue("token for a zimbra account is a zimbra user", token.isZimbraUser());
    }

    @Test
    public void getUsageConstructedWithUsageReturnsThatUsage() {
        // Arrange
        ZimbraJWToken token = new ZimbraJWToken(account, Usage.TWO_FACTOR_AUTH);

        // Act / Assert
        assertEquals(Usage.TWO_FACTOR_AUTH, token.getUsage());
    }

    @Test
    public void getUsageDefaultConstructorReturnsAuth() {
        // Arrange
        ZimbraJWToken token = new ZimbraJWToken(account);

        // Act / Assert
        assertEquals(Usage.AUTH, token.getUsage());
    }

    @Test
    public void getEncodedFreshTokenProducesNonEmptyJwt() throws Exception {
        // Arrange
        ZimbraJWToken token = new ZimbraJWToken(account);

        // Act
        String jwt = token.getEncoded();

        // Assert — JWTs have three dot-delimited segments
        assertNotNull(jwt);
        assertTrue("encoded JWT must be non-empty", jwt.length() > 0);
        assertEquals("JWT must have header.payload.signature", 3, jwt.split("\\.").length);
    }

    @Test
    public void getEncodedCalledTwiceIsCached() throws Exception {
        // Arrange
        ZimbraJWToken token = new ZimbraJWToken(account);

        // Act
        String first = token.getEncoded();
        String second = token.getEncoded();

        // Assert — once generated, the encoded form is cached and stable
        assertEquals(first, second);
        assertEquals(first, token.toString());
    }

    @Test
    public void getProxyAuthTokenSetThenResetRoundTripsToNull() {
        // Arrange
        ZimbraJWToken token = new ZimbraJWToken(account);

        // Act / Assert — set, read back, then clear
        token.setProxyAuthToken("proxy-value");
        assertEquals("proxy-value", token.getProxyAuthToken());
        token.resetProxyAuthToken();
        assertEquals(null, token.getProxyAuthToken());
    }

    @Test
    public void getJWTokenGarbageInputThrowsAuthTokenException() {
        // Arrange — an unparseable JWT string
        // Act / Assert
        try {
            ZimbraJWToken.getJWToken("not-a-valid-jwt", "salt");
            fail("expected AuthTokenException for invalid JWT");
        } catch (AuthTokenException e) {
            assertTrue("message should mention JWT validation",
                    e.getMessage().toLowerCase().contains("jwt"));
        }
    }
}
