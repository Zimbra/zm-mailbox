/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2016 Synacor, Inc.
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
