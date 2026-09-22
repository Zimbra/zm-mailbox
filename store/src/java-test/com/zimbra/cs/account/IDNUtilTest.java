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
import com.zimbra.cs.account.AttributeManager.IDNType;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Functional tests for {@link IDNUtil} — IDN/ACE conversion of domain names and email
 * addresses. ASCII inputs are unchanged by conversion, IDN inputs gain the ACE prefix,
 * and malformed email addresses are rejected with INVALID_REQUEST.
 */
public class IDNUtilTest {

    @Test
    public void toAsciiDomainNameAsciiInputUnchanged() {
        // Act
        String result = IDNUtil.toAsciiDomainName("example.com");

        // Assert
        assertEquals("example.com", result);
    }

    @Test
    public void toAsciiDomainNameUnicodeLabelAddsAcePrefix() {
        // Arrange — a label with a non-ASCII char must be punycode-encoded
        String unicode = "bücher.com"; // "bücher.com"

        // Act
        String ascii = IDNUtil.toAsciiDomainName(unicode);

        // Assert
        assertTrue("ACE-encoded label expected, got: " + ascii, ascii.contains(IDNUtil.ACE_PREFIX));
        assertTrue("the .com segment is preserved", ascii.endsWith(".com"));
    }

    @Test
    public void toUnicodeDomainNameAceInputRoundTripsToUnicode() {
        // Arrange
        String unicode = "bücher.com";
        String ascii = IDNUtil.toAsciiDomainName(unicode);

        // Act
        String back = IDNUtil.toUnicodeDomainName(ascii);

        // Assert
        assertEquals(unicode, back);
    }

    @Test
    public void toAsciiDomainNamePreservesTrailingDot() {
        // Act — per bug 68964 the trailing dot must be preserved
        String result = IDNUtil.toAsciiDomainName(".a.");

        // Assert
        assertEquals(".a.", result);
    }

    @Test
    public void toAsciiEmailValidAddressConvertsOnlyDomain() throws Exception {
        // Act
        String result = IDNUtil.toAsciiEmail("user@bücher.com");

        // Assert
        assertTrue(result.startsWith("user@"));
        assertTrue(result.contains(IDNUtil.ACE_PREFIX));
    }

    @Test
    public void toAsciiEmailNoAtSignThrowsInvalidRequest() {
        // Act / Assert
        try {
            IDNUtil.toAsciiEmail("nodomain");
            fail("expected INVALID_REQUEST for non-email");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void toUnicodeEmailMultipleAtSignsThrowsInvalidRequest() {
        // Act / Assert
        try {
            IDNUtil.toUnicodeEmail("a@b@c");
            fail("expected INVALID_REQUEST for malformed email");
        } catch (ServiceException e) {
            assertEquals(ServiceException.INVALID_REQUEST, e.getCode());
        }
    }

    @Test
    public void toUnicodeEmailValidAddressConvertsDomainBackToUnicode() throws Exception {
        // Arrange
        String asciiEmail = IDNUtil.toAsciiEmail("user@bücher.com");

        // Act
        String back = IDNUtil.toUnicodeEmail(asciiEmail);

        // Assert
        assertEquals("user@bücher.com", back);
    }

    @Test
    public void toAsciiNullNameReturnsNull() {
        // Act / Assert
        assertNull(IDNUtil.toAscii(null));
    }

    @Test
    public void toAsciiEmailContainingAtRoutesThroughEmailConversion() {
        // Act
        String result = IDNUtil.toAscii("user@bücher.com");

        // Assert
        assertTrue(result.startsWith("user@"));
        assertTrue(result.contains(IDNUtil.ACE_PREFIX));
    }

    @Test
    public void toAsciiDomainOnlyRoutesThroughDomainConversion() {
        // Act
        String result = IDNUtil.toAscii("bücher.com");

        // Assert
        assertTrue(result.contains(IDNUtil.ACE_PREFIX));
    }

    @Test
    public void toUnicodeNullNameReturnsNull() {
        // Act / Assert
        assertNull(IDNUtil.toUnicode(null));
    }

    @Test
    public void toAsciiWithIDNTypeEmailConvertsDomain() {
        // Act
        String result = IDNUtil.toAscii("user@bücher.com", IDNType.email);

        // Assert
        assertTrue(result.startsWith("user@"));
        assertTrue(result.contains(IDNUtil.ACE_PREFIX));
    }

    @Test
    public void toAsciiWithIDNTypeNoneReturnsInputUnchanged() {
        // Act
        String result = IDNUtil.toAscii("user@bücher.com", IDNType.none);

        // Assert
        assertEquals("user@bücher.com", result);
    }

    @Test
    public void toAsciiWithIDNTypeEmailpConvertsDomainInAddrPart() {
        // Act
        String result = IDNUtil.toAscii("user@bücher.com", IDNType.emailp);

        // Assert — domain encoded to ACE while preserving local part
        assertTrue("got: " + result, result.contains(IDNUtil.ACE_PREFIX));
        assertTrue(result.contains("user@"));
    }

    @Test
    public void toAsciiWithIDNTypeCsEmailpConvertsEachCommaSeparatedAddress() {
        // Act
        String result = IDNUtil.toAscii("a@bücher.com, c@example.com", IDNType.cs_emailp);

        // Assert
        assertTrue("first address domain encoded: " + result, result.contains(IDNUtil.ACE_PREFIX));
        assertTrue("second address preserved", result.contains("c@example.com"));
        assertTrue("addresses joined with comma", result.contains(", "));
        // L338: the 'first' flag suppresses the separator before the first address. If that branch
        // were inverted, a leading ", " would be emitted and the separator placement would shift.
        assertFalse("must not emit a leading separator before the first address",
                result.startsWith(", "));
        assertTrue("the first (encoded) address leads the output", result.startsWith("a@"));
        // exactly one ", " separator between the two addresses
        assertEquals("exactly one comma-space separator for two addresses",
                1, countOccurrences(result, ", "));
    }

    @Test
    public void toUnicodeWithIDNTypeEmailRoundTripsFromAscii() {
        // Arrange
        String ascii = IDNUtil.toAscii("user@bücher.com", IDNType.email);

        // Act
        String unicode = IDNUtil.toUnicode(ascii, IDNType.email);

        // Assert
        assertEquals("user@bücher.com", unicode);
    }

    @Test
    public void toUnicodeWithIDNTypeNoneReturnsInputUnchanged() {
        // Act
        String result = IDNUtil.toUnicode("anything@example.com", IDNType.none);

        // Assert
        assertEquals("anything@example.com", result);
    }

    @Test
    public void toUnicodeWithIDNTypeEmailpConvertsDomainBackToUnicode() {
        // Arrange — produce an ACE-encoded address-with-personal first
        String ascii = IDNUtil.toAscii("user@bücher.com", IDNType.emailp);

        // Act — emailp routes through the personal-part aware unicode path
        String unicode = IDNUtil.toUnicode(ascii, IDNType.emailp);

        // Assert — the domain comes back in its unicode form
        assertTrue("got: " + unicode, unicode.contains("bücher.com"));
        assertTrue(unicode.contains("user@"));
    }

    @Test
    public void toUnicodeWithIDNTypeCsEmailpConvertsEachCommaSeparatedAddress() {
        // Arrange
        String ascii = IDNUtil.toAscii("a@bücher.com, c@example.com", IDNType.cs_emailp);

        // Act
        String unicode = IDNUtil.toUnicode(ascii, IDNType.cs_emailp);

        // Assert — each address is decoded back; the ACE prefix is gone
        assertFalse("ACE prefix should be decoded away: " + unicode,
                unicode.contains(IDNUtil.ACE_PREFIX));
        assertTrue(unicode.contains("bücher.com"));
        assertTrue(unicode.contains("c@example.com"));
        assertTrue("addresses joined with comma", unicode.contains(", "));
        // L474: same 'first' separator-suppression logic on the unicode path.
        assertFalse("must not emit a leading separator before the first address",
                unicode.startsWith(", "));
        assertTrue("the first decoded address leads the output", unicode.startsWith("a@"));
        assertEquals("exactly one comma-space separator for two addresses",
                1, countOccurrences(unicode, ", "));
    }

    @Test
    public void toAsciiWithIDNTypeEmailpPersonalPartPreservedDomainEncoded() {
        // Arrange — an address with a personal part and an IDN domain
        String input = "Foo Bar <user@bücher.com>";

        // Act — emailp path extracts the domain, ACE-encodes it, and rebuilds the address
        String ascii = IDNUtil.toAscii(input, IDNType.emailp);

        // Assert
        assertTrue("domain must be ACE-encoded: " + ascii, ascii.contains(IDNUtil.ACE_PREFIX));
        assertTrue("personal part preserved: " + ascii, ascii.contains("Foo Bar"));
    }

    @Test
    public void toAsciiWithIDNTypeEmailpNonAsciiPersonalPartRfc2047Encoded() {
        // Arrange — non-ASCII chars in the personal part must be RFC 2047 encoded (mail-safe)
        String input = "中文 <user@example.com>";

        // Act
        String ascii = IDNUtil.toAscii(input, IDNType.emailp);

        // Assert — result is pure US-ASCII (RFC 2047 form). L425 re-wraps the parsed address with
        // the personal part under an explicit UTF-8 charset, so "中文" must be the UTF-8 base64
        // encoded-word =?utf-8?B?5Lit5paH?=. Skipping that re-wrap (negated L425) drops the
        // UTF-8-charset re-encoding of the personal part.
        assertTrue("expected RFC2047 encoded personal part: " + ascii, ascii.contains("=?"));
        assertTrue("personal part must be UTF-8 base64 encoded-word: " + ascii,
                ascii.contains("=?utf-8?B?5Lit5paH?="));
        assertTrue("the address part must be preserved", ascii.contains("<user@example.com>"));
        // pure US-ASCII output (mail-safe)
        for (int i = 0; i < ascii.length(); i++) {
            assertTrue("output must be pure US-ASCII: " + ascii, ascii.charAt(i) < 128);
        }
    }

    @Test
    public void toUnicodeWithIDNTypeEmailpNonAsciiPersonalPartReturnedAsUnicode() {
        // Arrange — RFC 2047 encoded personal part should decode back to unicode under emailp
        String encoded = IDNUtil.toAscii("中文 <user@example.com>", IDNType.emailp);

        // Act
        String unicode = IDNUtil.toUnicode(encoded, IDNType.emailp);

        // Assert — personal part returns to its unicode characters
        assertTrue("expected decoded unicode personal part: " + unicode,
                unicode.contains("中文"));
    }

    @Test
    public void toAsciiWithIDNTypeEmailpPlainAddressNoPersonalEncodesDomain() {
        // Act — no personal part, matcher still extracts and encodes the domain
        String ascii = IDNUtil.toAscii("user@bücher.com", IDNType.emailp);

        // Assert
        assertTrue(ascii.contains(IDNUtil.ACE_PREFIX));
        assertTrue(ascii.contains("user@"));
    }

    @Test
    public void toAsciiWithIDNTypeEmailpMalformedAddressReturnsInputUnchanged() {
        // Arrange — a string with no parseable address; conversion gracefully returns input
        String input = "not an address at all";

        // Act
        String result = IDNUtil.toAscii(input, IDNType.emailp);

        // Assert — on AddressException the original string is returned
        assertEquals(input, result);
    }

    @Test
    public void toUnicodeDomainNameAsciiInputUnchanged() {
        // Act
        String result = IDNUtil.toUnicodeDomainName("example.com");

        // Assert
        assertEquals("example.com", result);
    }

    @Test
    public void toUnicodeEmailContainingAtRoutesThroughEmailConversion() throws Exception {
        // Arrange
        String ascii = IDNUtil.toAsciiEmail("user@bücher.com");

        // Act
        String result = IDNUtil.toUnicode(ascii);

        // Assert
        assertEquals("user@bücher.com", result);
    }

    @Test
    public void toUnicodeDomainOnlyRoutesThroughDomainConversion() {
        // Arrange
        String ascii = IDNUtil.toAsciiDomainName("bücher.com");

        // Act
        String result = IDNUtil.toUnicode(ascii);

        // Assert
        assertEquals("bücher.com", result);
    }

    /* Count non-overlapping occurrences of {@code needle} in {@code haystack}. */
    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) != -1) {
            count++;
            idx += needle.length();
        }
        return count;
    }
}
