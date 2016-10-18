/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.imap;

import java.io.IOException;
import java.io.InputStream;

import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.InputStreamWithSize;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.imap.ImapPartSpecifier.BinaryDecodingException;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.util.JMSession;

public class ImapPartSpecifierTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    private void checkBody(MimeMessage mm, String part, String modifier, String startsWith, String endsWith)
    throws IOException, ImapPartSpecifier.BinaryDecodingException, ServiceException {
        checkPartial(mm, part, modifier, -1, -1, startsWith, endsWith);
    }

    private void checkPartial(MimeMessage mm, String part, String modifier, int start, int count, String startsWith, String endsWith) throws IOException, BinaryDecodingException, ServiceException {
        ImapPartSpecifier pspec = new ImapPartSpecifier("BODY", part, modifier, start, count);
        InputStreamWithSize content = pspec.getContent(mm);
        if (startsWith == null) {
            Assert.assertNull(pspec.getSectionSpec() + " is null", content);
        } else {
            Assert.assertNotNull(pspec.getSectionSpec() + " is not null", content.stream);
            String data = new String(ByteUtil.getContent(content.stream, content.size.intValue())).trim();
            if (startsWith.length() > 0) {
                Assert.assertTrue(pspec.getSectionSpec() + " start matches", data.startsWith(startsWith));
            } else {
                Assert.assertTrue(pspec.getSectionSpec() + " start empty", data.length() == 0);
            }
            if (endsWith.length() > 0) {
                Assert.assertTrue(pspec.getSectionSpec() + " end matches", data.endsWith(endsWith));
            } else {
                Assert.assertTrue(pspec.getSectionSpec() + " end empty", data.length() == 0);
            }
        }
    }

    @Test
    public void structure() throws Exception {
        InputStream is = getClass().getResourceAsStream("toplevel-nested-message");
        MimeMessage mm = new ZMimeMessage(JMSession.getSession(), is);

        checkBody(mm, "", "HEADER", "X-Gmail-Received: ea9cadad8b81db887fe7ca769a31384c1468b618", "X-OriginalArrivalTime: 24 Feb 2005 01:28:49.0382 (UTC) FILETIME=[31002860:01C51A10]");
        checkBody(mm, "", "TEXT", "Return-Path: <pubcookie-dev-bounces@mailman.u.washington.edu>", "http://mailman.u.washington.edu/mailman/listinfo/pubcookie-dev");
        checkBody(mm, "1", "", "Return-Path: <pubcookie-dev-bounces@mailman.u.washington.edu>", "http://mailman.u.washington.edu/mailman/listinfo/pubcookie-dev");
        checkBody(mm, "1.1", "", "On Fri, 27 Feb 2004, Jim Fox wrote:", "http://mailman.u.washington.edu/mailman/listinfo/pubcookie-dev");
        checkBody(mm, "1", "MIME", "X-Gmail-Received: ea9cadad8b81db887fe7ca769a31384c1468b618", "X-OriginalArrivalTime: 24 Feb 2005 01:28:49.0382 (UTC) FILETIME=[31002860:01C51A10]");
        checkBody(mm, "1", "HEADER", "Return-Path: <pubcookie-dev-bounces@mailman.u.washington.edu>", "X-Evolution: 00000001-0110");
        checkBody(mm, "1", "TEXT", "On Fri, 27 Feb 2004, Jim Fox wrote:", "http://mailman.u.washington.edu/mailman/listinfo/pubcookie-dev");
        checkBody(mm, "1.1", "MIME", "Return-Path: <pubcookie-dev-bounces@mailman.u.washington.edu>", "X-Evolution: 00000001-0110");

        checkPartial(mm, "", "", 0, 100000, "Return-Path: <pubcookie-dev-bounces@mailman.u.washington.edu>", "http://mailman.u.washington.edu/mailman/listinfo/pubcookie-dev");
        checkPartial(mm, "", "", 0, 10, "Return-Pat", "Pat");
        checkPartial(mm, "", "", 1, 10, "eturn-Path", "Path");
        checkPartial(mm, "", "", 100000, 10, "", "");

        is = getClass().getResourceAsStream("calendar-bounce");
        mm = new ZMimeMessage(JMSession.getSession(), is);
        checkBody(mm, "", "HEADER", "Received: from localhost (localhost.localdomain [127.0.0.1])", "Message-Id: <20061109004631.07DB1810C39@mta02.zimbra.com>");
        checkBody(mm, "", "TEXT", "This is a MIME-encapsulated message.", "--6E4E3810C27.1163033191/mta02.zimbra.com--");
        checkBody(mm, "1", "", "This is the Postfix program at host mta02.zimbra.com.", "c18si105209hub (in reply to RCPT TO command)");
        checkBody(mm, "1", "MIME", "Content-Description: Notification", "Content-Type: text/plain");
        checkBody(mm, "2", "", "Reporting-MTA: dns; mta02.zimbra.com", "said: 550 5.1.1 No such user c18si105209hub (in reply to RCPT TO command)");
        checkBody(mm, "2", "MIME", "Content-Description: Delivery report", "Content-Type: message/delivery-status");
        checkBody(mm, "3", "", "Received: from dogfood.zimbra.com (dogfood.liquidsys.com [66.92.25.198])", "------=_Part_235_901532167.1163033483782--");
        checkBody(mm, "3", "MIME", "Content-Description: Undelivered Message", "Content-Transfer-Encoding: 8bit");
        checkBody(mm, "3", "HEADER", "Received: from dogfood.zimbra.com (dogfood.liquidsys.com [66.92.25.198])", "boundary=\"----=_Part_235_901532167.1163033483782\"");
        checkBody(mm, "3", "TEXT", "------=_Part_235_901532167.1163033483782", "------=_Part_235_901532167.1163033483782--");
        checkBody(mm, "3.1", "", "The following is a new meeting request:", "Testing bounce messages.");
        checkBody(mm, "3.1", "MIME", "Content-Type: text/plain; charset=utf-8", "Content-Transfer-Encoding: 7bit");
        checkBody(mm, "3.2", "", "<html><body><h3>The following is a new meeting request:</h3>", "<div>*~*~*~*~*~*~*~*~*~*</div><br>Testing bounce messages.</body></html>");
        checkBody(mm, "3.2", "MIME", "Content-Type: text/html; charset=utf-8", "Content-Transfer-Encoding: 7bit");
        checkBody(mm, "3.3", "", "BEGIN:VCALENDAR", "END:VCALENDAR");
        checkBody(mm, "3.3", "MIME", "Content-Type: text/calendar; name=meeting.ics; method=REQUEST; charset=utf-8", "Content-Transfer-Encoding: 7bit");

    }
}
