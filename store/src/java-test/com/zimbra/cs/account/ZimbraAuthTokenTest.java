/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
import com.zimbra.cs.account.auth.AuthMechanism.AuthMech;
import com.zimbra.cs.ephemeral.EphemeralStore;
import com.zimbra.cs.ephemeral.InMemoryEphemeralStore;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit test for {@link ZimbraAuthToken}.
 *
 * @author ysasaki
 */
public class ZimbraAuthTokenTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
        Provisioning.getInstance().createAccount("user1@example.zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Test
    public void test() throws Exception {
        Account a = Provisioning.getInstance().get(AccountBy.name, "user1@example.zimbra.com");
        ZimbraAuthToken at = new ZimbraAuthToken(a);
        long start = System.currentTimeMillis();
        String encoded = at.getEncoded();
        for (int i = 0; i < 1000; i++) {
            new ZimbraAuthToken(encoded);
        }
        System.out.println("Encoded 1000 auth-tokens elapsed=" + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            ZimbraAuthToken.getAuthToken(encoded);
        }
        System.out.println("Decoded 1000 auth-tokens elapsed=" + (System.currentTimeMillis() - start));
    }
    
    @Test
    public void testEncodedDifferentOnTokenIDReset() throws Exception {
        Account a = Provisioning.getInstance().get(AccountBy.name, "user1@example.zimbra.com");
        ZimbraAuthToken at = new ZimbraAuthToken(a);
        ZimbraAuthToken clonedAuthToken = at.clone();
        clonedAuthToken.resetTokenId();
        Assert.assertFalse(at.getEncoded().equals(clonedAuthToken.getEncoded()));
    }

    private Account account() throws Exception {
        return Provisioning.getInstance().get(AccountBy.name, "user1@example.zimbra.com");
    }

    // ---------- encode/decode round trips ----------

    @Test
    public void encodeThenDecodeRoundTripsAccountId() throws Exception {
        // Arrange
        Account a = account();
        ZimbraAuthToken at = new ZimbraAuthToken(a);

        // Act — encode, then reconstruct from the encoded string
        String encoded = at.getEncoded();
        ZimbraAuthToken decoded = new ZimbraAuthToken(encoded);

        // Assert — the decoded token carries the same account id and a valid (future) expiry
        Assert.assertEquals(a.getId(), decoded.getAccountId());
        Assert.assertFalse("freshly issued token must not be expired", decoded.isExpired());
        Assert.assertTrue(decoded.isZimbraUser());
    }

    @Test
    public void getInfoValidEncodedReturnsAttrs() throws Exception {
        // Arrange
        Account a = account();
        String encoded = new ZimbraAuthToken(a).getEncoded();

        // Act
        Map<?, ?> info = ZimbraAuthToken.getInfo(encoded);

        // Assert — the metadata map exposes the account id
        Assert.assertNotNull(info);
        Assert.assertEquals(a.getId(), info.get(AuthTokenProperties.C_ID));
    }

    @Test
    public void getInfoMalformedEncodedThrowsAuthTokenException() throws Exception {
        // Act / Assert — fewer than 3 underscore-separated parts is invalid
        try {
            ZimbraAuthToken.getInfo("only_two");
            Assert.fail("expected AuthTokenException for a malformed token");
        } catch (AuthTokenException e) {
            Assert.assertTrue(e.getMessage().toLowerCase().contains("format"));
        }
    }

    @Test
    public void decodeConstructorNoUnderscoreThrowsAuthTokenException() throws Exception {
        // Act / Assert — completely malformed token (no separator)
        try {
            new ZimbraAuthToken("garbage");
            Assert.fail("expected AuthTokenException for a token with no separator");
        } catch (AuthTokenException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    @Test
    public void decodeConstructorUnknownKeyVersionThrowsAuthTokenException() throws Exception {
        // Act / Assert — three parts but a bogus key version
        try {
            new ZimbraAuthToken("999_deadbeef_cafe");
            Assert.fail("expected AuthTokenException for an unknown key version");
        } catch (AuthTokenException e) {
            Assert.assertNotNull(e.getMessage());
        }
    }

    // ---------- admin token encoding ----------

    @Test
    public void adminTokenEncodeSetsAdminFlagsAndAuthMech() throws Exception {
        // Arrange — the admin flag is honored only when the account is actually an admin
        // (AuthTokenProperties gates it on zimbraIsAdminAccount=TRUE), so create such an account.
        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account admin = Provisioning.getInstance().createAccount("admin1@example.zimbra.com", "secret", adminAttrs);
        ZimbraAuthToken at = new ZimbraAuthToken(admin, true, AuthMech.zimbra);

        // Act — encode (exercises the admin + auth-mech encoding branches), then decode
        String encoded = at.getEncoded();
        ZimbraAuthToken decoded = new ZimbraAuthToken(encoded);

        // Assert
        Assert.assertTrue("admin flag must round-trip", at.isAdmin());
        Assert.assertTrue(decoded.isAdmin());
        Assert.assertEquals(AuthMech.zimbra, decoded.getAuthMech());
    }

    // ---------- constructors ----------

    @Test
    public void constructorWithExpiresSetsExpiry() throws Exception {
        // Arrange — explicit future expiry skips the lifetime computation
        Account a = account();
        long expires = System.currentTimeMillis() + 60000L;

        // Act
        ZimbraAuthToken at = new ZimbraAuthToken(a, expires);

        // Assert
        Assert.assertEquals(expires, at.getExpires());
        Assert.assertFalse(at.isExpired());
    }

    @Test
    public void constructorWithUsageTwoFactorComputesLifetime() throws Exception {
        // Arrange / Act — usage-based lifetime branch (expires == 0)
        Account a = account();
        ZimbraAuthToken at = new ZimbraAuthToken(a, Usage.TWO_FACTOR_AUTH);

        // Assert — a positive lifetime was derived and the usage preserved
        Assert.assertTrue("computed expiry must be in the future", at.getExpires() > System.currentTimeMillis());
        Assert.assertEquals(Usage.TWO_FACTOR_AUTH, at.getUsage());
    }

    @Test
    public void externalAccountConstructorExposesExternalEmailAndDigest() throws Exception {
        // Arrange — external (non-zimbra) token built from raw fields
        long expires = System.currentTimeMillis() + 60000L;

        // Act
        ZimbraAuthToken at = new ZimbraAuthToken("acctId123", "ext@other.com", "pass", "digestVal", expires);

        // Assert
        Assert.assertEquals("ext@other.com", at.getExternalUserEmail());
        Assert.assertEquals("digestVal", at.getDigest());
        Assert.assertEquals(expires, at.getExpires());
        Assert.assertFalse("external user is not a zimbra user", at.isZimbraUser());
    }

    // ---------- getters / flags ----------

    @Test
    public void freshTokenFlagDefaultsAreFalse() throws Exception {
        // Arrange
        ZimbraAuthToken at = new ZimbraAuthToken(account());

        // Act / Assert — a plain user token has no admin/delegated/domain-admin flags
        Assert.assertFalse(at.isAdmin());
        Assert.assertFalse(at.isDelegatedAdmin());
        Assert.assertFalse(at.isDomainAdmin());
        Assert.assertNull(at.getAdminAccountId());
        Assert.assertNull(at.getAccessKey());
        Assert.assertEquals(Usage.AUTH, at.getUsage());
    }

    @Test
    public void getValidityValueMatchesAccount() throws Exception {
        // Arrange
        Account a = account();
        ZimbraAuthToken at = new ZimbraAuthToken(a);

        // Act / Assert — token validity value is seeded from the account
        Assert.assertEquals(a.getAuthTokenValidityValue(), at.getValidityValue());
    }

    @Test
    public void toStringIncludesAccountId() throws Exception {
        // Arrange
        Account a = account();
        ZimbraAuthToken at = new ZimbraAuthToken(a);

        // Act
        String s = at.toString();

        // Assert
        Assert.assertTrue(s.contains(a.getId()));
    }

    // ---------- crumb ----------

    @Test
    public void getCrumbIsDeterministicHexDigest() throws Exception {
        // Arrange
        ZimbraAuthToken at = new ZimbraAuthToken(account());

        // Act
        String crumb1 = at.getCrumb();
        String crumb2 = at.getCrumb();

        // Assert — a stable, non-empty hex HMAC over the encoded token
        Assert.assertNotNull(crumb1);
        Assert.assertFalse(crumb1.isEmpty());
        Assert.assertEquals("crumb must be deterministic for the same token", crumb1, crumb2);
        Assert.assertTrue("crumb must be lowercase hex", crumb1.matches("[0-9a-f]+"));
    }

    // ---------- registration ----------

    @Test
    public void isRegisteredLowAuthVersionReturnsTrue() throws Exception {
        // Arrange — local server advertises auth version 1, so registration is implicit
        ZimbraAuthToken at = new ZimbraAuthToken(account());

        // Act / Assert
        Assert.assertTrue(at.isRegistered());
    }

    @Test
    public void deRegisterFreshTokenNoException() throws Exception {
        // Arrange
        ZimbraAuthToken at = new ZimbraAuthToken(account());

        // Act — de-registering a token for an existing account must not throw
        at.deRegister();

        // Assert — still encodable afterward
        Assert.assertNotNull(at.getEncoded());
    }

    // ---------- CSRF / encoded caching ----------

    @Test
    public void setCsrfTokenEnabledTrueForcesReEncodeAndFlag() throws Exception {
        // Arrange
        ZimbraAuthToken at = new ZimbraAuthToken(account());
        String before = at.getEncoded();
        Assert.assertFalse(at.isCsrfTokenEnabled());

        // Act — enabling CSRF clears the cached encoding and flips the flag
        at.setCsrfTokenEnabled(true);

        // Assert
        Assert.assertTrue(at.isCsrfTokenEnabled());
        String after = at.getEncoded();
        Assert.assertFalse("enabling CSRF must change the encoded token", before.equals(after));
    }

    @Test
    public void setCsrfTokenEnabledSameValueIsNoOp() throws Exception {
        // Arrange
        ZimbraAuthToken at = new ZimbraAuthToken(account());
        String before = at.getEncoded();

        // Act — setting to the current (false) value leaves the encoding intact
        at.setCsrfTokenEnabled(false);

        // Assert
        Assert.assertFalse(at.isCsrfTokenEnabled());
        Assert.assertEquals(before, at.getEncoded());
    }

    // ---------- proxy token ----------

    @Test
    public void proxyAuthTokenSetGetResetRoundTrips() throws Exception {
        // Arrange
        ZimbraAuthToken at = new ZimbraAuthToken(account());

        // Act / Assert — set, read back, then reset
        at.setProxyAuthToken("proxy-encoded-value");
        Assert.assertEquals("proxy-encoded-value", at.getProxyAuthToken());
        at.resetProxyAuthToken();
        Assert.assertNull(at.getProxyAuthToken());
    }

    // ---------- properties accessors ----------

    @Test
    public void getSetPropertiesReplacesUnderlyingProperties() throws Exception {
        // Arrange
        ZimbraAuthToken at = new ZimbraAuthToken(account());
        AuthTokenProperties original = at.getProperties();
        Assert.assertNotNull(original);

        // Act — swap in a clone and confirm the accessor reflects it
        AuthTokenProperties clone = original.clone();
        at.setProperties(clone);

        // Assert
        Assert.assertSame(clone, at.getProperties());
    }

    // ---------- token id reset ----------

    @Test
    public void resetTokenIdChangesTokenIdAndEncoding() throws Exception {
        // Arrange
        ZimbraAuthToken at = new ZimbraAuthToken(account());
        String before = at.getEncoded();
        Integer idBefore = at.getProperties().getTokenID();

        // Act
        at.resetTokenId();

        // Assert — both the token id and the cached encoding change
        Assert.assertFalse("token id must change", idBefore.equals(at.getProperties().getTokenID()));
        Assert.assertFalse(before.equals(at.getEncoded()));
    }

    // ============================================================================================
    // Lifetime selection (L195): isAdmin chooses the admin lifetime, else the user lifetime.
    // ============================================================================================

    @Test
    public void constructorExpiresZeroAdminVsUserSelectsDistinctLifetime() throws Exception {
        // Give the account very different admin vs user token lifetimes, then build two tokens with
        // expires==0 (forcing the lifetime computation) — one with isAdmin=true, one false. The
        // admin token must expire roughly a day later than the user token. If L195's conditional
        // were negated the two windows would swap, which the ordering assertion below catches.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        attrs.put(Provisioning.A_zimbraAuthTokenLifetime, "1h");
        attrs.put(Provisioning.A_zimbraAdminAuthTokenLifetime, "2d");
        Account a = Provisioning.getInstance().createAccount("lifetime@example.zimbra.com", "secret", attrs);

        long before = System.currentTimeMillis();
        ZimbraAuthToken userTok = new ZimbraAuthToken(a, 0, false, null, null);
        ZimbraAuthToken adminTok = new ZimbraAuthToken(a, 0, true, null, null);
        long after = System.currentTimeMillis();

        long userLifetime = userTok.getExpires() - before;
        long adminLifetime = adminTok.getExpires() - after;

        // user lifetime ~ 1h, admin lifetime ~ 2d: admin must be far larger.
        Assert.assertTrue("user token ~1h", userLifetime <= 2L * 60 * 60 * 1000);
        Assert.assertTrue("admin token ~2d", adminLifetime >= 36L * 60 * 60 * 1000);
        Assert.assertTrue("admin lifetime must exceed user lifetime",
                adminTok.getExpires() > userTok.getExpires());

        Provisioning.getInstance().deleteAccount(a.getId());
    }

    // ============================================================================================
    // isExpired boundary (L237): now > expires.
    // ============================================================================================

    @Test
    public void isExpiredPastExpiryTrueFutureExpiryFalse() throws Exception {
        Account a = account();
        // Expiry well in the past -> expired.
        ZimbraAuthToken past = new ZimbraAuthToken(a, System.currentTimeMillis() - 60000L);
        Assert.assertTrue("token with past expiry must be expired", past.isExpired());
        // Expiry well in the future -> not expired.
        ZimbraAuthToken future = new ZimbraAuthToken(a, System.currentTimeMillis() + 600000L);
        Assert.assertFalse("token with future expiry must not be expired", future.isExpired());
    }

    // ============================================================================================
    // Decode constructor caches the original encoded string (L146): a decoded token must report the
    // exact bytes it was decoded from, never re-encode them.
    // ============================================================================================

    @Test
    public void decodeConstructorGetEncodedReturnsOriginalString() throws Exception {
        Account a = account();
        String encoded = new ZimbraAuthToken(a).getEncoded();

        ZimbraAuthToken decoded = new ZimbraAuthToken(encoded);

        // If setEncoded(encoded) were dropped (L146), getEncoded() would re-derive a fresh string;
        // here it must echo the exact input.
        Assert.assertEquals(encoded, decoded.getEncoded());
    }

    // ============================================================================================
    // clone() deep-copies properties (L582): a clone must NOT share the properties object, so
    // resetTokenId() on the clone leaves the original's token id untouched.
    // ============================================================================================

    @Test
    public void cloneHasIndependentPropertiesResetDoesNotAffectOriginal() throws Exception {
        ZimbraAuthToken original = new ZimbraAuthToken(account());
        Integer originalId = original.getProperties().getTokenID();

        ZimbraAuthToken cloned = original.clone();
        // Distinct properties instance (kills the dropped setProperties(clone) at L582).
        Assert.assertNotSame("clone must hold its own properties object",
                original.getProperties(), cloned.getProperties());

        cloned.resetTokenId();

        // Mutating the clone must not change the original's token id.
        Assert.assertEquals("original token id unchanged by clone mutation",
                originalId, original.getProperties().getTokenID());
        Assert.assertFalse("clone token id changed",
                originalId.equals(cloned.getProperties().getTokenID()));
    }

    // ============================================================================================
    // getEncoded() field-by-field encoding (L322-L350): build a fully-loaded admin token and assert
    // every optional metadata field is present (and correct) in the decoded attr map. A dropped
    // encodeMetaData call or a negated guard removes the corresponding key.
    // ============================================================================================

    @Test
    public void getEncodedFullyLoadedAdminTokenEncodesEveryField() throws Exception {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        attrs.put(Provisioning.A_zimbraIsDomainAdminAccount, "TRUE");
        attrs.put(Provisioning.A_zimbraIsDelegatedAdminAccount, "TRUE");
        Account acct = Provisioning.getInstance().createAccount("fulladmin@example.zimbra.com", "secret", attrs);

        Map<String, Object> adminAttrs = new HashMap<String, Object>();
        adminAttrs.put(Provisioning.A_zimbraIsAdminAccount, "TRUE");
        Account adminActing = Provisioning.getInstance().createAccount(
                "acting@example.zimbra.com", "secret", adminAttrs);

        // expires explicit + isAdmin + adminAcct + authMech + AUTH usage.
        long expires = System.currentTimeMillis() + 3600000L;
        ZimbraAuthToken at = new ZimbraAuthToken(acct, expires, true, adminActing, AuthMech.zimbra, Usage.AUTH);
        at.setCsrfTokenEnabled(true);

        String encoded = at.getEncoded();
        Map<?, ?> info = ZimbraAuthToken.getInfo(encoded);

        // Always-present fields.
        Assert.assertEquals(acct.getId(), info.get(AuthTokenProperties.C_ID));
        Assert.assertEquals(Long.toString(expires), info.get(AuthTokenProperties.C_EXP));
        Assert.assertEquals(AuthTokenProperties.C_TYPE_ZIMBRA_USER, info.get(AuthTokenProperties.C_TYPE));
        Assert.assertNotNull("token id must always be encoded (L346)",
                info.get(AuthTokenProperties.C_TOKEN_ID));
        Assert.assertEquals(String.valueOf(at.getProperties().getTokenID()),
                info.get(AuthTokenProperties.C_TOKEN_ID));

        // Optional fields driven by the L322-L350 guards.
        Assert.assertEquals("adminAccountId encoded (L322)",
                adminActing.getId(), info.get(AuthTokenProperties.C_AID));
        Assert.assertEquals("isAdmin flag encoded (L325)", "1", info.get(AuthTokenProperties.C_ADMIN));
        Assert.assertEquals("isDomainAdmin flag encoded (L328)", "1", info.get(AuthTokenProperties.C_DOMAIN));
        Assert.assertEquals("isDelegatedAdmin flag encoded (L331)", "1", info.get(AuthTokenProperties.C_DLGADMIN));
        Assert.assertEquals("authMech encoded (L340)", AuthMech.zimbra.name(),
                info.get(AuthTokenProperties.C_AUTH_MECH));
        Assert.assertEquals("usage encoded (L344)", Usage.AUTH.getCode(),
                info.get(AuthTokenProperties.C_USAGE));
        Assert.assertEquals("csrf flag encoded (L350)", "1", info.get(AuthTokenProperties.C_CSRF));

        // Round-trips through the decode constructor too.
        ZimbraAuthToken decoded = new ZimbraAuthToken(encoded);
        Assert.assertTrue(decoded.isAdmin());
        Assert.assertTrue(decoded.isDomainAdmin());
        Assert.assertTrue(decoded.isDelegatedAdmin());
        Assert.assertEquals(adminActing.getId(), decoded.getAdminAccountId());
        Assert.assertEquals(AuthMech.zimbra, decoded.getAuthMech());
        Assert.assertTrue(decoded.isCsrfTokenEnabled());

        Provisioning.getInstance().deleteAccount(acct.getId());
        Provisioning.getInstance().deleteAccount(adminActing.getId());
    }

    @Test
    public void getEncodedNonAdminTokenOmitsAdminAndCsrfKeys() throws Exception {
        // The negated/opposite side of the L325/L328/L331/L322/L350 guards: a plain user token must
        // NOT carry the admin, domain-admin, delegated-admin, admin-account-id or csrf keys.
        Account a = account();
        ZimbraAuthToken at = new ZimbraAuthToken(a);
        Map<?, ?> info = ZimbraAuthToken.getInfo(at.getEncoded());

        Assert.assertNull("no admin flag on a user token", info.get(AuthTokenProperties.C_ADMIN));
        Assert.assertNull("no domain-admin flag", info.get(AuthTokenProperties.C_DOMAIN));
        Assert.assertNull("no delegated-admin flag", info.get(AuthTokenProperties.C_DLGADMIN));
        Assert.assertNull("no admin-account id", info.get(AuthTokenProperties.C_AID));
        Assert.assertNull("no csrf flag", info.get(AuthTokenProperties.C_CSRF));
    }

    @Test
    public void getEncodedExternalTokenEncodesEmailAndDigest() throws Exception {
        // Covers the C_EXTERNAL_USER_EMAIL (L347) and C_DIGEST (L348) encodeMetaData calls: an
        // external token built with an email and digest must surface both in the decoded map.
        long expires = System.currentTimeMillis() + 60000L;
        ZimbraAuthToken at = new ZimbraAuthToken("acctXYZ", "ext@partner.example", "pw", "DIGESTVAL", expires);

        Map<?, ?> info = ZimbraAuthToken.getInfo(at.getEncoded());

        Assert.assertEquals("ext@partner.example", info.get(AuthTokenProperties.C_EXTERNAL_USER_EMAIL));
        Assert.assertEquals("DIGESTVAL", info.get(AuthTokenProperties.C_DIGEST));
        Assert.assertEquals(AuthTokenProperties.C_TYPE_EXTERNAL_USER, info.get(AuthTokenProperties.C_TYPE));
    }

    @Test
    public void getEncodedServerVersionSetEncodesServerVersionKey() throws Exception {
        // Covers the C_SERVER_VERSION encodeMetaData (L349): when the account's server advertises a
        // version, the encoded token must carry it. Set the local server version, build a token,
        // and confirm the key is present; restore the original version afterward.
        Provisioning prov = Provisioning.getInstance();
        Server localServer = prov.getLocalServer();
        String priorVersion = localServer.getServerVersion();
        try {
            Map<String, Object> svrChange = new HashMap<String, Object>();
            svrChange.put(Provisioning.A_zimbraServerVersion, "10.1.99");
            prov.modifyAttrs(localServer, svrChange);

            Account a = prov.createAccount("svrver@example.zimbra.com", "secret", new HashMap<String, Object>());
            ZimbraAuthToken at = new ZimbraAuthToken(a);
            Map<?, ?> info = ZimbraAuthToken.getInfo(at.getEncoded());

            Assert.assertEquals("server version must be encoded (L349)", "10.1.99",
                    info.get(AuthTokenProperties.C_SERVER_VERSION));

            prov.deleteAccount(a.getId());
        } finally {
            Map<String, Object> restore = new HashMap<String, Object>();
            restore.put(Provisioning.A_zimbraServerVersion, priorVersion == null ? "" : priorVersion);
            prov.modifyAttrs(localServer, restore);
        }
    }

    // ============================================================================================
    // setCsrfTokenEnabled removes the stale cached encoding (L526): if the token had been encoded
    // (so getEncoded() != null), enabling CSRF must drop the cached entry from the decode CACHE so
    // a subsequent getAuthToken(oldEncoded) rebuilds rather than returning the now-stale instance.
    // ============================================================================================

    @Test
    public void setCsrfTokenEnabledAfterEncodeEvictsOldEncodingFromCache() throws Exception {
        ZimbraAuthToken at = new ZimbraAuthToken(account());
        String oldEncoded = at.getEncoded();
        // Seed the decode cache with the old encoding.
        AuthToken cachedBefore = ZimbraAuthToken.getAuthToken(oldEncoded);
        Assert.assertSame("decode cache seeded", cachedBefore, ZimbraAuthToken.getAuthToken(oldEncoded));

        // Act — enabling CSRF must remove oldEncoded from CACHE and clear this token's encoding.
        at.setCsrfTokenEnabled(true);

        // Assert — the token re-encodes to a different value (CSRF flag now set).
        String newEncoded = at.getEncoded();
        Assert.assertFalse("CSRF change must alter the encoding", oldEncoded.equals(newEncoded));
        Assert.assertTrue(at.isCsrfTokenEnabled());
    }

    // ============================================================================================
    // register() side effects (L281 non-zimbra-user guard, L286 auth-version gate, L203 ctor calls
    // register, L551 resetTokenId calls register). With the local server advertising auth version
    // 2, creating a zimbra-user token must register its token id in the (in-memory) ephemeral store.
    // ============================================================================================

    @Test
    public void registerAuthVersion2RegistersTokenAndResetReRegisters() throws Exception {
        Provisioning prov = Provisioning.getInstance();
        // register() persists the token id to the ephemeral store; use the in-memory store so the
        // MockProvisioning-backed account can actually record it (the default LdapEphemeralStore
        // rejects MockProvisioning).
        EphemeralStore.setFactory(InMemoryEphemeralStore.Factory.class);
        // The in-memory store keeps a static, shared instance across tests; drop it so this test
        // starts from a pristine store and does not depend on the class's method-execution order.
        EphemeralStore.getFactory().shutdown();
        Server localServer = prov.getLocalServer();
        int priorAuthVersion = localServer.getLowestSupportedAuthVersion();
        String priorServerVersion = localServer.getServerVersion();
        try {
            Map<String, Object> svrChange = new HashMap<String, Object>();
            svrChange.put(Provisioning.A_zimbraLowestSupportedAuthVersion, "2");
            // register() stores the server version as the token's value; it must be non-null for the
            // token id to be retrievable, so set it here rather than relying on another test to have
            // set it first (this test must be self-contained / order-independent).
            svrChange.put(Provisioning.A_zimbraServerVersion, "10.1.20");
            prov.modifyAttrs(localServer, svrChange);

            Account a = prov.createAccount("registerme@example.zimbra.com", "secret", new HashMap<String, Object>());

            // Constructing the token calls register() (L203) which, under auth version > 1 (L286),
            // adds the token id to the account (L293 in source addAuthTokens).
            ZimbraAuthToken at = new ZimbraAuthToken(a);
            Integer firstId = at.getProperties().getTokenID();
            Account reloaded = prov.getAccountById(a.getId());
            Assert.assertTrue("token id must be registered after construction",
                    reloaded.hasAuthTokens(String.valueOf(firstId)));
            Assert.assertTrue("token reports registered", at.isRegistered());

            // resetTokenId() calls register() again (L551) for the new id.
            at.resetTokenId();
            Integer newId = at.getProperties().getTokenID();
            Assert.assertFalse("token id changed", firstId.equals(newId));
            Account reloaded2 = prov.getAccountById(a.getId());
            Assert.assertTrue("new token id registered after reset",
                    reloaded2.hasAuthTokens(String.valueOf(newId)));

            prov.deleteAccount(a.getId());
        } finally {
            Map<String, Object> restore = new HashMap<String, Object>();
            restore.put(Provisioning.A_zimbraLowestSupportedAuthVersion, String.valueOf(priorAuthVersion));
            restore.put(Provisioning.A_zimbraServerVersion, priorServerVersion == null ? "" : priorServerVersion);
            prov.modifyAttrs(localServer, restore);
        }
    }

    @Test
    public void registerExternalTokenIsNotRegisteredAndIsRegisteredTrue() throws Exception {
        // Covers register() L281 (!isZimbraUser early return): an external (non-zimbra) token is
        // never written to the ephemeral store, yet isRegistered() returns true for it (the
        // non-zimbra short-circuit), regardless of the server auth version.
        Provisioning prov = Provisioning.getInstance();
        Server localServer = prov.getLocalServer();
        int priorAuthVersion = localServer.getLowestSupportedAuthVersion();
        try {
            Map<String, Object> svrChange = new HashMap<String, Object>();
            svrChange.put(Provisioning.A_zimbraLowestSupportedAuthVersion, "2");
            prov.modifyAttrs(localServer, svrChange);

            long expires = System.currentTimeMillis() + 60000L;
            ZimbraAuthToken ext = new ZimbraAuthToken("extAcct123", "ext@nope.example", "pw", "dg", expires);

            Assert.assertFalse("external token is not a zimbra user", ext.isZimbraUser());
            Assert.assertTrue("non-zimbra token reports registered (L281 short-circuit)",
                    ext.isRegistered());
        } finally {
            Map<String, Object> restore = new HashMap<String, Object>();
            restore.put(Provisioning.A_zimbraLowestSupportedAuthVersion, String.valueOf(priorAuthVersion));
            prov.modifyAttrs(localServer, restore);
        }
    }

}
