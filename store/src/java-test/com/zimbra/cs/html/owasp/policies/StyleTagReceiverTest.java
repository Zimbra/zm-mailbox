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

package com.zimbra.cs.html.owasp.policies;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.owasp.html.HtmlStreamEventReceiver;
import org.owasp.validator.html.Policy;

public class StyleTagReceiverTest {

    private static final String OUTLOOK_STYLE =
            "\n<!--\n"
                    + " /* Font Definitions */\n"
                    + " @font-face {font-family:Aptos; mso-font-charset:0; mso-generic-font-family:swiss;}\n"
                    + " /* Style Definitions */\n"
                    + " p.MsoNormal, li.MsoNormal, div.MsoNormal\n"
                    + "\t{mso-style-unhide:no; mso-style-qformat:yes; margin:0cm;\n"
                    + "\tfont-size:12.0pt; font-family:\"Aptos\",sans-serif; mso-ascii-font-family:Aptos;}\n"
                    + " p.MsoAutoSig, li.MsoAutoSig, div.MsoAutoSig\n"
                    + "\t{mso-style-priority:99; mso-style-link:\"E-mail Signature Char\"; margin:0cm;\n"
                    + "\tfont-size:12.0pt; font-family:\"Aptos\",sans-serif;}\n"
                    + "-->\n";

    private static final String POLICY_NOT_LOADED = "antisamy.xml not loaded";

    private static boolean policyLoaded;

    @BeforeClass
    public static void loadPolicy() throws Exception {
        Field f = StyleTagReceiver.class.getDeclaredField("policy");
        f.setAccessible(true);
        if (f.get(null) == null) {
            File policyFile = findAntisamyXml();
            if (policyFile != null) {
                f.set(null, Policy.getInstance(policyFile.toURI().toURL()));
            }
        }
        policyLoaded = f.get(null) != null;
    }

    @Test
    public void stripNullReturnsEmpty() {
        Assert.assertEquals("", StyleTagReceiver.stripWrappingHtmlComment(null));
    }

    @Test
    public void stripEmptyReturnsEmpty() {
        Assert.assertEquals("", StyleTagReceiver.stripWrappingHtmlComment(""));
    }

    @Test
    public void stripNoWrapperUnchanged() {
        String css = "p.MsoNormal{margin:0cm}";
        Assert.assertEquals(css, StyleTagReceiver.stripWrappingHtmlComment(css));
    }

    @Test
    public void stripOuterWrapperRemoved() {
        Assert.assertEquals(" p{margin:0cm} ",
                StyleTagReceiver.stripWrappingHtmlComment("<!-- p{margin:0cm} -->"));
    }

    @Test
    public void stripOuterWrapperWithSurroundingWhitespaceRemoved() {
        Assert.assertEquals("\np{margin:0cm}\n",
                StyleTagReceiver.stripWrappingHtmlComment("\n  <!--\np{margin:0cm}\n-->  \n"));
    }

    @Test
    public void stripOutlookStyleNoMarkersLeft() {
        String out = StyleTagReceiver.stripWrappingHtmlComment(OUTLOOK_STYLE);
        Assert.assertFalse(out.contains("<!--"));
        Assert.assertFalse(out.contains("-->"));
        Assert.assertTrue(out.contains("p.MsoAutoSig"));
        Assert.assertTrue(out.contains("margin:0cm"));
    }

    @Test
    public void stripNestedWrappersAllRemoved() {
        Assert.assertEquals(" p{color:red} ",
                StyleTagReceiver.stripWrappingHtmlComment("<!--<!-- p{color:red} -->-->"));
    }

    @Test
    public void stripOnlyLeadingMarkerRemoved() {
        Assert.assertEquals(" p{color:red}",
                StyleTagReceiver.stripWrappingHtmlComment("<!-- p{color:red}"));
    }

    @Test
    public void stripOnlyTrailingMarkerRemoved() {
        Assert.assertEquals("p{color:red} ",
                StyleTagReceiver.stripWrappingHtmlComment("p{color:red} -->"));
    }

    @Test
    public void stripMarkersInMiddleLeftAlone() {
        String css = "p{color:red}<!-- x -->q{color:blue}";
        Assert.assertEquals(css, StyleTagReceiver.stripWrappingHtmlComment(css));
    }

    @Test
    public void stripDoesNotStitchStyleCloseTag() {
        String css = "p{}</sty<!---->le><img src=x onerror=alert(1)>";
        String out = StyleTagReceiver.stripWrappingHtmlComment(css);
        Assert.assertEquals(css, out);
        Assert.assertFalse(out.toLowerCase().contains("</style"));
    }

    @Test
    public void stripDoesNotStitchExpression() {
        String out = StyleTagReceiver.stripWrappingHtmlComment(
                "<!-- p{width:ex<!---->pression(alert(1))} -->");
        Assert.assertFalse(out.contains("expression("));
    }

    @Test
    public void stripDoesNotStitchImport() {
        String out = StyleTagReceiver.stripWrappingHtmlComment(
                "<!-- @imp<!---->ort url(http://evil.example/x.css); -->");
        Assert.assertFalse(out.contains("@import"));
    }

    @Test
    public void sanitizeOutlookWrappedStyleKeepsSignatureCss() {
        Assume.assumeTrue(POLICY_NOT_LOADED, policyLoaded);
        String out = sanitizeStyle(OUTLOOK_STYLE).toLowerCase();
        Assert.assertTrue("signature rule missing: " + out, out.contains("msoautosig"));
        Assert.assertTrue("margin missing: " + out, out.contains("margin"));
        Assert.assertTrue("font-size missing: " + out, out.contains("font-size"));
        Assert.assertTrue("font-family missing: " + out, out.contains("font-family"));
    }

    @Test
    public void sanitizeOutlookWrappedStyleDropsMsoProperties() {
        Assume.assumeTrue(POLICY_NOT_LOADED, policyLoaded);
        String out = sanitizeStyle(OUTLOOK_STYLE).toLowerCase();
        Assert.assertFalse(out.contains("mso-style-priority"));
        Assert.assertFalse(out.contains("mso-style-link"));
        Assert.assertFalse(out.contains("<!--"));
        Assert.assertFalse(out.contains("-->"));
    }

    @Test
    public void sanitizeWrappedAndUnwrappedSameResult() {
        Assume.assumeTrue(POLICY_NOT_LOADED, policyLoaded);
        String css = "p.MsoAutoSig{margin:0cm;font-size:12.0pt}";
        Assert.assertEquals(sanitizeStyle(css).trim(),
                sanitizeStyle("\n<!--\n" + css + "\n-->\n").trim());
    }

    @Test
    public void sanitizeStitchedStyleCloseNoBreakout() {
        Assume.assumeTrue(POLICY_NOT_LOADED, policyLoaded);
        String out = sanitizeStyle(
                "<!-- p{color:red} --></sty<!---->le><img src=x onerror=alert(1)>").toLowerCase();
        Assert.assertFalse("style breakout: " + out, out.contains("</style"));
        Assert.assertFalse("img leaked: " + out, out.contains("<img"));
        Assert.assertFalse("handler leaked: " + out, out.contains("onerror"));
    }

    @Test
    public void sanitizeStitchedExpressionRemoved() {
        Assume.assumeTrue(POLICY_NOT_LOADED, policyLoaded);
        String out = sanitizeStyle("<!-- p{width:ex<!---->pression(alert(1))} -->").toLowerCase();
        Assert.assertFalse(out.contains("expression("));
        Assert.assertFalse(out.contains("alert("));
    }

    @Test
    public void sanitizeStitchedImportRemoved() {
        Assume.assumeTrue(POLICY_NOT_LOADED, policyLoaded);
        String out = sanitizeStyle(
                "<!-- @imp<!---->ort url(http://evil.example/x.css); p{color:red} -->").toLowerCase();
        Assert.assertFalse(out.contains("@import"));
        Assert.assertFalse(out.contains("evil.example"));
    }

    @Test
    public void sanitizeWrappedImportRemoved() {
        Assume.assumeTrue(POLICY_NOT_LOADED, policyLoaded);
        String out = sanitizeStyle(
                "<!-- @import url(http://evil.example/x.css); p{color:red} -->").toLowerCase();
        Assert.assertFalse(out.contains("@import"));
        Assert.assertFalse(out.contains("evil.example"));
    }

    @Test
    public void sanitizeWrappedJavascriptUrlRemoved() {
        Assume.assumeTrue(POLICY_NOT_LOADED, policyLoaded);
        String out = sanitizeStyle(
                "<!-- p{background-image:url(javascript:alert(1))} -->").toLowerCase();
        Assert.assertFalse(out.contains("javascript:"));
    }

    @Test
    public void sanitizeWrappedExpressionRemoved() {
        Assume.assumeTrue(POLICY_NOT_LOADED, policyLoaded);
        String out = sanitizeStyle("<!-- p{width:expression(alert(1))} -->").toLowerCase();
        Assert.assertFalse(out.contains("expression("));
    }

    @Test
    public void sanitizeWrappedFixedPositionRemoved() {
        Assume.assumeTrue(POLICY_NOT_LOADED, policyLoaded);
        // policy only allows position: static | inherit (anti-phishing overlay)
        String out = sanitizeStyle("<!-- div{position:fixed;top:0;left:0} -->").toLowerCase();
        Assert.assertFalse(out.contains("fixed"));
    }

    @Test
    public void sanitizeUnclosedWrapperNoException() {
        Assume.assumeTrue(POLICY_NOT_LOADED, policyLoaded);
        String out = sanitizeStyle("<!-- p{color:red}");
        Assert.assertFalse(out.contains("<!--"));
    }

    @Test
    public void sanitizeNullStyleTextNoException() {
        Assume.assumeTrue(POLICY_NOT_LOADED, policyLoaded);
        // must not throw
        sanitizeStyle(null);
    }

    @Test
    public void nonStyleTextPassedThroughUnchanged() {
        RecordingReceiver rec = new RecordingReceiver();
        StyleTagReceiver r = new StyleTagReceiver(rec);
        r.openDocument();
        r.openTag("p", Collections.<String>emptyList());
        r.text("Thanks and Regards,");
        r.closeTag("p");
        r.closeDocument();
        Assert.assertEquals("Thanks and Regards,", rec.getText());
    }

    private static String sanitizeStyle(String css) {
        RecordingReceiver rec = new RecordingReceiver();
        StyleTagReceiver r = new StyleTagReceiver(rec);
        r.openDocument();
        r.openTag("style", Collections.<String>emptyList());
        r.text(css);
        r.closeTag("style");
        r.closeDocument();
        return rec.getStyleText();
    }

    private static File findAntisamyXml() {
        String override = System.getProperty("antisamy.policy");
        if (override != null && new File(override).isFile()) {
            return new File(override);
        }
        String[] candidates = {
                "conf/antisamy.xml",
                "store/conf/antisamy.xml",
                "../store/conf/antisamy.xml"
        };
        for (String candidate : candidates) {
            File f = new File(candidate);
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }

    private static class RecordingReceiver implements HtmlStreamEventReceiver {

        private final StringBuilder text = new StringBuilder();

        private final StringBuilder styleText = new StringBuilder();

        private boolean inStyle;

        String getText() {
            return text.toString();
        }

        String getStyleText() {
            return styleText.toString();
        }

        @Override
        public void openDocument() {
            // nothing to record
        }

        @Override
        public void closeDocument() {
            // nothing to record
        }

        @Override
        public void openTag(String elementName, List<String> attrs) {
            inStyle = "style".equalsIgnoreCase(elementName);
        }

        @Override
        public void closeTag(String elementName) {
            inStyle = false;
        }

        @Override
        public void text(String t) {
            if (t == null) {
                return;
            }
            if (inStyle) {
                styleText.append(t);
            } else {
                text.append(t);
            }
        }
    }
}
