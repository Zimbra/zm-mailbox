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
        touchedItems.get().add(item);
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
            resetChanges();
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
            //clear out any caches built by getters invoked *outside* of a transaction
            clearTouchedItems();
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
        clearTouchedItems();
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

    public boolean isInTransaction() {
        return inTransaction.get();
    }
}