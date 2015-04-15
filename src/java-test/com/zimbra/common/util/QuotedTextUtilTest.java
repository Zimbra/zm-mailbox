/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.common.util;

import junit.framework.Assert;

import org.junit.Test;

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
        expected = "<HTML>\n<HEAD>\n<META http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
        "</HEAD>\n<BODY xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
        "<DIV style=\"font-family: verdana,helvetica,sans-serif; font-size: 10pt; color: #000000\">\n" +
        "<DIV>Venue Details as Below. Please be there by 07:00PM today evening.</DIV>\n" +
        "</DIV>\n</BODY>\n</HTML>\n";
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
    }
}
