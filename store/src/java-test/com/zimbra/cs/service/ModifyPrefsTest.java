/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017, 2018 Synacor, Inc.
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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.junit.rules.TestWatchman;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.ZAttrProvisioning.FeatureAddressVerificationStatus;
import com.zimbra.common.mime.MimeDetect;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.account.ModifyPrefs;
import com.zimbra.cs.service.mail.ServiceTestUtil;
import com.zimbra.soap.JaxbUtil;
import com.zimbra.soap.account.message.ModifyPrefsRequest;
import com.zimbra.soap.account.type.Pref;

import junit.framework.Assert;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MimeDetect.class)
@PowerMockIgnore({ "javax.crypto.*", "javax.xml.bind.annotation.*" })
public class ModifyPrefsTest {

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
        prov.createAccount("test@zimbra.com", "secret", attrs);

        MailboxManager.setInstance(new MailboxManager() {

            @Override
            protected Mailbox instantiateMailbox(MailboxData data) {
                return new Mailbox(data) {

                    @Override
                    public MailSender getMailSender() {
                        return new MailSender() {

                            @Override
                            protected Collection<Address> sendMessage(Mailbox mbox, MimeMessage mm,
                                Collection<RollbackData> rollbacks)
                                throws SafeMessagingException, IOException {
                                try {
                                    return Arrays.asList(getRecipients(mm));
                                } catch (Exception e) {
                                    return Collections.emptyList();
                                }
                            }
                        };
                    }
                };
            }
        });

        L10nUtil.setMsgClassLoader("../store-conf/conf/msgs");
    }

    @Before
    public void setUp() throws Exception {
        System.out.println(testName.getMethodName());
        MailboxTestUtil.clearData();
    }

    @After
    public void tearDown() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void testZCS2670() throws Exception {
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct1);
        acct1.setFeatureMailForwardingEnabled(true);
        acct1.setFeatureAddressVerificationEnabled(true);
        Assert.assertNull(acct1.getPrefMailForwardingAddress());
        Assert.assertNull(acct1.getFeatureAddressUnderVerification());
        ModifyPrefsRequest request = new ModifyPrefsRequest();
        Pref pref = new Pref(Provisioning.A_zimbraPrefMailForwardingAddress,
            "test1@somedomain.com");
        request.addPref(pref);
        Element req = JaxbUtil.jaxbToElement(request);
        new ModifyPrefs().handle(req, ServiceTestUtil.getRequestContext(mbox.getAccount()));
        /*
         * Verify that the forwarding address is not directly stored into
         * 'zimbraPrefMailForwardingAddress' Instead, it is stored in
         * 'zimbraFeatureAddressUnderVerification' till the time it
         * gets verification
         */
        Assert.assertNull(acct1.getPrefMailForwardingAddress());
        Assert.assertEquals("test1@somedomain.com",
            acct1.getFeatureAddressUnderVerification());
        /*
         * disable the verification feature and check that the forwarding
         * address is directly stored into 'zimbraPrefMailForwardingAddress'
         */
        acct1.setPrefMailForwardingAddress(null);
        acct1.setFeatureAddressUnderVerification(null);
        acct1.setFeatureAddressVerificationEnabled(false);
        new ModifyPrefs().handle(req, ServiceTestUtil.getRequestContext(mbox.getAccount()));
        Assert.assertNull(acct1.getFeatureAddressUnderVerification());
        Assert.assertEquals("test1@somedomain.com", acct1.getPrefMailForwardingAddress());
        Assert.assertEquals(FeatureAddressVerificationStatus.pending, acct1.getFeatureAddressVerificationStatus());
    }

    @Test
    public void testZCS3545() throws Exception {
        Account acct1 = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct1);
        Assert.assertNull(acct1.getPrefAccountProfileImageAsString());
        InputStream is = getClass().getResourceAsStream("img.jpg");
        is = getClass().getResourceAsStream("img.jpg");
        FileUploadServlet.Upload up = FileUploadServlet.saveUpload(is, "img.jpg", "image/jpeg",
            acct1.getId());
        PowerMockito.stub(PowerMockito.method(MimeDetect.class, "detect", InputStream.class))
            .toReturn("image/jpeg");
        ModifyPrefsRequest request = new ModifyPrefsRequest();
        Pref pref = new Pref(Provisioning.A_zimbraPrefAccountProfileImage, up.getId());
        request.addPref(pref);
        Element req = JaxbUtil.jaxbToElement(request);
        new ModifyPrefs().handle(req, ServiceTestUtil.getRequestContext(mbox.getAccount()));
        Assert.assertEquals(
            "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",
            acct1.getPrefAccountProfileImageAsString());
    }
}
