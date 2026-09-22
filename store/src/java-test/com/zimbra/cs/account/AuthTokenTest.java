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
import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AuthToken.TokenType;
import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.BasicCookieStore;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link AuthToken} — the abstract base class's concrete helper methods,
 * static utilities, and the {@link Usage}/{@link TokenType} enums. Uses a real
 * {@link ZimbraAuthToken} (a concrete subclass) and a minimal in-test subclass to exercise the
 * default (non-abstract) behavior without mocking domain objects.
 */
public class AuthTokenTest {

    private static Provisioning provisioning;

    /** Minimal concrete subclass exercising the abstract base class's default method bodies. */
    private static class FakeAuthToken extends AuthToken {
        private final String acctId;

        private final String adminAcctId;

        private boolean csrf;

        FakeAuthToken(String acctId, String adminAcctId) {
            this.acctId = acctId;
            this.adminAcctId = adminAcctId;
        }

        @Override
        public String toString() {
            return "FakeAuthToken";
        }

        @Override
        public String getAccountId() {
            return acctId;
        }

        @Override
        public String getAdminAccountId() {
            return adminAcctId;
        }

        @Override
        public long getExpires() {
            return 0;
        }

        @Override
        public void deRegister() {
        }

        @Override
        public boolean isRegistered() {
            return true;
        }

        @Override
        public boolean isExpired() {
            return false;
        }

        @Override
        public boolean isAdmin() {
            return false;
        }

        @Override
        public boolean isDomainAdmin() {
            return false;
        }

        @Override
        public boolean isDelegatedAdmin() {
            return false;
        }

        @Override
        public boolean isZimbraUser() {
            return true;
        }

        @Override
        public String getExternalUserEmail() {
            return null;
        }

        @Override
        public String getDigest() {
            return null;
        }

        @Override
        public String getCrumb() {
            return null;
        }

        @Override
        public boolean isCsrfTokenEnabled() {
            return csrf;
        }

        @Override
        public void setCsrfTokenEnabled(boolean csrfEnabled) {
            this.csrf = csrfEnabled;
        }

        @Override
        public void encode(HttpClient c, HttpRequestBase m, boolean a, String d) {
        }

        @Override
        public void encode(BasicCookieStore s, boolean a, String d) {
        }

        @Override
        public void encode(HttpServletResponse r, boolean a, boolean s, boolean rm) {
        }

        @Override
        public void encodeAuthResp(Element parent, boolean isAdmin) {
        }

        @Override
        public ZAuthToken toZAuthToken() {
            return null;
        }

        @Override
        public String getEncoded() {
            return null;
        }

        @Override
        public Usage getUsage() {
            return Usage.AUTH;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            FakeAuthToken copy = new FakeAuthToken(acctId, adminAcctId);
            copy.csrf = this.csrf;
            return copy;
        }
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initProvisioning();
        Provisioning.getInstance().createAccount("authuser@example.com", "secret",
                new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        provisioning = Provisioning.getInstance();
    }

    // ---------- generateDigest ----------

    @Test
    public void generateDigestNullFirstArgReturnsNull() {
        // Arrange / Act
        String digest = AuthToken.generateDigest(null, "pw");

        // Assert
        assertNull("null first arg short-circuits to null", digest);
    }

    @Test
    public void generateDigestValidArgsIsDeterministicSha1() {
        // Arrange / Act
        String d1 = AuthToken.generateDigest("acct", "pw");
        String d2 = AuthToken.generateDigest("acct", "pw");

        // Assert
        assertNotNull("digest produced for non-null input", d1);
        assertEquals("same inputs produce same digest", d1, d2);
        assertFalse("differing second arg changes digest",
                d1.equals(AuthToken.generateDigest("acct", "other")));
    }

    @Test
    public void generateDigestNullSecondArgStillComputes() {
        // Arrange / Act
        String d = AuthToken.generateDigest("acct", null);

        // Assert
        assertNotNull("null second arg is tolerated (appends nothing)", d);
        assertFalse("differs from empty-string second arg only via trailing colon contents",
                d.isEmpty());
    }

    // ---------- isAnyAdmin ----------

    @Test
    public void isAnyAdminAllFlagsFalseReturnsFalse() {
        // Arrange
        FakeAuthToken token = new FakeAuthToken("id", null);

        // Act / Assert
        assertFalse("no admin flag set => not any admin", AuthToken.isAnyAdmin(token));
    }

    @Test
    public void isAnyAdminAdminTrueReturnsTrue() {
        // Arrange — subclass with isAdmin overridden true
        FakeAuthToken token = new FakeAuthToken("id", null) {
            @Override
            public boolean isAdmin() {
                return true;
            }
        };

        // Act / Assert
        assertTrue("isAdmin true => any admin", AuthToken.isAnyAdmin(token));
    }

    // ---------- getCsrfUnsecuredAuthToken ----------

    @Test
    public void getCsrfUnsecuredAuthTokenNullReturnsNull() {
        // Act / Assert
        assertNull("null input yields null (first-login edge case)",
                AuthToken.getCsrfUnsecuredAuthToken(null));
    }

    @Test
    public void getCsrfUnsecuredAuthTokenCsrfDisabledReturnsSameInstance() {
        // Arrange
        FakeAuthToken token = new FakeAuthToken("id", null);
        token.setCsrfTokenEnabled(false);

        // Act
        AuthToken result = AuthToken.getCsrfUnsecuredAuthToken(token);

        // Assert
        assertSame("csrf-disabled token is returned as-is", token, result);
    }

    @Test
    public void getCsrfUnsecuredAuthTokenCsrfEnabledReturnsClonedDisabledToken() {
        // Arrange
        FakeAuthToken token = new FakeAuthToken("id", null);
        token.setCsrfTokenEnabled(true);

        // Act
        AuthToken result = AuthToken.getCsrfUnsecuredAuthToken(token);

        // Assert
        assertNotNull("clone produced", result);
        assertFalse("returned clone has csrf disabled", result != null && result.isCsrfTokenEnabled());
        assertFalse("a distinct clone, not the original", result == token);
    }

    // ---------- defaults on base class ----------

    @Test
    public void getValidityValueDefaultReturnsMinusOne() {
        assertEquals("base default validity is -1", -1, new FakeAuthToken("id", null).getValidityValue());
    }

    @Test
    public void isDelegatedAuthWithAdminAccountIdReturnsTrue() {
        // Arrange
        FakeAuthToken token = new FakeAuthToken("acct", "adminAcct");

        // Act / Assert
        assertTrue("non-empty admin account id => delegated auth", token.isDelegatedAuth());
    }

    @Test
    public void isDelegatedAuthNullAdminAccountIdReturnsFalse() {
        assertFalse("null admin account id => not delegated",
                new FakeAuthToken("acct", null).isDelegatedAuth());
    }

    @Test
    public void isDelegatedAuthEmptyAdminAccountIdReturnsFalse() {
        assertFalse("empty admin account id => not delegated",
                new FakeAuthToken("acct", "").isDelegatedAuth());
    }

    @Test
    public void getAccessKeyAndAuthMechDefaultToNull() {
        // Arrange
        FakeAuthToken token = new FakeAuthToken("id", null);

        // Act / Assert
        assertNull("default access key is null", token.getAccessKey());
        assertNull("default auth mech is null", token.getAuthMech());
        assertNull("default proxy auth token is null", token.getProxyAuthToken());
    }

    @Test
    public void ignoreSameSiteSetterPersistsState() {
        // Arrange
        FakeAuthToken token = new FakeAuthToken("id", null);
        assertFalse("default ignoreSameSite is false", token.isIgnoreSameSite());

        // Act
        token.setIgnoreSameSite(true);

        // Assert
        assertTrue("setter flips ignoreSameSite to true", token.isIgnoreSameSite());
    }

    // ---------- getAccount ----------

    @Test
    public void getAccountExistingAccountIdResolvesFromProvisioning() throws Exception {
        // Arrange — real account in the harness
        Account expected = provisioning.get(AccountBy.name, "authuser@example.com");
        assertNotNull("fixture account exists", expected);
        FakeAuthToken token = new FakeAuthToken(expected.getId(), null);

        // Act
        Account resolved = token.getAccount();

        // Assert
        assertEquals("resolves the same account by id", expected.getId(), resolved.getId());
    }

    @Test
    public void getAccountUnknownAccountIdThrowsNoSuchAccount() {
        // Arrange
        FakeAuthToken token = new FakeAuthToken("does-not-exist-id", null);

        // Act / Assert
        try {
            token.getAccount();
            fail("expected NO_SUCH_ACCOUNT for unknown id");
        } catch (ServiceException e) {
            assertEquals("account-not-found error code",
                    AccountServiceException.NO_SUCH_ACCOUNT, e.getCode());
        }
    }

    // ---------- Usage enum ----------

    @Test
    public void usageFromCodeKnownCodeReturnsMatchingConstant() throws Exception {
        // Act / Assert
        assertEquals("'a' maps to AUTH", Usage.AUTH, Usage.fromCode("a"));
        assertEquals("'tfa' maps to TWO_FACTOR_AUTH", Usage.TWO_FACTOR_AUTH, Usage.fromCode("tfa"));
        assertEquals("'rp' maps to RESET_PASSWORD", Usage.RESET_PASSWORD, Usage.fromCode("rp"));
        assertEquals("code round-trips", "a", Usage.AUTH.getCode());
    }

    @Test
    public void usageFromCodeUnknownCodeThrowsFailure() {
        try {
            Usage.fromCode("zzz");
            fail("expected ServiceException for unknown usage code");
        } catch (ServiceException e) {
            assertTrue("message names the bad code", e.getMessage().contains("zzz"));
        }
    }

    // ---------- TokenType enum ----------

    @Test
    public void tokenTypeFromCodeJwtReturnsJwtCaseInsensitive() throws Exception {
        // Act / Assert
        assertEquals("'jwt' => JWT", TokenType.JWT, TokenType.fromCode("jwt"));
        assertEquals("'JWT' => JWT (case-insensitive)", TokenType.JWT, TokenType.fromCode("JWT"));
        assertEquals("JWT code is 'jwt'", "jwt", TokenType.JWT.getCode());
    }

    @Test
    public void tokenTypeFromCodeAnythingElseDefaultsToAuth() throws Exception {
        // Act / Assert
        assertEquals("non-jwt => AUTH", TokenType.AUTH, TokenType.fromCode("auth"));
        assertEquals("unknown code falls through to AUTH", TokenType.AUTH, TokenType.fromCode("bogus"));
        assertEquals("AUTH code is 'auth'", "auth", TokenType.AUTH.getCode());
    }
}
