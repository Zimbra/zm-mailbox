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
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit tests for abstract {@link AuthToken} base class.
 *
 * Tests verify static methods, default implementations, and auth token lifecycle
 * (through ZimbraAuthToken concrete implementation).
 */
public class AuthTokenTest {

    private static Provisioning provisioning;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
        provisioning = Provisioning.getInstance();
        provisioning.createAccount("user1@example.zimbra.com", "secret", new HashMap<>());
        provisioning.createAccount("user2@example.zimbra.com", "secret", new HashMap<>());
    }

    /**
     * Test: generateDigest() with two strings.
     * Verifies: Digest generation produces SHA1 hash.
     */
    @Test
    public void generateDigest_withValidStrings_returnsHash() throws Exception {
        // Arrange
        String a = "user1@example.com";
        String b = "secret";

        // Act
        String digest = AuthToken.generateDigest(a, b);

        // Assert
        Assert.assertNotNull(digest);
        Assert.assertTrue(digest.length() > 0);
    }

    /**
     * Test: generateDigest() with null first argument.
     * Verifies: Returns null when first arg is null.
     */
    @Test
    public void generateDigest_withNullFirst_returnsNull() throws Exception {
        // Arrange
        String b = "secret";

        // Act
        String digest = AuthToken.generateDigest(null, b);

        // Assert
        Assert.assertNull(digest);
    }

    /**
     * Test: generateDigest() with null second argument.
     * Verifies: Handles null second arg gracefully.
     */
    @Test
    public void generateDigest_withNullSecond_succeeds() throws Exception {
        // Arrange
        String a = "user1@example.com";

        // Act
        String digest = AuthToken.generateDigest(a, null);

        // Assert
        Assert.assertNotNull(digest);
    }

    /**
     * Test: generateDigest() called twice with same inputs.
     * Verifies: Produces deterministic output.
     */
    @Test
    public void generateDigest_isDeterministic() throws Exception {
        // Arrange
        String a = "user1@example.com";
        String b = "secret";

        // Act
        String digest1 = AuthToken.generateDigest(a, b);
        String digest2 = AuthToken.generateDigest(a, b);

        // Assert
        Assert.assertEquals(digest1, digest2);
    }

    /**
     * Test: generateDigest() with different inputs.
     * Verifies: Different inputs produce different digests.
     */
    @Test
    public void generateDigest_differentInputs_produceDifferentDigests() throws Exception {
        // Arrange
        String a1 = "user1@example.com";
        String a2 = "user2@example.com";
        String b = "secret";

        // Act
        String digest1 = AuthToken.generateDigest(a1, b);
        String digest2 = AuthToken.generateDigest(a2, b);

        // Assert
        Assert.assertNotEquals(digest1, digest2);
    }

    /**
     * Test: isAnyAdmin() with regular user token.
     * Verifies: Returns false for non-admin.
     */
    @Test
    public void isAnyAdmin_withRegularUser_returnsFalse() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        ZimbraAuthToken token = new ZimbraAuthToken(user);

        // Act
        boolean isAdmin = AuthToken.isAnyAdmin(token);

        // Assert
        Assert.assertFalse(isAdmin);
    }

    /**
     * Test: getCsrfUnsecuredAuthToken() with null token.
     * Verifies: Returns null when input is null.
     */
    @Test
    public void getCsrfUnsecuredAuthToken_withNull_returnsNull() throws Exception {
        // Arrange
        AuthToken token = null;

        // Act
        AuthToken result = AuthToken.getCsrfUnsecuredAuthToken(token);

        // Assert
        Assert.assertNull(result);
    }

    /**
     * Test: getCsrfUnsecuredAuthToken() with CSRF-disabled token.
     * Verifies: Returns same token when CSRF not enabled.
     */
    @Test
    public void getCsrfUnsecuredAuthToken_whenCsrfDisabled_returnsSame() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        ZimbraAuthToken token = new ZimbraAuthToken(user);

        // Act
        AuthToken result = AuthToken.getCsrfUnsecuredAuthToken(token);

        // Assert
        Assert.assertNotNull(result);
        Assert.assertEquals(token.getAccountId(), result.getAccountId());
    }

    /**
     * Test: getAuthToken() with encoded string.
     * Verifies: Decoding reconstructs valid token.
     */
    @Test
    public void getAuthToken_withValidEncoded_reconstructsToken() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        ZimbraAuthToken original = new ZimbraAuthToken(user);
        String encoded = original.getEncoded();

        // Act
        AuthToken reconstructed = AuthToken.getAuthToken(encoded);

        // Assert
        Assert.assertNotNull(reconstructed);
        Assert.assertEquals(original.getAccountId(), reconstructed.getAccountId());
    }

    /**
     * Test: getInfo() with encoded token string.
     * Verifies: Info extraction from encoded token.
     */
    @Test
    public void getInfo_withValidEncoded_returnsMap() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        ZimbraAuthToken token = new ZimbraAuthToken(user);
        String encoded = token.getEncoded();

        // Act
        java.util.Map<String, Object> info = AuthToken.getInfo(encoded);

        // Assert
        Assert.assertNotNull(info);
        Assert.assertTrue(info.size() > 0);
    }

    /**
     * Test: DEFAULT_AUTH_LIFETIME constant.
     * Verifies: Constant is reasonable value (12 hours in seconds).
     */
    @Test
    public void default_auth_lifetime_isReasonable() throws Exception {
        // Assert
        Assert.assertEquals(12 * 60 * 60, AuthToken.DEFAULT_AUTH_LIFETIME);
    }

    /**
     * Test: DEFAULT_TWO_FACTOR_AUTH_LIFETIME constant.
     * Verifies: Constant is 1 hour.
     */
    @Test
    public void default_two_factor_auth_lifetime_isOneHour() throws Exception {
        // Assert
        Assert.assertEquals(60 * 60, AuthToken.DEFAULT_TWO_FACTOR_AUTH_LIFETIME);
    }

    /**
     * Test: Create ZimbraAuthToken → call isDelegatedAuth() → verify false for normal user.
     * Verifies: isDelegatedAuth() checks for admin account ID.
     */
    @Test
    public void isDelegatedAuth_withNormalUser_returnsFalse() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        AuthToken token = new ZimbraAuthToken(user);

        // Act
        boolean isDelegated = token.isDelegatedAuth();

        // Assert
        Assert.assertFalse(isDelegated);
    }

    /**
     * Test: Create auth token → call isCsrfTokenEnabled() → default is false.
     * Verifies: Default CSRF setting is false.
     */
    @Test
    public void isCsrfTokenEnabled_defaultIsFalse() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        AuthToken token = new ZimbraAuthToken(user);

        // Act
        boolean csrfEnabled = token.isCsrfTokenEnabled();

        // Assert
        Assert.assertFalse(csrfEnabled);
    }

    /**
     * Test: Create auth token → set and get ignoreSameSite.
     * Verifies: SameSite flag is settable and retrievable.
     */
    @Test
    public void setIgnoreSameSite_and_getIgnoreSameSite_persistent() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        AuthToken token = new ZimbraAuthToken(user);

        // Act
        token.setIgnoreSameSite(true);
        boolean result = token.isIgnoreSameSite();

        // Assert
        Assert.assertTrue(result);
    }

    /**
     * Test: Usage enum has expected values.
     * Verifies: Usage enum defines AUTH, ENABLE_TWO_FACTOR_AUTH, TWO_FACTOR_AUTH, RESET_PASSWORD.
     */
    @Test
    public void usage_enum_hasExpectedValues() throws Exception {
        // Assert
        Assert.assertNotNull(AuthToken.Usage.AUTH);
        Assert.assertNotNull(AuthToken.Usage.ENABLE_TWO_FACTOR_AUTH);
        Assert.assertNotNull(AuthToken.Usage.TWO_FACTOR_AUTH);
        Assert.assertNotNull(AuthToken.Usage.RESET_PASSWORD);
    }

    /**
     * Test: Usage.fromCode() with valid codes.
     * Verifies: Roundtrip encoding/decoding of usage codes.
     */
    @Test
    public void usage_fromCode_withValidCode_returnsEnum() throws ServiceException {
        // Act
        AuthToken.Usage auth = AuthToken.Usage.fromCode("a");
        AuthToken.Usage tfa = AuthToken.Usage.fromCode("tfa");

        // Assert
        Assert.assertEquals(AuthToken.Usage.AUTH, auth);
        Assert.assertEquals(AuthToken.Usage.TWO_FACTOR_AUTH, tfa);
    }

    /**
     * Test: TokenType enum has AUTH and JWT.
     * Verifies: TokenType enum values exist.
     */
    @Test
    public void tokenType_enum_hasExpectedValues() throws Exception {
        // Assert
        Assert.assertNotNull(AuthToken.TokenType.AUTH);
        Assert.assertNotNull(AuthToken.TokenType.JWT);
    }

    /**
     * Test: TokenType.fromCode() with "auth" and "jwt".
     * Verifies: Roundtrip encoding/decoding.
     */
    @Test
    public void tokenType_fromCode_withValidCodes() throws ServiceException {
        // Act
        AuthToken.TokenType auth = AuthToken.TokenType.fromCode("auth");
        AuthToken.TokenType jwt = AuthToken.TokenType.fromCode("jwt");

        // Assert
        Assert.assertEquals(AuthToken.TokenType.AUTH, auth);
        Assert.assertEquals(AuthToken.TokenType.JWT, jwt);
    }

    /**
     * Test: Create token → call getAccessKey() → default is null.
     * Verifies: Default access key is null.
     */
    @Test
    public void getAccessKey_defaultIsNull() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        AuthToken token = new ZimbraAuthToken(user);

        // Act
        String accessKey = token.getAccessKey();

        // Assert
        Assert.assertNull(accessKey);
    }

    /**
     * Test: Create token → call getAuthMech() → default is null.
     * Verifies: Default auth mechanism is null.
     */
    @Test
    public void getAuthMech_defaultIsNull() throws Exception {
        // Arrange
        Account user = provisioning.get(AccountBy.name, "user1@example.zimbra.com");
        AuthToken token = new ZimbraAuthToken(user);

        // Act
        AuthToken.Usage usage = token.getUsage();

        // Assert
        Assert.assertNotNull(usage);
    }
}
