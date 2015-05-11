/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
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

package com.zimbra.cs.redolog.txn;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.MockProvisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.TransactionId;

public abstract class AbstractTxnTrackerTest {

    public abstract TxnTracker getTracker();

    @Test
    public void testAdd() throws ServiceException {
        TxnTracker tracker = getTracker();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        int time0 = (int) (System.currentTimeMillis() / 1000);
        int counter0 = 1;
        tracker.addActiveTxn(mbox.getId(), new TransactionId(time0, counter0));

        Assert.assertTrue(tracker.hasActiveTransactions(mbox.getId()));
        List<TransactionId> txns = tracker.getActiveTransactions(mbox.getId());
        Assert.assertNotNull(txns);
        Assert.assertEquals(1, txns.size());
        Assert.assertEquals(time0, txns.get(0).getTime());
        Assert.assertEquals(counter0, txns.get(0).getCounter());

        int time1 = (int) (System.currentTimeMillis() / 1000);
        int counter1 = 2;
        tracker.addActiveTxn(mbox.getId(), new TransactionId(time1, counter1));

        Assert.assertTrue(tracker.hasActiveTransactions(mbox.getId()));
        txns = tracker.getActiveTransactions(mbox.getId());
        Assert.assertNotNull(txns);
        Assert.assertEquals(2, txns.size());
        Assert.assertEquals(time0, txns.get(0).getTime());
        Assert.assertEquals(counter0, txns.get(0).getCounter());

        Assert.assertEquals(time1, txns.get(1).getTime());
        Assert.assertEquals(counter1, txns.get(1).getCounter());
    }


    @Test
    public void testRemove() throws ServiceException {
        TxnTracker tracker = getTracker();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        int time0 = (int) (System.currentTimeMillis() / 1000);
        int counter0 = 1;
        tracker.addActiveTxn(mbox.getId(), new TransactionId(time0, counter0));

        int time1 = (int) (System.currentTimeMillis() / 1000);
        int counter1 = 2;
        tracker.addActiveTxn(mbox.getId(), new TransactionId(time1, counter1));

        int time2 = (int) (System.currentTimeMillis() / 1000);
        int counter2 = 3;
        tracker.addActiveTxn(mbox.getId(), new TransactionId(time2, counter2));

        Assert.assertTrue(tracker.hasActiveTransactions(mbox.getId()));
        List<TransactionId> txns = tracker.getActiveTransactions(mbox.getId());
        Assert.assertNotNull(txns);
        Assert.assertEquals(3, txns.size());

        tracker.removeActiveTxn(mbox.getId(), new TransactionId(time1, counter1));
        txns = tracker.getActiveTransactions(mbox.getId());
        Assert.assertNotNull(txns);
        Assert.assertEquals(2, txns.size());

        Assert.assertEquals(time0, txns.get(0).getTime());
        Assert.assertEquals(counter0, txns.get(0).getCounter());

        Assert.assertEquals(time2, txns.get(1).getTime());
        Assert.assertEquals(counter2, txns.get(1).getCounter());

    }

    @Test
    public void testMultiAddSingleRemove() throws ServiceException {
        //in some cases a given op may call log() twice but remove() once.
        //this creates two records for the op, but only removes once

        TxnTracker tracker = getTracker();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        int time0 = (int) (System.currentTimeMillis() / 1000);
        int counter0 = 1;
        //use new txnId in each add so we confirm equals() comparison works - not just reference comparison
        tracker.addActiveTxn(mbox.getId(), new TransactionId(time0, counter0));
        tracker.addActiveTxn(mbox.getId(), new TransactionId(time0, counter0));

        Assert.assertTrue(tracker.hasActiveTransactions(mbox.getId()));

        tracker.removeActiveTxn(mbox.getId(), new TransactionId(time0, counter0));

        Assert.assertFalse(tracker.hasActiveTransactions(mbox.getId()));

    }

    @Test
    public void testGetNull() throws ServiceException {
        TxnTracker tracker = getTracker();
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(MockProvisioning.DEFAULT_ACCOUNT_ID);
        Assert.assertFalse(tracker.hasActiveTransactions(mbox.getId()));
        Assert.assertNull(tracker.getActiveTransactions(mbox.getId()));
    }

}
