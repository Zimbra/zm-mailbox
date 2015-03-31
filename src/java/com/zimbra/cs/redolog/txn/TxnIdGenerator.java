package com.zimbra.cs.redolog.txn;

import com.zimbra.cs.redolog.TransactionId;

/**
 * Interface for automatically generated redolog transaction IDs
 */
public interface TxnIdGenerator {

    public abstract TransactionId getNext();

}