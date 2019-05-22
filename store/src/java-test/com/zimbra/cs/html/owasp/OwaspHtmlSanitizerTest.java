/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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
package com.zimbra.cs.html.owasp;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.cs.html.owasp.OwaspHtmlSanitizer;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.mime.ParsedMessage;

public class OwaspHtmlSanitizerTest {

    private static String EMAIL_BASE_DIR = "data/unittest/email/";

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        EMAIL_BASE_DIR = MailboxTestUtil.getZimbraServerDir("") + EMAIL_BASE_DIR;
        OwaspPolicy.load("conf/owasp_policy.xml");
    }

    /*
     * Cut down version of original Bug 101227 test data which was significantly
     * larger
     */
    private static final String urlWithInlinePNG = "background-image:\n  url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAA+/AA\n"
        + "WAP457/97wejL8Ovcj5LuH/FEVEf7d+/etuVtN/OIknPPDGFlCoJlWx/lkf/78/mGP2zUhTzC0AAAAASUVORK5CYII=');\n"
        + "    background-repeat: no-repeat; background-position: center;";

    private static final String defangedUrlWithInlinePNG = "background-repeat:no-repeat;background-position:center";
    private static String htmlTemplateForUrlWithInlinePNG = "<table><tbody><tr><td style=\"%s\"></td></tr></tbody></table>";

    /*
     * Verified in Firefox that multi-line url with this html displays the
     * background image (give a valid paper.gif file from
     * http://www.w3schools.com/cssref/paper.gif) So we definitely should be
     * stripping out multi-line functions Owasp policy doesn't allow text inside
     * style by default
     */
    private static String templateHtmlWithNonInlinedBackgroundImageURL = "<html><head><style>"
        + "</style></head><body><h1>Hello World!</h1></body></html>";
    private static String nonInlinedBackgroundImageURL = "url(\n" + "            \"paper.gif\"\n"
        + "            )";

    private void defangHtmlString(String html, String expected) throws IOException {
        String result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals("Defanged HTML result", expected, result);
    }

    /**
     * Utility method that gets the html body part from a mime message and
     * returns its input stream
     * 
     * @param fileName
     *            The name of the email file to load from the unit test data dir
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
        for (MPartInfo body : bodyparts) {
            if (body.getContentType().contains("html")) {
                htmlStream = body.getMimePart().getInputStream();
            }
        }
        return htmlStream;
    }

    @Test
    public void testHtmlWithStyleValueContainingMultiLineUrl() throws Exception {
        defangHtmlString(String.format(htmlTemplateForUrlWithInlinePNG, urlWithInlinePNG),
            String.format(htmlTemplateForUrlWithInlinePNG, defangedUrlWithInlinePNG));
        defangHtmlString(
            String.format(templateHtmlWithNonInlinedBackgroundImageURL,
                nonInlinedBackgroundImageURL),
            String.format(templateHtmlWithNonInlinedBackgroundImageURL, ""));
    }

    /**
     * Tests to make sure target="_blank" is added to anythign with an href
     * 
     * @throws Exception
     */
    @Test
    public void testBug46948() throws Exception {
        String fileName = "bug_46948.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        String html = CharStreams.toString(new InputStreamReader(htmlStream, Charsets.UTF_8));
        String result = new OwaspHtmlSanitizer(html, true).sanitize();
        // Make sure each area tag has a target
        int index = result.indexOf("<area");
        while (index >= 0) {
            int closingIndex = result.indexOf(">", index);
            int targetIndex = result.indexOf("target=", index);
            // Make sure we got a target
            Assert.assertTrue(targetIndex != -1);
            // make sure its before the closing tag
            Assert.assertTrue(targetIndex < closingIndex);
            index = result.indexOf("<area", index + 1);
        }
    }

    @Test
    public void testBug98215() throws Exception {
        String html = "<a href=\"vbscript:alert(parent.csrfToken)\">CLICK</a>";
        String result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "CLICK"); // a tag removed

        html = "<a href=\"Vbscr&amp;#0009;ip&#009;t:alert(parent.csrfToken)\">CLICK</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "CLICK"); // a tag removed

        html = "<a href=\"java&amp;Tab;script:alert(parent.csrfToken)\">CLICK</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "CLICK"); // a tag removed

        html = "<a href=\"&amp;Tab;javascript:alert(parent.csrfToken)\">CLICK</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "CLICK"); // a tag removed

        html = "<a href=\"javascr&amp;#09;ipt:alert(parent.csrfToken)\">CLICK</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "CLICK"); // a tag removed

        html = "<form id=\"test\" action=\"javascript:alert(1)\"><p>test</p>"
            + "<button form=\"test\">Test</button></form>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        // action attribute removed
        Assert.assertEquals(result, "<form id=\"test\"><p>test</p><button>Test</button></form>");

        html = "<form id=\"test\" action=\"ja&amp;Tab;vascript:alert(1)\"><p>test</p>"
            + "<button form=\"test\">Test</button></form>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        // action attribute removed
        Assert.assertEquals(result, "<form id=\"test\"><p>test</p><button>Test</button></form>");

        html = "<a href=\"&amp;#009;java&#00009;scr&amp;#09;i\t\tpt:alert(parent.csrfToken)\">CLICK</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "CLICK"); // a tag removed
    }

    @Test
    /*
     * Verify that a new line in a html based signature is maintained after
     * passing through the defanger.
     * 
     * @throws Exception
     */
    public void testBug105001() throws Exception {
        String html = "<html><body><div> <a href=\"javascript\n: alert('XSS')\">XSS LINK</a>"
            + "<br data-mce-bogus=\"1\"></div></div></body></html>";
        String result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "<html><body><div> XSS LINK<br /></div></body></html>");

        html = "<a href=\"vbscript\n\n:alert(parent.csrfToken)\">CLICK</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        // a tag removed
        Assert.assertEquals(result, "CLICK");

        html = "<a href=\"Vbscr&amp;#0009;ip&#009;t\n\n:alert(parent.csrfToken)\">CLICK</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        // a tag removed
        Assert.assertEquals(result, "CLICK");

        html = "<a href=\"Vbscr&amp;#0009;ip&#009;t\r\n:alert(parent.csrfToken)\">CLICK</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        // a tag removed
        Assert.assertEquals(result, "CLICK");

        html = "<a href=\"Vbscr&amp;#0009;ip&#009;t\r&#009\n:alert(parent.csrfToken)\">CLICK</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        // a tag removed
        Assert.assertEquals(result, "CLICK");

        html = "<html>" + " <body>\n" + " <a href=\"j\n" + " av\n" + " ascript\n" + " :\n"
            + "alert(1)\n" + "\"\n" + ">XSS(1)</a>\n" + "</body>\n" + "</html>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "<html><body>\n XSS(1)\n</body></html>");

        html = "<a href=j&#97;v&#97;script&#x3A;&#97;lert(document.domain)>ClickMe</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        // a tag removed
        Assert.assertEquals(result, "ClickMe");

    }

    @Test
    public void testBug82303() throws Exception {
        String html = "<a href=\"http://ebobby.org/2013/05/18/"
            + "Fun-with-Javascript-and-function-tracing.html\" "
            + "style=\"color: #187AAB; text-decoration: none\" target=\"_blank\">";
        String result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertTrue(result.contains("Fun-with-Javascript-and-function-tracing.html"));

        html = "<a href=\"javascript-and-function-tracing.html\" "
            + "style=\"color: #187AAB; text-decoration: none\" target=\"_blank\">";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertTrue(result.contains("javascript-and-function-tracing.html"));

        html = "<a href=\"javascript:myJsFunc()\">Link Text</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertFalse(result.contains("javascript"));

        html = "<a href=\"javascriptlessDestination.html\" onclick=\"myJSFunc(); "
            + "return false;\">Link text</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertTrue(result.contains("javascriptlessDestination.html"));

        html = "<a href=\"javascript:alert('Hello');\"></a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertFalse(result.contains("javascript"));

        html = "<a href=\"http://ebobby.org/2013/05/18/" + "javascript/Lessonsinjavascript.html\" "
            + "style=\"color: #187AAB; text-decoration: none\" target=\"_blank\">";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertTrue(result.contains("javascript/Lessonsinjavascript.html"));

        html = "<a href='javascript:myFunction()'> Click Me! <a/>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertFalse(result.contains("javascript"));

        html = " <a href=\"javascript:void(0)\" onclick=\"loadProducts(<?php echo $categoryId ?>)\"> ";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertFalse(result.contains("javascript"));

        html = "<a href=\"#\" onclick=\"someFunction();\" return false;\">LINK</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertTrue(result.equals("<a href=\"#\" rel=\"nofollow\">LINK</a>"));

        html = "<a href='javascript:my_Function()'> Click Me! <a/>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertFalse(result.contains("javascript"));

        html = "<a href='javascript:myFunction(field1, field2)'> Click Me! <a/>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertFalse(result.contains("javascript"));

        html = "<a href='javaScript:document.f1.findString(this.t1.value)'>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertFalse(result.contains("javascript"));

        html = "<a href='javaScript:document.f1.findString(this.t1.value)'>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertFalse(result.contains("javascript"));

        html = "<a href=\"#\" onclick=\"findString(document.getElementById('t1').value); return false;\">Click Me</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertTrue(result.contains("<a href=\"#\" rel=\"nofollow\">Click Me</a>"));

        html = "<a href=\"javascript:alert('0');\">Click Me</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertFalse(result.contains("javascript"));

        html = "<a href=\"  javascript:alert('0');\">Click Me</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertFalse(result.contains("javascript"));

    }

    /**
     * Checks that rgb() in style value is not removed.
     * @throws Exception
     */
    @Test
    public void testBug67537() throws Exception {
        String html = "<html><body><span style=\"color: rgb(255, 0, 0);\">This is RED</span></body></html>";
        String result = new OwaspHtmlSanitizer(html,true).sanitize();
        Assert.assertTrue(result.contains("style=\"color:rgb( 255 , 0 , 0 )\""));
    }

    @Test
    public void testBug76500() throws Exception {
        String html = "<blockquote style=\"border-left:2px solid rgb(16, 16, 255);\">";
        String result = new OwaspHtmlSanitizer(html,true).sanitize();
        Assert.assertTrue(result.contains("rgb( 16 , 16 , 255 )"));
    }

    @Test
    public void testBug97443() throws Exception {
        String html = "<html><head></head><body><table><tr><td><B>javascript-blocked test </B></td>"
            + "</tr><tr><td><a href=\"javascript:alert('Hello!');\">alert</a>"
            + "</td></tr></table></body></html>";
        String result = new OwaspHtmlSanitizer(html,true).sanitize();
        Assert.assertTrue(result
            .contains("<body><table><tbody><tr><td><b>javascript-blocked test </b></td></tr><tr><td>alert</td></tr></tbody></table></body>"));

        html = "<html><head><base href=\"http://lbpe.wikispaces.com/\" /></head><body>"
            + "<table><tr><td><B>javascript-blocked test</B></td></tr><tr><td>"
            + "<a href=\"javascript:alert('Hello!');\">alert</a></td></tr></table>"
            + "</body></html>";
        result = new OwaspHtmlSanitizer(html,true).sanitize();
        Assert.assertTrue(result
                .contains("<body><table><tbody><tr><td><b>javascript-blocked test</b></td></tr><tr><td>alert</td></tr></tbody></table></body>"));
    }
    
    @Test
    public void testBug78902() throws Exception {

        String html = "<html><head></head><body><a target=\"_blank\" href=\"Neptune.gif\"></a></body></html>";
        String result = new OwaspHtmlSanitizer(html,true).sanitize();
        Assert.assertTrue(result
                .contains("<a href=\"Neptune.gif\" target=\"_blank\" rel=\"nofollow noopener noreferrer\"></a>"));


        html = "<html><body>My pictures <a href=\"javascript:document.write('%3C%61%20%68%72%65%66%3D%22%6A%61%76%"
            + "61%73%63%72%69%70%74%3A%61%6C%65%72%74%28%31%29%22%20%6F%6E%4D%6F%75%73%65%4F%76%65%72%3D%61%6C%65%"
            + "72%74%28%5C%22%70%30%77%6E%5C%22%29%3E%4D%6F%75%73%65%20%6F%76%65%72%20%68%65%72%65%3C%2F%61%3E')\">here</a></body></html>";
        result = new OwaspHtmlSanitizer(html,true).sanitize();
        Assert.assertEquals(result,
                "<html><body>My pictures here</body></html>");

        html =  "<html><head></head><body><a target=\"_blank\" href=\"Neptune.txt\"></a></body></html>";
        result = new OwaspHtmlSanitizer(html,true).sanitize();
        Assert.assertEquals(result,
                "<html><head></head><body><a href=\"Neptune.txt\" target=\"_blank\" rel=\"nofollow noopener noreferrer\"></a></body></html>");

        html =  "<html><head></head><body><a target=\"_blank\" href=\"Neptune.pptx\"></a></body></html>";
        result = new OwaspHtmlSanitizer(html,true).sanitize();
        Assert.assertEquals(result,
                "<html><head></head><body><a href=\"Neptune.pptx\" target=\"_blank\" rel=\"nofollow noopener noreferrer\"></a></body></html>");

        html = "<li><a href=\"poc.zip?view=html&archseq=0\">\"/><script>alert(1);</script>AAAAAAAAAA</a></li>";
        result = new OwaspHtmlSanitizer(html,true).sanitize();
        Assert.assertTrue(!result
                .contains("<script>"));
    }

    @Test
    public void testBug101813() throws Exception {
        String html = "<textarea><img title=\"</<!-- -->textarea><img src=x onerror=alert(1)></img>";
        String result = new OwaspHtmlSanitizer(html, true).sanitize();
        // make sure that the javascript content is escaped
        Assert.assertTrue(result.contains("onerror&#61;alert(1)&gt;"));

        html = "<textarea><IMG title=\"</<!-- -->textarea><img src=x onerror=alert(1)></img>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertTrue(result.contains("onerror&#61;alert(1)&gt;"));

        html = "<textarea><   img title=\"</<!-- -->textarea><img src=x onerror=alert(1)></img>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertTrue(result.contains("onerror&#61;alert(1)&gt;"));
    }

    /**
     * Tests to make sure we properly defang images that are neither inline/internal nor external images.
     * @throws Exception
     */
    @Test
    public void testBug64903() throws Exception {
        String fileName = "bug_60769.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        String html = CharStreams.toString(new InputStreamReader(htmlStream, Charsets.UTF_8));
        String result = new OwaspHtmlSanitizer(html,true).sanitize();
        Assert.assertFalse(result.contains("src=\"image001.gif\""));
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
        String html = CharStreams.toString(new InputStreamReader(htmlStream, Charsets.UTF_8));
        String result = new OwaspHtmlSanitizer(html,true).sanitize();
        // just make sure we made it here, as this was NPEing out..
        Assert.assertNotNull(result);
        // Make sure the input got changed
        Assert.assertFalse(result.contains("src=\"http://www.google.com/intl/en_com/images/srpr/logo3w.png\""));
    }

    /**
     * Checks to ensure that we're properly removing src for input tags as well.
     * @throws Exception
     */
    @Test
    public void testBug58889() throws Exception {
        String fileName = "bug_58889.txt";
        InputStream htmlStream = getHtmlBody(fileName);
        Assert.assertNotNull(htmlStream);
        String html = CharStreams.toString(new InputStreamReader(htmlStream, Charsets.UTF_8));
        String result = new OwaspHtmlSanitizer(html,true).sanitize();
        // just make sure we made it here, as this was NPEing out..
        Assert.assertNotNull(result);
        Assert.assertFalse(result.contains(" src=\"https://grepular.com/email_privacy_tester/"));
    }

    /**
     * Checks that CDATA section in HTML is reported as a comment and removed.
     * @throws Exception
     */
    @Test
    public void testBug64974() throws Exception {
        String html = "<html><body><![CDATA[--><a href=\"data:text/html;base64,PHNjcmlwdD4KYWxlcnQoZG9jdW1lbnQuY29va2llKQo8L3NjcmlwdD4=\">click</a]]></body></html>";
        String result = new OwaspHtmlSanitizer(html,true).sanitize();
        Assert.assertTrue(result.equals("<html><body>click</body></html>"));
    }

    @Test
    /**
     * Verify that a new line in a html based signature is maintained after passing through the defanger.
     * @throws Exception
     */
    public void testBug104666() throws Exception {
        String html = "<div></div><div></div><div id=\"5589f382-9e9b-47cd-ab09-3ea973fd4f6a\" data-marker=\"__SIG_PRE__\">"
            + "<div>LIne 1</div>" + "</div>" + "<div>Line 2</div>" + "</div>";
        String result = new OwaspHtmlSanitizer(html,true).sanitize();
        Assert.assertTrue(result.contains("<div>LIne 1</div></div><div>Line 2</div>"));

        html = "<div>Thanks</div><div><img src=\"/home/ews01@zdev-vm002.eng.zimbra.com/Briefcase/rupali.jpeg\" "
                + "data-mce-src=\"/home/ews01@zdev-vm002.eng.zimbra.com/Briefcase/rupali.jpeg\"></div>";
        result = new OwaspHtmlSanitizer(html,true).sanitize();
        Assert.assertTrue(result.contains("data-mce-src=\"/home/ews01"));
    }

    @Test
    /**
     * Verify that data-mce-src attributes of img tag are maintained after passing through the defanger.
     * @throws Exception
     */
    public void testBug106162() throws Exception {

        String html = "<div>Thanks</div><div><img src=\"/home/ews01@zdev-vm002.eng.zimbra.com/Briefcase/rupali.jpeg\" "
                + "data-mce-src=\"/home/ews01@zdev-vm002.eng.zimbra.com/Briefcase/rupali.jpeg\"></div>";
        String result = new OwaspHtmlSanitizer(html,true).sanitize();
        Assert.assertTrue(result.contains("data-mce-src=\"/home/ews01"));
    }

    @Test
    public void testBug85478() throws Exception {
        String html = "<a href=\"data:text/html;base64,PHNjcmlwdD5hbGVydCgiSGVsbG8hIik7PC9zY3JpcHQ+\" "
            + "data-mce-href=\"data:text/html;base64,PHNjcmlwdD5hbGVydCgiSGVsbG8hIik7PC9zY3JpcHQ+\">Bug</a>";
        String result = new OwaspHtmlSanitizer(html, true).sanitize();
        // make sure that it removed the href link with 'data' URI
        Assert.assertEquals(result, "Bug");

        html = "<a href=\"data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAErkJggg==\" />Bug</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "Bug");

        html = "<a target=_blank href=\"data:text/html,<script>alert(opener.document.body.innerHTML)</script>\">"
            + " clickme in Opera/FF</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "<a target=\"_blank\"> clickme in Opera/FF</a>");

        html = "<a target=_blank href=\"data.html\"> Data fIle</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertTrue(result.contains("data.html"));

        html = "<a href=\"data:;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAErkJggg==\" />Bug</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "Bug");

        // make sure that it doesn't remove the img src with 'data' URI
        html = "<img src=\"data:image/jpeg;base64,/9j/4AAAAAxITGlubwIQAABtbnRyUkdCI\"><br>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertTrue(
            result.contains("data:image/jpeg;base64,/9j/4AAAAAxITGlubwIQAABtbnRyUkdCI"));

        html = "<img src=\"DaTa:image/jpeg;base64,/9j/4AAAAAxITGlubwIQAABtbnRyUkdCI\"><br>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertTrue(
            result.contains("DaTa:image/jpeg;base64,/9j/4AAAAAxITGlubwIQAABtbnRyUkdCI"));

        html = "<a href=\"DATA:;base64,iVBORw0KGgoAAAANSUhEUgAAAAUAErkJggg==\" />Bug</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "Bug");

        html = "<a href=\"data\n\n:\n\ntext/html;base64,PHNjcmlwdD5hbGVydCgiSGVsbG8hIik7PC9zY3JpcHQ+\">Bug</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "Bug");

        html = "<a href=\"data\r\n:text/html;base64,PHNjcmlwdD5hbGVydCgiSGVsbG8hIik7PC9zY3JpcHQ+\">Bug</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "Bug");

        html = "<a href=\"data:text/html;base64,PHNjcmlwdD5hbGVydCgiSGVsbG8hIik7PC9zY3JpcHQ+\">Bug</a>";
        result = new OwaspHtmlSanitizer(html, true).sanitize();
        Assert.assertEquals(result, "Bug");

    }

}
