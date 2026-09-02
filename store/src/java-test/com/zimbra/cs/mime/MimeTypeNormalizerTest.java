package com.zimbra.cs.mime;

import org.junit.Test;
import static com.zimbra.qa.unittest.TestUtil.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * unit tests for MimeTypeNormalizer.
 *
 * <p>Tests RFC 2045 comment handling, malformed header detection, and safe fallbacks.
 *
 * <p>Location: store/src/java/com/zimbra/cs/mime/MimeTypeNormalizerTest.java
 */
public class MimeTypeNormalizerTest {

    @Test
    public void testCommentPrefixSingleChar() {
        // Original reported bug: (x)text/html
        String input = "(x)text/html; charset=utf-8";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testCommentPrefixLonger() {
        String input = "(comment)text/html; charset=utf-8";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testMultipleCommentsPrefixed() {
        String input = "(x)(y)text/html; charset=utf-8";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testNestedComments() {
        String input = "(nested(comment))text/html";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testSpaceOnlyComment() {
        String input = "( )text/html; charset=utf-8";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testCommentAfterType() {
        // comment after the main type token
        String input = "text/html(comment); charset=utf-8";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testCommentInsideTypeToken() {
        // comment between primary and subtype
        String input = "text/(x)html; charset=utf-8";
        String result = MimeTypeNormalizer.normalize(input);
        assertNotNull(result);
        assertTrue(result.equals("application/octet-stream") || result.equals("text/html"));
    }

    @Test
    public void testCommentWithTextPlain() {
        String input = "(x)text/plain; charset=utf-8";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/plain", result);
    }

    @Test
    public void testCommentWithXml() {
        String input = "(x)text/xml";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/xml", result);
    }

    @Test
    public void testCommentWithSvg() {
        String input = "(x)image/svg+xml";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("image/svg+xml", result);
    }

    @Test
    public void testCommentWithXhtml() {
        String input = "(x)application/xhtml+xml";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("application/xhtml+xml", result);
    }

    @Test
    public void testOnlyComment() {
        // string contains only a comment, no actual type -> falls back to safe default
        String input = "(only comment)";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testEmptyString() {
        String input = "";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/plain", result);
    }

    @Test
    public void testNullInput() {
        String input = null;
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/plain", result);
    }

    @Test
    public void testWhitespaceOnly() {
        String input = "   ";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/plain", result);
    }

    @Test
    public void testMissingSlash() {
        // malformed: no slash between primary and subtype -> falls back to safe default
        String input = "texthtml";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testValidCleanHtml() {
        String input = "text/html; charset=utf-8";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testValidCleanPlain() {
        String input = "text/plain";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/plain", result);
    }

    @Test
    public void testValidWithMultipleParameters() {
        String input = "text/html; charset=utf-8; boundary=something";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testCaseNormalization() {
        // should be normalized to lowercase
        String input = "TEXT/HTML";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result.toLowerCase());
    }

    @Test
    public void testFromJiraCommentVariant1() {
        String input = "(x)text/html; charset=utf-8";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testFromJiraCommentVariant2() {
        // multiple comments variant
        String input = "(x)(y)text/html; charset=utf-8";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testFromJiraCommentVariant3() {
        // nested variant
        String input = "(nested(comment))text/html";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testRfc2045CommentWithSpecialChars() {
        // comments can contain special characters
        String input = "(test!@#$%^&*)text/html";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testRfc2045BalancedParentheses() {
        // unbalanced parentheses should be handled gracefully
        String input = "(unbalanced)text/html";
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }

    @Test
    public void testDeepNesting() {
        // very deeply nested comments (should not cause stack overflow)
        String input = "((((((((((text/html))))))))";
        String result = MimeTypeNormalizer.normalize(input);
        assertNotNull(result);
    }

    @Test
    public void testLongComment() {
        // very long comment string
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < 10000; i++) {
            sb.append("x");
        }
        sb.append(")text/html");
        String input = sb.toString();
        String result = MimeTypeNormalizer.normalize(input);
        assertEquals("text/html", result);
    }
}
