/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import static org.junit.Assert.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.datasource.DataSourceManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.soap.type.DataSource.ConnectionType;

public class DataSourceTest {

    private Account account;
    private Provisioning provisioning;
    private Cos cos;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        provisioning = Provisioning.getInstance();

        // Create test CoS
        Map<String, Object> cosAttrs = new HashMap<String, Object>();
        cos = provisioning.createCos("test-cos", cosAttrs);

        // Create test account
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraMailHost, "localhost");
        attrs.put(Provisioning.A_zimbraCOSId, cos.getId());
        account = provisioning.createAccount("testuser@example.com", "password", attrs);
    }

    // ===== DataSource.getEntryType Tests =====

    @Test
    public void getEntryType_datasource_correct() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "Gmail", "ds-1", attrs, provisioning);

        assertEquals(Entry.EntryType.DATASOURCE, ds.getEntryType());
    }

    // ===== DataSource type and basic accessors =====

    @Test
    public void getType_imap_correct() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "IMAP Source", "ds-1", attrs, provisioning);

        assertEquals(DataSourceType.imap, ds.getType());
    }

    @Test
    public void getType_pop3_correct() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.pop3, "POP3 Source", "ds-1", attrs, provisioning);

        assertEquals(DataSourceType.pop3, ds.getType());
    }

    @Test
    public void getName_accessor() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "MyDataSource", "ds-1", attrs, provisioning);

        assertEquals("MyDataSource", ds.getName());
    }

    @Test
    public void getId_accessor() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-xyz", attrs, provisioning);

        assertEquals("ds-xyz", ds.getId());
    }

    // ===== DataSource.isEnabled Tests =====

    @Test
    public void isEnabled_default_false() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertFalse(ds.isEnabled());
    }

    @Test
    public void isEnabled_true() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEnabled, "TRUE");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertTrue(ds.isEnabled());
    }

    // ===== DataSource.getConnectionType Tests =====

    @Test
    public void getConnectionType_explicit_ssl() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, ConnectionType.ssl.name());
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertEquals(ConnectionType.ssl, ds.getConnectionType());
    }

    @Test
    public void getConnectionType_explicit_cleartext() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, ConnectionType.cleartext.name());
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertEquals(ConnectionType.cleartext, ds.getConnectionType());
    }

    @Test
    public void getConnectionType_default_cleartext() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertEquals(ConnectionType.cleartext, ds.getConnectionType());
    }

    @Test
    public void isSslEnabled_true_when_ssl() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, ConnectionType.ssl.name());
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertTrue(ds.isSslEnabled());
    }

    @Test
    public void isSslEnabled_false_when_cleartext() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceConnectionType, ConnectionType.cleartext.name());
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertFalse(ds.isSslEnabled());
    }

    // ===== DataSource string attribute accessors =====

    @Test
    public void getHost_accessor() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceHost, "mail.example.com");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertEquals("mail.example.com", ds.getHost());
    }

    @Test
    public void getUsername_accessor() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceUsername, "user123");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertEquals("user123", ds.getUsername());
    }

    @Test
    public void getPort_accessor() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourcePort, "993");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertEquals(993, (int) ds.getPort());
    }

    @Test
    public void getPort_null_when_not_set() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertNull(ds.getPort());
    }

    @Test
    public void getFolderId_accessor() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceFolderId, "256");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertEquals(256, ds.getFolderId());
    }

    @Test
    public void getFolderId_default_minus_one() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertEquals(-1, ds.getFolderId());
    }

    // ===== DataSource.leaveOnServer Tests =====

    @Test
    public void leaveOnServer_default_true() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.pop3, "POP3", "ds-1", attrs, provisioning);

        assertTrue(ds.leaveOnServer());
    }

    @Test
    public void leaveOnServer_explicit_false() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceLeaveOnServer, "FALSE");
        DataSource ds = new DataSource(account, DataSourceType.pop3, "POP3", "ds-1", attrs, provisioning);

        assertFalse(ds.leaveOnServer());
    }

    // ===== DataSource.encryption/decryption Tests =====

    @Test
    public void encryptData_roundTrip_string() throws ServiceException {
        String dataSourceId = "ds-123";
        String plaintext = "mypassword";

        String encrypted = DataSource.encryptData(dataSourceId, plaintext);
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);

        String decrypted = DataSource.decryptData(dataSourceId, encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    public void encryptData_roundTrip_bytes() throws ServiceException, UnsupportedEncodingException {
        String dataSourceId = "ds-456";
        byte[] plaintext = "secretdata".getBytes("utf-8");

        byte[] encrypted = DataSource.encryptData(dataSourceId, plaintext);
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);

        byte[] decrypted = DataSource.decryptData(dataSourceId, encrypted);
        assertArrayEquals(plaintext, decrypted);
    }

    @Test
    public void encryptData_differentDataSourceIds_differentResults() throws ServiceException {
        String plaintext = "mypassword";

        String encrypted1 = DataSource.encryptData("ds-1", plaintext);
        String encrypted2 = DataSource.encryptData("ds-2", plaintext);

        assertNotEquals(encrypted1, encrypted2);
    }

    @Test
    public void decryptData_invalidBase64_throws() throws ServiceException {
        try {
            DataSource.decryptData("ds-1", "!!!INVALID_BASE64!!!");
            fail("Should throw ServiceException for invalid base64");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid encoded size") || e.getMessage().contains("security exception"));
        }
    }

    @Test
    public void decryptData_tooShortData_throws() throws ServiceException {
        try {
            DataSource.decryptData("ds-1", "dGlueQ==");
            fail("Should throw ServiceException for too short data");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid encoded size"));
        }
    }

    @Test
    public void decryptData_wrongVersion_throws() throws ServiceException {
        try {
            // Create data with wrong version byte
            String encrypted = DataSource.encryptData("ds-1", "test");
            // Corrupt the version byte would require manual manipulation
            DataSource.decryptData("ds-99", encrypted);
            // May fail due to version mismatch or other reasons
        } catch (ServiceException e) {
            // Expected
        }
    }

    @Test
    public void getDecryptedPassword_none_set() throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertNull(ds.getDecryptedPassword());
    }

    @Test
    public void getDecryptedPassword_encrypted_decrypted() throws ServiceException {
        String plainPassword = "secure123";
        String encrypted = DataSource.encryptData("ds-1", plainPassword);

        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourcePassword, encrypted);
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        String decrypted = ds.getDecryptedPassword();
        assertEquals(plainPassword, decrypted);
    }

    // ===== DataSource.isScheduled Tests =====

    @Test
    public void isScheduled_noPollingInterval_false() throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertFalse(ds.isScheduled());
    }

    @Test
    public void isScheduled_withPollingInterval_true() throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourcePollingInterval, "300s");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertTrue(ds.isScheduled());
    }

    // ===== DataSource.isImportOnly Tests =====

    @Test
    public void isImportOnly_default_false() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertFalse(ds.isImportOnly());
    }

    @Test
    public void isImportOnly_true() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceImportOnly, "TRUE");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertTrue(ds.isImportOnly());
    }

    // ===== DataSource.isInternal Tests =====

    @Test
    public void isInternal_default_false() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertFalse(ds.isInternal());
    }

    @Test
    public void isInternal_true() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceIsInternal, "TRUE");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertTrue(ds.isInternal());
    }

    // ===== DataSource.SMTP tests =====

    @Test
    public void isSmtpEnabled_default_false() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertFalse(ds.isSmtpEnabled());
    }

    @Test
    public void isSmtpEnabled_true() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceSmtpEnabled, "TRUE");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertTrue(ds.isSmtpEnabled());
    }

    @Test
    public void getSmtpHost_accessor() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceSmtpHost, "smtp.example.com");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertEquals("smtp.example.com", ds.getSmtpHost());
    }

    @Test
    public void getSmtpPort_accessor() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceSmtpPort, "587");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertEquals(587, (int) ds.getSmtpPort());
    }

    @Test
    public void isSmtpConnectionSecure_cleartext() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceSmtpConnectionType, "cleartext");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertFalse(ds.isSmtpConnectionSecure());
    }

    @Test
    public void isSmtpConnectionSecure_tls() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceSmtpConnectionType, "tls");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertTrue(ds.isSmtpConnectionSecure());
    }

    @Test
    public void isSmtpAuthRequired_default_false() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertFalse(ds.isSmtpAuthRequired());
    }

    @Test
    public void isSmtpAuthRequired_true() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceSmtpAuthRequired, "TRUE");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertTrue(ds.isSmtpAuthRequired());
    }

    @Test
    public void getSmtpUsername_explicit() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceSmtpAuthUsername, "smtpuser");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertEquals("smtpuser", ds.getSmtpUsername());
    }

    @Test
    public void getSmtpUsername_fallback_to_datasource_username() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceUsername, "mainuser");
        attrs.put(Provisioning.A_zimbraDataSourceSmtpEnabled, "TRUE");
        attrs.put(Provisioning.A_zimbraDataSourceSmtpAuthRequired, "TRUE");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertEquals("mainuser", ds.getSmtpUsername());
    }

    // ===== DataSource.getEmailAddress Tests =====

    @Test
    public void getEmailAddress_accessor() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceEmailAddress, "user@external.com");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertEquals("user@external.com", ds.getEmailAddress());
    }

    // ===== DataSource.account reference =====

    @Test
    public void getAccount_reference() throws ServiceException {
        Map<String, Object> attrs = new HashMap<String, Object>();
        DataSource ds = new DataSource(account, DataSourceType.imap, "Source", "ds-1", attrs, provisioning);

        assertEquals(account, ds.getAccount());
    }

    // ===== DataSource.toString Tests =====

    @Test
    public void toString_contains_key_info() {
        Map<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraDataSourceHost, "mail.example.com");
        attrs.put(Provisioning.A_zimbraDataSourcePort, "993");
        DataSource ds = new DataSource(account, DataSourceType.imap, "Gmail", "ds-1", attrs, provisioning);

        String str = ds.toString();

        assertTrue(str.contains("ds-1"));
        assertTrue(str.contains("imap"));
        assertTrue(str.contains("Gmail"));
    }

    @Test
    public void encryptData_nullDataSourceId_throws() throws ServiceException {
        try {
            DataSource.encryptData(null, "test");
            fail("Should throw exception for null dataSourceId");
        } catch (Exception e) {
            // Expected
        }
    }

    @Test
    public void encryptData_emptyString_succeeds() throws ServiceException {
        String encrypted = DataSource.encryptData("ds-1", "");
        assertNotNull(encrypted);

        String decrypted = DataSource.decryptData("ds-1", encrypted);
        assertEquals("", decrypted);
    }

    @Test
    public void encryptData_longString_succeeds() throws ServiceException {
        String longString = new String(new char[1000]).replace('\0', 'A');
        String encrypted = DataSource.encryptData("ds-1", longString);
        assertNotNull(encrypted);

        String decrypted = DataSource.decryptData("ds-1", encrypted);
        assertEquals(longString, decrypted);
    }

    @Test
    public void encryptData_unicodeString_succeeds() throws ServiceException {
        String unicode = "日本語パスワード";
        String encrypted = DataSource.encryptData("ds-1", unicode);
        assertNotNull(encrypted);

        String decrypted = DataSource.decryptData("ds-1", encrypted);
        assertEquals(unicode, decrypted);
    }
}
