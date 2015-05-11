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

import com.zimbra.cs.redolog.TransactionId;

/**
 * Tracker for partially committed mailbox transactions.
 * Used to quickly detect if a given mailbox has redoable transactions which have been logged but not committed.
 * While nested redoable transactions are not currently supported, implementations must maintain insertion order internally so they can be supported in the future.
 * Implementations must also take care to maintain mailboxId uniqueness when data is placed into shared memory which spans multiple metadata stores.
 */
public interface TxnTracker {

    /**
     * Add an active transactionId.
     * The behavior when the same txnId is added is unspecified; it may or may not appear multiple times in the active list.
     */
    public void addActiveTxn(int mboxId, TransactionId txnId);

    /**
     * Remove a transactionId. If the same txnId has been added more than once all instances will be removed.
     */
    public void removeActiveTxn(int mboxId, TransactionId txnId);

    /**
     * List the active transactionIds, in the order they were added.
     * The behavior when the same txnId is added is unspecified; it may or may not appear multiple times in the active list.
     */
    public List<TransactionId> getActiveTransactions(int mboxId);

    /**
     * Check if a mailbox has active transactions
     */
    public boolean hasActiveTransactions(int mboxId);
}
