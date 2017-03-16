/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.qa.unittest;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.junit.Test;

import com.zimbra.client.ZFeatures;
import com.zimbra.client.ZFolder;
import com.zimbra.client.ZGetInfoResult;
import com.zimbra.client.ZMailbox;
import com.zimbra.client.ZMailbox.Options;
import com.zimbra.client.ZMessage;
import com.zimbra.client.ZPrefs;
import com.zimbra.client.ZSignature;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.imap.RemoteImapMailboxStore;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.soap.mail.message.ItemActionResponse;

public class TestZClient
extends TestCase {
    private static String NAME_PREFIX = "TestZClient";
    private static String RECIPIENT_USER_NAME = "user2";
    private static final String USER_NAME = "user1";
    private static final String FOLDER_NAME = "testfolder";
    private static ZFolder folder;

    @Override
    public void setUp()
    throws Exception {
        cleanUp();
    }

    /**
     * Confirms that the prefs accessor works (bug 51384).
     */
    public void testPrefs()
    throws Exception {
        Account account = TestUtil.getAccount(USER_NAME);
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZPrefs prefs = mbox.getPrefs();
        assertEquals(account.getPrefLocale(), prefs.getLocale());
    }

    /**
     * Confirms that the features accessor doesn't throw NPE (bug 51384).
     */
    public void testFeatures()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        ZFeatures features = mbox.getFeatures();
        features.getPop3Enabled();
    }

    public void testChangePassword()
    throws Exception {
        Account account = TestUtil.getAccount(USER_NAME);
        Options options = new Options();
        options.setAccount(account.getName());
        options.setAccountBy(AccountBy.name);
        options.setPassword(TestUtil.DEFAULT_PASSWORD);
        options.setNewPassword("test456");
        options.setUri(TestUtil.getSoapUrl());
        ZMailbox.changePassword(options);

        try {
            TestUtil.getZMailbox(USER_NAME);
        } catch (SoapFaultException e) {
            assertEquals(AuthFailedServiceException.AUTH_FAILED, e.getCode());
        }
    }

    /**
     * Confirms that the {@code List} of signatures returned by {@link ZMailbox#getSignatures}
     * is modifiable (see bug 51842).
     */
    public void testModifySignatures()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        List<ZSignature> signatures = mbox.getSignatures();
        try {
            signatures.set(signatures.size(), null);
        } catch (IndexOutOfBoundsException e) {
            // Not UnsupportedOperationException, so we're good.
        }

        ZGetInfoResult info = mbox.getAccountInfo(true);
        signatures = info.getSignatures();
        try {
            signatures.set(signatures.size(), null);
        } catch (IndexOutOfBoundsException e) {
            // Not UnsupportedOperationException, so we're good.
        }
    }

    public void testCopyItemAction() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        String sender = TestUtil.getAddress(USER_NAME);
        String recipient = TestUtil.getAddress(RECIPIENT_USER_NAME);
        String subject = NAME_PREFIX + " testCopyItemAction";
        String content = new MessageBuilder().withSubject(subject).withFrom(sender).withToRecipient(recipient).create();

        // add a msg flagged as sent; filterSent=TRUE
        mbox.addMessage(Integer.toString(Mailbox.ID_FOLDER_DRAFTS), null, null, System.currentTimeMillis(), content, false, false);
        ZMessage msg = TestUtil.waitForMessage(mbox, "in:drafts " + subject);
        List<Integer> ids = new ArrayList<Integer>();
        ids.add(Integer.parseInt(msg.getId()));
        ItemActionResponse resp = mbox.copyItemAction(Mailbox.ID_FOLDER_SENT, ids);
        Assert.assertNotNull(resp);
        Assert.assertNotNull(resp.getAction());
        Assert.assertNotNull(resp.getAction().getId());

        ZMessage copiedMessage = mbox.getMessageById(resp.getAction().getId());
        Assert.assertNotNull(copiedMessage);
        Assert.assertEquals(subject, copiedMessage.getSubject());
        //msg.getId()

    }

    @Test
    public void testSubscribeFolder() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        try {
            folder = mbox.createFolder(Mailbox.ID_FOLDER_USER_ROOT+"", FOLDER_NAME, ZFolder.View.unknown, ZFolder.Color.DEFAULTCOLOR, null, null);
        } catch (ServiceException e) {
            if (e.getCode().equals(MailServiceException.ALREADY_EXISTS)) {
                folder = mbox.getFolderByPath("/"+FOLDER_NAME);
            } else {
                throw e;
            }
        }
        mbox.flagFolderAsSubscribed(null, folder);
        Assert.assertTrue(folder.isIMAPSubscribed());
        mbox.flagFolderAsUnsubscribed(null, folder);
        Assert.assertFalse(folder.isIMAPSubscribed());
    }

    @Test
    public void testResetImapUID() throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        try {
            folder = mbox.createFolder(Mailbox.ID_FOLDER_USER_ROOT+"", FOLDER_NAME, ZFolder.View.unknown, ZFolder.Color.DEFAULTCOLOR, null, null);
        } catch (ServiceException e) {
            if (e.getCode().equals(MailServiceException.ALREADY_EXISTS)) {
                folder = mbox.getFolderByPath("/"+FOLDER_NAME);
            } else {
                throw e;
            }
        }
        List<Integer> ids = new LinkedList<Integer>();
        ids.add(Integer.valueOf(TestUtil.addMessage(mbox, "imap message 1")));
        ids.add(Integer.valueOf(TestUtil.addMessage(mbox, "imap message 2")));
        RemoteImapMailboxStore store = new RemoteImapMailboxStore(mbox, TestUtil.getAccount(USER_NAME).getId());
        store.resetImapUid(ids);
    }
    @Override
    public void tearDown()
    throws Exception {
        cleanUp();
    }

    private void cleanUp()
    throws Exception {
        ZMailbox mbox = TestUtil.getZMailbox(USER_NAME);
        if (folder != null) {
            try {
                mbox.deleteFolder(folder.getId());
            } catch (ServiceException e) {}
        }
        Account account = TestUtil.getAccount(USER_NAME);
        account.setPassword(TestUtil.DEFAULT_PASSWORD);
        TestUtil.deleteTestData(USER_NAME, NAME_PREFIX);
        TestUtil.deleteTestData(RECIPIENT_USER_NAME, NAME_PREFIX);
    }

    public static void main(String[] args)
    throws Exception {
        TestUtil.cliSetup();
        TestUtil.runTest(TestZClient.class);
    }
}
