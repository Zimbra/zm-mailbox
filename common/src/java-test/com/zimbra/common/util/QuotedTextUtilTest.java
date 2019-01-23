/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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

package com.zimbra.common.util;

import javax.xml.XMLConstants;
import javax.xml.transform.TransformerFactory;

import org.junit.Test;

import junit.framework.Assert;

public class QuotedTextUtilTest {

    /**
     * Test method for {@link com.zimbra.common.util.QuotedTextUtil#getOriginalContent(java.lang.String, boolean)}.
     */
    @Test
    public void testGetOriginalContent() {
        QuotedTextUtil quotedTextUtil = new QuotedTextUtil();

        String content = "Hello Roland, nope, there's no way of modifying the javadocs.\nDavid\n" +
        "----- Original Message ----- \n From: \"Roland Smith\" <smith@stanford.edu>\n" +
            "> This is test quoted text";
        String originalContent = quotedTextUtil.getOriginalContent(content, false);
        String expected = "Hello Roland, nope, there's no way of modifying the javadocs.\nDavid\n";
        Assert.assertEquals(expected, originalContent);

        content = "<fixed><bigger><bigger>Thanks for the help.  It has been invaluable.\n" +
        "Tom Condon\n" +
        "</bigger></bigger></fixed>\n" +
        "On Dec 4, 2003, at 4:30 PM, David Bau wrote:\n" +
        "<excerpt>In XML and XML Schema\n" +
        "<<body>body<</body>\n" +
        "(or document)<<body>.\n" +
        "David";
        originalContent = quotedTextUtil.getOriginalContent(content, false);
        expected = "<fixed><bigger><bigger>Thanks for the help.  It has been invaluable.\n" +
            "Tom Condon\n" +
            "</bigger></bigger></fixed>\n";
        Assert.assertEquals(expected, originalContent);

        content = "<html><body><div style=\"font-family: verdana,helvetica,sans-serif; " +
            "font-size: 10pt; color: #000000\"><div>Venue Details as Below. " +
            "Please be there by 07:00PM today evening.</div>" +
            "<hr id=\"zwchr\">The following is a new meeting request: <br><br>Subject: " +
            "Dinner with Thom <br>Organizer: &quot;Avanti Gowaikar (C)&quot; &lt;agowaikar@zimbra.com&gt;" +
            "<br><br>Time: Wednesday, January 28, 2015, 7:00:00 PM - 10:00:00 PM GMT +05:30 Chennai, " +
            "Kolkata, Mumbai, New Delhi <br><br>Invitees: pune-employees@zimbra.com <br><br>" +
            "<br>*~*~*~*~*~*~*~*~*~*<br></div></body></html>";
        originalContent = quotedTextUtil.getOriginalContent(content, true);
        expected = "<HTML>\n    <HEAD>\n        <META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
        "    </HEAD>\n    <BODY xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
        "        <DIV style=\"font-family: verdana,helvetica,sans-serif; font-size: 10pt; color: #000000\">\n" +
        "            <DIV>Venue Details as Below. Please be there by 07:00PM today evening.</DIV>\n" +
        "        </DIV>\n    </BODY>\n</HTML>\n";
        Assert.assertEquals(expected, originalContent);

        content = "FYI...\n"
            + "----- Forwarded Message ----- \n From: \"Demo User One\" <user1@host.local>\n"
            + "To: \"Three, Demo\" <user3@host.local>\n"
            + "Sent: Monday, March 30, 2015 6:27:17 PM\n" + "Subject: Fwd: test mail" + "new mail";
        originalContent = quotedTextUtil.getOriginalContent(content, false);
        expected = "FYI...\n";
        Assert.assertEquals(expected, originalContent);

        content = ""
            + "\n----- Forwarded Message ----- \n From: \"Demo User One\" <user1@host.local>\n"
            + "To: \"Three, Demo\" <user3@host.local>\n"
            + "Sent: Monday, March 30, 2015 6:27:17 PM\n" + "Subject: Fwd: test mail" + "new mail";
        originalContent = quotedTextUtil.getOriginalContent(content, false);
        expected = "";
        Assert.assertEquals(expected, originalContent);

        content = ""
            + "\n From: \"Demo User One\" <user1@host.local>\n"
            + "To: \"Three, Demo\" <user3@host.local>\n"
            + "Sent: Monday, March 30, 2015 6:27:17 PM\n" + "Subject: Fwd: test mail" + "new mail";
        originalContent = quotedTextUtil.getOriginalContent(content, false);
        expected = "";
        Assert.assertEquals(expected, originalContent);

        content = "<div dir=\"ltr\"><div class=\"gmail_quote\"><br><br><br><div dir=\"ltr\">"
            + "<span style=\"font-size:12.8000001907349px\">asdasdasdasasfasfasfasfasfasfa</span>"
            + "<span class=\"HOEnZb\"><font color=\"#888888\"><br clear=\"all\"><div><br></div>-- "
            + "<br><div><div dir=\"ltr\">Thanks and regards,<div>Saurabh</div></div></div>"
            + "</font></span></div></div>";
        originalContent = quotedTextUtil.getOriginalContent(content, true);
        expected = "<div dir=\"ltr\"><div class=\"gmail_quote\"><br><br><br><div dir=\"ltr\">"
            + "<span style=\"font-size:12.8000001907349px\">asdasdasdasasfasfasfasfasfasfa</span><span class=\"HOEnZb\">"
            + "<font color=\"#888888\"><br clear=\"all\"><div><br></div>-- <br>"
            + "<div><div dir=\"ltr\">Thanks and regards,<div>Saurabh</div></div></div></font>"
            + "</span></div></div>";
        Assert.assertEquals(expected, originalContent);

        content = "<div dir=\"ltr\">This is my reply<br><div><div class=\"gmail_extra\"><br>"
            + "<div class=\"gmail_quote\">On Fri, Sep 4, 2015 at 4:35 PM, Rohan Ambasta "
            + "<span dir=\"ltr\">&lt;<a href=\"mailto:rambasta@zimbra.com\" target=\"_blank\">"
            + "rambasta@zimbra.com</a>&gt;</span> wrote:<br><blockquote class=\"gmail_quote\""
            + "style=\"margin:0 0 0 .8ex;border-left:1px #ccc solid;padding-left:1ex\"><div>"
            + "<div style=\"font-family:arial,helvetica,sans-serif;font-size:12pt;color:#000000\">"
            + "<div>teat mail re 1<br></div></div></div></blockquote></div><br></div></div></div>";
        originalContent = quotedTextUtil.getOriginalContent(content, true);
        expected = "<HTML>\n    <HEAD>\n        <META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n"
            + "    </HEAD>\n    <BODY xmlns=\"http://www.w3.org/1999/xhtml\">\n"
            + "        <DIV dir=\"ltr\">\n            This is my reply\n            <BR>\n            <DIV>\n                <DIV class=\"gmail_extra\">\n"
            + "                    <BR>\n                </DIV>\n            </DIV>\n        </DIV>\n    </BODY>\n</HTML>\n";
        Assert.assertEquals(expected, originalContent);
    }

    @Test
    public void testMakeTransformerFactory() {
        TransformerFactory factory = QuotedTextUtil.makeTransformerFactory();
        Assert.assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_DTD));
        Assert.assertEquals("", factory.getAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET));
    }
}

