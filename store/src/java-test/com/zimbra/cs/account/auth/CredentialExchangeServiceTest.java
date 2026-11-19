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

package com.zimbra.cs.account.auth;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CredentialExchangeServiceTest {

    private static final String DOMAIN = "example.com";

    private static final String VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk"; // 43 chars

    private static final MockProvisioning PROV = new MockProvisioning();

    private CredentialExchangeService service;

    private Account alice;

    private Account bob;

    private String challenge;

    private InMemoryCredentialStore store;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning.setInstance(PROV);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        final Map<String, Object> domainAttrs = new HashMap<>();
        domainAttrs.put(Provisioning.A_zimbraWebClientLoginURL, "https://mail.example.com/service/extension/samllogin");
        domainAttrs.put(Provisioning.A_zimbraSamlSSOURL, "https://idp.example.com/sso");
        PROV.createDomain(DOMAIN, domainAttrs);

        alice = createAccount("alice");
        bob = createAccount("bob");

        store = new InMemoryCredentialStore();
        service = new CredentialExchangeService(() -> store);
        challenge = challengeOf(VERIFIER);
    }

    private Account createAccount(String local) throws ServiceException {
        final Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraId, LdapUtil.generateUUID());
        return PROV.createAccount(local + "@" + DOMAIN, "Passw0rd!", attrs);
    }

    private static String challengeOf(String verifier) throws Exception {
        final byte[] h = MessageDigest.getInstance("SHA-256").digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(h);
    }

    private void assertRejected(String handle, String verifier, Account requested) throws Exception {
        assertRejected(service, handle, verifier, requested);
    }

    private static void assertRejected(CredentialExchangeService svc, String handle, String verifier,
                                       Account requested) throws Exception {
        try {
            svc.redeem(handle, verifier, requested);
            Assert.fail("expected the redemption to be rejected");
        } catch (AuthFailedServiceException expected) {
            Assert.assertEquals(AuthFailedServiceException.AUTH_FAILED, expected.getCode());
        }
    }

    private static void assertServiceCode(String code, ThrowingRunnable action) throws Exception {
        try {
            action.run();
            Assert.fail("expected " + code);
        } catch (ServiceException expected) {
            Assert.assertEquals(code, expected.getCode());
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static String recordKey(String handle) throws Exception {
        final byte[] h = MessageDigest.getInstance("SHA-256").digest(handle.getBytes(StandardCharsets.US_ASCII));
        final StringBuilder sb = new StringBuilder("cred:");
        for (byte b : h) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private void putRecord(String handle, String version, String audience, long expiresAt) throws Exception {
        store.put(recordKey(handle), String.join("|", version, alice.getId(), challenge, audience,
                Long.toString(expiresAt)), 60_000);
    }

    @Test
    public void issuedHandleHasPrefixAndLength() throws Exception {
        final String handle = service.issueCredential(alice, challenge);
        Assert.assertTrue(handle.startsWith(CredentialExchangeService.CREDENTIAL_PREFIX));
        Assert.assertEquals(5 + 43, handle.length());
        Assert.assertNotEquals("each call gets a distinct handle", handle, service.issueCredential(alice, challenge));
    }

    @Test
    public void issueRejectsBadChallenge() throws Exception {
        try {
            service.issueCredential(alice, "too-short");
            Assert.fail("expected INVALID_REQUEST");
        } catch (ServiceException expected) {
            Assert.assertEquals(ServiceException.INVALID_REQUEST, expected.getCode());
        }
    }

    @Test
    public void redeemSucceedsOnceThenFails() throws Exception {
        final String handle = service.issueCredential(alice, challenge);
        Assert.assertEquals(alice.getId(), service.redeem(handle, VERIFIER, alice).getId());
        assertRejected(handle, VERIFIER, alice); // single use
    }

    @Test
    public void wrongVerifierBurnsTheCredential() throws Exception {
        final String handle = service.issueCredential(alice, challenge);
        assertRejected(handle, "A" + VERIFIER.substring(1), alice);
        assertRejected(handle, VERIFIER, alice); // already burned, even with the right verifier
    }

    @Test
    public void accountMismatchBurnsTheCredential() throws Exception {
        final String handle = service.issueCredential(alice, challenge);
        assertRejected(handle, VERIFIER, bob); // credential belongs to alice
        assertRejected(handle, VERIFIER, alice); // and is burned
    }

    @Test
    public void missingRequestedAccountIsRejected() throws Exception {
        final String handle = service.issueCredential(alice, challenge);
        assertRejected(handle, VERIFIER, null);
    }

    @Test
    public void malformedAndUnknownInputIsRejected() throws Exception {
        assertRejected(null, VERIFIER, alice);
        assertRejected("zcx1.short", VERIFIER, alice);
        assertRejected("zcx1." + VERIFIER, VERIFIER, alice); // well formed, never issued
        assertRejected(service.issueCredential(alice, challenge), "short", alice);
    }

    @Test
    public void disabledAccountIsRejected() throws Exception {
        final String handle = service.issueCredential(alice, challenge);
        alice.setAccountStatus(Provisioning.AccountStatus.closed);
        assertRejected(handle, VERIFIER, alice);
    }

    @Test
    public void domainWithoutBrowserAuthIsRejected() throws Exception {
        final Map<String, Object> carolAttrs = new HashMap<>();
        carolAttrs.put(Provisioning.A_zimbraId, LdapUtil.generateUUID());
        final Account carol = PROV.createAccount("carol@other.example.com", "Passw0rd!", carolAttrs);
        final String handle = service.issueCredential(carol, challenge);
        assertRejected(handle, VERIFIER, carol);
    }

    @Test
    public void authnRequestRecordIsConsumedOnce() throws Exception {
        final String nonce = "AAAAAAAAAAAAAAAA"; // 16 chars
        service.recordAuthnRequest(nonce, "_abc123");
        Assert.assertFalse("wrong InResponseTo", service.consumeAuthnRequest(nonce, "_other"));
        Assert.assertTrue(service.consumeAuthnRequest(nonce, "_abc123"));
        Assert.assertFalse("replay", service.consumeAuthnRequest(nonce, "_abc123"));
        Assert.assertFalse("unknown nonce", service.consumeAuthnRequest("BBBBBBBBBBBBBBBB", "_abc123"));
        Assert.assertFalse("missing InResponseTo", service.consumeAuthnRequest(nonce, null));
    }

    @Test
    public void inMemoryStoreDeleteHasOneWinner() {
        final InMemoryCredentialStore store = new InMemoryCredentialStore();
        store.put("k", "v", 60_000);
        Assert.assertTrue(store.delete("k"));
        Assert.assertFalse(store.delete("k"));
    }

    @Test
    public void inMemoryStoreExpires() throws Exception {
        final InMemoryCredentialStore store = new InMemoryCredentialStore();
        store.put("k", "v", 1);
        Thread.sleep(20);
        Assert.assertNull(store.get("k"));
        Assert.assertFalse(store.delete("k"));
    }

    @Test
    public void expiredRecordIsRejectedAndBurned() throws Exception {
        final String handle = "zcx1." + challengeOf("expired");
        putRecord(handle, "v1", CredentialExchangeService.AUDIENCE, System.currentTimeMillis() - 1_000);
        assertRejected(handle, VERIFIER, alice);
        Assert.assertNull("an expired record is deleted", store.get(recordKey(handle)));
    }

    @Test
    public void otherAudienceIsRejectedAndBurned() throws Exception {
        // Also what happens to a credential issued before the audience rename
        final String handle = "zcx1." + challengeOf("audience");
        putRecord(handle, "v1", "legacy", System.currentTimeMillis() + 60_000);
        assertRejected(handle, VERIFIER, alice);
        Assert.assertNull(store.get(recordKey(handle)));
    }

    @Test
    public void unknownRecordVersionIsRejected() throws Exception {
        final String handle = "zcx1." + challengeOf("version");
        putRecord(handle, "v0", CredentialExchangeService.AUDIENCE, System.currentTimeMillis() + 60_000);
        assertRejected(handle, VERIFIER, alice);
    }

    @Test
    public void lostRaceIsRejected() throws Exception {
        final InMemoryCredentialStore backing = new InMemoryCredentialStore();
        final CredentialStore otherNodeWon = new CredentialStore() {
            @Override
            public boolean put(String key, String value, long ttlMs) {
                return backing.put(key, value, ttlMs);
            }

            @Override
            public String get(String key) {
                return backing.get(key);
            }

            @Override
            public boolean delete(String key) {
                return false;
            }
        };
        final CredentialExchangeService racing = new CredentialExchangeService(() -> otherNodeWon);
        final String handle = racing.issueCredential(alice, challenge);
        assertRejected(racing, handle, VERIFIER, alice);
    }

    @Test
    public void concurrentRedeemHasOneWinner() throws Exception {
        final String handle = service.issueCredential(alice, challenge);
        final int threads = 8;
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            results.add(pool.submit(() -> {
                start.await();
                try {
                    service.redeem(handle, VERIFIER, alice);
                    return true;
                } catch (AuthFailedServiceException e) {
                    return false;
                }
            }));
        }
        start.countDown();
        int wins = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) {
                wins++;
            }
        }
        pool.shutdown();
        Assert.assertEquals(1, wins);
    }

    @Test
    public void storeWriteFailureIsServiceFailure() throws Exception {
        final CredentialStore full = new CredentialStore() {
            @Override
            public boolean put(String key, String value, long ttlMs) {
                return false;
            }

            @Override
            public String get(String key) {
                return null;
            }

            @Override
            public boolean delete(String key) {
                return false;
            }
        };
        final CredentialExchangeService failing = new CredentialExchangeService(() -> full);
        assertServiceCode(ServiceException.FAILURE, () -> failing.issueCredential(alice, challenge));
        assertServiceCode(ServiceException.FAILURE, () -> failing.recordAuthnRequest("AAAAAAAAAAAAAAAA", "_id"));
    }

    @Test
    public void malformedInputNeverTouchesTheStore() throws Exception {
        final CredentialStore untouchable = new CredentialStore() {
            @Override
            public boolean put(String key, String value, long ttlMs) {
                throw new AssertionError("store must not be touched");
            }

            @Override
            public String get(String key) {
                throw new AssertionError("store must not be touched");
            }

            @Override
            public boolean delete(String key) {
                throw new AssertionError("store must not be touched");
            }
        };
        final CredentialExchangeService svc = new CredentialExchangeService(() -> untouchable);
        assertServiceCode(AuthFailedServiceException.AUTH_FAILED, () -> svc.checkFormat("bad", VERIFIER));
        assertServiceCode(AuthFailedServiceException.AUTH_FAILED, () -> svc.checkFormat("zcx1." + VERIFIER, "bad"));
        assertRejected(svc, "zcx1.short", VERIFIER, alice);
    }

    @Test
    public void authnRequestRecordRejectsInvalidInput() throws Exception {
        assertServiceCode(ServiceException.INVALID_REQUEST, () -> service.recordAuthnRequest("short", "_id"));
        assertServiceCode(ServiceException.INVALID_REQUEST,
                () -> service.recordAuthnRequest("AAAAAAAAAAAAAAAA", "has space"));
        Assert.assertFalse(service.consumeAuthnRequest("short", "_id"));
    }

    @Test
    public void issueRejectsNullAccount() throws Exception {
        assertServiceCode(ServiceException.INVALID_REQUEST, () -> service.issueCredential(null, challenge));
    }

    @Test
    public void deletedAccountIsRejected() throws Exception {
        final String handle = service.issueCredential(alice, challenge);
        PROV.deleteAccount(alice.getId());
        assertRejected(handle, VERIFIER, alice);
    }

    @Test
    public void domainWithoutSsoUrlIsRejected() throws Exception {
        final Map<String, Object> domainAttrs = new HashMap<>();
        domainAttrs.put(Provisioning.A_zimbraWebClientLoginURL, "https://mail.example.com/service/extension/samllogin");
        PROV.createDomain("nosso.example.com", domainAttrs);
        final Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraId, LdapUtil.generateUUID());
        final Account dave = PROV.createAccount("dave@nosso.example.com", "Passw0rd!", attrs);
        assertRejected(service.issueCredential(dave, challenge), VERIFIER, dave);
    }

    @Test
    public void inMemoryStoreRefusesWritesWhenFull() {
        final InMemoryCredentialStore s = new InMemoryCredentialStore();
        for (int i = 0; i < 10_000; i++) {
            Assert.assertTrue(s.put("k" + i, "v", 60_000));
        }
        Assert.assertFalse(s.put("overflow", "v", 60_000));
    }
}
