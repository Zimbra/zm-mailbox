 /*
  * ***** BEGIN LICENSE BLOCK *****
  * Zimbra Collaboration Suite Server
  * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016, 2017 Synacor, Inc.
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
package com.zimbra.cs.html;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.servlet.ZThreadLocal;
import com.zimbra.soap.RequestContext;

/**
 * Tired of regressions in the defang filter. Unit test based on fixes I found in bugzilla over the years for different
 * problems to make sure they still work
 * @author jpowers
 *
 */
public class DefangFilterTest {
    private static String EMAIL_BASE_DIR = "data/unittest/email/";
    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        EMAIL_BASE_DIR = MailboxTestUtil.getZimbraServerDir("") + EMAIL_BASE_DIR;
        Provisioning prov = Provisioning.getInstance();
        prov.getConfig().setUseOwaspHtmlSanitizer(false);
        Account acct = prov.createAccount("test@in.telligent.com", "secret", new HashMap<String, Object>());
    }

    /**
     * Check to makes sure ftp:// urls are passed through...
     * @throws Exception
     */
    @Test
    public void testBug37098() throws Exception {
        String fileName = "bug_37098.txt";
        try (InputStream htmlStream = getHtmlBody(fileName)) {
            String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
            // Make sure it didn't delete ftp://
            Assert.assertTrue(result.contains("ftp://ftp.perftech.com/hidden/aaeon/cpupins.jpg"));
        }
    }

    private void defangHtmlString(String html, String expected) throws IOException {
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        Assert.assertEquals("Defanged HTML result", expected,
            DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true));
    }

    public void defangStyleUnwantedFunc(String styleValue, String expected) throws Exception {
        String after = DefangFilter.STYLE_UNWANTED_FUNC.matcher(styleValue).replaceAll("");
        Assert.assertEquals("StyleUnwantedFunc result", expected, after);
    }

    /* Cut down version of original Bug 101227 test data which was significantly larger */
    private static final String urlWithInlinePNG =
            "background-image:\n  url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAA+/AA\n" +
            "WAP457/97wejL8Ovcj5LuH/FEVEf7d+/etuVtN/OIknPPDGFlCoJlWx/lkf/78/mGP2zUhTzC0AAAAASUVORK5CYII=');\n" +
            "    background-repeat: no-repeat; background-position: center;";

    private static final String defangedUrlWithInlinePNG =
            "background-image:\n  ;\n" + "    background-repeat: no-repeat; background-position: center;";

    private static String htmlTemplateForUrlWithInlinePNG =
            "<td style=\"%s\n\"></td>";

    /* Verified in Firefox that multi-line url with this html displays the background image
     * (give a valid paper.gif file from http://www.w3schools.com/cssref/paper.gif)
     * So we definitely should be stripping out multi-line functions
     */
    private static String templateHtmlWithNonInlinedBackgroundImageURL =
            "<html><head><style>\n" +
            "body  {\n" +
            "    background-image: %s;\n" +
            "    background-color: #cccccc;\n" +
            "}\n" +
            "</style>\n" +
            "</head>\n" +
            "<body><h1>Hello World!</h1></body>\n" +
            "</html>\n";
    private static String nonInlinedBackgroundImageURL =
            "url(\n" +
            "            \"paper.gif\"\n" +
            "            )";

    /* Bug 101227 CPU load & latency when open mail with data:image/png:base64 inline image
     *  Need multi-line url() detection.
     */
    @Test
    public void testStyleValueContainingMultiLineUrl() throws Exception {
        defangStyleUnwantedFunc(urlWithInlinePNG, defangedUrlWithInlinePNG);
    }

    @Test
    public void testHtmlWithStyleValueContainingMultiLineUrl() throws Exception {
        defangHtmlString(String.format(htmlTemplateForUrlWithInlinePNG, urlWithInlinePNG),
                String.format(htmlTemplateForUrlWithInlinePNG, defangedUrlWithInlinePNG));
        defangHtmlString(
                String.format(templateHtmlWithNonInlinedBackgroundImageURL, nonInlinedBackgroundImageURL),
                String.format(templateHtmlWithNonInlinedBackgroundImageURL, ""));
    }

    @Test
    public void testDefangStyleUnwantedFunc() throws Exception {
        String before = "background: #e7e7e7; background-image:url('http://example.com/image/k-hdr.jpg');height=";
        defangStyleUnwantedFunc(before, "background: #e7e7e7; background-image:;height=");
        before = "@media (max-width: 480px) {.body{font-size: 0.938em;} }";
        defangStyleUnwantedFunc(before, before);
        before = "@media all and (max-width: 699px) and (min-width: 520px)";
        defangStyleUnwantedFunc(before, before);
        before = "@media (orientation:portrait)";
        defangStyleUnwantedFunc(before, before);
        before = "@media (min-width: 700px), handheld and (orientation: landscape) { ... }";
        defangStyleUnwantedFunc(before, before);
        before = "@media not screen and (color), print and (color)";
        defangStyleUnwantedFunc(before, before);
        before = "@media (max-width 480px) {.body{font-size: 0.938em;} }";
        defangStyleUnwantedFunc(before, before);
    }

    /**
     * Tests to make sure target="_blank" is added to anythign with an href
     * @throws Exception
     */
    @Test
    public void testBug46948() throws Exception {
        String fileName = "bug_46948.txt";
        InputStream htmlStream = getHtmlBody(fileName);

        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        // Make sure each area tag has a target
        int index = result.indexOf("<area");
        while(index >= 0){
            int closingIndex = result.indexOf(">", index);
            int targetIndex =  result.indexOf("target=", index);
            // Make sure we got a target
            Assert.assertTrue(targetIndex != -1);
            // make sure its before the closing tag
            Assert.assertTrue(targetIndex < closingIndex);
            index = result.indexOf("<area", index+1);
        }
    }

    /**
     * Check to make sure we don't defang a url because we don't like the end of it.
     * @throws Exception
     */
    @Test
    public void testBug49452() throws Exception {
        String fileName = "bug_49452.txt";
        InputStream htmlStream = getHtmlBody(fileName);

        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        // make sure the link is still there
        // There should be a bunch of data after this link, but there's a few \n that seem to break it up.
        Assert.assertTrue(result.contains("https://www.plus1staging.net/plus1staging.net/companyAuthorization.jsp"));
    }

    /**
     * Checks to make sure the base url is prepended to any of the relative links
     * @throws Exception
     */
    @Test
    public void testBug11464() throws Exception {
        String fileName = "bug_11464.txt";
        InputStream htmlStream = getHtmlBody(fileName);

        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);

        // Make sure this has been replaced
        Assert.assertTrue(!result.contains("src=\"_media/zimbra_logo.gif\""));
    }


    /**
     * Utility method that gets the html body part from a mime message and returns its input stream
     * @param fileName The name of the email file to load from the unit test data dir
     * @return The input stream for the html body if successful
     * @throws Exception
     */
    private InputStream getHtmlBody(String fileName) throws Exception {
        // Get an input stream of a test pdf to test with
        InputStream inputStream = new FileInputStream(EMAIL_BASE_DIR + fileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteUtil.copy(inputStream, true, baos, true);

        ParsedMessage msg = new ParsedMessage(baos.toByteArray(), false);
        Set<MPartInfo> bodyparts = Mime.getBody(msg.getMessageParts(), true);

        InputStream htmlStream = null;
        for(MPartInfo body: bodyparts) {
            if(body.getContentType().contains("html")){
                htmlStream=  body.getMimePart().getInputStream();
            }
        }
        return htmlStream;
    }

    /**
     * Utility method that gets the html body part from a mime message and returns its input stream
     * @param fileName The name of the email file to load from the unit test data dir
     * @return The input stream for the html body if successful
     * @throws Exception
     */
    private InputStream getHtmlPart(String fileName, int partNum) throws Exception {
        // Get an input stream of a test pdf to test with
        InputStream inputStream = new FileInputStream(EMAIL_BASE_DIR + fileName);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ByteUtil.copy(inputStream, true, baos, true);

        ParsedMessage msg = new ParsedMessage(baos.toByteArray(), false);
        List<MPartInfo> parts = msg.getMessageParts();//Mime.getBody(msg.getMessageParts(), true);

        InputStream htmlStream = null;
        for(MPartInfo body: parts) {
               if(body.getPartNum() == partNum){
                htmlStream=  body.getMimePart().getInputStream();
               }
        }
        return htmlStream;
    }

    /**
     * Tests to make sure we allow just image names to come through
     * @throws Exception
     */
    @Test
    public void testBug60769() throws Exception {
        String fileName = "bug_60769.txt";
        InputStream htmlStream = getHtmlBody(fileName);

        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);

        Assert.assertTrue(!result.contains("dfsrc=\"image001.gif\""));
        Assert.assertTrue(result.contains("src=\"image001.gif\""));
    }

    /**
     * Tests to make sure we properly defang images that are neither inline/internal nor external images.
     * @throws Exception
     */
    @Test
    public void testBug64903() throws Exception {
        String fileName = "bug_60769.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("pnsrc=\"image001.gif\""));
    }

    /**
     * Tests to make sure we can handle inline image data embeded with a data: protocol
     * without tying up the system
     * @throws Exception
     */
    @Test
    public void testBug62605() throws Exception {
        String fileName = "bug_62605.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        long startTime = System.currentTimeMillis();
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        long endTime = System.currentTimeMillis();

        // Make sure this takes less than one second
        Assert.assertTrue("Possible slowness in a regex", (endTime - startTime) < 2000);
        // Make sure this has been replaced
        Assert.assertTrue(result.contains("src=\"data:"));
    }

    /**
     * Makes sure we don't defang inline images
     * @throws Exception
     */
    @Test
    public void testBug62632() throws Exception {
        String fileName = "bug_62632.txt";
        InputStream htmlStream = getHtmlBody(fileName);

        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);

        // Mare sure dfsrc isn't in there
        Assert.assertTrue(!result.contains("dfsrc=\"data:"));
        // and make sure we still have the src link..
        Assert.assertTrue(result.contains("src=\"data:"));
    }

    /**
     * Makes sure we don't defang inline images
     * @throws Exception
     */
    @Test
    public void testBug63150() throws Exception {
        String fileName = "bug_63150.txt";
        InputStream htmlStream = getHtmlBody(fileName);

        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);

        // Check to make sure the link needed is still in there.
        Assert.assertTrue(result.contains("BillingInfoDisplayCmd?bi_URL"));
    }

    /**
     * Makes sure we don't defang input button images
     * @throws Exception
     */
    @Test
    public void testBug62346() throws Exception {
        String fileName = "bug_62346.txt";
        InputStream htmlStream = getHtmlPart(fileName, 2);
        Assert.assertNotNull(htmlStream);

        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, false);

        // Check to make sure the link needed is still in there.
        Assert.assertTrue(result.contains("https://secure.sslpost.com/static/images/open_document.png"));
    }
    /**
     * Test to make sure there aren't NPE's when there isn't an src in an img tag
     * @throws Exception
     */
    @Test
    public void testBug64188() throws Exception {
        String fileName = "bug_64188.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        Assert.assertNotNull(htmlStream);

        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
         // just make sure we made it here, as this was NPEing out..
        Assert.assertNotNull(result);

    }
    /**
     * Checks to make sure we actually defang external content
     * @throws Exception
     */
    @Test
    public void testBug64726() throws Exception {
        String fileName = "bug_64726.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        Assert.assertNotNull(htmlStream);

        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
         // just make sure we made it here, as this was NPEing out..


        Assert.assertNotNull(result);
        // Make sure the input got changed
        Assert.assertTrue(result.contains("dfsrc=\"http://www.google.com/intl/en_com/images/srpr/logo3w.png\""));
    }

    /**
     * Checks to ensure that we're properly swapping src to dfsrc for input tags as well.
     * @throws Exception
     */
    @Test
    public void testBug58889() throws Exception {
        String fileName = "bug_58889.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        Assert.assertNotNull(htmlStream);

        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
         // just make sure we made it here, as this was NPEing out..


        Assert.assertNotNull(result);

        Assert.assertFalse(result.contains(" src=\"https://grepular.com/email_privacy_tester/"));
        Assert.assertTrue(result.contains(" dfsrc=\"https://grepular.com/email_privacy_tester/"));

    }

    /**
     * Checks that CDATA section in HTML is reported as a comment and removed.
     * @throws Exception
     */
    @Test
    public void testBug64974() throws Exception {
        String html = "<html><body><![CDATA[--><a href=\"data:text/html;base64,PHNjcmlwdD4KYWxlcnQoZG9jdW1lbnQuY29va2llKQo8L3NjcmlwdD4=\">click</a]]></body></html>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.equals("<html><body></body></html>"));
    }

    /**
     * Checks that expression() in style value is removed.
     * @throws Exception
     */
    @Test
    public void testBug67021() throws Exception {
        String html =
                "<html><body>" +
                "<div style=\"{ left:\\0065\\0078pression( alert('XSS2') ) }\">" +
                "<div style=\"{ left:&#x5c;0065&#x5c;0078pression( alert('XSS3') ) }\">" +
                "<style>\n" +
                "*{width:ex\\pression( eval(alert(\"XSS4\")));}\n" +
                "</style>" +
                "<span style=\"ldsf;lksdf;lksdf:expre\\ss\\ion( alert('XSS5' ) )\">\n" +
                "</span>" +
                "</body></html>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertFalse(result.contains("XSS2"));
        Assert.assertFalse(result.contains("XSS3"));
        Assert.assertFalse(result.contains("XSS4"));
        Assert.assertFalse(result.contains("XSS5"));
    }

    /**
     * Checks that rgb() in style value is not removed.
     * @throws Exception
     */
    @Test
    public void testBug67537() throws Exception {
        String html = "<html><body><span style=\"color: rgb(255, 0, 0);\">This is RED</span></body></html>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("style=\"color: rgb(255, 0, 0);\""));
    }

    @Test
    public void testBug74715() throws Exception {
        String html = "<DIV STYLE=\"width: expression(alert('XSS'));\">";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertFalse(result.contains("XSS"));
    }

    @Test
    public void testBug76500() throws Exception {
        String html = "<blockquote style=\"border-left:2px solid rgb(16, 16, 255);\">";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("rgb(16, 16, 255)"));
    }

    @Test
    public void testBug78997() throws Exception {
        String fileName = "bug_78997.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        // and make sure we still have the @media link and the device width attribute..
        Assert.assertTrue(result.contains("and (max-device-width: 480px)"));

        //Some more tests with different media attributes
        String html =  "<td style= \"@media only  @media (max-width: 480px) {.body{font-size: 0.938em;} }\"/> ";
        htmlStream  = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("(max-width: 480px)"));

        html = "<td style= \" @media only screen and (-webkit-min-device-pixel-ratio: 2) {body {font-size: 0.938em;}  }\"/>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("and (-webkit-min-device-pixel-ratio: 2)"));

        html = "<td style= \"@media (min-width: 1200px) {.two{margin-top:8em;} }\"/>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("(min-width: 1200px)"));

        html = "<td style= \"@media (max-height: 600px) {.two{margin-top:4em;} }\"/>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("(max-height: 600px) {.two{margin-top:4em;} }"));

        html = "<td style= \" @media (min-height: 1020px) { .two{margin-top:9em;} }\">";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("(min-height: 1020px) { .two{margin-top:9em;} }"));


        html = "@media (min-height: 750px) and (max-height: 770px) {"
              + ".two{margin-top:7em;} }";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("(min-height: 750px) and (max-height: 770px)"));

    }

    @Test
    public void testBug73874() throws Exception {
        String fileName = "bug_73874.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);

        // and make sure we have the the complete URL for
        Assert.assertTrue(result
          .contains("https://wiki.tomsawyer.com/download/thumbnails/27132023/Screen&#43;Shot&#43;2012-05-02&#43;at&#43;08.08.12&#43;"
              + "AM.png?version&#61;1&amp;modificationDate&#61;1335967057000"));

        // case where base URL does not have a trailing '/'
        String html = "<html><head><base href=\"https://wiki.tomsawyer.com\"/>"
            + "</head><body>"
            + "<img  width=\"100\"  src=\"/download/thumbnails/27132023/Screen+Shot+"
            + "2012-05-02+at+08.08.12+AM.png?version=3D1&modificationDate=3D1335967057"
            + "000\"/></body></html>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result
                .contains("https://wiki.tomsawyer.com/download/thumbnails/27132023/Screen&#43;Shot&#43;2012-05-02&#43;at&#43;08.08.12&#43;"
              + "AM.png?version&#61;3D1&amp;modificationDate&#61;3D1335967057000"));

        // case where base URL has a trailing '/'
        html = "<html><head><base href=\"https://wiki.tomsawyer.com/\" />"
            + "</head><body>"
            + "<img  width=\"100\"  src=\"download/thumbnails/27132023/Screen+Shot+"
            + "2012-05-02+at+08.08.12+AM.png?version=3D1&modificationDate=3D1335967057"
            + "000\"/></body></html>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result
                .contains("https://wiki.tomsawyer.com//download/thumbnails/27132023/Screen&#43;Shot&#43;2012-05-02&#43;at&#43;08.08.12&#43;"
              + "AM.png?version&#61;3D1&amp;modificationDate&#61;3D1335967057000"));

       // case where base URL has a single parameter'/'
        html = "<html><head><base href=\"https://wiki.tomsawyer.com/\" />"
            + "</head><body>"
            + "<img  width=\"100\"  src=\"download/thumbnails/27132023/Screen+Shot+"
            + "2012-05-02+at+08.08.12+AM.png?version=3D1\"/></body></html>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result
                .contains("https://wiki.tomsawyer.com//download/thumbnails/27132023/Screen&#43;Shot&#43;2012-05-02&#43;at&#43;08.08.12&#43;"
              + "AM.png?version&#61;3D1"));

     // case where base URL no parameters
        html = "<html><head><base href=\"https://wiki.tomsawyer.com/\" />"
            + "</head><body>"
            + "<img  width=\"100\"  src=\"download/thumbnails/27132023/Screen+Shot+"
            + "2012-05-02+at+08.08.12+AM.png\"/></body></html>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result
                .contains("https://wiki.tomsawyer.com//download/thumbnails/27132023/Screen&#43;Shot&#43;2012-05-02&#43;at&#43;08.08.12&#43;"
              + "AM.png"));

     // case where relative URL is invalidsomething like.pngxxx.gif
        html = "<html><head><base href=\"https://wiki.tomsawyer.com/\" />"
            + "</head><body>"
            + "<img  width=\"100\"  src=\"download/thumbnails/27132023/Screen+Shot.pngTest.gif\"/></body></html>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(!result
                .contains("https://wiki.tomsawyer.com//download/thumbnails/27132023/Screen&#43;Shot&#43;2012-05-02&#43;at&#43;08.08.12&#43;"
              + "AM.png"));
    }


    @Test
    public void testBug97443() throws Exception {
        String html = "<html><head></head><body><table><tr><td><B>javascript-blocked test </B></td>"
            + "</tr><tr><td><a href=\"javascript:alert('Hello!');\">alert</a>"
            + "</td></tr></table></body></html>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML)
                .defang(htmlStream, true);
        Assert.assertTrue(result
            .contains("href=\"JAVASCRIPT-BLOCKED:alert(&#39;Hello!&#39;);\""));

        html = "<html><head><base href=\"http://lbpe.wikispaces.com/\" /></head><body>"
            + "<table><tr><td><B>javascript-blocked test</B></td></tr><tr><td>"
            + "<a href=\"javascript:alert('Hello!');\">alert</a></td></tr></table>"
            + "</body></html>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML)
                .defang(htmlStream, true);
        Assert.assertTrue(result
                .contains("href=\"JAVASCRIPT-BLOCKED:alert(&#39;Hello!&#39;);\""));
    }

    @Test
    public void testBug78902() throws Exception {

        String html = "<html><head></head><body><a target=\"_blank\" href=\"Neptune.gif\"></a></body></html>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML)
                .defang(htmlStream, true);
        Assert.assertTrue(result
                .contains("<a target=\"_blank\" href=\"Neptune.gif\"></a>"));


        html = "<html><body>My pictures <a href=\"javascript:document.write('%3C%61%20%68%72%65%66%3D%22%6A%61%76%"
            + "61%73%63%72%69%70%74%3A%61%6C%65%72%74%28%31%29%22%20%6F%6E%4D%6F%75%73%65%4F%76%65%72%3D%61%6C%65%"
            + "72%74%28%5C%22%70%30%77%6E%5C%22%29%3E%4D%6F%75%73%65%20%6F%76%65%72%20%68%65%72%65%3C%2F%61%3E')\">here</a></body></html>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML)
                .defang(htmlStream, true);
        Assert.assertTrue(result
                .contains("JAVASCRIPT-BLOCKED"));

        html =  "<html><head></head><body><a target=\"_blank\" href=\"Neptune.txt\"></a></body></html>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML)
                .defang(htmlStream, true);
        Assert.assertTrue(result
                .contains("<a target=\"_blank\" href=\"Neptune.txt\"></a>"));

        html =  "<html><head></head><body><a target=\"_blank\" href=\"Neptune.pptx\"></a></body></html>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML)
                .defang(htmlStream, true);
        Assert.assertTrue(result
                .contains("<a target=\"_blank\" href=\"Neptune.pptx\"></a>"));

        html = "<li><a href=\"poc.zip?view=html&archseq=0\">\"/><script>alert(1);</script>AAAAAAAAAA</a></li>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML)
                .defang(htmlStream, true);
        Assert.assertTrue(!result
                .contains("<script>"));
    }


    @Test
    public void testBug73037() throws Exception {

        String html = "<html><head></head><body><a target=\"_blank\"" +
        " href=\"smb://Aurora._smb._tcp.local/untitled/folder/03 DANDIYA MIX.mp3\"></a></body></html>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains(html));

        html = "<html><head></head><body><a target=\"_blank\"" +
            " href=\"smb://Aurora._smb._tcp.local/untitled/folder/03%20DANDIYA%20MIX.mp3\"></a></body></html>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
                true);
        Assert.assertTrue(result.contains(html));

        html = "<html><head></head><body><a target=\"_blank\"" +
            " href=\"//Shared_srv/folder/file.txt\"></a></body></html>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
                true);
        Assert.assertTrue(result.equals(html));

        html = "<html><head></head><body><a target=\"_blank\"" +
            " href=\"//Shared_srv/folder/file with spaces.txt\"></a></body></html>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
                true);
        Assert.assertTrue(result.equals(html));
    }

    @Test
    public void testBug81641() throws Exception {

        String html = "<td style=\"background: #e7e7e7; background-image:"
            + "url(\'http://www.househuntnews.com/marketing/images/keller-header.jpg');"
            + "height=\"97\" width=\"598\">";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(!result.contains("url('http://www.househuntnews.com/marketing/images/keller-header.jpg')"));

        html = "<td style= \"@media (max-width: 480px) {.body{font-size: 0.938em;} }\" />";
        htmlStream  = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("(max-width: 480px)"));

        html = "<td style= \"@media all and (max-width: 699px) and (min-width: 520px)\" />";
        htmlStream  = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains(" (max-width: 699px) and (min-width: 520px)"));

        html ="<td style= \"@media (orientation:portrait)\" />";
        htmlStream  = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("(orientation:portrait)"));

        html = "<td style= \"@media (min-width: 700px), handheld and (orientation: landscape) { ... }\"/>";
        htmlStream  = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("(min-width: 700px), handheld and (orientation: landscape)"));

        html = "<td style= \"@media not screen and (color), print and (color)\"/>";
        htmlStream  = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("not screen and (color), print and (color)"));

        html = "<td style=  \"@media (max-width 480px) {.body{font-size: 0.938em;} }\"/>";
        htmlStream  = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("(max-width 480px)"));

    }
    @Test
    public void testBug82181() throws Exception {

        String html = "<html><head><style> sf { display: block;}@media print {.xsfnoprint{ display : none ;}} "
            + " /* Default css layout information   for SAP Smart Forms (XSF Output)   Last modified: 12.05.2003 */ "
            + "@media screen {  body {    background-color : #EFEFEF ;  }}"
            + "@media screen {  .page {    border-style : outset ;    border-width : 2pt ;    background-color : white ;  }}"
            + "/*@media print {  .page {    overflow: hidden;  }}*/"
            + "/* unification browser-dependent  settings */"
            + "table {    border-spacing: 0pt;    empty-cells: show;}"
            + "tr { vertical-align: top; }"
            + "td { padding: 0pt; }"
            + "input {    font: inherit;    padding: 0pt;    margin: 0pt;}"
            + "img {    display: block;}"
            + "img.icon {    display: inline;}"
            + "/* End of default.css */"
            + ".ZNOW-USER-REGISTER-STYLE  div#P1.par{    font-family : \"Times New Roman\" ;    font-size : 12pt ;    font-weight :20normal ;    line-height : 4.23mm ;    text-decoration : none ;    text-align : left ;    clear : both ;}"
            + ".ZNOW-USER-REGISTER-STYLE  div#P2.par{    font-family : \"Times New Roman\" ;   font-size : 12pt ;    font-weight : bold ;    line-height : 4.23mm ;    text-decoration : none ;    text-align : left ;    clear : both ;}"
            + ".ZNOW-USER-REGISTER-STYLE  a{    color : #000000 ;}"
            + ".ZNOW-USER-REGISTER-STYLE  span#C1.char{    font-family : \"Times New Roman\" ;    font-size : 12pt ;}"
            + ".ZNOW-USER-REGISTER-STYLE  span#C2.char{    font-family : \"Times New Roman\" ;    font-size : 12pt ;    font-weight : bold ;}"
            + "#sf--PAGE1-001.page{    position : absolute ;    height : 210mm ;    width : 297mm ;    top : 0pt ;}"
            + "@media screen {#MAIN.win{    overflow : auto ;}}"
            + "@media print {#MAIN.win{    overflow : hidden ;}}"
            + "#sf--PAGE1-001.page  #MAIN.win{    position : absolute ;    left : 1.15cm ;    top : 1.03cm ;    width : 21.90cm ;    height : 12.15cm ;}</style></head><html>";

        String htmlWithMultiLineComment = "<head>\n"
            + " <style> sf {\n"
            + "    display: block;\n"
            + "}\n"
            + "   @media print {\n"
            + ".xsfnoprint {\n"
            + "          display: none;\n"
            + "      }\n"
            + "  }\n"
            + "     \n"
            + "/* Default css layout information   for SAP Smart Forms (XSF Output)   Last modified: 12.05.2003 \n"
            + " adding  one more line */\n"
            + " @media screen {\n"
            + "    body {\n"
            + "         background-color: #EFEFEF;\n"
            + "      }\n"
            + "  }\n"
            + "  \n"
            + "  @media screen {\n"
            + "      .page {\n"
            + "           border-style: outset;\n"
            + "           border-width: 2pt;\n"
            + "          background-color: white;\n"
            + "       }\n"
            + "   }\n"
            + "   \n"
            + "       /*@media print {  .page {    overflow: hidden;  }}*//* unification browser-dependent settings */\n"
            + "  table {\n" + "      border-spacing: 0pt;\n" + "      empty-cells: show;\n"
            + "    }\n" + "    \n" + "    tr {\n" + "        vertical-align: top;\n" + "     }\n"
            + "     \n" + "    td {\n" + "       padding: 0pt;\n" + "    }\n" + "    \n"
            + "   input {\n" + "        font: inherit;\n" + "       padding: 0pt;\n"
            + "       margin: 0pt;\n" + "    }\n" + "    \n" + "   img {\n"
            + "        display: block;\n" + "   }\n" + "    \n" + "   img.icon {\n"
            + "        display: inline;\n" + "    }\n" + "    /* End of default.css */\n"
            + "  .ZNOW-USER-REGISTER-STYLE  div#P1.par {\n"
            + "        font-family: \"Times New Roman\";\n" + "       font-size: 12pt;\n"
            + "        font-weight: normal;\n" + "        line-height: 4.23mm;\n"
            + "        text-decoration: none;\n" + "        text-align: left;\n"
            + "        clear: both;\n" + "    }\n" + "\n"
            + "    .ZNOW-USER-REGISTER-STYLE  div#P2.par {\n"
            + "       font-family: \"Times New Roman\";\n" + "       font-size: 12pt;\n"
            + "        font-weight: bold;\n" + "        line-height: 4.23mm;\n"
            + "        text-decoration: none;\n" + "        text-align: left;\n"
            + "        clear: both;\n" + "    }\n" + "\n" + "    .ZNOW-USER-REGISTER-STYLE  a {\n"
            + "       color: #000000;\n" + "    }\n" + "\n"
            + "   .ZNOW-USER-REGISTER-STYLE  span#C1.char {\n"
            + "        font-family: \"Times New Roman\";\n" + "        font-size: 12pt;\n"
            + "    }\n" + "\n" + "    .ZNOW-USER-REGISTER-STYLE  span#C2.char {\n"
            + "       font-family: \"Times New Roman\";\n" + "       font-size: 12pt;\n"
            + "       font-weight: bold;\n" + "    }\n" + "\n" + "    #sf--PAGE1-001.page {\n"
            + "        position: absolute;\n" + "        height: 210mm;\n"
            + "        width: 297mm;\n" + "       top: 0pt;\n" + "    }\n" + "\n"
            + "   @media screen {\n" + "       #MAIN.win {\n" + "            overflow: auto;\n"
            + "       }\n" + "    }\n" + "\n" + "    @media print {\n" + "        #MAIN.win {\n"
            + "            overflow: hidden;\n" + "        }\n" + "   }\n" + "\n"
            + "    #sf--PAGE1-001.page #MAIN.win {\n" + "        position: absolute;\n"
            + "        left: 1.15cm;\n" + "        top: 1.03cm;\n" + "        width: 21.90cm;\n"
            + "        height: 12.15cm;\n" + "    }</style>\n" + "</head>\n";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("background-color : #EFEFEF"));

        htmlStream = new ByteArrayInputStream(htmlWithMultiLineComment.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("background-color: #EFEFEF"));
        Assert.assertTrue(!result.contains("Default css layout information   for SAP Smart Forms"));

    }

    @Test
    public void testBug82303() throws Exception {
        String html = "<a href=\"http://ebobby.org/2013/05/18/"
            + "Fun-with-Javascript-and-function-tracing.html\" "
            + "style=\"color: #187AAB; text-decoration: none\" target=\"_blank\">";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("Fun-with-Javascript-and-function-tracing.html"));

        html = "<a href=\"javascript-and-function-tracing.html\" "
            + "style=\"color: #187AAB; text-decoration: none\" target=\"_blank\">";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("javascript-and-function-tracing.html"));

        html = "<a href=\"javascript:myJsFunc()\">Link Text</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = "<a href=\"javascriptlessDestination.html\" onclick=\"myJSFunc(); "
            + "return false;\">Link text</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("javascriptlessDestination.html"));

        html = "<a href=\"javascript:alert('Hello');\"></a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = "<a href=\"http://ebobby.org/2013/05/18/" + "javascript/Lessonsinjavascript.html\" "
            + "style=\"color: #187AAB; text-decoration: none\" target=\"_blank\">";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("javascript/Lessonsinjavascript.html"));

        html = "<a href='javascript:myFunction()'> Click Me! <a/>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = " <a href=\"javascript:void(0)\" onclick=\"loadProducts(<?php echo $categoryId ?>)\"> ";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = "<a href=\"#\" onclick=\"someFunction();\" return false;\">LINK</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.equals("<a href=\"#\">LINK</a>"));

        html = "<a href='javascript:my_Function()'> Click Me! <a/>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = "<a href='javascript:myFunction(field1, field2)'> Click Me! <a/>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = "<a href='javaScript:document.f1.findString(this.t1.value)'>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = "<a href='javaScript:document.f1.findString(this.t1.value)'>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = "<a href=\"#\" onclick=\"findString(document.getElementById('t1').value); return false;\">Click Me</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("<a href=\"#\">Click Me</a>"));

        html = "<a href=\"javascript:alert('0');\">Click Me</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = "<a href=\"  javascript:alert('0');\">Click Me</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

    }

    @Test
    public void testBug83999() throws IOException {

        RequestContext reqContext = new RequestContext();
        reqContext.setVirtualHost("mail.zimbra.com");
        ZThreadLocal.setContext(reqContext);

        String html = "<FORM NAME=\"buy\" ENCTYPE=\"text/plain\" " +
        		"action=\"http://mail.zimbra.com:7070/service/soap/ModifyFilterRulesRequest\" METHOD=\"POST\">";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("SAMEHOSTFORMPOST-BLOCKED"));


        html = "<FORM NAME=\"buy\" ENCTYPE=\"text/plain\" "
            + "action=\"http://zimbra.vmware.com:7070/service/soap/ModifyFilterRulesRequest\" METHOD=\"POST\">";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(!result.contains("SAMEHOSTFORMPOST-BLOCKED"));

        html = "<FORM NAME=\"buy\" ENCTYPE=\"text/plain\" "
            + "action=\"http://mail.zimbra.com/service/soap/ModifyFilterRulesRequest\" METHOD=\"POST\">";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("SAMEHOSTFORMPOST-BLOCKED"));

        html = "<FORM NAME=\"buy\" ENCTYPE=\"text/plain\" "
            + "action=\"/service/soap/ModifyFilterRulesRequest\" METHOD=\"POST\">";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("SAMEHOSTFORMPOST-BLOCKED"));


        ZThreadLocal.unset();
    }

    //Privacy leak and possible XSS in ZWC with Chrome 30 on Win 7 x64 when viewing a conversation

    @Test
    public void testBug84337() throws Exception {
        String html = "<style type=\"text/css=\">@import \"https://emailprivacytester." +
        		"com/cb/55fa19d5db052ced/\";</style>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(!result.contains("@import 'https://emailprivacytester"));

        html = "<style type=\"text/css=\">@import url(\'newstyles.css\');</style>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
        true);
        Assert.assertTrue(!result.contains("@import"));

        html = "<style type=\"text/css=\">@import \"style2.css\";</style>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
        true);
        Assert.assertTrue(!result.contains("@import"));

        html = "<style type=\"text/css=\">@import \"style2.css\"</style>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
        true);
        Assert.assertTrue(!result.contains("@import"));

        html = "<style type=\"text/css=\">@import \"  style1.css  \";</style>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
        true);
        Assert.assertTrue(!result.contains("@import"));

        html = "<style> "
        + "@import url('a.css'); "
        + "@import url('b.css');"
        + "@import url('c.css');"
        + "@import url('d.css');"
        + "@import url('e.css');"
        + "@import url('f.css');"
        + "</style>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
        true);
        Assert.assertTrue(!result.contains("@import"));

        html = "<style type=\"text/css\">  @import url(\"import3.css\");  p { color : #f00; }"
            + "</style>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
        true);
        Assert.assertTrue(result.contains("p { color : #f00; }"));

        html = "<style type=\"text/css\">  @import 'import3.css';  p { color : #f00; }"
            + "</style>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
        true);
        Assert.assertTrue(result.contains("p { color : #f00; }"));
        Assert.assertTrue(!result.contains("import3.css"));

        html = "<style type=\"text/css\">  @import \" import3.css \";  p { color : #f00; }"
            + "</style>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
        true);
        Assert.assertTrue(result.contains("p { color : #f00; }"));
        Assert.assertTrue(!result.contains("import3.css"));

        // adding spaces before the semicolon
        html = "<style type=\"text/css\">  @import \" import3.css \"   ;  p { color : #f00; }"
            + "</style>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
        true);
        Assert.assertTrue(result.contains("p { color : #f00; }"));
        Assert.assertTrue(!result.contains("import3.css"));


        html = "<style type=\"text/css\">  @import \" import3.css \"     p { color : #f00; }"
            + "</style>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
        true);
        Assert.assertTrue(result.contains("p { color : #f00; }"));
        Assert.assertTrue(!result.contains("import3.css"));
    }



    @Test
    public void testBug85478() throws Exception {
        String html = "<a href=\"data:text/html;base64,PHNjcmlwdD5hbGVydCgiSGVsbG8hIik7PC9zY3JpcHQ+\" "
        		+ "data-mce-href=\"data:text/html;base64,PHNjcmlwdD5hbGVydCgiSGVsbG8hIik7PC9zY3JpcHQ+\">Bug</a>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("DATAURI-BLOCKED"));

        html = "<a href=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAErkJggg==\" />Bug</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAErkJggg&#61;&#61;"));

        html = "<a target=_blank href=\"data:text/html,<script>alert(opener.document.body.innerHTML)</script>\">"
        		+" clickme in Opera/FF</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("DATAURI-BLOCKED"));

        html = "<a target=_blank href=\"data.html\"> Data fIle</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("data.html"));

        html = "<a href=\"data:;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAErkJggg==\" />Bug</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("DATAURI-BLOCKED"));

        html = "<img src=\"data:image/jpeg;base64,/9j/4AAAAAxITGlubwIQAABtbnRyUkdCI\"><br>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("data:image/jpeg;base64,/9j/4AAAAAxITGlubwIQAABtbnRyUkdCI"));

        html = "<img src=\"DaTa:image/jpeg;base64,/9j/4AAAAAxITGlubwIQAABtbnRyUkdCI\"><br>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("DaTa:image/jpeg;base64,/9j/4AAAAAxITGlubwIQAABtbnRyUkdCI"));


        html = "<a href=\"DATA:;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAErkJggg==\" />Bug</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("DATAURI-BLOCKED"));

        html = "<a href=\"data\n\n:\n\ntext/html;base64,PHNjcmlwdD5hbGVydCgiSGVsbG8hIik7PC9zY3JpcHQ+\">Bug</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("DATAURI-BLOCKED"));

        html = "<a href=\"data\r\n:text/html;base64,PHNjcmlwdD5hbGVydCgiSGVsbG8hIik7PC9zY3JpcHQ+\">Bug</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("DATAURI-BLOCKED"));

        html = "<a href=\"data:text/html;base64,PHNjcmlwdD5hbGVydCgiSGVsbG8hIik7PC9zY3JpcHQ+\">Bug</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("DATAURI-BLOCKED"));

    }

    @Test
    public void testSvg() throws Exception {
        String svgXml = "<?xml version='1.0' encoding='UTF-8'?>"
            + "<svg id=\"xss\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">"
            + "<use "
            + "xlink:href=\"data:image/svg+xml;base64,PHN2ZyBpZD0icmVjdGFuZ2xlIiB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHhtbG5z"
            + "OnhsaW5rPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5L3hsaW5rIiAgICB3aWR0aD0iMTAwIiBoZWlnaHQ9IjEwMCI+PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0"
            + "Pg0KIDxmb3JlaWduT2JqZWN0IHdpZHRoPSIxMDAiIGhlaWdodD0iNTAiDQogICAgICAgICAgICAgICAgICAgcmVxdWlyZWRFeHRlbnNpb25zPSJodHRwOi8vd"
            + "3d3LnczLm9yZy8xOTk5L3hodG1sIj4NCgk8ZW1iZWQgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGh0bWwiIHNyYz0iamF2YXNjcmlwdDphbGVydC"
            + "hsb2NhdGlvbikiIC8+DQogICAgPC9mb3JlaWduT2JqZWN0Pg0KPC9zdmc+#rectangle\"/>"
            + "</svg>";
        InputStream svgStream = new ByteArrayInputStream(svgXml.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_IMAGE_SVG).defang(svgStream,true);
        Assert.assertTrue(!result.contains("<use"));

        InputStream xmlStream = new ByteArrayInputStream(svgXml.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_XML_LEGACY).defang(xmlStream,true);
        Assert.assertTrue(!result.contains("<use"));

        svgXml = "<svg id=\"xss\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">"
            + "<foreignObject requiredExtensions=\"http://www.w3.org/1999/xhtml\">"
            + "<embed xmlns=\"http://www.w3.org/1999/xhtml\" src=\"javascript:alert(location)\" />"
            + "</foreignObject>"
            + "</svg>";
        svgStream = new ByteArrayInputStream(svgXml.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_IMAGE_SVG).defang(svgStream,true);
        Assert.assertTrue(!result.contains("<foreignObject"));
        Assert.assertTrue(!result.contains("<embed"));

        xmlStream = new ByteArrayInputStream(svgXml.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_XML_LEGACY).defang(xmlStream,true);
        Assert.assertTrue(!result.contains("<foreignObject"));
        Assert.assertTrue(!result.contains("<embed"));

        svgXml = "<svg xmlns=\"http://www.w3.org/2000/svg\">"
            + "<a xmlns:xlink=\"http://www.w3.org/1999/xlink\" xlink:href=\"?\">"
            + "<circle r=\"400\"></circle>"
            + "<animate attributeName=\"xlink:href\" begin=\"0\" from=\"javascript:alert(opener.csrfToken)\" to=\"&amp;amp;\"/>"
            + "</a></svg>";
        svgStream = new ByteArrayInputStream(svgXml.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_IMAGE_SVG).defang(svgStream,true);
        Assert.assertTrue(!result.contains("<animate"));
        Assert.assertTrue(!result.contains("javascript"));

        xmlStream = new ByteArrayInputStream(svgXml.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_XML_LEGACY).defang(xmlStream,true);
        Assert.assertTrue(!result.contains("<animate"));
        Assert.assertTrue(!result.contains("javascript"));
    }

    @Test
    public void testBug88360() throws Exception {
        String fileName = "bug_88360.txt";
        InputStream inputStream = new FileInputStream(EMAIL_BASE_DIR + fileName);
        try {
            String result = DefangFactory.getDefanger(
                MimeConstants.CT_TEXT_HTML).defang(inputStream, true);
            Assert.assertFalse(StringUtil.isAsciiString(result));
        } catch (Exception e) {
            fail("Should not throw exception." + e.getMessage());
        }

        String html = "<style type=\"text/css\">  @import 'import3.css'; p { color : #f00; }"
            + "灻扵楬</style>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML)
            .defang(htmlStream, true);
        Assert.assertFalse(StringUtil.isAsciiString(result));
        Assert.assertTrue(result.contains("p { color : #f00; }"));
        Assert.assertTrue(!result.contains("import3.css"));
    }

    @Test
    public void testBug98215() throws Exception {
        String html = "<a href=\"vbscript:alert(parent.csrfToken)\">CLICK</a>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("VBSCRIPT-BLOCKED"));

        html = "<a href=\"Vbscr&amp;#0009;ip&#009;t:alert(parent.csrfToken)\">CLICK</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("VBSCRIPT-BLOCKED"));

        html = "<a href=\"java&amp;Tab;script:alert(parent.csrfToken)\">CLICK</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = "<a href=\"&amp;Tab;javascript:alert(parent.csrfToken)\">CLICK</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = "<a href=\"javascr&amp;#09;ipt:alert(parent.csrfToken)\">CLICK</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = "<form id=\"test\" action=\"javascript:alert(1)\"><p>test</p>"
            + "<button form=\"test\">Test</button></form>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = "<form id=\"test\" action=\"ja&amp;Tab;vascript:alert(1)\"><p>test</p>"
            + "<button form=\"test\">Test</button></form>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = "<a href=\"&amp;#009;java&#00009;scr&amp;#09;i\t\tpt:alert(parent.csrfToken)\">CLICK</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));
    }

    /**
     * Checks that the input data is extracted and sanitized correctly.
     * @throws Exception
     */
    @Test
    public void testBug100966() throws Exception {
        String inputString = new String(ByteUtil.getContent(new File(EMAIL_BASE_DIR + "bug_100966.txt")));
        String expectedResult = new String (ByteUtil.getContent(new File(EMAIL_BASE_DIR + "bug_100966_op.txt")));
        String result = new DefangFilter(false).extractAndSanitizeAsciiData(inputString);
        Assert.assertTrue(result.equals(expectedResult));
    }


    @Test
    public void testBug101813() throws Exception {
        String html = "<textarea><img title=\"</<!-- -->textarea><img src=x onerror=alert(1)></img>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        int index = result.indexOf("alert(1)");
        Assert.assertEquals(-1, index);

        html = "<textarea><IMG title=\"</<!-- -->textarea><img src=x onerror=alert(1)></img>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        index = result.indexOf("alert(1)");
        Assert.assertEquals(-1, index);

        html = "<textarea><   img title=\"</<!-- -->textarea><img src=x onerror=alert(1)></img>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        index = result.indexOf("alert(1)");
        Assert.assertEquals(-1, index);
    }

    @Test
    public void testBug102637() throws Exception {
        String html = "<html><body><div style=\"font-family: arial, helvetica, sans-serif; font-size: 12pt; color: #000000\">"
            + "<div><br></div><div><a href=\"&amp;#106&amp;#097&amp;#118&amp;#097&amp;#115&amp;#099&amp;#114&amp;#105&amp;"
            + "#112&amp;#116&amp;#058&amp;#097&amp;#108&amp;#101&amp;#114&amp;#116&amp;#040&amp;#039&amp;#088&amp;#083&amp;"
            + "#083&amp;#039&amp;#041\" data-mce-href=\"&amp;#106&amp;#097&amp;#118&amp;#097&amp;#115&amp;#099&amp;"
                + "#114&amp;#105&amp;#112&amp;#116&amp;#058&amp;#097&amp;#108&amp;#101&amp;#114&amp;#116&amp;#040&amp;"
                + "#039&amp;#088&amp;#083&amp;#083&amp;#039&amp;#041\">test</a><br data-mce-bogus=\"1\"></div><div>"
                + "<br data-mce-bogus=\"1\"></div><div>Test message<br data-mce-bogus=\"1\"></div></div></body></html>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));
    }

    @Test
    public void testBug102910() throws Exception {
        String html = "<div><img pnsrc=\"cid:1040f05975d4d4b8fcf8747be3eb9ae3c08e5cd4@zimbra\" "
            + "data-mce-src=\"cid:1040f05975d4d4b8fcf8747be3eb9ae3c08e5cd4@zimbra\" "
            + "src=\"cid:1040f05975d4d4b8fcf8747be3eb9ae3c08e5cd4@zimbra\"></div>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("@zimbra"));
        Assert.assertTrue(!result.contains("&#64;zimbra"));
    }

    @Test
    /**
     * Verify that a new line in a html based signature is maintained after passing through the defanger.
     * @throws Exception
     */
    public void testBug104666() throws Exception {
        String html = "<div></div><div></div><div id=\"5589f382-9e9b-47cd-ab09-3ea973fd4f6a\" data-marker=\"__SIG_PRE__\">"
            + "<div>LIne 1</div>" + "</div>" + "<div>Line 2</div>" + "</div>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("<div>LIne 1</div></div><div>Line 2</div>"));

        html = "<div>Thanks</div><div><img src=\"/home/ews01@zdev-vm002.eng.zimbra.com/Briefcase/rupali.jpeg\" "
                + "dfsrc=\"doc:Briefcase/rupali.jpeg\" "
                + "data-mce-src=\"/home/ews01@zdev-vm002.eng.zimbra.com/Briefcase/rupali.jpeg\"></div>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        System.out.println(result);
        Assert.assertTrue(result.contains("dfsrc=\"doc:Briefcase/rupali.jpeg\""));
        Assert.assertTrue(result.contains("data-mce-src=\"/home/ews01"));
    }

    @Test
    /**
     * Verify that a new line in a html based signature is maintained after passing through the defanger.
     * @throws Exception
     */
    public void testBug105001() throws Exception {
        String html =  "<html><body><div> <a href=\"javascript\n: alert('XSS')\">XSS LINK</a>" +
            "<br data-mce-bogus=\"1\"></div></div></body></html>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html = "<a href=\"vbscript\n\n:alert(parent.csrfToken)\">CLICK</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("VBSCRIPT-BLOCKED"));

        html = "<a href=\"Vbscr&amp;#0009;ip&#009;t\n\n:alert(parent.csrfToken)\">CLICK</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("VBSCRIPT-BLOCKED"));

        html = "<a href=\"Vbscr&amp;#0009;ip&#009;t\r\n:alert(parent.csrfToken)\">CLICK</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("VBSCRIPT-BLOCKED"));

        html = "<a href=\"Vbscr&amp;#0009;ip&#009;t\r&#009\n:alert(parent.csrfToken)\">CLICK</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("VBSCRIPT-BLOCKED"));

        html= "<html>"
               +" <body>\n"
               +" <a href=\"j\n"
               +" av\n"
               +" ascript\n"
               +" :\n"
               +"alert(1)\n"
               +"\"\n"
               +">XSS(1)</a>\n"
               +"</body>\n"
               +"</html>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

        html ="<a href=j&#97;v&#97;script&#x3A;&#97;lert(document.domain)>ClickMe</a>";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result.contains("JAVASCRIPT-BLOCKED"));

    }

    @Test
    /**
     * Verify that dfsrc and data-mce-src attributes of img tag are maintained after passing through the defanger.
     * @throws Exception
     */
    public void testBug106162() throws Exception {

        String html = "<div>Thanks</div><div><img src=\"/home/ews01@zdev-vm002.eng.zimbra.com/Briefcase/rupali.jpeg\" "
                + "dfsrc=\"doc:Briefcase/rupali.jpeg\" "
                + "data-mce-src=\"/home/ews01@zdev-vm002.eng.zimbra.com/Briefcase/rupali.jpeg\"></div>";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(result.contains("dfsrc=\"doc:Briefcase/rupali.jpeg\""));
        Assert.assertTrue(result.contains("data-mce-src=\"/home/ews01"));
    }

    @Test
    public void testemoveAnySpacesAndEncodedChars() {
        String  html= "j\n"
              +" av\n"
              +" ascript\n"
              +" :\n"
              +"alert(1)\n"
              +"\n"
              +">XSS(1)</a>\n"
              +"</body>\n"
              +"</html>";
        String result = DefangFilter.removeAnySpacesAndEncodedChars(html);
        Assert.assertTrue(result.startsWith("javascript:"));

        html= "j&#97;v&#97;script&#x3A;:&#97;lert(document.domain)\n"
                +">XSS(1)</a>\n"
                +"</body>\n"
                +"</html>";
        result = DefangFilter.removeAnySpacesAndEncodedChars(html);
        Assert.assertTrue(result.startsWith("javascript:"));
    }
    
    /**
     * Check span does not contain repetition on "'" character
     * @throws Exception
     */
    @Test
    public void testzbug184() throws Exception {
        String fileName = "zbug184.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(!result.contains("'''''"));
    }

    /**
     * Check alert is removed from script.
     */
    @Test
    public void testZCS5696() throws Exception {
        String html = "<textarea></textarea/><body/oNloAd=alert('bug109020-2')>bug109020-2";
        InputStream htmlStream = new ByteArrayInputStream(html.getBytes());
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(!result.contains("alert"));

        html = "<textarea></textarea/><body/oNloAd=javascript:alert('bug109020-2')>bug109020-2";
        htmlStream = new ByteArrayInputStream(html.getBytes());
        result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream, true);
        Assert.assertTrue(!result.contains("alert"));
    }

    /**
     * Checking mime which were causing high CPU utilization
     * @throws Exception
     */
    @Test
    public void testzbug736Mime1() throws Exception {
        Provisioning.getInstance().getConfig().setUseOwaspHtmlSanitizer(false);
        String fileName = "zbug736_2.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        String result = DefangFactory.getDefanger(MimeConstants.CT_TEXT_HTML).defang(htmlStream,
            true);
        Assert.assertTrue(result != null);
    }

    /**
     * Checking zimbraUseOwaspHtmlSanitizer is true
     * @throws Exception
     */
    @Test
    public void testzcs6871OwaspDefanger() throws Exception {
        BrowserDefang defanger2 = DefangFactory.getDefanger(null);
        Assert.assertFalse(defanger2 instanceof OwaspHtmlSanitizer);

        Provisioning.getInstance().getConfig().setUseOwaspHtmlSanitizer(true);
        BrowserDefang defanger = DefangFactory.getDefanger(null);
        Assert.assertTrue(defanger instanceof OwaspHtmlSanitizer);
    }
}
