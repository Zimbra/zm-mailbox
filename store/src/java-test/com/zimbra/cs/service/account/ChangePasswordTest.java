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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.soap.account.message.ChangePasswordRequest;
import com.zimbra.soap.type.AccountSelector;
import com.zimbra.soap.JaxbUtil;

public class ChangePasswordTest {
    private static final String USERNAME_1 = "ron@zcs.fazigu.org";
    private static final String USERNAME_2 = "rob@zcs.fazigu.org";
    private static final String PASSWORD_1 = "H3@pBigPassw0rd";
    private static final String PASSWORD_2 = "An0therP@$$w0rd";

    private static final MockProvisioning prov = new MockProvisioning();

    private Account account1;
    private Account account2;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning.setInstance(prov);
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();

        final Map<String,Object> attrs1 = new HashMap<>(1);
        attrs1.put(Provisioning.A_zimbraId, LdapUtil.generateUUID());
        account1 = prov.createAccount(USERNAME_1, PASSWORD_1, attrs1);

        final Map<String,Object> attrs2 = new HashMap<>(1);
        attrs2.put(Provisioning.A_zimbraId, LdapUtil.generateUUID());
        account2 = prov.createAccount(USERNAME_2, PASSWORD_2, attrs2);
    }

    @Test
    public void testBasicHandlerOK() throws Exception {
        final ChangePasswordRequest request = new ChangePasswordRequest()
            .setAccount(AccountSelector.fromName(USERNAME_1))
            .setOldPassword(PASSWORD_1)
            .setPassword(PASSWORD_1);

        final ChangePassword handler = new ChangePassword();
        final Map<String,Object> context = ServiceTestUtil.getRequestContext(account1);

        final Element response = handler.handle(JaxbUtil.jaxbToElement(request), context);
        Assert.assertNotNull("response", response);

        final String authToken = response.getAttribute(AccountConstants.E_AUTH_TOKEN);
        Assert.assertNotNull("authtoken", authToken);
    }

    @Test
    public void testBasicHandlerWrongAuth() throws Exception {
        final ChangePasswordRequest request = new ChangePasswordRequest()
            .setAccount(AccountSelector.fromName(USERNAME_1)) // Not the authed user from context below
            .setOldPassword(PASSWORD_1)
            .setPassword(PASSWORD_1);

        final ChangePassword handler = new ChangePassword();
        final Map<String,Object> context = ServiceTestUtil.getRequestContext(account2);

        try {
            handler.handle(JaxbUtil.jaxbToElement(request), context);
        } catch (final ServiceException ex) {
            Assert.assertEquals("service exception type", ServiceException.PERM_DENIED, ex.getCode());
            return;
        }

        Assert.fail("expected handler to fail");
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
        prov.deleteAccount(account1.getId());
        prov.deleteAccount(account1.getId());
    }
}

