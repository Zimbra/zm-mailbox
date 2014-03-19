/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
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

package com.zimbra.cs.account;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.SendMsgTest.DirectInsertionMailboxManager;

/**
 * @author zimbra
 *
 */
@Ignore
public class ShareInfoTest {

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
         MailboxTestUtil.initServer();
         Provisioning prov = Provisioning.getInstance();

         prov.createAccount("test@zimbra.com", "secret", Maps.<String, Object>newHashMap());

         Map<String, Object> attrs = Maps.newHashMap();
         attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
         prov.createAccount("rcpt@zimbra.com", "secret", attrs);

         // this MailboxManager does everything except use SMTP to deliver mail
         MailboxManager.setInstance(new DirectInsertionMailboxManager());
    }

    @Test
    public void testGenNotifyBody() {

        Locale locale = new Locale("en", "US");
        String notes = "none";

        ShareInfoData sid = new ShareInfoData();
        sid.setGranteeDisplayName("Demo User Three");
        sid.setGranteeId("46031e4c-deb4-4724-b5bb-8f854d0c518a");
        sid.setGranteeName("Demo User Three");
        sid.setGranteeType(ACL.GRANTEE_USER);

        sid.setPath("/Calendar/Cal1");
        sid.setFolderDefaultView(MailItem.Type.APPOINTMENT);
        sid.setItemUuid("9badf685-3420-458b-9ce5-826b0bec638f");
        sid.setItemId(257);

        sid.setOwnerAcctId("bbf152ca-e7cd-477e-9f72-70fef715c5f9");
        sid.setOwnerAcctEmail("test@zimbra.com");
        sid.setOwnerAcctDisplayName("Demo User Two");

        try {

            sid.setRights(ACL.stringToRights("rwidxap"));
            MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(sid,
                    notes, locale, null, null);
            Assert.assertNotNull(mmp);
            String body = (String) mmp.getBodyPart(0).getDataHandler()
                    .getContent();
            assertTrue(body.indexOf("Role: Admin") != -1);

        } catch (ServiceException | MessagingException | IOException e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }


    @Test
    public void testGenNotifyBodyForCustom() {

        Locale locale = new Locale("en", "US");
        String notes = "none";

        ShareInfoData sid = new ShareInfoData();
        sid.setGranteeDisplayName("Demo User Three");
        sid.setGranteeId("46031e4c-deb4-4724-b5bb-8f854d0c518a");
        sid.setGranteeName("Demo User Three");
        sid.setGranteeType(ACL.GRANTEE_USER);

        sid.setPath("/Calendar/Cal1");
        sid.setFolderDefaultView(MailItem.Type.APPOINTMENT);
        sid.setItemUuid("9badf685-3420-458b-9ce5-826b0bec638f");
        sid.setItemId(257);

        sid.setOwnerAcctId("bbf152ca-e7cd-477e-9f72-70fef715c5f9");
        sid.setOwnerAcctEmail("test@zimbra.com");
        sid.setOwnerAcctDisplayName("Demo User Two");

        try {

            sid.setRights(ACL.stringToRights("rwdxap"));
            MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(sid,
                    notes, locale, null, null);
            Assert.assertNotNull(mmp);
            String body = (String) mmp.getBodyPart(0).getDataHandler()
                    .getContent();
            assertTrue(body.indexOf("Role: Custom") != -1);

        } catch (ServiceException | MessagingException | IOException e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

}
