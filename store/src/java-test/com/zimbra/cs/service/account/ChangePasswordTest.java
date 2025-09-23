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
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.ChangePasswordRequest;
import com.zimbra.soap.type.AccountSelector;

public class ChangePasswordTest {
    private static final String USERNAME_1 = "ron@zcs.fazigu.org";
    private static final String USERNAME_2 = "rob@zcs.fazigu.org";
    private static final String USERNAME_3 = "rock@zcs.fazigu.org";
    private static final String USERNAME_4 = "testEnabled@zcs.fazigu.org";
    private static final String USERNAME_5 = "testDisabled@zcs.fazigu.org";
    private static final String PASSWORD_1 = "H3@pBigPassw0rd";
    private static final String PASSWORD_2 = "An0therP@$$w0rd";
    private static final String PASSWORD_3 = "An0therP@1$$w0rd";
    private static final String PASSWORD_4 = "An0therP@1$$word";
    private static final String PASSWORD_5   = "AnotherP@1$$w0rd";

    private static final MockProvisioning prov = new MockProvisioning();

    private Account account1;
    private Account account2;
    private Account account3;
    private Account account4;
    private Account account5;

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

        final Map<String,Object> attrs3 = new HashMap<>(1);
        attrs3.put(Provisioning.A_zimbraId, LdapUtil.generateUUID());
        attrs3.put(Provisioning.A_zimbraPasswordMustChange, "TRUE");
        account3 = prov.createAccount(USERNAME_3, PASSWORD_3, attrs3);

        final Map<String,Object> attrs4 = new HashMap<>(1);
        attrs4.put(Provisioning.A_zimbraFeatureChangePasswordEnabled, "TRUE");
        account4 = prov.createAccount(USERNAME_4, PASSWORD_4, attrs4);

        final Map<String,Object> attrs5 = new HashMap<>(1);
        attrs5.put(Provisioning.A_zimbraFeatureChangePasswordEnabled, "FALSE");
        account5 = prov.createAccount(USERNAME_5, PASSWORD_5, attrs5);
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
    public void testNeedsAuth() {
        final ChangePassword handler = new ChangePassword();
        final Map<String,Object> context = Collections.emptyMap();

        Assert.assertFalse("handler.needsAuth()", handler.needsAuth(context));
    }

    @Test
    public void testZimbraFeatureChangePasswordEnabled() throws Exception {
        final ChangePasswordRequest request = new ChangePasswordRequest()
                .setAccount(AccountSelector.fromName(USERNAME_4)) // Not the authed user from context below
                .setOldPassword(PASSWORD_4)
                .setPassword(PASSWORD_4);
        final ChangePassword handler = new ChangePassword();
        final Map<String,Object> context = ServiceTestUtil.getRequestContext(account4);
        Element response = null;
        try {
            response = handler.handle(JaxbUtil.jaxbToElement(request), context);
        } catch (final ServiceException ex) {
            Assert.fail("expected handler to execute the request");
        }
        Assert.assertNotNull("response", response);
        final String authToken = response.getAttribute(AccountConstants.E_AUTH_TOKEN);
        Assert.assertNotNull("authtoken", authToken);
    }

    @Test
    public void testZimbraFeatureChangePasswordDisabled() throws Exception {
        final ChangePasswordRequest request = new ChangePasswordRequest()
                .setAccount(AccountSelector.fromName(USERNAME_5)) // Not the authed user from context below
                .setOldPassword(PASSWORD_5)
                .setPassword(PASSWORD_5);
        final ChangePassword handler = new ChangePassword();
        final Map<String,Object> context = ServiceTestUtil.getRequestContext(account5);
        try {
            handler.handle(JaxbUtil.jaxbToElement(request), context);
        } catch (final ServiceException ex) {
            Assert.assertEquals("service exception type", ServiceException.OPERATION_DENIED, ex.getCode());
            return;
        }
        Assert.fail("expected handler to fail");
    }

    @Test
    public void testBasicHandlerWithResetPasswordAuthTokenUsageIfMustChangePasswordIsEnabled() throws Exception {
        AuthToken authToken = AuthProvider.getAuthToken(account3, AuthToken.Usage.RESET_PASSWORD , AuthToken.TokenType.AUTH);
        final ChangePasswordRequest request = new ChangePasswordRequest()
                .setAccount(AccountSelector.fromName(USERNAME_3)) // Not the authed user from context below
                .setOldPassword(PASSWORD_3)
                .setPassword(PASSWORD_3)
                .setAuthToken(new com.zimbra.soap.account.type.AuthToken(authToken.getEncoded(), false));

        final ChangePassword handler = new ChangePassword();
        final Map<String,Object> context = ServiceTestUtil.getRequestContext(account3);

        final Element response = handler.handle(JaxbUtil.jaxbToElement(request), context);
        Assert.assertNotNull("response", response);

        try {
            response.getAttribute(AccountConstants.E_AUTH_TOKEN);
        } catch (final ServiceException ex) {
            Assert.assertEquals("service exception type", ServiceException.INVALID_REQUEST, ex.getCode());
            Assert.assertTrue("exception message", ex.getMessage().contains("missing required attribute: authToken"));
        }
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
        prov.deleteAccount(account1.getId());
        prov.deleteAccount(account2.getId());
        prov.deleteAccount(account3.getId());
    }
}
