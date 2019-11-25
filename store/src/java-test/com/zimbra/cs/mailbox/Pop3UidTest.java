/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2019 Synacor, Inc.
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
package com.zimbra.cs.mailbox;

import static org.junit.Assert.fail;

import java.util.HashMap;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mime.ParsedMessage;

public final class Pop3UidTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();

        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void validUidFormat() throws Exception {
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        DeliveryOptions opt = new DeliveryOptions();
        opt.setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg = mbox.addMessage(null, new ParsedMessage(
                "From: from1@zimbra.com\r\nTo: to1@zimbra.com".getBytes(), false), opt, null);

        String uids[] = {
          /* Classic Zimbra POP3 unique-id */
            "321.45+ue,SUITUpKBHpfRA+IFm430VSxhx+wbru8WzbEwU=",
          /* up to 70 characters */
            "0123456789012345678901234567890123456789012345678901234567890123456789",
          /* RFC wise unique-id should have at least 1 character, but Mailbox.setPop3Uid()
           * accepts the length 0 uid (to reset the existing custom uid)
           */
            ""
        };

        for (String uid : uids) {
            try {
                mbox.setPop3Uid(null, msg.getId(), MailItem.Type.MESSAGE, uid);
            } catch (ServiceException e) {
                fail("Invalid unique-id");
            }
        }
    }

    @Test
    public void invalidUidFormat() throws Exception {
        Account account = Provisioning.getInstance().getAccountById(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        DeliveryOptions opt = new DeliveryOptions();
        opt.setFolderId(Mailbox.ID_FOLDER_INBOX);
        Message msg = mbox.addMessage(null, new ParsedMessage(
                "From: from1@zimbra.com\r\nTo: to1@zimbra.com".getBytes(), false), opt, null);

        String uids[] = {
          /* Out of range 0x21 - 0x7E*/
            "\n",
            " ",
          /* longer than 70 octets */
            "01234567890123456789012345678901234567890123456789012345678901234567890"
        };

        for (String uid : uids) {
            try {
                mbox.setPop3Uid(null, msg.getId(), MailItem.Type.MESSAGE, uid);
                fail("Invalid unique-id");
            } catch (ServiceException e) {
            }
        }
    }
}