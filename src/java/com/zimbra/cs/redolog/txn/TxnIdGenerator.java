package com.zimbra.cs.redolog.txn;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.cs.redolog.TransactionId;

/**
 * Interface for automatically generated redolog transaction IDs
 */
public interface TxnIdGenerator {

    /**
     * Get the next transaction ID
     */
    public abstract TransactionId getNext();

    /**
     * Set the next transaction ID to be returned.
     * Primarily visible for testing; not required in the runtime interface
     */
    @VisibleForTesting
    default void setPreviousTransactionId(TransactionId next) {};
}