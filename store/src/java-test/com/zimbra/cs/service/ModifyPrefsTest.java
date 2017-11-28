/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2017 Synacor, Inc.
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.mail.Address;
import javax.mail.internet.MimeMessage;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.ZAttrProvisioning.FeatureAddressVerificationStatus;
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

public class ModifyPrefsTest {

    public static String zimbraServerDir = "";

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
        MailboxTestUtil.clearData();
    }

    @Test
    public void testMsgMaxAttr() throws Exception {
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
}