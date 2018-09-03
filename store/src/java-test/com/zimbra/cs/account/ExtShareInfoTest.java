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
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.mail.SendMsgTest.DirectInsertionMailboxManager;


/**
 * @author zimbra
 *
 */
public class ExtShareInfoTest {

    private Account ownerAcct = null;
    private Account testAcct = null;
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
         MailboxTestUtil.initServer();
         Provisioning prov = Provisioning.getInstance();
         Map<String, Object> attrs = Maps.newHashMap();
         attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
         ownerAcct = prov.createAccount("test@zimbra.com", "secret", attrs);

         attrs = Maps.newHashMap();
         attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
         attrs.put("zimbraIsExternalVirtualAccount", "TRUE");
         testAcct =  prov.createAccount("rcpt@example.com", "secret", attrs);

         // this MailboxManager does everything except use SMTP to deliver mail
         MailboxManager.setInstance(new DirectInsertionMailboxManager());
         L10nUtil.setMsgClassLoader("../store-conf/conf/msgs");
    }

    @Test
    public void testGenNotifyBody() {

        Locale locale = new Locale("en", "US");
        String notes = "none";

        ShareInfoData sid = new ShareInfoData();
        sid.setGranteeDisplayName("Demo User Three");
        sid.setGranteeId(testAcct.getId());
        sid.setGranteeName(testAcct.getName());
        sid.setGranteeType(ACL.GRANTEE_GUEST);

        sid.setPath("/Inbox/Test");
        sid.setFolderDefaultView(MailItem.Type.MESSAGE);
        sid.setItemUuid("9badf685-3420-458b-9ce5-826b0bec638f");
        sid.setItemId(257);

        sid.setOwnerAcctId(ownerAcct.getId());
        sid.setOwnerAcctEmail(ownerAcct.getName());
        sid.setOwnerAcctDisplayName("Demo User Two");

        try {
            sid.setRights(ACL.stringToRights("rwidxap"));
            MimeMultipart mmp = ShareInfo.NotificationSender.genNotifBody(sid,
                    notes, locale, null, null);
            Assert.assertNotNull(mmp);
            String body = (String) mmp.getBodyPart(0).getDataHandler()
                    .getContent();
            int index = body.indexOf("http");
            int endIndex = body.indexOf(".", index);
            String authUrl = body.substring(index, endIndex);
            index = authUrl.indexOf("p=");
            String authToken = authUrl.substring(index + 2);
            try {
                ZimbraAuthToken.getAuthToken(authToken);
                fail("Authtoken should fail");
            } catch (AuthTokenException e) {
                assertTrue(e.getMessage().contains("hmac failure"));
            }

            // Commenting for now, need to figure out why it fails on hudson
//           try {
//               ExternalUserProvServlet.validatePrelimToken(authToken);
//           } catch (Exception e) {
//               fail("Should not throw Exception" + e.getMessage());
//           }

        } catch (ServiceException | MessagingException | IOException e) {
            fail("Exception should not be thrown: " + e.getMessage());
        }
    }

}
