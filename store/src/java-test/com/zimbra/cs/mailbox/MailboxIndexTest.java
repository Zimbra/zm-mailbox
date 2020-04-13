/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2023 Synacor, Inc.
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

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.zimbra.common.account.ZAttrProvisioning.DelayedIndexStatus;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;

/**
 * Unit test for {@link Folder}.
 */
public final class MailboxIndexTest {

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
    public void needToReIndexTest() throws Exception {
        Account acct = Provisioning.getInstance().getAccountByName("test@zimbra.com");
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(acct);
        MailboxIndex index = mbox.index;
        index.deleteIndex();
        acct.setFeatureMobileSyncEnabled(false);

        acct.unsetFeatureDelayedIndexEnabled();
        acct.unsetDelayedIndexStatus();
        Assert.assertTrue("zimbraFeatureDelayedIndexEnabled is unset and zimbraDelayedIndex is unset", index.needToReIndex());

        acct.setDelayedIndexStatus(DelayedIndexStatus.suppressed);
        Assert.assertTrue("zimbraFeatureDelayedIndexEnabled is unset and zimbraDelayedIndex is suppressed", index.needToReIndex());

        acct.setDelayedIndexStatus(DelayedIndexStatus.waitingForSearch);
        Assert.assertTrue("zimbraFeatureDelayedIndexEnabled is unset and zimbraDelayedIndex is waitingForSearch", index.needToReIndex());

        acct.setDelayedIndexStatus(DelayedIndexStatus.indexing);
        Assert.assertTrue("zimbraFeatureDelayedIndexEnabled is unset and zimbraDelayedIndex is indexing", index.needToReIndex());

        acct.setFeatureDelayedIndexEnabled(false);
        acct.unsetDelayedIndexStatus();
        Assert.assertTrue("zimbraFeatureDelayedIndexEnabled is FALSE and zimbraDelayedIndex is unset", index.needToReIndex());

        acct.setDelayedIndexStatus(DelayedIndexStatus.suppressed);
        Assert.assertTrue("zimbraFeatureDelayedIndexEnabled is FALSE and zimbraDelayedIndex is suppressed", index.needToReIndex());

        acct.setDelayedIndexStatus(DelayedIndexStatus.waitingForSearch);
        Assert.assertTrue("zimbraFeatureDelayedIndexEnabled is FALSE and zimbraDelayedIndex is waitingForSearch", index.needToReIndex());

        acct.setDelayedIndexStatus(DelayedIndexStatus.indexing);
        Assert.assertTrue("zimbraFeatureDelayedIndexEnabled is FALSE and zimbraDelayedIndex is indexing", index.needToReIndex());

        acct.setFeatureDelayedIndexEnabled(true);
        acct.unsetDelayedIndexStatus();
        Assert.assertFalse("zimbraFeatureDelayedIndexEnabled is TRUE and zimbraDelayedIndex is unset", index.needToReIndex());

        acct.setDelayedIndexStatus(DelayedIndexStatus.suppressed);
        Assert.assertFalse("zimbraFeatureDelayedIndexEnabled is TRUE and zimbraDelayedIndex is suppressed", index.needToReIndex());

        acct.setDelayedIndexStatus(DelayedIndexStatus.waitingForSearch);
        Assert.assertTrue("zimbraFeatureDelayedIndexEnabled is TRUE and zimbraDelayedIndex is waitingForSearch", index.needToReIndex());

        acct.setDelayedIndexStatus(DelayedIndexStatus.indexing);
        Assert.assertFalse("zimbraFeatureDelayedIndexEnabled is TRUE and zimbraDelayedIndex is indexing", index.needToReIndex());

        acct.setFeatureMobileSyncEnabled(true);
        Assert.assertFalse("zimbraFeatureDelayedIndexEnabled is TRUE, zimbraDelayedIndex is indexing and " +
                "zimbraFeatureMobileSyncEnabled is TRUE, but not Mobile Sync Access", index.needToReIndex());
    }
}
