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

/*
 * ZBUG-5664 - unit tests for the "too common" password check
 * (zimbraPasswordBlockCommonEnabled / common-password bloom filter).
 *
 * Standalone test class created solely for the ZBUG-5664 cases
 * (QA TC1-TC6 and TC12-TC15). Transport-level cases (QA TC7-TC11:
 * Admin Console UI, Modern & Classic Web Client, SOAP SetPassword /
 * ChangePassword) are owned by the QA automation suite and are
 * intentionally NOT present here.
 */
package com.zimbra.cs.account.ldap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.unboundid.InMemoryLdapServer;

public class LdapProvisioningTest {

    /* ==================== test support ==================== */

    private static final String DICT_PATH =
            System.getProperty("java.io.tmpdir") + "/zbug5664_common_passwords.txt";
    private static final String DOMAIN_NAME = "zbug5664.local";

    private static Provisioning prov;
    private static int seq = 0;

    static {
        // Test dictionary. The ticket password and the passphrase are listed
        // ON PURPOSE: pre-fix code rejects them (bloom hit), post-fix code
        // must accept them (length > 20 bypasses the bloom check).
        try {
            Files.write(Paths.get(DICT_PATH), Arrays.asList(
                            "password123",
                            "admin@123",
                            "welcome123",
                            "1234567890",
                            "Password123!",
                            "q8W]]PR?Diy`}5#Y,ArhjUl9H",
                            "correcthorsebatterystaple123!]]"),
                    StandardCharsets.UTF_8);
            // Must be applied BEFORE the LdapProvisioning singleton builds the
            // bloom filter.
            LC.common_passwords_txt.setDefault(DICT_PATH);
        } catch (Exception e) {
            throw new RuntimeException("cannot prepare test password dictionary", e);
        }
    }

    @BeforeClass
    public static void setUp() throws Exception {
        // Enable in-memory LDAP mode so Provisioning.getInstance() returns
        // the LdapProvisioning wired to the in-memory server.
        DebugConfig.useInMemoryLdapServer = true;

        InMemoryLdapServer.start(
                InMemoryLdapServer.ZIMBRA_LDAP_SERVER,
                new InMemoryLdapServer.ServerConfig());

        prov = Provisioning.getInstance();

        try {
            prov.createCos(Provisioning.DEFAULT_COS_NAME, null);
        } catch (ServiceException e) {
            // default cos already present in this LDAP instance
        }

        Map<String, Object> dAttrs = new HashMap<>();
        dAttrs.put(Provisioning.A_zimbraPasswordBlockCommonEnabled, "TRUE");
        dAttrs.put(Provisioning.A_zimbraPasswordMinLength, "6");
        dAttrs.put(Provisioning.A_zimbraPasswordMinUpperCaseChars, "0");
        dAttrs.put(Provisioning.A_zimbraPasswordMinLowerCaseChars, "0");
        dAttrs.put(Provisioning.A_zimbraPasswordMinDigitsOrPuncs, "0");
        dAttrs.put(Provisioning.A_zimbraPasswordMaxLength, "128");
        prov.createDomain(DOMAIN_NAME, dAttrs);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        InMemoryLdapServer.stop(InMemoryLdapServer.ZIMBRA_LDAP_SERVER);
    }

    private static String uniqueName() {
        return "zbug5664-" + (++seq) + "@" + DOMAIN_NAME;
    }

    private static void assertAccepted(String password) throws Exception {
        prov.createAccount(uniqueName(), password, null);
    }

    private static void assertTooCommon(String password) {
        try {
            prov.createAccount(uniqueName(), password, null);
            fail("expected account.INVALID_PASSWORD (too common) for: " + password);
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.INVALID_PASSWORD, e.getCode());
            assertTrue("expected 'too common' in: " + e.getMessage(),
                    e.getMessage().contains("too common"));
        }
    }

    /* ==================== test cases ==================== */

    /** QA TC1 - JIRA regression: 25-char password containing "]]" must be accepted. */
    @Test
    public void testTicketPasswordLongWithBracketsAccepted() throws Exception {
        assertAccepted("q8W]]PR?Diy`}5#Y,ArhjUl9H");
    }

    /** QA TC2 - 21 chars: above the guard threshold, bloom check skipped. */
    @Test
    public void testLength21BypassesCommonCheck() throws Exception {
        assertAccepted("aB3#kL9]]mN5@pQ7$rT1!");
    }

    /** QA TC3 - exactly 20 chars: still evaluated by filter; non-common accepted. */
    @Test
    public void testLength20NonCommonAccepted() throws Exception {
        assertAccepted("aB3#kL9]]mN5@pQ7$rT1");
    }

    /** QA TC4 - 19 chars, non-common, contains "]]": accepted. */
    @Test
    public void testLength19NonCommonAccepted() throws Exception {
        assertAccepted("P@ss]]w0rd!2026_XyZ");
    }

    /** QA TC5 - security: real common passwords (<=20) must STILL be blocked. */
    @Test
    public void testCommonPasswordsBlockedWhenEnabled() throws Exception {
        assertTooCommon("password123");
        assertTooCommon("admin@123");
        assertTooCommon("welcome123");
        assertTooCommon("1234567890");
    }

    /** QA TC6 - feature disabled at account level: common password accepted. */
    @Test
    public void testCommonAcceptedWhenFeatureDisabled() throws Exception {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraPasswordBlockCommonEnabled, "FALSE");
        prov.createAccount(uniqueName(), "Password123!", attrs);
    }

    /** QA TC12 - long passphrase made of common words accepted (>20 bypass). */
    @Test
    public void testLongPassphraseOfCommonWordsAccepted() throws Exception {
        assertAccepted("correcthorsebatterystaple123!]]");
    }

    /** QA TC13 - max-length boundaries (64 and 128 chars) accepted. */
    @Test
    public void testMaxLengthPasswordsAccepted() throws Exception {
        String unit = "aB3#kL9]]mN5@pQ7$rT1!";                                        // 21 chars
        String p64  = String.join("", java.util.Collections.nCopies(3, unit)) + "X";  // 64
        String p128 = String.join("", java.util.Collections.nCopies(6, unit)) + "Xy"; // 128
        assertAccepted(p64);
        assertAccepted(p128);
    }

    /** QA TC14 - other password policies still enforced on >20-char passwords. */
    @Test
    public void testOtherPoliciesStillEnforcedOnLongPasswords() throws Exception {
        Map<String, Object> dAttrs = new HashMap<>();
        dAttrs.put(Provisioning.A_zimbraPasswordBlockCommonEnabled, "TRUE");
        dAttrs.put(Provisioning.A_zimbraPasswordMinUpperCaseChars, "1");
        dAttrs.put(Provisioning.A_zimbraPasswordMinDigitsOrPuncs, "2");
        prov.createDomain("zbug5664-policy.local", dAttrs);

        // compliant long password: accepted
        prov.createAccount("ok@zbug5664-policy.local", "ValidLongP@ssword123]]!", null);

        // non-compliant long password: must fail on COMPLEXITY, not on "too common"
        try {
            prov.createAccount("bad@zbug5664-policy.local",
                    "validlongpasswordwithoutpuncsordigits", null);
            fail("expected complexity violation for non-compliant long password");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.INVALID_PASSWORD, e.getCode());
            assertFalse("must be a complexity error, not common-check: " + e.getMessage(),
                    e.getMessage().contains("too common"));
        }
    }

    /** QA TC15 - domain-level override of zimbraPasswordBlockCommonEnabled. */
    @Test
    public void testDomainLevelOverride() throws Exception {
        Map<String, Object> off = new HashMap<>();
        off.put(Provisioning.A_zimbraPasswordBlockCommonEnabled, "FALSE");
        prov.createDomain("zbug5664-off.local", off);

        Map<String, Object> on = new HashMap<>();
        on.put(Provisioning.A_zimbraPasswordBlockCommonEnabled, "TRUE");
        prov.createDomain("zbug5664-on.local", on);

        // domain override FALSE: common password accepted
        prov.createAccount("u1@zbug5664-off.local", "password123", null);

        // domain override TRUE: common blocked, long "]]" password accepted
        try {
            prov.createAccount("u2@zbug5664-on.local", "password123", null);
            fail("expected too common on domain with override TRUE");
        } catch (ServiceException e) {
            assertEquals(AccountServiceException.INVALID_PASSWORD, e.getCode());
        }
        prov.createAccount("u3@zbug5664-on.local", "q8W]]PR?Diy`}5#Y,ArhjUl9H", null);
    }
}