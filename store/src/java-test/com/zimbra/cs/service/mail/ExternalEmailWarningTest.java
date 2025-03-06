/*
 *
 *  * ***** BEGIN LICENSE BLOCK *****
 *  * Zimbra Collaboration Suite, Network Edition.
 *  * Copyright (C) 2025 Synacor, Inc.  All Rights Reserved.
 *  * ***** END LICENSE BLOCK *****
 *
 */

package com.zimbra.cs.service.mail;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.lmtpserver.ExternalEmailWarning;
import com.zimbra.cs.lmtpserver.LmtpAddress;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class ExternalEmailWarningTest extends TestCase{

    Provisioning prov = Provisioning.getInstance();
    List<LmtpAddress> recipients = new ArrayList<>();
    ExternalEmailWarning externalEmailWarning = ExternalEmailWarning.getInstance();
    String rcptEmail1;
    String rcptEmail2;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
    }

    protected void setUp() throws Exception {
        String recipient1 = "<\"test1.\"@domain1.com>";
        String recipient2 = "<\"test2.\"@domain2.com>";
        recipients.add(new LmtpAddress(recipient1, null, null));
        recipients.add(new LmtpAddress(recipient2, null, null));
        Map<String, Object> attrs1 = new HashMap<String, Object>();
        attrs1.put(Provisioning.A_zimbraFeatureExternalEmailWarningEnabled, "TRUE");
        rcptEmail1 = recipients.get(0).getEmailAddress();
        Map<String, Object> attrs2 = new HashMap<String, Object>();
        attrs2.put(Provisioning.A_zimbraFeatureExternalEmailWarningEnabled, "FALSE");
        rcptEmail2 = recipients.get(1).getEmailAddress();
        if (prov.getDomainByName("domain1.com") == null) {
            prov.createDomain("domain1.com", attrs1);
        }
        if (prov.getDomainByName("domain2.com") == null) {
            prov.createDomain("domain2.com", attrs2);
        }
        prov.createAccount(rcptEmail1, "test123", new HashMap<>());
        prov.createAccount(rcptEmail2, "test123", new HashMap<>());
        externalEmailWarning.findRecipientsWithEEWEnabled(recipients);
    }

    public void testIsEnabled() throws ServiceException {
        Account account1 = prov.get(Key.AccountBy.name, rcptEmail1);
        Assert.assertTrue(externalEmailWarning.isEnabled(account1));
        Account account2 = prov.get(Key.AccountBy.name, rcptEmail2);
        Assert.assertFalse(externalEmailWarning.isEnabled(account2));
    }

    public void testZimbraExternalEmailWarningMessage() throws ServiceException {
        Account account1 = prov.get(Key.AccountBy.name, rcptEmail1);
        Domain domain1 = prov.getDomain(account1);
        Assert.assertEquals("This message originated outside of your organization.", domain1.getExternalEmailWarningMessage());
    }
}
