/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.mailbox;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.account.Provisioning;

/**
 * Shared unit tests for {@link MailItemCache} adapters.
 */
public abstract class AbstractMailItemCacheTest extends AbstractCacheTest {
    protected MailItemCache cache;

    protected abstract MailItemCache constructCache() throws ServiceException;

    @Test
    public void testAddAndRemoveById() throws Exception {
        cache = constructCache();
        Assert.assertNotNull(cache);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(Mailbox.ID_FOLDER_INBOX);

        Assert.assertEquals(null, cache.get(mbox, folder.getId()));
        cache.put(mbox, folder);
        MailItem folder_ = cache.get(mbox, folder.getId());
        Assert.assertNotNull(folder_);
        Assert.assertEquals(folder.getId(), folder_.getId());

        cache.remove(mbox, folder.getId());
        Assert.assertEquals(null, cache.get(mbox, folder.getId()));
    }

    @Test
    public void testAddAndRemoveByUuid() throws Exception {
        cache = constructCache();
        Assert.assertNotNull(cache);
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Folder folder = mbox.getFolderById(Mailbox.ID_FOLDER_INBOX);
        Assume.assumeTrue(folder.getUuid() != null);

        // Put
        Assert.assertEquals(null, cache.get(mbox, folder.getId()));
        Assert.assertEquals(null, cache.get(mbox, folder.getUuid()));
        cache.put(mbox, folder);

        // Get by id
        MailItem folder_ = cache.get(mbox, folder.getId());
        Assert.assertNotNull(folder_);
        Assert.assertEquals(folder.getId(), folder_.getId());

        // Get by uuid
        folder_ = cache.get(mbox, folder.getUuid());
        Assert.assertNotNull(folder_);
        Assert.assertEquals(folder.getId(), folder_.getId());

        // Remove and get by both id and uuid
        cache.remove(mbox, folder.getId());
        Assert.assertEquals(null, cache.get(mbox, folder.getId()));
        Assert.assertEquals(null, cache.get(mbox, folder.getUuid()));
    }

    @Test
    public void testRemoveAllOfOneMailboxesItems() throws Exception {
        cache = constructCache();
        Assert.assertNotNull(cache);

        Provisioning prov = Provisioning.getInstance();
        Account acct1 = prov.createAccount("test1@zimbra.com", "secret", new HashMap<String, Object>());
        Account acct2 = prov.createAccount("test2@zimbra.com", "secret", new HashMap<String, Object>());

        Mailbox mbox1 = MailboxManager.getInstance().getMailboxByAccount(acct1);
        Mailbox mbox2 = MailboxManager.getInstance().getMailboxByAccount(acct2);

        // Put 2 account's inbox folders into the cache
        cache.put(mbox1, mbox1.getFolderById(Mailbox.ID_FOLDER_INBOX));
        cache.put(mbox2, mbox2.getFolderById(Mailbox.ID_FOLDER_INBOX));

        // Remove all cached items for the 1st mailbox
        try {
            cache.remove(mbox1);
        } catch (ServiceException e) {}

        // Expect the first account's inbox folder to be gone
        Assert.assertEquals(null, cache.get(mbox1, mbox1.getFolderById(Mailbox.ID_FOLDER_INBOX).getId()));

        // Expect the 2nd account's inbox folder to remain cached
        MailItem folder_ = mbox2.getFolderById(Mailbox.ID_FOLDER_INBOX);
        Assert.assertNotNull(folder_);
    }
}
