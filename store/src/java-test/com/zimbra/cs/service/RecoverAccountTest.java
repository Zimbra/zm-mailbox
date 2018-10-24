/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

package com.zimbra.cs.service;

import java.util.ArrayList;
import org.junit.Ignore;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.ZAttrProvisioning.FeatureResetPasswordStatus;
import com.zimbra.common.account.ZAttrProvisioning.PrefPasswordRecoveryAddressStatus;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.DeliveryOptions;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.mail.RecoverAccount;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.cs.service.util.ResetPasswordUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.mail.message.RecoverAccountRequest;
import com.zimbra.soap.mail.message.RecoverAccountResponse;
import com.zimbra.soap.mail.type.RecoverAccountOperation;
import com.zimbra.soap.type.Channel;

import junit.framework.Assert;

@Ignore("ZCS-5608 - Please restore when redis is setup on Circleci") public class RecoverAccountTest {

    public static String zimbraServerDir = "";

    @Rule
    public TestName testName = new TestName();
    @Rule
    public MethodRule watchman = new TestWatchman() {

        @Override
        public void failed(Throwable e, FrameworkMethod method) {
            System.out.println(method.getName() + " " + e.getClass().getSimpleName());
        }
    };

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();

        prov.createDomain("zimbra.com", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("testRecovery@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddress, "testRecovery@zimbra.com");
        attrs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus, "verified");
        prov.createAccount("test4798@zimbra.com", "secret", attrs);
        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        attrs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddress, "testRecovery@zimbra.com");
        attrs.put(Provisioning.A_zimbraPrefPasswordRecoveryAddressStatus, "verified");
        prov.createAccount("test4798a@zimbra.com", "secret", attrs);

        MailboxManager.setInstance(new DirectInsertionMailboxManager());

        L10nUtil.setMsgClassLoader("../store-conf/conf/msgs");
    }

    public static class DirectInsertionMailboxManager extends MailboxManager {

        public DirectInsertionMailboxManager() throws ServiceException {
            super();
        }

        @Override
        protected Mailbox instantiateMailbox(MailboxData data) {
            return new Mailbox(data) {

                @Override
                public MailSender getMailSender() {
                    return new MailSender() {

                        @Override
                        protected Collection<Address> sendMessage(Mailbox mbox, MimeMessage mm,
                            Collection<RollbackData> rollbacks) {
                            List<Address> successes = new ArrayList<Address>();
                            Address[] addresses;
                            try {
                                addresses = getRecipients(mm);
                            } catch (Exception e) {
                                addresses = new Address[0];
                            }
                            DeliveryOptions dopt = new DeliveryOptions()
                                .setFolderId(Mailbox.ID_FOLDER_INBOX).setFlags(Flag.BITMASK_UNREAD);
                            for (Address addr : addresses) {
                                try {
                                    Account acct = Provisioning.getInstance()
                                        .getAccountByName(((InternetAddress) addr).getAddress());
                                    if (acct != null) {
                                        Mailbox target = MailboxManager.getInstance()
                                            .getMailboxByAccount(acct);
                                        target.addMessage(null, new ParsedMessage(mm, false), dopt,
                                            null);
                                        successes.add(addr);
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace(System.out);
                                }
                            }
                            if (successes.isEmpty() && !isSendPartial()) {
                                for (RollbackData rdata : rollbacks) {
                                    if (rdata != null) {
                                        rdata.rollback();
                                    }
                                }
                            }
                            return successes;
                        }
                    };
                }
            };
        }
    }

    @Before
    public void setUp() throws Exception {
        System.out.println(testName.getMethodName());
        MailboxTestUtil.clearData();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testGetRecoveryEmail() throws Exception {
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test4798@zimbra.com");
        acct1.setFeatureResetPasswordStatus(FeatureResetPasswordStatus.enabled);
        Assert.assertEquals("testRecovery@zimbra.com", acct1.getPrefPasswordRecoveryAddress());
        Assert.assertEquals(PrefPasswordRecoveryAddressStatus.verified, acct1.getPrefPasswordRecoveryAddressStatus());
        RecoverAccountRequest request = new RecoverAccountRequest();
        request.setOp(RecoverAccountOperation.GET_RECOVERY_ACCOUNT);
        request.setEmail("test4798@zimbra.com");
        request.setChannel(Channel.EMAIL);
        Element req = JaxbUtil.jaxbToElement(request);
        Element response = new RecoverAccount().handle(req, ServiceTestUtil.getRequestContext(acct1));
        RecoverAccountResponse resp = JaxbUtil.elementToJaxb(response);
        Assert.assertEquals(StringUtil.maskEmail("testRecovery@zimbra.com"), resp.getRecoveryAccount());
    }

    @Test
    public void testGetRecoveryEmail_Negative() throws Exception {
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test4798@zimbra.com");
        acct1.setFeatureResetPasswordStatus(FeatureResetPasswordStatus.enabled);
        acct1.setPrefPasswordRecoveryAddressStatus(PrefPasswordRecoveryAddressStatus.pending);
        Assert.assertEquals("testRecovery@zimbra.com", acct1.getPrefPasswordRecoveryAddress());
        RecoverAccountRequest request = new RecoverAccountRequest();
        request.setOp(RecoverAccountOperation.GET_RECOVERY_ACCOUNT);
        request.setEmail("test4798@zimbra.com");
        request.setChannel(Channel.EMAIL);
        Element req = JaxbUtil.jaxbToElement(request);
        try {
            new RecoverAccount().handle(req, ServiceTestUtil.getRequestContext(acct1));
        } catch(ServiceException se) {
            Assert.assertEquals("service exception: Something went wrong. Please contact your administrator.", se.getMessage());
        }
    }

    @Test
    public void testSendRecoveryCode() throws Exception {
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test4798@zimbra.com");
        acct1.setFeatureResetPasswordStatus(FeatureResetPasswordStatus.enabled);
        acct1.setPrefPasswordRecoveryAddressStatus(PrefPasswordRecoveryAddressStatus.verified);
        Assert.assertEquals("testRecovery@zimbra.com", acct1.getPrefPasswordRecoveryAddress());
        RecoverAccountRequest request = new RecoverAccountRequest();
        request.setOp(RecoverAccountOperation.SEND_RECOVERY_CODE);
        request.setEmail("test4798@zimbra.com");
        request.setChannel(Channel.EMAIL);
        Element req = JaxbUtil.jaxbToElement(request);
        try {
            new RecoverAccount().handle(req, ServiceTestUtil.getRequestContext(acct1));
        } catch(ServiceException se) {
            Assert.fail("ServiceException should not be thrown.");
        }
        Account recoveryAccount = Provisioning.getInstance().get(Key.AccountBy.name, "testRecovery@zimbra.com");
        Mailbox recoveryMailbox = MailboxManager.getInstance().getMailboxByAccount(recoveryAccount);
        Message msg = (Message) recoveryMailbox.getItemList(null, MailItem.Type.MESSAGE).get(0);
        Assert.assertEquals("Reset your zimbra.com password", msg.getSubject());
    }

    @Test
    public void testResendRecoveryCodePositiveAndNegativeScenarios() throws Exception {
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test4798a@zimbra.com");
        acct1.setFeatureResetPasswordStatus(FeatureResetPasswordStatus.enabled);
        acct1.setPrefPasswordRecoveryAddressStatus(PrefPasswordRecoveryAddressStatus.verified);
        acct1.setPasswordRecoveryMaxAttempts(3);
        acct1.setFeatureResetPasswordSuspensionTime("10s");
        Assert.assertEquals("testRecovery@zimbra.com", acct1.getPrefPasswordRecoveryAddress());
        RecoverAccountRequest request = new RecoverAccountRequest();
        request.setOp(RecoverAccountOperation.SEND_RECOVERY_CODE);
        request.setEmail("test4798a@zimbra.com");
        request.setChannel(Channel.EMAIL);
        Element req = JaxbUtil.jaxbToElement(request);
        try {
            new RecoverAccount().handle(req, ServiceTestUtil.getRequestContext(acct1)); // resend count = 0
        } catch(ServiceException se) {
            Assert.fail("ServiceException should not be thrown.");
        }

        try {
            new RecoverAccount().handle(req, ServiceTestUtil.getRequestContext(acct1));// resend count = 1
            new RecoverAccount().handle(req, ServiceTestUtil.getRequestContext(acct1));// resend count = 2
            new RecoverAccount().handle(req, ServiceTestUtil.getRequestContext(acct1));// resend count = 3
        } catch(ServiceException se) {
            Assert.fail("ServiceException should not be thrown.");
        }
        try {
            new RecoverAccount().handle(req, ServiceTestUtil.getRequestContext(acct1));// resend count = 4, and it should fail
            Assert.fail("ServiceException should be thrown.");
        } catch(ServiceException se) {
            Assert.assertEquals("service exception: Max re-send attempts reached, feature is suspended.", se.getMessage());
        }

        try {
            ResetPasswordUtil.validateFeatureResetPasswordStatus(acct1);
            Assert.fail("ServiceException should be thrown.");
        } catch (ServiceException e) {
            Assert.assertEquals("service exception: Password reset feature is suspended.", e.getMessage());
        }

        Thread.sleep(10000);
        try {
            ResetPasswordUtil.validateFeatureResetPasswordStatus(acct1);
        } catch (ServiceException e) {
            Assert.fail("ServiceException should not be thrown as suspension time is over.");
        }
    }

    @Test
    public void testResetPasswordUtilFeatureDisabled() throws ServiceException {
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test4798a@zimbra.com");
        acct1.setFeatureResetPasswordStatus(FeatureResetPasswordStatus.disabled);
        try {
            ResetPasswordUtil.validateFeatureResetPasswordStatus(acct1);
            Assert.fail("ServiceException should be thrown.");
        } catch (ServiceException e) {
            Assert.assertEquals("service exception: Password reset feature is disabled.", e.getMessage());
        }
    }

    @Test
    public void testRecoverAccount_MissingChannel() throws Exception {
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test4798@zimbra.com");
        acct1.setFeatureResetPasswordStatus(FeatureResetPasswordStatus.enabled);
        acct1.setPrefPasswordRecoveryAddressStatus(PrefPasswordRecoveryAddressStatus.verified);
        Assert.assertEquals("testRecovery@zimbra.com", acct1.getPrefPasswordRecoveryAddress());
        RecoverAccountRequest request = new RecoverAccountRequest();
        request.setOp(RecoverAccountOperation.GET_RECOVERY_ACCOUNT);
        request.setEmail("test4798@zimbra.com");
        Element req = JaxbUtil.jaxbToElement(request);
        try {
            new RecoverAccount().handle(req, ServiceTestUtil.getRequestContext(acct1));// resend count = 3
        } catch(ServiceException se) {
            Assert.fail("Exception should not be thrown");
        }
    }
}
