package com.zimbra.common.util;

import java.io.StringReader;
import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HtmlBodyTextExtractorTest {

    @Test
    public void testExtractNoLimitReturnsFullContent() throws Exception {
        String html = "<html><body>" +
                "Join Microsoft Teams Meeting<br>" +
                "Dial-in number: +1 123 456 7890<br>" +
                "Conference ID: 987654321" +
                "</body></html>";

        String text = HtmlBodyTextExtractor.extract(new StringReader(html), HtmlBodyTextExtractor.NO_LIMIT);

        assertTrue(text.contains("Join Microsoft Teams Meeting"));
        assertTrue(text.contains("Dial-in number"));
        assertTrue(text.contains("+1 123 456 7890"));
        assertTrue(text.contains("Conference ID"));
        assertTrue(text.contains("987654321"));
    }

    @Test
    public void testExtractWithLimitTruncatesTrailingContent() throws Exception {
        String html = "<html><body>" +
                "Join Microsoft Teams Meeting<br>" +
                "Dial-in number: +1 123 456 7890<br>" +
                "Conference ID: 987654321" +
                "</body></html>";

        String text = HtmlBodyTextExtractor.extract(new StringReader(html), 50);

        // should truncate tail content
        assertFalse(text.contains("Conference ID"));
    }
}
