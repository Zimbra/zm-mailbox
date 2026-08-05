/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.account.names;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.names.NameUtil.EmailAddress;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link NameUtil} and its inner {@link NameUtil.EmailAddress} parser.
 *
 * <p>Most methods are pure string/validation logic. {@link NameUtil#validNewDomainName} reaches into
 * {@code Provisioning.getInstance().getConfig()} for the non-LDH policy flag, so the in-memory
 * MockProvisioning harness is booted and the real {@link Config} entry is mutated to drive both
 * sides of that branch (default allow-non-LDH = true, and the restricted false case).
 */
public class NameUtilTest {

    private Provisioning prov;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MailboxTestUtil.initServer();
    }

    @Before
    public void setUp() throws Exception {
        prov = Provisioning.getInstance();
        // reset the non-LDH policy to its documented default before each test
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAllowNonLDHCharsInDomain, "TRUE");
        prov.modifyAttrs(prov.getConfig(), attrs);
    }

    // ---------- validEmailAddress ----------

    @Test
    public void validEmailAddressPlainAddressPasses() throws Exception {
        // Arrange / Act - a bare addr-spec with no personal part is valid
        NameUtil.validEmailAddress("user@example.com");

        // Assert - reaching here without exception is the contract; confirm via re-parse
        assertEquals("example.com", EmailAddress.getDomainNameFromEmail("user@example.com"));
    }

    @Test
    public void validEmailAddressWithPersonalNameThrowsInvalidRequest() throws Exception {
        // Arrange - "Name <addr>" form carries a personal part, which is rejected
        try {
            // Act
            NameUtil.validEmailAddress("Display Name <user@example.com>");
            fail("expected ServiceException for address carrying a personal name");
        } catch (ServiceException e) {
            // Assert
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("invalid email address"));
        }
    }

    @Test
    public void validEmailAddressMalformedAddressThrowsInvalidRequest() throws Exception {
        // Arrange - a string that fails strict InternetAddress parsing
        try {
            // Act
            NameUtil.validEmailAddress("not a valid address @@");
            fail("expected ServiceException for malformed address");
        } catch (ServiceException e) {
            // Assert
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("invalid email address"));
        }
    }

    // ---------- validNewDomainName ----------

    @Test
    public void validNewDomainNameSimpleNamePasses() throws Exception {
        // Arrange / Act - a normal ASCII domain under the length cap, allow-non-LDH default true
        NameUtil.validNewDomainName("example.com");

        // Assert - no exception; sanity-check the policy flag the method read
        assertTrue("default policy must allow non-LDH",
                prov.getConfig().getBooleanAttr(Provisioning.A_zimbraAllowNonLDHCharsInDomain, true));
    }

    @Test
    public void validNewDomainNameExceedsMaxLengthThrowsInvalidRequest() throws Exception {
        // Arrange - 256-char domain exceeds the 255 RFC-1035 cap
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            sb.append('a');
        }
        try {
            // Act
            NameUtil.validNewDomainName(sb.toString());
            fail("expected ServiceException for over-length domain");
        } catch (ServiceException e) {
            // Assert
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("cannot exceed"));
        }
    }

    @Test
    public void validNewDomainNameExactlyMaxLengthPasses() throws Exception {
        // Arrange - a domain of EXACTLY 255 chars sits on the boundary: the guard is
        // "length() > 255", so 255 must pass while 256 (tested above) must fail. This pins the
        // ConditionalsBoundary at the length check (> vs >=). Build a 255-char dotted label
        // domain ("a.a.a..." -> 128 'a' separated by 127 dots = 255) so it also survives
        // strict email parsing.
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 128; i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append('a');
        }
        String domain = sb.toString();
        assertEquals("test fixture must be exactly the max length", 255, domain.length());

        // Act - must NOT throw at exactly 255
        NameUtil.validNewDomainName(domain);

        // Assert - reached here without exception; confirm the boundary fixture really is 255
        assertEquals(255, domain.length());
    }

    @Test
    public void validNewDomainNameInvalidAddressFormThrowsInvalidRequest() throws Exception {
        // Arrange - a domain that makes "test@<domain>" fail email validation
        try {
            // Act
            NameUtil.validNewDomainName("bad domain with spaces");
            fail("expected ServiceException for domain that fails email validation");
        } catch (ServiceException e) {
            // Assert
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("invalid domain name"));
        }
    }

    @Test
    public void validNewDomainNameNonLDHCharsWhenDisallowedThrowsInvalidRequest() throws Exception {
        // Arrange - turn off the non-LDH policy, then use a domain with an underscore (non-LDH).
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAllowNonLDHCharsInDomain, "FALSE");
        prov.modifyAttrs(prov.getConfig(), attrs);
        assertFalse("policy must now disallow non-LDH",
                prov.getConfig().getBooleanAttr(Provisioning.A_zimbraAllowNonLDHCharsInDomain, true));

        try {
            // Act - "test@bad_domain.com" is rejected by strict InternetAddress parsing because
            // the underscore is an illegal domain character, so validNewDomainName fails on the
            // email-validation step (this guards the input before the non-LDH policy check).
            NameUtil.validNewDomainName("bad_domain.com");
            fail("expected ServiceException for a domain that fails email validation");
        } catch (ServiceException e) {
            // Assert - the email-validation wrapper reports an invalid domain name
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue("expected invalid-domain-name message but was: " + e.getMessage(),
                    e.getMessage().contains("invalid domain name"));
        }
    }

    @Test
    public void validNewDomainNameLdhOnlyWhenDisallowedPasses() throws Exception {
        // Arrange - policy off, but a pure letters/dots domain has no non-LDH chars
        HashMap<String, Object> attrs = new HashMap<String, Object>();
        attrs.put(Provisioning.A_zimbraAllowNonLDHCharsInDomain, "FALSE");
        prov.modifyAttrs(prov.getConfig(), attrs);

        // Act - must pass the containsNonLDH check (dots are skipped)
        NameUtil.validNewDomainName("clean.example.com");

        // Assert
        Config config = prov.getConfig();
        assertFalse(config.getBooleanAttr(Provisioning.A_zimbraAllowNonLDHCharsInDomain, true));
    }

    // ---------- EmailAddress ----------

    @Test
    public void emailAddressLocalAndDomainSplitsCorrectly() throws Exception {
        // Arrange / Act
        EmailAddress email = new EmailAddress("john@example.com");

        // Assert - both parts parsed
        assertEquals("john", email.getLocalPart());
        assertEquals("example.com", email.getDomain());
    }

    @Test
    public void emailAddressNoAtSignNonStrictKeepsLocalPartAndNullDomain() throws Exception {
        // Arrange / Act - non-strict mode tolerates a missing domain
        EmailAddress email = new EmailAddress("justlocal", false);

        // Assert
        assertEquals("justlocal", email.getLocalPart());
        assertNull("domain must be null when no @ present", email.getDomain());
    }

    @Test
    public void emailAddressNoAtSignStrictThrowsInvalidRequest() throws Exception {
        // Arrange - strict (default) mode requires a domain
        try {
            // Act
            new EmailAddress("justlocal");
            fail("expected ServiceException for missing domain in strict mode");
        } catch (ServiceException e) {
            // Assert
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("must be valid email address"));
        }
    }

    @Test
    public void emailAddressTrailingAtSignStrictThrowsInvalidRequest() throws Exception {
        // Arrange - "user@" yields an empty domain, rejected by the strict null/empty check
        try {
            // Act
            new EmailAddress("user@", true);
            fail("expected ServiceException for empty domain in strict mode");
        } catch (ServiceException e) {
            // Assert
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
            assertTrue(e.getMessage().contains("must be valid email address"));
        }
    }

    @Test
    public void getAddressLocalAndDomainJoinsWithAtSign() {
        // Arrange / Act
        String addr = EmailAddress.getAddress("alice", "example.org");

        // Assert
        assertEquals("alice@example.org", addr);
    }

    @Test
    public void getDomainNameFromEmailValidAddressReturnsDomain() throws Exception {
        // Arrange / Act
        String domain = EmailAddress.getDomainNameFromEmail("bob@mail.example.net");

        // Assert
        assertEquals("mail.example.net", domain);
    }

    @Test
    public void getLocalPartFromEmailValidAddressReturnsLocalPart() throws Exception {
        // Arrange / Act
        String local = EmailAddress.getLocalPartFromEmail("carol@example.com");

        // Assert
        assertEquals("carol", local);
    }

    @Test
    public void getDomainNameFromEmailNoAtSignThrowsInvalidRequest() throws Exception {
        // Arrange - underlying EmailAddress strict ctor rejects a missing domain
        try {
            // Act
            EmailAddress.getDomainNameFromEmail("nodomain");
            fail("expected ServiceException for address with no domain");
        } catch (ServiceException e) {
            // Assert
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    // ---------- isDot (private) ----------
    //
    // isDot() and containsNonLDH() are unreachable through validNewDomainName() with inputs that
    // exercise their interesting branches, because strict InternetAddress parsing rejects every
    // non-ASCII-LDH domain (including the unicode dot characters) BEFORE the non-LDH policy check
    // is reached. To pin the conditional/return mutations in those private helpers we invoke them
    // directly via reflection (same-package test).

    private static boolean invokeIsDot(int c) throws Exception {
        Method m = NameUtil.class.getDeclaredMethod("isDot", int.class);
        m.setAccessible(true);
        return ((Boolean) m.invoke(null, Integer.valueOf(c))).booleanValue();
    }

    private static boolean invokeContainsNonLDH(String s) throws Exception {
        Method m = NameUtil.class.getDeclaredMethod("containsNonLDH", String.class);
        m.setAccessible(true);
        return ((Boolean) m.invoke(null, s)).booleanValue();
    }

    @Test
    public void isDotRecognizesAllFourDotCharsAndRejectsNonDots() throws Exception {
        // Assert - the four recognized dot code points return true (kills the NegateConditionals
        // on the if at L32 and the BooleanTrueReturn on the true branch at L35: if either side were
        // wrong these would not all be true).
        assertTrue("ASCII full stop is a dot", invokeIsDot('.'));
        assertTrue("ideographic full stop is a dot", invokeIsDot(0x3002));
        assertTrue("fullwidth full stop is a dot", invokeIsDot(0xff0e));
        assertTrue("halfwidth ideographic full stop is a dot", invokeIsDot(0xff61));

        // Assert - ordinary characters are NOT dots (kills a mutant that returns true for the else
        // branch / negates the conditional so non-dots are wrongly classified).
        assertFalse("letter is not a dot", invokeIsDot('a'));
        assertFalse("digit is not a dot", invokeIsDot('0'));
        assertFalse("comma is not a dot", invokeIsDot(','));
    }

    @Test
    public void containsNonLDHCleanDomainReturnsFalse() throws Exception {
        // Assert - a letters/digits/hyphen/dot domain has no non-LDH char. This exercises the loop
        // (L39) over multiple characters and the all-clean path returning false. If the loop guard
        // were negated (never iterating) it would still be false, but the true-case test below
        // pins that the loop actually inspects characters.
        assertFalse(invokeContainsNonLDH("clean-domain0.example.com"));
    }

    @Test
    public void containsNonLDHNonLDHCharacterReturnsTrue() throws Exception {
        // Assert - a comma (0x2c) is a non-LDH character and must be detected. This kills:
        //   * the L49 ConditionalsBoundary (c <= 0x2c): under "<", 0x2c would slip through as LDH;
        //   * the L49 NegateConditionals: a flipped comparison would misclassify it;
        //   * the L39 loop NegateConditionals: a non-iterating loop would never see the comma;
        //   * the isDot L35 BooleanTrueReturn: if isDot always returned true the comma would be
        //     skipped as a "dot" and wrongly treated as LDH.
        assertTrue("comma at the 0x2c boundary is non-LDH", invokeContainsNonLDH("ab,cd"));
    }

    @Test
    public void containsNonLDHHyphenAndDigitsAreLDHReturnsFalse() throws Exception {
        // Assert - 0x2d ('-') sits just past the 0x2c boundary and IS a valid LDH char; if the
        // boundary mutant widened the flagged range it would wrongly mark '-' as non-LDH. Digits
        // (0x30-0x39) are also LDH. So a hyphen+digit string must report false.
        assertFalse("hyphen is LDH (just above the 0x2c boundary)", invokeContainsNonLDH("a-b"));
        assertFalse("digits are LDH", invokeContainsNonLDH("a1b9"));
    }

    @Test
    public void containsNonLDHEmptyStringReturnsFalse() throws Exception {
        // Assert - with no characters the loop body never runs and the method returns false.
        assertFalse(invokeContainsNonLDH(""));
    }
}
