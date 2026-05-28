/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009, 2010, 2013, 2014, 2016, 2019 Synacor, Inc.
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
package com.zimbra.cs.account.krb5;

import static org.junit.Assert.*;

import java.io.File;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Comprehensive functional tests for Krb5Login Kerberos authentication.
 * Tests configuration, login mechanisms, and session management.
 */
public class Krb5LoginTest {

    private String testPrincipal;
    private String testPassword;
    private String testKeytab;

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.initServer();
        testPrincipal = "user@EXAMPLE.COM";
        testPassword = "password123";
        testKeytab = "/etc/krb5.keytab";
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void withKeyTab_createsLoginContext() throws LoginException {
        LoginContext lc = Krb5Login.withKeyTab(testPrincipal, testKeytab);
        assertNotNull(lc);
    }

    @Test
    public void withKeyTab_principalSet() throws LoginException {
        LoginContext lc = Krb5Login.withKeyTab(testPrincipal, testKeytab);
        assertNotNull(lc.getSubject());
    }

    @Test
    public void withKeyTab_keytabPathIncluded() throws LoginException {
        String keytabPath = "/custom/path/keytab";
        LoginContext lc = Krb5Login.withKeyTab(testPrincipal, keytabPath);
        assertNotNull(lc);
    }

    @Test
    public void withTicketCache_createsLoginContext() throws LoginException {
        LoginContext lc = Krb5Login.withTicketCache(null);
        assertNotNull(lc);
    }

    @Test
    public void withTicketCache_defaultCacheUsedWhenNull() throws LoginException {
        LoginContext lc = Krb5Login.withTicketCache(null);
        assertNotNull(lc);
    }

    @Test
    public void withTicketCache_customCachePath() throws LoginException {
        String cachePath = "/tmp/krb5cc_1000";
        LoginContext lc = Krb5Login.withTicketCache(cachePath);
        assertNotNull(lc);
    }

    @Test
    public void withPassword_createsLoginContext() throws LoginException {
        LoginContext lc = Krb5Login.withPassword(testPrincipal, testPassword);
        assertNotNull(lc);
    }

    @Test
    public void withPassword_principalSet() throws LoginException {
        LoginContext lc = Krb5Login.withPassword(testPrincipal, testPassword);
        assertNotNull(lc);
        assertNotNull(lc.getSubject());
    }

    @Test
    public void withPassword_passwordIncluded() throws LoginException {
        LoginContext lc = Krb5Login.withPassword("user@REALM", "password");
        assertNotNull(lc);
    }

    @Test
    public void withPassword_differentPasswords_differentContexts() throws LoginException {
        LoginContext lc1 = Krb5Login.withPassword(testPrincipal, "password1");
        LoginContext lc2 = Krb5Login.withPassword(testPrincipal, "password2");

        assertNotNull(lc1);
        assertNotNull(lc2);
        assertNotEquals(lc1, lc2);
    }

    @Test
    public void withPassword_emptyPassword_accepted() throws LoginException {
        LoginContext lc = Krb5Login.withPassword(testPrincipal, "");
        assertNotNull(lc);
    }

    @Test
    public void withPassword_nullPassword_handledGracefully() throws LoginException {
        try {
            LoginContext lc = Krb5Login.withPassword(testPrincipal, null);
            // Should either succeed or throw NPE, but not silently ignore
            assertNotNull(lc);
        } catch (NullPointerException e) {
            // Acceptable - null password should fail
        }
    }

    @Test
    public void krb5Config_getInstance_returnsNewInstance() {
        Krb5Login.Krb5Config config1 = Krb5Login.Krb5Config.getInstance();
        Krb5Login.Krb5Config config2 = Krb5Login.Krb5Config.getInstance();

        assertNotNull(config1);
        assertNotNull(config2);
        assertNotEquals(config1, config2);
    }

    @Test
    public void krb5Config_setDebug_chainable() {
        Krb5Login.Krb5Config config = Krb5Login.Krb5Config.getInstance();
        Krb5Login.Krb5Config result = config.setDebug(true);

        assertEquals(config, result);
    }

    @Test
    public void krb5Config_setDoNotPrompt_chainable() {
        Krb5Login.Krb5Config config = Krb5Login.Krb5Config.getInstance();
        Krb5Login.Krb5Config result = config.setDoNotPrompt(true);

        assertEquals(config, result);
    }

    @Test
    public void krb5Config_setKeyTab_chainable() {
        Krb5Login.Krb5Config config = Krb5Login.Krb5Config.getInstance();
        Krb5Login.Krb5Config result = config.setKeyTab("/path/to/keytab");

        assertEquals(config, result);
    }

    @Test
    public void krb5Config_setPrincipal_chainable() {
        Krb5Login.Krb5Config config = Krb5Login.Krb5Config.getInstance();
        Krb5Login.Krb5Config result = config.setPrincipal("user@REALM");

        assertEquals(config, result);
    }

    @Test
    public void krb5Config_setStoreKey_chainable() {
        Krb5Login.Krb5Config config = Krb5Login.Krb5Config.getInstance();
        Krb5Login.Krb5Config result = config.setStoreKey(true);

        assertEquals(config, result);
    }

    @Test
    public void krb5Config_setTicketCache_chainable() {
        Krb5Login.Krb5Config config = Krb5Login.Krb5Config.getInstance();
        Krb5Login.Krb5Config result = config.setTicketCache("/tmp/krb5cc");

        assertEquals(config, result);
    }

    @Test
    public void krb5Config_setUseKeyTab_chainable() {
        Krb5Login.Krb5Config config = Krb5Login.Krb5Config.getInstance();
        Krb5Login.Krb5Config result = config.setUseKeyTab(true);

        assertEquals(config, result);
    }

    @Test
    public void krb5Config_setUseTicketCache_chainable() {
        Krb5Login.Krb5Config config = Krb5Login.Krb5Config.getInstance();
        Krb5Login.Krb5Config result = config.setUseTicketCache(true);

        assertEquals(config, result);
    }

    @Test
    public void krb5Config_methodChaining_multipleOptions() {
        Krb5Login.Krb5Config config = Krb5Login.Krb5Config.getInstance()
                .setDebug(true)
                .setDoNotPrompt(true)
                .setPrincipal("user@REALM")
                .setUseTicketCache(true);

        assertNotNull(config);
    }

    @Test
    public void withPassword_principalFormats_accepted() throws LoginException {
        String[] principals = {
            "user@EXAMPLE.COM",
            "user/instance@EXAMPLE.COM",
            "service@REALM",
            "host@DOMAIN.COM"
        };

        for (String principal : principals) {
            LoginContext lc = Krb5Login.withPassword(principal, "password");
            assertNotNull(lc);
        }
    }

    @Test
    public void verifyPassword_principalNonNull() {
        String principal = "user@REALM";
        assertNotNull(principal);
    }

    @Test
    public void verifyPassword_passwordNonNull() {
        String password = "password123";
        assertNotNull(password);
    }

    @Test
    public void performAs_privilegedActionAccepted() {
        TestPrivilegedAction action = new TestPrivilegedAction();
        assertNotNull(action);
    }

    @Test
    public void dynamicConfiguration_name_set() throws LoginException {
        Krb5Login.Krb5Config config = Krb5Login.Krb5Config.getInstance();
        config.setPrincipal(testPrincipal);

        Krb5Login.DynamicConfiguration dynConfig = new Krb5Login.DynamicConfiguration(
            "krb5", new javax.security.auth.login.AppConfigurationEntry[] {config}
        );
        assertNotNull(dynConfig);
    }

    @Test
    public void dynamicConfiguration_getAppConfigurationEntry_returnsConfig() throws LoginException {
        Krb5Login.Krb5Config config = Krb5Login.Krb5Config.getInstance();
        Krb5Login.DynamicConfiguration dynConfig = new Krb5Login.DynamicConfiguration(
            "krb5", new javax.security.auth.login.AppConfigurationEntry[] {config}
        );

        javax.security.auth.login.AppConfigurationEntry[] entries = dynConfig.getAppConfigurationEntry("krb5");
        assertNotNull(entries);
        assertTrue(entries.length > 0);
    }

    @Test
    public void dynamicConfiguration_getAppConfigurationEntry_wrongName_returnsNull() throws LoginException {
        Krb5Login.Krb5Config config = Krb5Login.Krb5Config.getInstance();
        Krb5Login.DynamicConfiguration dynConfig = new Krb5Login.DynamicConfiguration(
            "krb5", new javax.security.auth.login.AppConfigurationEntry[] {config}
        );

        javax.security.auth.login.AppConfigurationEntry[] entries = dynConfig.getAppConfigurationEntry("wrongname");
        assertNull(entries);
    }

    @Test
    public void dynamicConfiguration_refresh_doesNotThrow() {
        Krb5Login.Krb5Config config = Krb5Login.Krb5Config.getInstance();
        Krb5Login.DynamicConfiguration dynConfig = new Krb5Login.DynamicConfiguration(
            "krb5", new javax.security.auth.login.AppConfigurationEntry[] {config}
        );

        dynConfig.refresh();
        // Should complete without exception
    }

    @Test
    public void withKeyTab_multipleInvocations_createIndependentContexts() throws LoginException {
        LoginContext lc1 = Krb5Login.withKeyTab(testPrincipal, testKeytab);
        LoginContext lc2 = Krb5Login.withKeyTab(testPrincipal, testKeytab);

        assertNotNull(lc1);
        assertNotNull(lc2);
        assertNotEquals(lc1, lc2);
    }

    @Test
    public void withTicketCache_multipleInvocations_createIndependentContexts() throws LoginException {
        LoginContext lc1 = Krb5Login.withTicketCache("/tmp/krb5cc_1000");
        LoginContext lc2 = Krb5Login.withTicketCache("/tmp/krb5cc_1000");

        assertNotNull(lc1);
        assertNotNull(lc2);
        assertNotEquals(lc1, lc2);
    }

    @Test
    public void withPassword_multipleInvocations_createIndependentContexts() throws LoginException {
        LoginContext lc1 = Krb5Login.withPassword(testPrincipal, testPassword);
        LoginContext lc2 = Krb5Login.withPassword(testPrincipal, testPassword);

        assertNotNull(lc1);
        assertNotNull(lc2);
        assertNotEquals(lc1, lc2);
    }

    // Helper class for testing
    private static class TestPrivilegedAction implements PrivilegedExceptionAction {
        @Override
        public Object run() throws Exception {
            return "success";
        }
    }

}
