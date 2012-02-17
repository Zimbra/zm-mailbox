/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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
package com.zimbra.cs.service.mail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.google.common.collect.Maps;
import com.zimbra.common.account.Key;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ShareInfoData;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailSender;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.MailboxData;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.service.util.ItemId;

public class SendShareNotificationTest extends SendShareNotification {

    @Override
    protected MimeMessage generateShareNotification(Account authAccount, Account ownerAccount, ShareInfoData sid, String notes, boolean action) throws ServiceException, MessagingException {
        MimeMessage mm = super.generateShareNotification(authAccount, ownerAccount, sid, notes, action);

        try {
            MimeMultipart mmp = (MimeMultipart)mm.getContent();
            BodyPart bp = mmp.getBodyPart(2);
            Assert.assertTrue(bp.getContentType().startsWith(MimeConstants.CT_XML_ZIMBRA_SHARE));
            Element share = Element.parseXML(bp.getInputStream());
            if (revoke) {
                Assert.assertEquals(share.getQName().getName(), MailConstants.ShareConstants.E_REVOKE);
            } else {
                Assert.assertEquals(share.getQName().getName(), MailConstants.ShareConstants.E_SHARE);
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        return mm;
    }

    static boolean revoke;

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer();
        Provisioning prov = Provisioning.getInstance();

        Map<String, Object> attrs = Maps.newHashMap();
        prov.createAccount("test@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test2@zimbra.com", "secret", attrs);

        attrs = Maps.newHashMap();
        attrs.put(Provisioning.A_zimbraId, UUID.randomUUID().toString());
        prov.createAccount("test3@zimbra.com", "secret", attrs);

        // this MailboxManager does everything except actually send mail
        MailboxManager.setInstance(new MailboxManager() {
            @Override
            protected Mailbox instantiateMailbox(MailboxData data) {
                return new Mailbox(data) {
                    @Override
                    public MailSender getMailSender() {
                        return new MailSender() {
                            @Override
                            protected Collection<Address> sendMessage(Mailbox mbox, MimeMessage mm, Collection<RollbackData> rollbacks)
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
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
    }

    @Test
    public void shareByOwner() throws Exception {
        revoke = false;
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        Folder f = mbox.createFolder(null, "f1", Mailbox.ID_FOLDER_BRIEFCASE, MailItem.Type.DOCUMENT, 0, (byte)0, null);

        mbox.grantAccess(null, f.getId(), acct2.getId(), ACL.GRANTEE_USER, ACL.RIGHT_ADMIN, null);

        Element request = new Element.XMLElement(MailConstants.SEND_SHARE_NOTIFICATION_REQUEST);
        request.addElement(MailConstants.E_ITEM).addAttribute(MailConstants.A_ID, f.getId());
        request.addElement(MailConstants.E_EMAIL).addAttribute(MailConstants.A_ADDRESS, "test2@zimbra.com");
        handle(request, GetFolderTest.getRequestContext(acct));
    }

    @Test
    public void shareByAdmin() throws Exception {
        revoke = false;
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");
        Account acct2 = Provisioning.getInstance().get(Key.AccountBy.name, "test2@zimbra.com");
        Account acct3 = Provisioning.getInstance().get(Key.AccountBy.name, "test3@zimbra.com");

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        Folder f = mbox.createFolder(null, "f1", Mailbox.ID_FOLDER_BRIEFCASE, MailItem.Type.DOCUMENT, 0, (byte)0, null);

        mbox.grantAccess(null, f.getId(), acct2.getId(), ACL.GRANTEE_USER, ACL.RIGHT_ADMIN, null);
        mbox.grantAccess(null, f.getId(), acct3.getId(), ACL.GRANTEE_USER, ACL.RIGHT_ADMIN, null);

        Element request = new Element.XMLElement(MailConstants.SEND_SHARE_NOTIFICATION_REQUEST);
        request.addElement(MailConstants.E_ITEM).addAttribute(MailConstants.A_ID, new ItemId(acct.getId(), f.getId()).toString());
        request.addElement(MailConstants.E_EMAIL).addAttribute(MailConstants.A_ADDRESS, "test3@zimbra.com");
        handle(request, GetFolderTest.getRequestContext(acct));
    }

    @Test
    public void revoke() throws Exception {
        revoke = true;
        Account acct = Provisioning.getInstance().get(Key.AccountBy.name, "test@zimbra.com");

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        Folder f = mbox.createFolder(null, "f1", Mailbox.ID_FOLDER_BRIEFCASE, MailItem.Type.DOCUMENT, 0, (byte)0, null);

        Element request = new Element.XMLElement(MailConstants.SEND_SHARE_NOTIFICATION_REQUEST);
        request.addAttribute(MailConstants.A_ACTION, "revoke");
        request.addElement(MailConstants.E_ITEM).addAttribute(MailConstants.A_ID, f.getId());
        request.addElement(MailConstants.E_EMAIL).addAttribute(MailConstants.A_ADDRESS, "test2@zimbra.com");
        handle(request, GetFolderTest.getRequestContext(acct));
    }
}
