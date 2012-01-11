/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.util.SpamHandler;
import com.zimbra.cs.util.JMSession;

/**
 * Unit tests for spam/whitelist filtering
 */
public class SpamTest {

    @BeforeClass
    public static void init() throws ServiceException {
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
