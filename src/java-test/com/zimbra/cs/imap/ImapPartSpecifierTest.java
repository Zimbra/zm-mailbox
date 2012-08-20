/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
package com.zimbra.cs.imap;

import java.io.IOException;
import java.io.InputStream;

import javax.mail.internet.MimeMessage;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.zmime.ZMimeMessage;
import com.zimbra.cs.util.JMSession;

public class ImapPartSpecifierTest {

    private void checkBody(MimeMessage mm, String part, String modifier, String startsWith, String endsWith)
    throws IOException, ImapPartSpecifier.BinaryDecodingException, ServiceException {
        ImapPartSpecifier pspec = new ImapPartSpecifier("BODY", part, modifier);
        Pair<Long, InputStream> content = pspec.getContent(mm);
        if (startsWith == null) {
            Assert.assertNull(pspec.getSectionSpec() + " is null", content);
        } else {
            Assert.assertNotNull(pspec.getSectionSpec() + " is not null", content.getSecond());
            String data = new String(ByteUtil.getContent(content.getSecond(), content.getFirst().intValue())).trim();
            Assert.assertTrue(pspec.getSectionSpec() + " start matches", data.startsWith(startsWith));
            Assert.assertTrue(pspec.getSectionSpec() + " end matches", data.endsWith(endsWith));
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
