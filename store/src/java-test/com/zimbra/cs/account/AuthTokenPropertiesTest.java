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

import com.zimbra.cs.account.AuthToken.Usage;
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link AuthTokenProperties} — exercised with real {@link Account} and
 * {@link GuestAccount} domain objects from the in-memory MockProvisioning harness, plus the
 * encoded-map decoding constructor. Covers the Zimbra-user, external/guest, and map-decode paths
 * as well as the value-parsing edge cases (validity value, token id, csrf, usage).
 */
public class AuthTokenPropertiesTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
    }

    private Account createAccount(String email, Map<String, Object> attrs) throws Exception {
        return prov.createAccount(email, "secret", attrs);
    }

    @Test
    public void ctorFromAccountNonAdminZimbraUserSetsZimbraTypeAndAccountId() throws Exception {
        // Arrange
        Account acct = createAccount("user1@zimbra.com", new HashMap<String, Object>());

        // Act
        AuthTokenProperties props = new AuthTokenProperties(acct, false, null, 1000L,
                AuthMech.zimbra, Usage.AUTH);

        // Assert
        assertEquals(acct.getId(), props.getAccountId());
        assertEquals(AuthTokenProperties.C_TYPE_ZIMBRA_USER, props.getType());
        assertFalse("non-admin flag passed in", props.isAdmin());
        assertFalse(props.isDomainAdmin());
        assertFalse(props.isDelegatedAdmin());
        assertEquals(1000L, props.getExpires());
        assertEquals(AuthMech.zimbra, props.getAuthMech());
        assertEquals(Usage.AUTH, props.getUsage());
        assertNull("adminAccount null -> adminAccountId null", props.getAdminAccountId());
        assertTrue("tokenID should be a positive random int", props.getTokenID() > 0);
    }

    @Test
    public void ctorFromAccountAdminAccountAndAdminFlagMarksAdmin() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account acct = createAccount("admin1@zimbra.com", attrs);
        Account adminAcct = createAccount("delegator@zimbra.com", new HashMap<String, Object>());

        // Act
        AuthTokenProperties props = new AuthTokenProperties(acct, true, adminAcct, 2000L,
                AuthMech.zimbra, Usage.AUTH);

        // Assert
        assertTrue("isAdmin should be true when flag set and attr TRUE", props.isAdmin());
        assertFalse("not a domain admin", props.isDomainAdmin());
        assertEquals(adminAcct.getId(), props.getAdminAccountId());
    }

    @Test
    public void ctorFromAccountAdminFlagButAttrFalseNotAdmin() throws Exception {
        // Arrange — admin flag passed but the account is not actually an admin account
        Account acct = createAccount("notadmin@zimbra.com", new HashMap<String, Object>());

        // Act
        AuthTokenProperties props = new AuthTokenProperties(acct, true, null, 500L,
                AuthMech.zimbra, Usage.AUTH);

        // Assert
        assertFalse("attr not TRUE => isAdmin false even with flag", props.isAdmin());
    }

    @Test
    public void ctorFromAccountDomainAdminAttrMarksDomainAdmin() throws Exception {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        Account acct = createAccount("domadmin@zimbra.com", attrs);

        // Act
        AuthTokenProperties props = new AuthTokenProperties(acct, true, null, 500L,
                AuthMech.zimbra, Usage.AUTH);

        // Assert
        assertTrue("domain admin attr TRUE + flag => domain admin", props.isDomainAdmin());
        assertFalse(props.isDelegatedAdmin());
    }

    @Test
    public void ctorFromAccountGuestAccountSetsExternalTypeAndDigest() throws Exception {
        // Arrange — a GuestAccount is the external-user branch
        GuestAccount guest = new GuestAccount("guest@external.com", "guestpw");

        // Act
        AuthTokenProperties props = new AuthTokenProperties(guest, false, null, 3000L,
                AuthMech.zimbra, Usage.AUTH);

        // Assert
        assertEquals(AuthTokenProperties.C_TYPE_EXTERNAL_USER, props.getType());
        assertEquals("guest@external.com", props.getExternalUserEmail());
        assertNotNull("guest digest should be carried over", props.getDigest());
        assertEquals(guest.getDigest(), props.getDigest());
    }

    @Test
    public void ctorFromAccountNullAccountLeavesAccountFieldsUnset() throws Exception {
        // Act — null account path skips the whole account block
        AuthTokenProperties props = new AuthTokenProperties(null, false, null, 9000L,
                AuthMech.zimbra, Usage.AUTH);

        // Assert
        assertNull("accountId stays null when acct null", props.getAccountId());
        assertNull("type stays null when acct null", props.getType());
        assertEquals(9000L, props.getExpires());
    }

    @Test
    public void zmgAppCtorNullExternalEmailDefaultsToPublic() throws Exception {
        // Act — legacy zmgApp signature with null external email
        AuthTokenProperties props = new AuthTokenProperties("acct-id-1", false, null, "pw",
                "digestval", 4000L);

        // Assert
        assertEquals("acct-id-1", props.getAccountId());
        assertEquals("public", props.getExternalUserEmail());
        assertEquals("digestval", props.getDigest());
        assertEquals(AuthTokenProperties.C_TYPE_EXTERNAL_USER, props.getType());
        assertEquals(4000L, props.getExpires());
    }

    @Test
    public void zmgAppCtorNullDigestGeneratesDigest() throws Exception {
        // Act — null digest must be generated from email+pass
        AuthTokenProperties props = new AuthTokenProperties("acct-id-2", false, "ext@x.com", "pw",
                null, 4000L);

        // Assert
        assertEquals("ext@x.com", props.getExternalUserEmail());
        assertNotNull("digest must be generated when null", props.getDigest());
    }

    @Test
    public void mapCtorFullMapDecodesAllFields() throws Exception {
        // Arrange
        Map<String, String> map = new HashMap<String, String>();
        map.put(AuthTokenProperties.C_ID, "id-100");
        map.put(AuthTokenProperties.C_AID, "aid-200");
        map.put(AuthTokenProperties.C_EXP, "123456");
        map.put(AuthTokenProperties.C_ADMIN, "1");
        map.put(AuthTokenProperties.C_DOMAIN, "1");
        map.put(AuthTokenProperties.C_DLGADMIN, "1");
        map.put(AuthTokenProperties.C_TYPE, AuthTokenProperties.C_TYPE_ZIMBRA_USER);
        map.put(AuthTokenProperties.C_AUTH_MECH, AuthMech.zimbra.name());
        map.put(AuthTokenProperties.C_USAGE, Usage.TWO_FACTOR_AUTH.getCode());
        map.put(AuthTokenProperties.C_EXTERNAL_USER_EMAIL, "ext@x.com");
        map.put(AuthTokenProperties.C_DIGEST, "dig");
        map.put(AuthTokenProperties.C_VALIDITY_VALUE, "7");
        map.put(AuthTokenProperties.C_TOKEN_ID, "42");
        map.put(AuthTokenProperties.C_SERVER_VERSION, "10.1");
        map.put(AuthTokenProperties.C_CSRF, "1");

        // Act
        AuthTokenProperties props = new AuthTokenProperties(map);

        // Assert
        assertEquals("id-100", props.getAccountId());
        assertEquals("aid-200", props.getAdminAccountId());
        assertEquals(123456L, props.getExpires());
        assertTrue(props.isAdmin());
        assertTrue(props.isDomainAdmin());
        assertTrue(props.isDelegatedAdmin());
        assertEquals(AuthMech.zimbra, props.getAuthMech());
        assertEquals(Usage.TWO_FACTOR_AUTH, props.getUsage());
        assertEquals("ext@x.com", props.getExternalUserEmail());
        assertEquals("dig", props.getDigest());
        assertEquals(7, props.getValidityValue());
        assertEquals(Integer.valueOf(42), props.getTokenID());
        assertEquals("10.1", props.getServerVersion());
        assertTrue(props.isCsrfTokenEnabled());
    }

    @Test
    public void mapCtorMissingUsageDefaultsToAuth() throws Exception {
        // Arrange — no usage code present
        Map<String, String> map = new HashMap<String, String>();
        map.put(AuthTokenProperties.C_ID, "id-1");
        map.put(AuthTokenProperties.C_EXP, "1");
        map.put(AuthTokenProperties.C_ADMIN, "0");

        // Act
        AuthTokenProperties props = new AuthTokenProperties(map);

        // Assert
        assertEquals("missing usage defaults to AUTH", Usage.AUTH, props.getUsage());
        assertFalse("admin flag 0 => not admin", props.isAdmin());
    }

    @Test
    public void mapCtorInvalidValidityAndTokenIdFallBackToMinusOne() throws Exception {
        // Arrange — non-numeric vv and tid should be caught and reset to -1
        Map<String, String> map = new HashMap<String, String>();
        map.put(AuthTokenProperties.C_ID, "id-2");
        map.put(AuthTokenProperties.C_EXP, "10");
        map.put(AuthTokenProperties.C_VALIDITY_VALUE, "notanumber");
        map.put(AuthTokenProperties.C_TOKEN_ID, "alsobad");

        // Act
        AuthTokenProperties props = new AuthTokenProperties(map);

        // Assert
        assertEquals("invalid validity value => -1", -1, props.getValidityValue());
        assertEquals("invalid token id => -1", Integer.valueOf(-1), props.getTokenID());
    }

    @Test
    public void mapCtorInvalidAuthMechThrowsAuthTokenException() throws Exception {
        // Arrange
        Map<String, String> map = new HashMap<String, String>();
        map.put(AuthTokenProperties.C_ID, "id-3");
        map.put(AuthTokenProperties.C_EXP, "10");
        map.put(AuthTokenProperties.C_AUTH_MECH, "bogusmech");

        // Act / Assert
        try {
            new AuthTokenProperties(map);
            fail("expected AuthTokenException for bad auth mech");
        } catch (AuthTokenException e) {
            assertTrue("message should mention service exception",
                    e.getMessage().toLowerCase().contains("service"));
        }
    }

    @Test
    public void settersEncodedProxyAndCsrfRoundTripAndClonePreservesState() throws Exception {
        // Arrange
        Account acct = createAccount("setter@zimbra.com", new HashMap<String, Object>());
        AuthTokenProperties props = new AuthTokenProperties(acct, false, null, 1L,
                AuthMech.zimbra, Usage.AUTH);

        // Act
        props.setEncoded("enc-string");
        props.setProxyAuthToken("proxy-tok");
        props.setCsrfTokenEnabled(true);
        props.setTokenID(99);
        AuthTokenProperties clone = props.clone();

        // Assert — setters persist and clone copies them
        assertEquals("enc-string", props.getEncoded());
        assertEquals("proxy-tok", props.getProxyAuthToken());
        assertTrue(props.isCsrfTokenEnabled());
        assertEquals(Integer.valueOf(99), props.getTokenID());
        assertEquals("clone preserves encoded", "enc-string", clone.getEncoded());
        assertEquals("clone preserves proxy", "proxy-tok", clone.getProxyAuthToken());
        assertEquals("clone preserves account id", acct.getId(), clone.getAccountId());
    }
}
