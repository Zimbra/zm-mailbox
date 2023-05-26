/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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
package com.zimbra.cs.filter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mime.ParsedMessage;

import java.util.Properties;
import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import javax.mail.Session;

import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(MockitoJUnitRunner.class)
public class OutgoingMessageHandlerTest {

    private static String sampleMsg = "from: xyz@example.com\n"
            + "Subject: test message\n"
            + "to: foo@example.com, baz@example.com\n"
            + "cc: qux@example.com\n"
            + "Subject: Bonjour\n"
            + "MIME-Version: 1.0\n"
            + "Content-Type: multipart/mixed; boundary=\"----=_Part_64_1822363563.1505482033554\"\n"
            + "\n"
            + "------=_Part_64_1822363563.1505482033554\n"
            + "Content-Type: text/plain; charset=utf-8\n"
            + "Content-Transfer-Encoding: 7bit\n"
            + "\n"
            + "Test message 2\n"
            + "------=_Part_64_1822363563.1505482033554\n"
            + "Content-Type: message/rfc822\n"
            + "Content-Disposition: attachment\n"
            + "\n"
            + "Date: Fri, 15 Sep 2017 22:26:43 +0900 (JST)\n"
            + "From: admin@synacorjapan.com\n"
            + "To: user1 <user1@synacorjapan.com>\n"
            + "Message-ID: <523389747.44.1505482003470.JavaMail.zimbra@synacorjapan.com>\n"
            + "Subject: Hello\n"
            + "MIME-Version: 1.0\n"
            + "Content-Type: multipart/alternative; boundary=\"=_37c6ca38-873e-4a06-ad29-25a254075e83\"\n"
            + "\n"
            + "--=_37c6ca38-873e-4a06-ad29-25a254075e83\n"
            + "Content-Type: text/plain; charset=utf-8\n"
            + "Content-Transfer-Encoding: 7bit\n"
            + "\n"
            + "This is a sample email\n"
            + "\n"
            + "--=_37c6ca38-873e-4a06-ad29-25a254075e83\n"
            + "Content-Type: text/html; charset=utf-8\n"
            + "Content-Transfer-Encoding: 7bit\n"
            + "\n"
            + "<html><body><div style=\"font-family: arial, helvetica, sans-serif; font-size: 12pt; color: #000000\"><div>Test message</div></div></body></html>\n"
            + "--=_37c6ca38-873e-4a06-ad29-25a254075e83--\n"
            + "\n"
            + "------=_Part_64_1822363563.1505482033554--\n";

    private OutgoingMessageHandler outgoingMessageHandler;
    private static OperationContext operationContext;
    private static Mailbox mailbox;
    private Session session;

    @Before
    public void setup() throws MessagingException, ServiceException{
        mailbox = Mockito.mock(Mailbox.class);
        operationContext = Mockito.mock(OperationContext.class);
        Properties props = Mockito.mock(Properties.class);
        session = Session.getDefaultInstance(props);
        MimeMessage mm = new MimeMessage(session);
        mm.setText(sampleMsg);
        outgoingMessageHandler = new OutgoingMessageHandler(mailbox, new ParsedMessage(mm, true), 0, false, 0, null, 0, operationContext);
    }
    
    @Test
    public void testgetMessageSize() {
        int size = outgoingMessageHandler.getMessageSize();
        assertNotNull(size);
        assertTrue(size > -1);
    }
}
