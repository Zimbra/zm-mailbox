/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.util.SpamHandler;
import com.zimbra.cs.util.JMSession;

/**
 * Unit tests for spam/whitelist filtering
 */
public class SpamTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        MockProvisioning prov = new MockProvisioning();
        Provisioning.setInstance(prov);
        Config config = prov.getConfig();
        config.setSpamWhitelistHeader("X-Whitelist-Flag");
        config.setSpamWhitelistHeaderValue("YES");
    }

    /**
     * Tests whitelisting takes precedence over marking spam.
     */
    @Test
    public void whitelist() throws Exception {
        String raw = "From: sender@zimbra.com\n" +
                "To: recipient@zimbra.com\n" +
                "X-Spam-Flag: YES\n" +
                "Subject: test\n" +
                "\n" +
                "Hello World.";
        MimeMessage msg = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(raw.getBytes()));
        Assert.assertTrue(SpamHandler.isSpam(msg));

        // add a whitelist header to the previous message
        raw = "From: sender@zimbra.com\n" +
                "To: recipient@zimbra.com\n" +
                "X-Whitelist-Flag: YES\n" +
                "X-Spam-Flag: YES\n" +
                "Subject: test\n" +
                "\n" +
                "Hello World.";
        msg = new Mime.FixedMimeMessage(JMSession.getSession(), new SharedByteArrayInputStream(raw.getBytes()));
        Assert.assertFalse(SpamHandler.isSpam(msg));
    }
}
