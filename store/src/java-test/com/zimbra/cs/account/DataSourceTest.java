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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.soap.type.DataSource.ConnectionType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link DataSource} — real DataSource objects backed by a real Account
 * via the in-memory harness. Exercises attribute-driven getters, encryption round-trips, and
 * connection-type resolution.
 */
public class DataSourceTest {

    private static Account account;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initProvisioning();
    }

    @Before
    public void setUp() throws Exception {
        account = Provisioning.getInstance().createAccount("dsuser@example.com", "secret",
                new HashMap<String, Object>());
    }

    private DataSource newDataSource(DataSourceType type, Map<String, Object> attrs) {
        return new DataSource(account, type, "ds1", UUID.randomUUID().toString(), attrs,
                Provisioning.getInstance());
    }

    @Test
    public void encryptDecryptDataRoundTripRecoversOriginalPlaintext() throws Exception {
        // Arrange
        String id = UUID.randomUUID().toString();
        String secret = "hello world password";

        // Act
        String encrypted = DataSource.encryptData(id, secret);
        String decrypted = DataSource.decryptData(id, encrypted);

        // Assert
        assertFalse("ciphertext must differ from plaintext", secret.equals(encrypted));
        assertEquals(secret, decrypted);
    }

    @Test
    public void decryptDataCorruptedShortInputThrowsFailure() {
        // Act / Assert — too short to contain version+salt+pad. The length guard is
        // "encoded.length < VERSION.length + SALT_SIZE_BYTES + AES_PAD_SIZE" (= 1+16+16 = 33).
        // "AAAA" base64-decodes to 3 bytes (< 33) so it must trip the size guard with the EXACT
        // "invalid encoded size: 3" message. Asserting the exact message + decoded length pins the
        // MathMutator at L485: any change to that arithmetic shifts the threshold so the short
        // input would instead fall through to a security/version failure (different message).
        try {
            DataSource.decryptData(UUID.randomUUID().toString(), "AAAA");
            fail("expected failure for too-short encoded data");
        } catch (ServiceException e) {
            assertEquals("invalid encoded size guard must fire with the decoded length",
                    "system failure: invalid encoded size: 3", e.getMessage());
        }
    }

    @Test
    public void decryptDataLengthOneBelowThresholdReportsThatExactSize() {
        // Arrange — 32 raw bytes is exactly one below the 33-byte threshold, so the guard fires
        // and reports "invalid encoded size: 32". 33 bytes (the unsupported-version test below)
        // passes the guard. Pinning the size in the message kills the MathMutator at L485, which
        // would move the boundary and let 32 through (or reject 33).
        byte[] raw = new byte[32];
        byte[] encoded = org.apache.commons.codec.binary.Base64.encodeBase64(raw);
        try {
            DataSource.decryptData(UUID.randomUUID().toString(), encoded);
            fail("expected failure for 32-byte payload");
        } catch (ServiceException e) {
            assertEquals("system failure: invalid encoded size: 32", e.getMessage());
        }
    }

    @Test
    public void encryptDataSameInputTwiceProducesDifferentCiphertextDueToRandomSalt() throws Exception {
        // Arrange — same id + same plaintext encrypted twice. randomSalt() seeds each ciphertext
        // with a fresh random salt (random.nextBytes(pad) at L433); if that call were removed the
        // salt would be a constant (all zeros) and the two ciphertexts would be byte-identical.
        String id = UUID.randomUUID().toString();
        String plain = "repeatable-secret";

        // Act
        String enc1 = DataSource.encryptData(id, plain);
        String enc2 = DataSource.encryptData(id, plain);

        // Assert — distinct ciphertexts, but both decrypt back to the same plaintext
        assertFalse("random salt must make repeated encryptions differ", enc1.equals(enc2));
        assertEquals(plain, DataSource.decryptData(id, enc1));
        assertEquals(plain, DataSource.decryptData(id, enc2));
    }

    @Test
    public void decryptDataWithWrongDataSourceIdFailsBecauseIdIsPartOfKey() throws Exception {
        // Arrange — encrypt under one id. getCipher() folds BOTH the salt (L439 md5.update(salt))
        // and the dataSourceId bytes (L440 md5.update(id)) into the AES key. Decrypting with a
        // DIFFERENT id derives a different key and the AES unpad fails. If either md5.update call
        // were dropped the key would no longer depend on that input and a wrong id would wrongly
        // decrypt successfully.
        String id = UUID.randomUUID().toString();
        String plain = "id-bound-secret";
        String enc = DataSource.encryptData(id, plain);

        // Sanity — the correct id recovers the plaintext exactly.
        assertEquals(plain, DataSource.decryptData(id, enc));

        // Act / Assert — a different id must NOT recover the plaintext; it throws a security failure.
        String wrongId = UUID.randomUUID().toString();
        try {
            String got = DataSource.decryptData(wrongId, enc);
            fail("decrypting with a different id must not recover the plaintext, but got: " + got);
        } catch (ServiceException e) {
            assertEquals("system failure: caught security exception", e.getMessage());
        }
    }

    @Test
    public void constructorGmailHostInitializesKnownServiceForFolderMapping() {
        // Arrange — the constructor calls initKnownService() (L78). For a gmail host the known
        // "gmail.com" service maps remote "INBOX" to local "/Inbox" and vice versa. If the
        // constructor's initKnownService() call were removed, knownService stays null and both
        // mappings return null.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceHost, "imap.gmail.com");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert — concrete known-folder mappings prove knownService was wired up.
        assertEquals("/Inbox", ds.mapRemoteToLocalPath("INBOX", null));
        assertEquals("INBOX", ds.mapLocalToRemotePath("/Inbox"));
    }

    @Test
    public void getTypeConstructedTypeIsReturned() {
        // Arrange
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act / Assert
        assertEquals(DataSourceType.imap, ds.getType());
        assertEquals(Entry.EntryType.DATASOURCE, ds.getEntryType());
    }

    @Test
    public void getDecryptedPasswordSetEncryptedPasswordReturnsPlaintext() throws Exception {
        // Arrange
        String id = UUID.randomUUID().toString();
        String plain = "myimappass";
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourcePassword, DataSource.encryptData(id, plain));
        DataSource ds = new DataSource(account, DataSourceType.imap, "ds1", id, attrs,
                Provisioning.getInstance());

        // Act / Assert
        assertEquals(plain, ds.getDecryptedPassword());
    }

    @Test
    public void getDecryptedPasswordNoPasswordAttrReturnsNull() throws Exception {
        // Arrange
        DataSource ds = newDataSource(DataSourceType.pop3, new HashMap<String, Object>());

        // Act / Assert
        assertNull(ds.getDecryptedPassword());
    }

    @Test
    public void getHostUsernameAndScalarAttrsReturnSetValues() {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceHost, "imap.example.com");
        attrs.put(Provisioning.A_zimbraDataSourceUsername, "bob");
        attrs.put(Provisioning.A_zimbraDataSourcePort, "993");
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, "5");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert
        assertEquals("imap.example.com", ds.getHost());
        assertEquals("bob", ds.getUsername());
        assertEquals(Integer.valueOf(993), ds.getPort());
        assertEquals(5, ds.getFolderId());
    }

    @Test
    public void getPortUnsetReturnsNull() {
        // Arrange
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act / Assert
        assertNull(ds.getPort());
    }

    @Test
    public void getConnectionTypeExplicitSslReturnsSslAndIsSslEnabledTrue() {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, "ssl");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert
        assertEquals(ConnectionType.ssl, ds.getConnectionType());
        assertTrue(ds.isSslEnabled());
    }

    @Test
    public void getConnectionTypeIllegalValueFallsBackToCleartext() {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, "bogus");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert
        assertEquals(ConnectionType.cleartext, ds.getConnectionType());
        assertFalse(ds.isSslEnabled());
    }

    @Test
    public void isEnabledAndLeaveOnServerDefaultsAndExplicitReflectAttrs() {
        // Arrange — defaults
        DataSource defaults = newDataSource(DataSourceType.pop3, new HashMap<String, Object>());

        // Assert — isEnabled defaults false, leaveOnServer defaults true
        assertFalse(defaults.isEnabled());
        assertTrue(defaults.leaveOnServer());

        // Arrange — explicit
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, "TRUE");
        attrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer, "FALSE");
        DataSource explicit = newDataSource(DataSourceType.pop3, attrs);

        // Assert
        assertTrue(explicit.isEnabled());
        assertFalse(explicit.leaveOnServer());
    }

    @Test
    public void getPollingIntervalSetBelowAccountMinimumClampedToAccountMinPollingInterval() throws Exception {
        // Arrange — request a 1-second interval. getPollingInterval() floors any positive
        // value to max(account zimbraDataSourceMinPollingInterval, 10s). The account default
        // for zimbraDataSourceMinPollingInterval is 1m (60000ms), which exceeds the 10s
        // safeguard, so the effective floor is 60000ms.
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourcePollingInterval, "1s");
        DataSource ds = newDataSource(DataSourceType.pop3, attrs);

        // Act
        long interval = ds.getPollingInterval();

        // Assert — floored to the account min polling interval (60000ms)
        assertEquals(60000L, interval);
        assertTrue(ds.isScheduled());
    }

    @Test
    public void getSmtpUsernameAuthRequiredNoExplicitFallsBackToUsername() {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceUsername, "bob");
        attrs.put(Provisioning.A_zimbraDataSourceSmtpEnabled, "TRUE");
        attrs.put(Provisioning.A_zimbraDataSourceSmtpAuthRequired, "TRUE");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert
        assertTrue(ds.isSmtpEnabled());
        assertTrue(ds.isSmtpAuthRequired());
        assertEquals("bob", ds.getSmtpUsername());
    }

    @Test
    public void getSmtpUsernameExplicitSmtpUserReturnsExplicit() {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceUsername, "bob");
        attrs.put(Provisioning.A_zimbraDataSourceSmtpAuthUsername, "smtpbob");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert
        assertEquals("smtpbob", ds.getSmtpUsername());
    }

    @Test
    public void getDomainNoExplicitDomainDerivesFromEmailAddress() {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEmailAddress, "alice@derived.example.org");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert
        assertEquals("derived.example.org", ds.getDomain());
    }

    @Test
    public void toStringIncludesIdTypeAndName() {
        // Arrange
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act
        String s = ds.toString();

        // Assert
        assertTrue(s.contains("ds1"));
        assertTrue(s.contains("imap"));
    }

    @Test
    public void getConnectionTypeNoAttrFallsBackToGlobalConfigCleartext() {
        // Arrange — no per-data-source connection type; global config defaults to cleartext
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act / Assert
        assertEquals(ConnectionType.cleartext, ds.getConnectionType());
        assertFalse(ds.isSslEnabled());
    }

    @Test
    public void timeoutAndTraceGettersUnsetReturnDefaults() {
        // Arrange
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act / Assert — getters return their supplied defaults when attrs absent
        assertEquals(Integer.valueOf(30), ds.getConnectTimeout(30));
        assertEquals(45, ds.getReadTimeout(45));
        assertEquals(64, ds.getMaxTraceSize());
    }

    @Test
    public void timeoutGettersExplicitAttrsReturnSetValues() {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceConnectTimeout, "11");
        attrs.put(Provisioning.A_zimbraDataSourceReadTimeout, "22");
        attrs.put(Provisioning.A_zimbraDataSourceMaxTraceSize, "33");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert
        assertEquals(Integer.valueOf(11), ds.getConnectTimeout(99));
        assertEquals(22, ds.getReadTimeout(99));
        assertEquals(33, ds.getMaxTraceSize());
    }

    @Test
    public void onlineFlagGettersReturnFixedOnlineDefaults() {
        // Arrange
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act / Assert — the online DataSource hard-codes these
        assertFalse(ds.isSyncInboxOnly());
        assertTrue(ds.isSyncEnabled("/anything"));
        assertTrue(ds.isSaveToSent());
        assertFalse(ds.isOffline());
        assertEquals(0L, ds.getSyncFrequency());
    }

    @Test
    public void isSyncNeededOnlineReturnsFalse() throws Exception {
        // Arrange
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act / Assert
        assertFalse(ds.isSyncNeeded());
    }

    @Test
    public void checkPendingMessagesOnlineReturnsFalse() throws Exception {
        // Arrange
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act / Assert
        assertFalse(ds.checkPendingMessages());
    }

    @Test
    public void reportErrorAndMailboxDeletedDoNothingNoException() {
        // Arrange
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act / Assert — no-op methods must not throw
        ds.reportError(7, "boom", new RuntimeException("x"));
        ds.mailboxDeleted();
    }

    @Test
    public void knownFolderMappingNoKnownServiceReturnsNullAndIgnoreFalse() {
        // Arrange — host that does not match a known service so knownService is null
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceHost, "imap.unknownhost.example");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert
        assertNull(ds.mapRemoteToLocalPath("INBOX", null));
        assertNull(ds.mapLocalToRemotePath("/Inbox"));
        assertFalse(ds.ignoreRemotePath("INBOX", null));
    }

    @Test
    public void getImapTrashFolderIdUnsetReturnsNegativeOne() {
        // Arrange
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act / Assert
        assertEquals(-1, ds.getImapTrashFolderId());
    }

    @Test
    public void getImapTrashFolderIdSetReturnsValue() {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceImapTrashFolderId, "42");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert
        assertEquals(42, ds.getImapTrashFolderId());
    }

    @Test
    public void smtpGettersExplicitAttrsReturnSetValues() {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceSmtpHost, "smtp.example.com");
        attrs.put(Provisioning.A_zimbraDataSourceSmtpPort, "587");
        attrs.put(Provisioning.A_zimbraDataSourceSmtpConnectionType, "ssl");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert
        assertEquals("smtp.example.com", ds.getSmtpHost());
        assertEquals(Integer.valueOf(587), ds.getSmtpPort());
        assertTrue("non-cleartext smtp connection is secure", ds.isSmtpConnectionSecure());
    }

    @Test
    public void isSmtpConnectionSecureCleartextReturnsFalse() {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceSmtpConnectionType, "cleartext");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert
        assertFalse(ds.isSmtpConnectionSecure());
    }

    @Test
    public void getSmtpUsernameNoAuthRequiredReturnsNull() {
        // Arrange — smtp not enabled / auth not required and no explicit smtp username
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act / Assert
        assertNull(ds.getSmtpUsername());
    }

    @Test
    public void getDecryptedSmtpPasswordExplicitEncryptedReturnsPlaintext() throws Exception {
        // Arrange
        String id = UUID.randomUUID().toString();
        String plain = "smtpsecret";
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceSmtpAuthPassword, DataSource.encryptData(id, plain));
        DataSource ds = new DataSource(account, DataSourceType.imap, "ds1", id, attrs,
                Provisioning.getInstance());

        // Act / Assert
        assertEquals(plain, ds.getDecryptedSmtpPassword());
    }

    @Test
    public void getDecryptedSmtpPasswordAuthRequiredNoExplicitFallsBackToDataSourcePassword() throws Exception {
        // Arrange — no smtp password, but smtp enabled + auth required => use data source password
        String id = UUID.randomUUID().toString();
        String plain = "mainpass";
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourcePassword, DataSource.encryptData(id, plain));
        attrs.put(Provisioning.A_zimbraDataSourceSmtpEnabled, "TRUE");
        attrs.put(Provisioning.A_zimbraDataSourceSmtpAuthRequired, "TRUE");
        DataSource ds = new DataSource(account, DataSourceType.imap, "ds1", id, attrs,
                Provisioning.getInstance());

        // Act / Assert
        assertEquals(plain, ds.getDecryptedSmtpPassword());
    }

    @Test
    public void getDecryptedSmtpPasswordNoPasswordNoAuthReturnsNull() throws Exception {
        // Arrange
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act / Assert
        assertNull(ds.getDecryptedSmtpPassword());
    }

    @Test
    public void getDecryptedOAuthTokenAndClientSecretUnsetReturnNull() throws Exception {
        // Arrange
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act / Assert
        assertNull(ds.getDecryptedOAuthToken());
        assertNull(ds.getDecryptedOAuthClientSecret());
    }

    @Test
    public void getDecryptedOAuthTokenSetEncryptedReturnsPlaintext() throws Exception {
        // Arrange
        String id = UUID.randomUUID().toString();
        String plain = "oauth-token-xyz";
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceOAuthToken, DataSource.encryptData(id, plain));
        DataSource ds = new DataSource(account, DataSourceType.imap, "ds1", id, attrs,
                Provisioning.getInstance());

        // Act / Assert
        assertEquals(plain, ds.getDecryptedOAuthToken());
    }

    @Test
    public void oauthAndSignatureGettersReturnSetValues() {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceAuthorizationId, "authid");
        attrs.put(Provisioning.A_zimbraDataSourceAuthMechanism, "XOAUTH2");
        attrs.put(Provisioning.A_zimbraDataSourceOAuthRefreshToken, "rt");
        attrs.put(Provisioning.A_zimbraDataSourceOAuthRefreshTokenUrl, "https://refresh.example");
        attrs.put(Provisioning.A_zimbraDataSourceOAuthClientId, "cid");
        attrs.put(Provisioning.A_zimbraDataSourceImportClassName, "com.example.Import");
        attrs.put(Provisioning.A_zimbraPrefDefaultSignatureId, "sig1");
        attrs.put(Provisioning.A_zimbraPrefForwardReplySignatureId, "sig2");
        attrs.put(Provisioning.A_zimbraPrefFromDisplay, "From Me");
        attrs.put(Provisioning.A_zimbraPrefReplyToAddress, "reply@example.com");
        attrs.put(Provisioning.A_zimbraPrefReplyToDisplay, "Reply Me");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert
        assertEquals("authid", ds.getAuthId());
        assertEquals("XOAUTH2", ds.getAuthMechanism());
        assertEquals("rt", ds.getOauthRefreshToken());
        assertEquals("https://refresh.example", ds.getOauthRefreshTokenUrl());
        assertEquals("cid", ds.getOauthClientId());
        assertEquals("com.example.Import", ds.getDataSourceImportClassName());
        assertEquals("sig1", ds.getDefaultSignature());
        assertEquals("sig2", ds.getForwardReplySignature());
        assertEquals("From Me", ds.getFromDisplay());
        assertEquals("reply@example.com", ds.getReplyToAddress());
        assertEquals("Reply Me", ds.getReplyToDisplay());
    }

    @Test
    public void importOnlyAndInternalAndDebugTraceReflectAttrsAndOverride() {
        // Arrange
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceImportOnly, "TRUE");
        attrs.put(Provisioning.A_zimbraDataSourceIsInternal, "TRUE");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert
        assertTrue(ds.isImportOnly());
        assertTrue(ds.isInternal());
        assertFalse("trace defaults off", ds.isDebugTraceEnabled());

        // Act — request-scope override turns trace on
        ds.setRequestScopeDebugTraceOn(true);

        // Assert
        assertTrue(ds.isDebugTraceEnabled());
    }

    @Test
    public void useAddressForForwardReplyDefaultsFalseExplicitTrue() {
        // Arrange — default
        DataSource def = newDataSource(DataSourceType.imap, new HashMap<String, Object>());
        assertFalse(def.useAddressForForwardReply());

        // Arrange — explicit
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceUseAddressForForwardReply, "TRUE");
        DataSource explicit = newDataSource(DataSourceType.imap, attrs);

        // Assert
        assertTrue(explicit.useAddressForForwardReply());
    }

    @Test
    public void encryptDecryptDataByteArrayRoundTripRecoversOriginalBytes() throws Exception {
        // Arrange
        String id = UUID.randomUUID().toString();
        byte[] data = "raw-binary-secret".getBytes("utf-8");

        // Act
        byte[] enc = DataSource.encryptData(id, data);
        byte[] dec = DataSource.decryptData(id, enc);

        // Assert
        assertTrue("decrypted bytes must match original", Arrays.equals(data, dec));
    }

    @Test
    public void decryptDataUnsupportedVersionBytesThrowsFailure() {
        // Arrange — well-formed length but wrong version byte (0) so the version check fails
        byte[] bogus = new byte[1 + 16 + 16];
        bogus[0] = 0; // VERSION expected to be {1}
        byte[] encoded = org.apache.commons.codec.binary.Base64.encodeBase64(bogus);

        // Act / Assert — 33 bytes PASSES the size guard (kills the L485 math from the other side:
        // if the threshold were raised, 33 bytes would wrongly trip "invalid encoded size"), so the
        // failure must come from the version check, not the size check.
        try {
            DataSource.decryptData(UUID.randomUUID().toString(), encoded);
            fail("expected failure for unsupported version");
        } catch (ServiceException e) {
            assertEquals("system failure: unsupported version", e.getMessage());
        }
    }

    @Test
    public void booleanFlagGettersDefaultFalseWhenAttrAbsent() {
        // Arrange — no attrs set. isImportOnly (L345), isInternal (L349), isSmtpEnabled (L521) and
        // isSmtpAuthRequired (L538) all default to false. The existing tests only assert the TRUE
        // case; a BooleanTrueReturnVals mutant forces these to always return true, which only a
        // concrete false assertion can catch.
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act / Assert
        assertFalse("isImportOnly defaults false", ds.isImportOnly());
        assertFalse("isInternal defaults false", ds.isInternal());
        assertFalse("isSmtpEnabled defaults false", ds.isSmtpEnabled());
        assertFalse("isSmtpAuthRequired defaults false", ds.isSmtpAuthRequired());
    }

    @Test
    public void smtpEnabledAndAuthRequiredExplicitTrueReturnTrue() {
        // Arrange — set both explicitly true so we still assert the true side alongside the
        // false-default test above (full both-sides coverage of L521 and L538).
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceSmtpEnabled, "TRUE");
        attrs.put(Provisioning.A_zimbraDataSourceSmtpAuthRequired, "TRUE");
        DataSource ds = newDataSource(DataSourceType.imap, attrs);

        // Act / Assert
        assertTrue(ds.isSmtpEnabled());
        assertTrue(ds.isSmtpAuthRequired());
    }

    @Test
    public void getPollingIntervalImapTypeNoExplicitIntervalUsesAccountImapDefault() throws Exception {
        // Arrange — no explicit data-source interval forces the account-default switch branch.
        // The default COS must exist so migratePollingIntervalIfNecessary() can read it.
        ensureDefaultCos();
        DataSource ds = newDataSource(DataSourceType.imap, new HashMap<String, Object>());

        // Act — account imap polling default is unset (-1) so getPollingInterval returns 0
        long interval = ds.getPollingInterval();

        // Assert
        assertEquals(0L, interval);
        assertFalse(ds.isScheduled());
    }

    @Test
    public void getPollingIntervalPop3TypeNoExplicitIntervalUsesAccountPop3Default() throws Exception {
        // Arrange
        ensureDefaultCos();
        DataSource ds = newDataSource(DataSourceType.pop3, new HashMap<String, Object>());

        // Act
        long interval = ds.getPollingInterval();

        // Assert
        assertEquals(0L, interval);
    }

    @Test
    public void getPollingIntervalRssTypeNoExplicitIntervalUsesAccountRssDefault() throws Exception {
        // Arrange
        ensureDefaultCos();
        DataSource ds = newDataSource(DataSourceType.rss, new HashMap<String, Object>());

        // Act — rss branch of the switch. The account default for
        // zimbraDataSourceRssPollingInterval is 12h (43200000ms), which exceeds the min
        // polling safeguard, so it passes through unchanged.
        long interval = ds.getPollingInterval();

        // Assert
        assertEquals(43200000L, interval);
        assertTrue(ds.isScheduled());
    }

    @Test
    public void getPollingIntervalGalTypeNoExplicitIntervalUsesAccountGalDefault() throws Exception {
        // Arrange
        ensureDefaultCos();
        DataSource ds = newDataSource(DataSourceType.gal, new HashMap<String, Object>());

        // Act / Assert — gal branch of the switch
        assertEquals(0L, ds.getPollingInterval());
    }

    @Test
    public void getPollingIntervalCalTypeNoExplicitIntervalUsesAccountCalDefault() throws Exception {
        // Arrange
        ensureDefaultCos();
        DataSource ds = newDataSource(DataSourceType.cal, new HashMap<String, Object>());

        // Act — cal branch of the switch. The account default for
        // zimbraDataSourceCalendarPollingInterval is 12h (43200000ms), which exceeds the
        // min polling safeguard, so it passes through unchanged.
        long interval = ds.getPollingInterval();

        // Assert
        assertEquals(43200000L, interval);
        assertTrue(ds.isScheduled());
    }

    @Test
    public void getPollingIntervalMinBelowSafeguardClampsToTenSecondSafeguard() throws Exception {
        // Arrange — account min polling = 5s, BELOW the hard 10s safeguard. With a 1s requested
        // interval the result must be floored to the 10s safeguard (10000ms), not to 5s. This
        // pins the L253 guard body "if (min < safeguard) min = safeguard": dropping that
        // assignment (VoidMethodCall) would clamp to 5000 instead of 10000.
        Map<String, Object> aattrs = new HashMap<String, Object>();
        aattrs.put(Provisioning.A_zimbraDataSourceMinPollingInterval, "5s");
        Account a = Provisioning.getInstance().createAccount("minlow@example.com", "secret", aattrs);
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraDataSourcePollingInterval, "1s");
        DataSource ds = new DataSource(a, DataSourceType.pop3, "ds1", UUID.randomUUID().toString(),
                dattrs, Provisioning.getInstance());

        // Act / Assert — floored to the 10s safeguard, NOT the 5s account min
        assertEquals(10000L, ds.getPollingInterval());
    }

    @Test
    public void getPollingIntervalMinAboveSafeguardClampsToAccountMin() throws Exception {
        // Arrange — account min polling = 20s, ABOVE the 10s safeguard, so the safeguard branch is
        // not taken and a sub-min request is floored to the 20s account min (20000ms). A 1s request
        // sits strictly between 0 and min, exercising the L256 clamp "if (0 < interval && interval
        // < min) interval = min".
        Map<String, Object> aattrs = new HashMap<String, Object>();
        aattrs.put(Provisioning.A_zimbraDataSourceMinPollingInterval, "20s");
        Account a = Provisioning.getInstance().createAccount("minhigh@example.com", "secret", aattrs);
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraDataSourcePollingInterval, "1s");
        DataSource ds = new DataSource(a, DataSourceType.pop3, "ds1", UUID.randomUUID().toString(),
                dattrs, Provisioning.getInstance());

        // Act / Assert — floored to the 20s account min
        assertEquals(20000L, ds.getPollingInterval());
    }

    @Test
    public void getPollingIntervalAboveMinPassesThroughUnclamped() throws Exception {
        // Arrange — account min polling = 20s, requested interval = 5m (300000ms), which is above
        // the min so the L256 clamp must NOT fire and the value passes through unchanged. This
        // distinguishes the clamp condition: a mutant that always clamps (or negates the bound)
        // would return 20000 instead of 300000.
        Map<String, Object> aattrs = new HashMap<String, Object>();
        aattrs.put(Provisioning.A_zimbraDataSourceMinPollingInterval, "20s");
        Account a = Provisioning.getInstance().createAccount("above@example.com", "secret", aattrs);
        Map<String, Object> dattrs = new HashMap<String, Object>();
        dattrs.put(Provisioning.A_zimbraDataSourcePollingInterval, "5m");
        DataSource ds = new DataSource(a, DataSourceType.pop3, "ds1", UUID.randomUUID().toString(),
                dattrs, Provisioning.getInstance());

        // Act / Assert — unchanged at 300000ms
        assertEquals(300000L, ds.getPollingInterval());
    }

    @Test
    public void getPollingIntervalAccountLevelLegacyIntervalIsMigratedToPop3AndImapAttrs() throws Exception {
        // Arrange — a legacy account-level zimbraDataSourcePollingInterval (5m). When a data source
        // without its own interval reads getPollingInterval(), migratePollingIntervalIfNecessary()
        // (the L216 call) copies the old value into the pop3/imap attrs and clears the old attr.
        // The L272 guard "if (!isNullOrEmpty(oldInterval))" must be true for the account branch.
        ensureDefaultCos();
        Map<String, Object> aattrs = new HashMap<String, Object>();
        aattrs.put(Provisioning.A_zimbraDataSourcePollingInterval, "5m");
        Account a = Provisioning.getInstance().createAccount("migrate@example.com", "secret", aattrs);
        // Precondition — pop3/imap not yet populated, legacy attr present
        assertNull(a.getAttr(Provisioning.A_zimbraDataSourcePop3PollingInterval, false));
        assertEquals("5m", a.getAttr(Provisioning.A_zimbraDataSourcePollingInterval, false));

        DataSource ds = new DataSource(a, DataSourceType.pop3, "ds1", UUID.randomUUID().toString(),
                new HashMap<String, Object>(), Provisioning.getInstance());

        // Act — pop3 type, no per-data-source interval => account default path runs migration first
        long interval = ds.getPollingInterval();

        // Assert — migration moved 5m into pop3 AND imap attrs and cleared the legacy attr; the
        // effective pop3 interval is then 5m (300000ms). Killing the L216 call removal: without
        // migration pop3 stays unset (-1) and getPollingInterval would return 0 here.
        assertEquals("5m", a.getAttr(Provisioning.A_zimbraDataSourcePop3PollingInterval, false));
        assertEquals("5m", a.getAttr(Provisioning.A_zimbraDataSourceImapPollingInterval, false));
        assertTrue("legacy account interval must be cleared after migration",
                a.getAttr(Provisioning.A_zimbraDataSourcePollingInterval, false) == null
                        || a.getAttr(Provisioning.A_zimbraDataSourcePollingInterval, false).isEmpty());
        assertEquals(300000L, interval);
    }

    @Test
    public void getPollingIntervalLegacyCosIntervalIsMigratedToCosPop3AndImapAttrs() throws Exception {
        // Arrange — a COS carrying a legacy zimbraDataSourcePollingInterval (7m), with an account
        // assigned to it. migratePollingIntervalIfNecessary() also migrates the COS value: the
        // L284 guard "if (!isNullOrEmpty(oldInterval))" on the COS branch must be TRUE, copying the
        // value into the COS pop3/imap attrs and clearing the legacy COS attr.
        Map<String, Object> cosAttrs = new HashMap<String, Object>();
        cosAttrs.put(Provisioning.A_zimbraDataSourcePollingInterval, "7m");
        Cos cos = Provisioning.getInstance().createCos("cosmig-" + UUID.randomUUID(), cosAttrs);
        Map<String, Object> aattrs = new HashMap<String, Object>();
        aattrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        Account a = Provisioning.getInstance().createAccount("cosmig@example.com", "secret", aattrs);
        // Precondition — COS pop3 not yet populated, legacy COS attr present
        assertNull(cos.getAttr(Provisioning.A_zimbraDataSourcePop3PollingInterval, false));
        assertEquals("7m", cos.getAttr(Provisioning.A_zimbraDataSourcePollingInterval, false));

        DataSource ds = new DataSource(a, DataSourceType.pop3, "ds1", UUID.randomUUID().toString(),
                new HashMap<String, Object>(), Provisioning.getInstance());

        // Act — triggers migration of both account (none here) and COS values
        ds.getPollingInterval();

        // Assert — the COS legacy value was migrated into the COS pop3/imap attrs and cleared.
        // Killing the L284 NegateConditionals: if that guard were flipped the COS branch would not
        // run and the pop3/imap COS attrs would stay null while the legacy attr stayed "7m".
        assertEquals("7m", cos.getAttr(Provisioning.A_zimbraDataSourcePop3PollingInterval, false));
        assertEquals("7m", cos.getAttr(Provisioning.A_zimbraDataSourceImapPollingInterval, false));
        assertTrue("legacy COS interval must be cleared after migration",
                cos.getAttr(Provisioning.A_zimbraDataSourcePollingInterval, false) == null
                        || cos.getAttr(Provisioning.A_zimbraDataSourcePollingInterval, false).isEmpty());
    }

    @Test
    public void getPollingIntervalNoLegacyAccountIntervalLeavesPop3AttrUnset() throws Exception {
        // Arrange — no legacy account-level interval. The L272 guard "if (!isNullOrEmpty(oldInterval))"
        // must be FALSE so migration does NOT write the pop3 attr. This is the complementary branch
        // to the migration test above; together they pin the L272 NegateConditionals.
        ensureDefaultCos();
        Account a = Provisioning.getInstance().createAccount("nomigrate@example.com", "secret",
                new HashMap<String, Object>());
        DataSource ds = new DataSource(a, DataSourceType.pop3, "ds1", UUID.randomUUID().toString(),
                new HashMap<String, Object>(), Provisioning.getInstance());

        // Act
        long interval = ds.getPollingInterval();

        // Assert — nothing migrated, pop3 attr remains unset, effective interval 0
        assertNull("no legacy value => pop3 attr must stay unset",
                a.getAttr(Provisioning.A_zimbraDataSourcePop3PollingInterval, false));
        assertEquals(0L, interval);
    }

    private static void ensureDefaultCos() throws Exception {
        Provisioning p = Provisioning.getInstance();
        if (p.get(com.zimbra.common.account.Key.CosBy.name, Provisioning.DEFAULT_COS_NAME) == null) {
            p.createCos(Provisioning.DEFAULT_COS_NAME, new HashMap<String, Object>());
        }
    }
}
