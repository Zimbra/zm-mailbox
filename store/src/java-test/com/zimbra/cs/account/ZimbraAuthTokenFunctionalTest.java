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
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
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
 * Functional tests for {@link ZimbraAuthToken} — token creation from a real Account via the
 * in-memory harness, encode/decode round-trips, the decode cache, CSRF re-encoding, cloning
 * with a fresh token id, and malformed-token error handling.
 */
public class ZimbraAuthTokenFunctionalTest {

    @BeforeClass
    public static void initOnce() throws Exception {
        MailboxTestUtil.initProvisioning();
    }

    @Before
    public void setUp() throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        Provisioning.getInstance().createAccount("atuser@example.com", "secret", attrs);
    }

    private Account account() throws Exception {
        return Provisioning.getInstance().get(AccountBy.name, "atuser@example.com");
    }

    @Test
    public void constructorFromAccountPopulatesAccountIdAndFutureExpiry() throws Exception {
        // Arrange
        Account a = account();

        // Act
        ZimbraAuthToken at = new ZimbraAuthToken(a);

        // Assert
        assertEquals(a.getId(), at.getAccountId());
        assertFalse("a freshly minted token must not be expired", at.isExpired());
        assertTrue("expiry must be in the future", at.getExpires() > System.currentTimeMillis());
        assertTrue("account token is a zimbra-user token", at.isZimbraUser());
        assertFalse(at.isAdmin());
    }

    @Test
    public void constructorAdminFlagSetsIsAdminTrue() throws Exception {
        // Arrange — the isAdmin token flag is only honored when the account is itself an
        // admin account (AuthTokenProperties ANDs the flag with zimbraIsAdminAccount=TRUE).
        Account a = account();
        Map<String, Object> changes = new HashMap<String, Object>();
        changes.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Provisioning.getInstance().modifyAttrs(a, changes);

        // Act
        ZimbraAuthToken at = new ZimbraAuthToken(a, true, null);

        // Assert
        assertTrue(at.isAdmin());
        assertEquals(a.getId(), at.getAccountId());
    }

    @Test
    public void getEncodedThenDecodeRoundTripsAccountIdAndExpiry() throws Exception {
        // Arrange
        Account a = account();
        ZimbraAuthToken original = new ZimbraAuthToken(a);

        // Act
        String encoded = original.getEncoded();
        ZimbraAuthToken decoded = (ZimbraAuthToken) ZimbraAuthToken.getAuthToken(encoded);

        // Assert
        assertNotNull(encoded);
        assertEquals(3, encoded.split("_").length);
        assertEquals(original.getAccountId(), decoded.getAccountId());
        assertEquals(original.getExpires(), decoded.getExpires());
    }

    @Test
    public void getEncodedCalledTwiceIsStable() throws Exception {
        // Arrange
        ZimbraAuthToken at = new ZimbraAuthToken(account());

        // Act
        String first = at.getEncoded();
        String second = at.getEncoded();

        // Assert — encoded value is cached and identical
        assertEquals(first, second);
    }

    @Test
    public void getAuthTokenSameEncodedTwiceReturnsCachedInstance() throws Exception {
        // Arrange
        String encoded = new ZimbraAuthToken(account()).getEncoded();

        // Act
        com.zimbra.cs.account.AuthToken first = ZimbraAuthToken.getAuthToken(encoded);
        com.zimbra.cs.account.AuthToken second = ZimbraAuthToken.getAuthToken(encoded);

        // Assert — non-expired tokens are cached, so the same instance comes back
        assertSame(first, second);
    }

    @Test
    public void getInfoValidEncodedReturnsNonEmptyAttrMap() throws Exception {
        // Arrange
        String encoded = new ZimbraAuthToken(account()).getEncoded();

        // Act
        Map<?, ?> info = ZimbraAuthToken.getInfo(encoded);

        // Assert
        assertNotNull(info);
        assertFalse(info.isEmpty());
    }

    @Test
    public void getInfoWrongPartCountThrowsAuthTokenException() {
        // Act / Assert
        try {
            ZimbraAuthToken.getInfo("onlyonepart");
            fail("expected AuthTokenException for malformed token");
        } catch (AuthTokenException e) {
            assertTrue(e.getMessage().contains("invalid authtoken format"));
        }
    }

    @Test
    public void decodeConstructorNoUnderscoreThrowsAuthTokenException() {
        // Act / Assert
        try {
            new ZimbraAuthToken("nounderscorehere");
            fail("expected AuthTokenException for missing version delimiter");
        } catch (AuthTokenException e) {
            assertTrue(e.getMessage().contains("invalid authtoken format"));
        }
    }

    @Test
    public void decodeConstructorTamperedHmacThrowsAuthTokenException() throws Exception {
        // Arrange — take a valid token and corrupt its hmac segment
        String encoded = new ZimbraAuthToken(account()).getEncoded();
        String[] parts = encoded.split("_", 3);
        String tampered = parts[0] + "_" + "deadbeef" + "_" + parts[2];

        // Act / Assert
        try {
            new ZimbraAuthToken(tampered);
            fail("expected hmac failure");
        } catch (AuthTokenException e) {
            assertTrue(e.getMessage().contains("hmac")
                    || e.getMessage().contains("unknown key"));
        }
    }

    @Test
    public void cloneThenResetTokenIdChangesEncodedValue() throws Exception {
        // Arrange
        ZimbraAuthToken at = new ZimbraAuthToken(account());
        String originalEncoded = at.getEncoded();

        // Act
        ZimbraAuthToken cloned = at.clone();
        cloned.resetTokenId();

        // Assert — same account, but a different token id yields a different encoding
        assertEquals(at.getAccountId(), cloned.getAccountId());
        assertFalse(originalEncoded.equals(cloned.getEncoded()));
    }

    @Test
    public void setCsrfTokenEnabledTrueForcesReEncodeWithCsrfFlag() throws Exception {
        // Arrange
        ZimbraAuthToken at = new ZimbraAuthToken(account());
        String beforeEncoded = at.getEncoded();
        assertFalse(at.isCsrfTokenEnabled());

        // Act
        at.setCsrfTokenEnabled(true);
        String afterEncoded = at.getEncoded();

        // Assert
        assertTrue(at.isCsrfTokenEnabled());
        assertFalse("enabling CSRF must force a fresh encoding",
                beforeEncoded.equals(afterEncoded));
    }

    @Test
    public void getCrumbValidTokenReturnsHexDigest() throws Exception {
        // Arrange
        ZimbraAuthToken at = new ZimbraAuthToken(account());

        // Act
        String crumb = at.getCrumb();

        // Assert
        assertNotNull(crumb);
        assertTrue("crumb should be a non-empty hex string", crumb.length() > 0);
        assertTrue(crumb.matches("[0-9a-fA-F]+"));
    }

    @Test
    public void proxyAuthTokenSetGetResetRoundTrips() throws Exception {
        // Arrange
        ZimbraAuthToken at = new ZimbraAuthToken(account());
        assertNull(at.getProxyAuthToken());

        // Act
        at.setProxyAuthToken("proxy-value");

        // Assert
        assertEquals("proxy-value", at.getProxyAuthToken());

        // Act — reset
        at.resetProxyAuthToken();

        // Assert
        assertNull(at.getProxyAuthToken());
    }

    @Test
    public void externalAccountConstructorSetsExternalEmailAndNotZimbraUser() throws Exception {
        // Arrange / Act
        ZimbraAuthToken at = new ZimbraAuthToken("acctid123", "ext@partner.com",
                "pass", "digest", System.currentTimeMillis() + 60000L);

        // Assert
        assertEquals("acctid123", at.getAccountId());
        assertEquals("ext@partner.com", at.getExternalUserEmail());
        assertFalse("external token is not a zimbra-user token", at.isZimbraUser());
    }

    @Test
    public void toStringIncludesAccountId() throws Exception {
        // Arrange
        ZimbraAuthToken at = new ZimbraAuthToken(account());

        // Act
        String s = at.toString();

        // Assert
        assertTrue(s.contains(at.getAccountId()));
    }
}
