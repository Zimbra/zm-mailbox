/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2024 Synacor, Inc.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.account;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.soap.account.message.ChangePasswordRequest;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.JaxbUtil;

public class ChangePasswordTest {
    private static final String USERNAME = "ron@zcs.fazigu.org";
    private static final String PASSWORD = "H3@pBigPassw0rd";

    private static final MockProvisioning prov = new MockProvisioning();

    private Account account;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning.setInstance(prov);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        account = prov.createAccount(USERNAME, PASSWORD, new HashMap<String,Object>());
    }

    @Test
    public void testBasicHandlerRun() throws Exception {
        final ChangePasswordRequest request = new ChangePasswordRequest()
            .setAccount(AccountSelector.fromName(USERNAME))
            .setPassword(PASSWORD)
            .setOldPassword(PASSWORD);

        final ChangePassword handler = new ChangePassword();
        final Map<String,Object> context = ServiceTestUtil.getRequestContext(account);

        final Element response = handler.handle(JaxbUtil.jaxbToElement(request), context);
        // for now, only testing that we get a response
        Assert.assertNotNull(response);
    }

	@Test
	public void testNeedsAuth() throws Exception {
        final ChangePassword handler = new ChangePassword();
        final Map<String,Object> context = Collections.emptyMap();

        Assert.assertTrue("handler.needsAuth()", handler.needsAuth(context));
	}

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
        prov.deleteAccount(account.getId());
    }
}

