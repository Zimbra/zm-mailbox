/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.names;

import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;

/**
 * Unit tests for NameUtil.
 *
 * Tests email address validation, domain name validation, and name parsing.
 */
public class NameUtilTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
    }

    // ========== Email Address Validation Tests ==========

    @Test
    public void validEmailAddress_simpleValidEmail_success() throws ServiceException {
        // Should not throw exception
        NameUtil.validEmailAddress("user@example.com");
    }

    @Test
    public void validEmailAddress_emailWithDot_success() throws ServiceException {
        NameUtil.validEmailAddress("john.smith@example.com");
    }

    @Test
    public void validEmailAddress_emailWithPlus_success() throws ServiceException {
        NameUtil.validEmailAddress("user+tag@example.com");
    }

    @Test
    public void validEmailAddress_emailWithUnderscore_success() throws ServiceException {
        NameUtil.validEmailAddress("user_name@example.com");
    }

    @Test
    public void validEmailAddress_emailWithNumbers_success() throws ServiceException {
        NameUtil.validEmailAddress("user123@example.com");
    }

    @Test
    public void validEmailAddress_emailWithHyphen_success() throws ServiceException {
        NameUtil.validEmailAddress("user-name@example.com");
    }

    @Test
    public void validEmailAddress_longLocalPart_success() throws ServiceException {
        String longLocal = "a".repeat(64); // Max local part is typically 64 chars
        NameUtil.validEmailAddress(longLocal + "@example.com");
    }

    @Test
    public void validEmailAddress_multipleSubdomains_success() throws ServiceException {
        NameUtil.validEmailAddress("user@sub.domain.example.com");
    }

    @Test
    public void validEmailAddress_withoutPersonalName_success() throws ServiceException {
        // Personal name portion should not be allowed
        NameUtil.validEmailAddress("user@example.com");
    }

    @Test
    public void validEmailAddress_missingAtSign_throwsException() throws ServiceException {
        try {
            NameUtil.validEmailAddress("usergexamplecom");
            fail("Should throw ServiceException for missing @");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid email address"));
        }
    }

    @Test
    public void validEmailAddress_missingLocalPart_throwsException() throws ServiceException {
        try {
            NameUtil.validEmailAddress("@example.com");
            fail("Should throw ServiceException for missing local part");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid email address"));
        }
    }

    @Test
    public void validEmailAddress_missingDomain_throwsException() throws ServiceException {
        try {
            NameUtil.validEmailAddress("user@");
            fail("Should throw ServiceException for missing domain");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid email address"));
        }
    }

    @Test
    public void validEmailAddress_emptyString_throwsException() throws ServiceException {
        try {
            NameUtil.validEmailAddress("");
            fail("Should throw ServiceException for empty email");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid email address"));
        }
    }

    @Test
    public void validEmailAddress_invalidCharacters_throwsException() throws ServiceException {
        try {
            NameUtil.validEmailAddress("user name@example.com"); // Space is invalid
            fail("Should throw ServiceException for space in email");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid email address"));
        }
    }

    @Test
    public void validEmailAddress_doubleAtSign_throwsException() throws ServiceException {
        try {
            NameUtil.validEmailAddress("user@@example.com");
            fail("Should throw ServiceException for double @");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid email address"));
        }
    }

    @Test
    public void validEmailAddress_invalidSpecialChar_throwsException() throws ServiceException {
        try {
            NameUtil.validEmailAddress("user#@example.com"); // # is typically invalid
            fail("Should throw ServiceException for # character");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid email address"));
        }
    }

    // ========== Domain Name Validation Tests ==========

    @Test
    public void validNewDomainName_simpleDomain_success() throws ServiceException {
        NameUtil.validNewDomainName("example.com");
    }

    @Test
    public void validNewDomainName_subdomains_success() throws ServiceException {
        NameUtil.validNewDomainName("sub.example.com");
    }

    @Test
    public void validNewDomainName_hyphens_success() throws ServiceException {
        NameUtil.validNewDomainName("my-domain.com");
    }

    @Test
    public void validNewDomainName_numbers_success() throws ServiceException {
        NameUtil.validNewDomainName("domain123.com");
    }

    @Test
    public void validNewDomainName_deepNesting_success() throws ServiceException {
        NameUtil.validNewDomainName("a.b.c.d.e.example.com");
    }

    @Test
    public void validNewDomainName_maxLength_success() throws ServiceException {
        // Create a domain name with 255 characters (max allowed)
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 250) {
            sb.append("label.");
        }
        sb.setLength(255);
        NameUtil.validNewDomainName(sb.toString());
    }

    @Test
    public void validNewDomainName_exceedsMaxLength_throwsException() throws ServiceException {
        // Create domain > 255 chars
        StringBuilder sb = new StringBuilder();
        while (sb.length() < 260) {
            sb.append("toolong");
        }
        String longDomain = sb.toString();

        try {
            NameUtil.validNewDomainName(longDomain);
            fail("Should throw ServiceException for domain exceeding 255 chars");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid domain name") ||
                      e.getMessage().contains("255"));
        }
    }

    @Test
    public void validNewDomainName_noDot_throwsException() throws ServiceException {
        try {
            NameUtil.validNewDomainName("localhost");
            fail("Should throw ServiceException for single-label domain");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid domain name"));
        }
    }

    @Test
    public void validNewDomainName_trailingDot_throwsException() throws ServiceException {
        try {
            NameUtil.validNewDomainName("example.com.");
            fail("Should throw ServiceException for trailing dot");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid domain name"));
        }
    }

    @Test
    public void validNewDomainName_leadingDot_throwsException() throws ServiceException {
        try {
            NameUtil.validNewDomainName(".example.com");
            fail("Should throw ServiceException for leading dot");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid domain name"));
        }
    }

    @Test
    public void validNewDomainName_emptyLabel_throwsException() throws ServiceException {
        try {
            NameUtil.validNewDomainName("example..com"); // Empty label between dots
            fail("Should throw ServiceException for empty label");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid domain name"));
        }
    }

    @Test
    public void validNewDomainName_invalidCharacter_throwsException() throws ServiceException {
        try {
            NameUtil.validNewDomainName("exam ple.com"); // Space is invalid
            fail("Should throw ServiceException for space");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid domain name"));
        }
    }

    @Test
    public void validNewDomainName_labelStartsWithHyphen_throwsException() throws ServiceException {
        try {
            NameUtil.validNewDomainName("-example.com");
            fail("Should throw ServiceException for label starting with hyphen");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid domain name"));
        }
    }

    @Test
    public void validNewDomainName_labelEndsWithHyphen_throwsException() throws ServiceException {
        try {
            NameUtil.validNewDomainName("example-.com");
            fail("Should throw ServiceException for label ending with hyphen");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid domain name"));
        }
    }

    @Test
    public void validNewDomainName_specialCharacter_throwsException() throws ServiceException {
        try {
            NameUtil.validNewDomainName("exam@le.com");
            fail("Should throw ServiceException for @ character");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("invalid domain name"));
        }
    }

    // ========== EmailAddress.getAddress Tests ==========

    @Test
    public void emailAddressGetAddress_validParts_success() {
        String result = NameUtil.EmailAddress.getAddress("user", "example.com");
        assertEquals("user@example.com", result);
    }

    @Test
    public void emailAddressGetAddress_withDotInLocalPart_success() {
        String result = NameUtil.EmailAddress.getAddress("john.smith", "example.com");
        assertEquals("john.smith@example.com", result);
    }

    @Test
    public void emailAddressGetAddress_withPlus_success() {
        String result = NameUtil.EmailAddress.getAddress("user+tag", "example.com");
        assertEquals("user+tag@example.com", result);
    }

    // ========== EmailAddress.getDomainNameFromEmail Tests ==========

    @Test
    public void emailAddressGetDomainNameFromEmail_validEmail_success() throws ServiceException {
        String domain = NameUtil.EmailAddress.getDomainNameFromEmail("user@example.com");
        assertEquals("example.com", domain);
    }

    @Test
    public void emailAddressGetDomainNameFromEmail_noAtSign_throwsException() throws ServiceException {
        try {
            NameUtil.EmailAddress.getDomainNameFromEmail("usergexamplecom");
            fail("Should throw ServiceException for missing domain");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("must be valid email address"));
        }
    }

    @Test
    public void emailAddressGetDomainNameFromEmail_subdomain_success() throws ServiceException {
        String domain = NameUtil.EmailAddress.getDomainNameFromEmail("user@mail.example.com");
        assertEquals("mail.example.com", domain);
    }

    // ========== EmailAddress.getLocalPartFromEmail Tests ==========

    @Test
    public void emailAddressGetLocalPartFromEmail_validEmail_success() throws ServiceException {
        String localPart = NameUtil.EmailAddress.getLocalPartFromEmail("user@example.com");
        assertEquals("user", localPart);
    }

    @Test
    public void emailAddressGetLocalPartFromEmail_withDot_success() throws ServiceException {
        String localPart = NameUtil.EmailAddress.getLocalPartFromEmail("john.smith@example.com");
        assertEquals("john.smith", localPart);
    }

    @Test
    public void emailAddressGetLocalPartFromEmail_noAtSign_throwsException() throws ServiceException {
        try {
            NameUtil.EmailAddress.getLocalPartFromEmail("usergexamplecom");
            fail("Should throw ServiceException for missing domain");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("must be valid email address"));
        }
    }

    // ========== EmailAddress Constructor Tests ==========

    @Test
    public void emailAddressConstructor_validEmail_success() throws ServiceException {
        NameUtil.EmailAddress email = new NameUtil.EmailAddress("user@example.com");
        assertEquals("user", email.getLocalPart());
        assertEquals("example.com", email.getDomain());
    }

    @Test
    public void emailAddressConstructor_emailWithDot_success() throws ServiceException {
        NameUtil.EmailAddress email = new NameUtil.EmailAddress("john.smith@example.com");
        assertEquals("john.smith", email.getLocalPart());
        assertEquals("example.com", email.getDomain());
    }

    @Test
    public void emailAddressConstructor_noDomain_strictFalse_success() throws ServiceException {
        NameUtil.EmailAddress email = new NameUtil.EmailAddress("user", false);
        assertEquals("user", email.getLocalPart());
        assertNull(email.getDomain());
    }

    @Test
    public void emailAddressConstructor_noDomain_strictTrue_throwsException() throws ServiceException {
        try {
            new NameUtil.EmailAddress("user", true);
            fail("Should throw ServiceException when domain is missing and strict=true");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("must be valid email address"));
        }
    }

    @Test
    public void emailAddressConstructor_emptyString_strictFalse_success() throws ServiceException {
        NameUtil.EmailAddress email = new NameUtil.EmailAddress("", false);
        assertEquals("", email.getLocalPart());
        assertNull(email.getDomain());
    }

    @Test
    public void emailAddressConstructor_emptyDomain_strictTrue_throwsException() throws ServiceException {
        try {
            new NameUtil.EmailAddress("user@", true);
            fail("Should throw ServiceException for empty domain with strict=true");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().contains("must be valid email address"));
        }
    }

    @Test
    public void emailAddressConstructor_localPartEmpty_success() throws ServiceException {
        // Strict mode but domain is provided, so should succeed
        NameUtil.EmailAddress email = new NameUtil.EmailAddress("@example.com", true);
        assertEquals("", email.getLocalPart());
        assertEquals("example.com", email.getDomain());
    }

    @Test
    public void emailAddressConstructor_multipleAtSigns_success() throws ServiceException {
        // Email parser takes first @ as separator
        NameUtil.EmailAddress email = new NameUtil.EmailAddress("user@@example.com", false);
        assertEquals("user", email.getLocalPart());
        assertEquals("@example.com", email.getDomain());
    }

    // ========== Boundary Conditions ==========

    @Test
    public void emailAddressConstructor_singleCharLocalPart_success() throws ServiceException {
        NameUtil.EmailAddress email = new NameUtil.EmailAddress("a@example.com");
        assertEquals("a", email.getLocalPart());
        assertEquals("example.com", email.getDomain());
    }

    @Test
    public void emailAddressConstructor_singleCharDomain_success() throws ServiceException {
        // Note: This might fail domain validation but parser should handle it
        NameUtil.EmailAddress email = new NameUtil.EmailAddress("user@a", false);
        assertEquals("user", email.getLocalPart());
        assertEquals("a", email.getDomain());
    }

    @Test
    public void validEmailAddress_longEmail_success() throws ServiceException {
        String longLocal = "a".repeat(64);
        String longDomain = "subdomain." + "a".repeat(63) + ".example.com";
        NameUtil.validEmailAddress(longLocal + "@" + longDomain);
    }

    // ========== Special Email Format Tests ==========

    @Test
    public void validEmailAddress_localPartWithAllowedSpecialChars_success() throws ServiceException {
        // These are typically allowed in local part
        NameUtil.validEmailAddress("user.name+tag@example.com");
    }

    @Test
    public void validEmailAddress_localPartWithAllowedNumbers_success() throws ServiceException {
        NameUtil.validEmailAddress("user123@example.com");
    }

    @Test
    public void validEmailAddress_localPartWithUnderscore_success() throws ServiceException {
        NameUtil.validEmailAddress("user_name@example.com");
    }

    @Test
    public void emailAddressGetAddress_roundTrip_preservesValues() throws ServiceException {
        String original = "test@example.com";
        String localPart = NameUtil.EmailAddress.getLocalPartFromEmail(original);
        String domain = NameUtil.EmailAddress.getDomainNameFromEmail(original);
        String reconstructed = NameUtil.EmailAddress.getAddress(localPart, domain);
        assertEquals(original, reconstructed);
    }
}
