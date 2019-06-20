/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2018 Synacor, Inc.
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.zimbra.common.util.ZimbraLog;

public abstract class TransactionCacheTracker implements TransactionListener {

    protected Mailbox mbox;
    protected ThreadLocal<Set<TransactionAware<?,?>>> touchedItems;
    private ThreadLocal<Boolean> inTransaction;

    public TransactionCacheTracker(Mailbox mbox) {
        this.mbox = mbox;
        touchedItems = ThreadLocal.withInitial(() -> new HashSet<>());
        inTransaction = ThreadLocal.withInitial(() -> false);
    }

    public void addToTracker(TransactionAware<?,?> item) {
        if (touchedItems.get().add(item)) {
            if (ZimbraLog.cache.isTraceEnabled()) {
                ZimbraLog.cache.trace("added %s to cache tracker", item);
            }
        }
    }

    private boolean hasChanges() {
        return touchedItems.get().stream().anyMatch(item -> item.hasChanges());
    }

    private void resetChanges() {
        touchedItems.get().stream().forEach(item -> item.resetChanges());
    }

    @Override
    public void transactionEnd(boolean success, boolean endChange) {
        if (endChange) {
            processItems(touchedItems.get());
            resetChanges();
            clearTouchedItems();
            inTransaction.set(false);
        }
    }

    @Override
    public void transactionBegin(boolean startChange) {
        if (startChange) {
            inTransaction.set(true);
            if (hasChanges()) {
                List<String> mapsWithChanges = touchedItems.get().stream()
                        .filter(item -> item.hasChanges()).map(item -> item.getName()).collect(Collectors.toList());
                ZimbraLog.cache.warn("maps with changes exist at the beginning of a transaction! %s", Joiner.on(",").join(mapsWithChanges));
            }
        }
    }

    protected abstract void processItems(Set<TransactionAware<?,?>> items);

    @Override
    public void commitCache(boolean endChange) {
        //only process cache changes if ending top-level transaction
        Set<TransactionAware<?,?>> touchedByThisThread = touchedItems.get();
        if (touchedByThisThread.isEmpty()) {
            return;
        }
        if (endChange) {
            processItems(touchedByThisThread);
            clearTouchedItems(touchedByThisThread);
        }
    }

    @Override
    public void rollbackCache() {
        resetChanges();
        clearTouchedItems();
    }

    public boolean isInTransaction() {
        return inTransaction.get();
    }


    protected void clearTouchedItems(Set<TransactionAware<?,?>> items) {
        items.stream().forEach(item -> item.clearLocalCache());
    }

    protected void clearTouchedItems() {
        Set<TransactionAware<?,?>> touchedByThisThread = touchedItems.get();
        if (touchedByThisThread.isEmpty()) {
            return;
        }
        clearTouchedItems(touchedByThisThread);
        touchedByThisThread.clear();
    }
}