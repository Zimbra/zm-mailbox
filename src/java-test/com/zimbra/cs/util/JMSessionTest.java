/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
package com.zimbra.cs.util;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailclient.smtp.SmtpTransport;
import com.zimbra.cs.mailclient.smtp.SmtpsTransport;

/**
 * Unit test for {@link JMSession}.
 *
 * @author ysasaki
 */
public class JMSessionTest {

    @BeforeClass
    public static void init() throws Exception {
        MockProvisioning prov = new MockProvisioning();
        prov.getLocalServer().setSmtpPort(25);
        Provisioning.setInstance(prov);
    }

    @Test
    public void getTransport() throws Exception {
        Assert.assertSame(SmtpTransport.class,
                JMSession.getSession().getTransport("smtp").getClass());
        Assert.assertSame(SmtpsTransport.class,
                JMSession.getSession().getTransport("smtps").getClass());

        Assert.assertSame(SmtpTransport.class,
                JMSession.getSmtpSession().getTransport("smtp").getClass());
        Assert.assertSame(SmtpsTransport.class,
                JMSession.getSmtpSession().getTransport("smtps").getClass());
    }

}
