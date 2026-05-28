/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.  All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ZimbraAuthTokenEncoded}.
 *
 * Tests verify token encoding, retrieval, and immutability.
 */
public class ZimbraAuthTokenEncodedTest {

    @Test
    public void createToken_withValidEncoding_storesEncoding() {
        String testToken = "encoded-token-value-12345";
        ZimbraAuthTokenEncoded token = new ZimbraAuthTokenEncoded(testToken);

        assertNotNull(token);
        assertEquals(testToken, token.getEncoded());
    }

    @Test
    public void getEncoded_returnsExactInputValue() {
        String input = "test-auth-token-encoded";
        ZimbraAuthTokenEncoded token = new ZimbraAuthTokenEncoded(input);

        assertEquals(input, token.getEncoded());
    }

    @Test
    public void createToken_withEmptyString_storesEmpty() {
        ZimbraAuthTokenEncoded token = new ZimbraAuthTokenEncoded("");

        assertNotNull(token);
        assertEquals("", token.getEncoded());
    }

    @Test
    public void createToken_withComplexToken_preservesContent() {
        String complexToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";
        ZimbraAuthTokenEncoded token = new ZimbraAuthTokenEncoded(complexToken);

        assertEquals(complexToken, token.getEncoded());
    }

    @Test
    public void createToken_withNullValue_allowsNull() {
        // Some implementations may allow null tokens
        ZimbraAuthTokenEncoded token = new ZimbraAuthTokenEncoded(null);

        assertNotNull(token);
        assertNull(token.getEncoded());
    }

    @Test
    public void createToken_withLongToken_preservesLengthAndContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("token-part-").append(i).append("|");
        }
        String longToken = sb.toString();

        ZimbraAuthTokenEncoded token = new ZimbraAuthTokenEncoded(longToken);

        assertEquals(longToken, token.getEncoded());
    }

    @Test
    public void createToken_withSpecialCharacters_preservesSpecialChars() {
        String special = "token!@#$%^&*()_+-={}[]|:;<>?,.";
        ZimbraAuthTokenEncoded token = new ZimbraAuthTokenEncoded(special);

        assertEquals(special, token.getEncoded());
    }

    @Test
    public void multipleInstances_withDifferentTokens_storeIndependently() {
        String token1 = "first-token";
        String token2 = "second-token";

        ZimbraAuthTokenEncoded encoded1 = new ZimbraAuthTokenEncoded(token1);
        ZimbraAuthTokenEncoded encoded2 = new ZimbraAuthTokenEncoded(token2);

        assertEquals(token1, encoded1.getEncoded());
        assertEquals(token2, encoded2.getEncoded());
        assertNotEquals(encoded1.getEncoded(), encoded2.getEncoded());
    }

    @Test
    public void createToken_extendsZimbraAuthToken_hasParentMethods() {
        ZimbraAuthTokenEncoded token = new ZimbraAuthTokenEncoded("test-token");

        // Should be instance of parent class
        assertTrue(token instanceof ZimbraAuthToken);
    }

    @Test
    public void createToken_withUnicodeCharacters_preservesUnicode() {
        String unicode = "token-with-unicode-中文-العربية";
        ZimbraAuthTokenEncoded token = new ZimbraAuthTokenEncoded(unicode);

        assertEquals(unicode, token.getEncoded());
    }

    @Test
    public void createToken_withBase64Encoding_preservesBase64() {
        String base64 = "aGVsbG8td29ybGQtdGhpcy1pcy1iYXNlNjQtZW5jb2Rpbmc=";
        ZimbraAuthTokenEncoded token = new ZimbraAuthTokenEncoded(base64);

        assertEquals(base64, token.getEncoded());
    }

    @Test
    public void getEncoded_calledMultipleTimes_returnsSameValue() {
        String originalToken = "stable-token-value";
        ZimbraAuthTokenEncoded token = new ZimbraAuthTokenEncoded(originalToken);

        String first = token.getEncoded();
        String second = token.getEncoded();
        String third = token.getEncoded();

        assertEquals(first, second);
        assertEquals(second, third);
        assertEquals(originalToken, first);
    }

    @Test
    public void createToken_withWhitespace_preservesWhitespace() {
        String tokenWithSpaces = "token   with   spaces   and\ttabs\nand\nnewlines";
        ZimbraAuthTokenEncoded token = new ZimbraAuthTokenEncoded(tokenWithSpaces);

        assertEquals(tokenWithSpaces, token.getEncoded());
    }
}
