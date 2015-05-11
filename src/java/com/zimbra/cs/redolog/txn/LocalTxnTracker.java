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
import java.util.Set;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import com.zimbra.cs.redolog.TransactionId;

/**
 * TxnTracker which uses memory.
 * Exists to support single node installs which lack Redis.
 * Tracking is cleared on a restart, so installs which use this still require full log scans for process crash recovery.
 */
public class LocalTxnTracker implements TxnTracker {

    private SetMultimap<Integer, TransactionId> activeTxns = Multimaps.synchronizedSetMultimap(LinkedHashMultimap.create());

    @Override
    public void addActiveTxn(int mboxId, TransactionId txnId) {
        activeTxns.put(mboxId, txnId);
    }

    @Override
    public void removeActiveTxn(int mboxId, TransactionId txnId) {
        activeTxns.remove(mboxId, txnId);
    }

    @Override
    public List<TransactionId> getActiveTransactions(int mboxId) {
        Set<TransactionId> ids = activeTxns.get(mboxId);
        return (ids == null || ids.size() < 1) ? null : Lists.newLinkedList(ids);
    }

    @Override
    public boolean hasActiveTransactions(int mboxId) {
        return activeTxns.containsKey(mboxId);
    }

}
