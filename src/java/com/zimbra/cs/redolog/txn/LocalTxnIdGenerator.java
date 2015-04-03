package com.zimbra.cs.redolog.txn;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.cs.redolog.TransactionId;

/**
 * Transaction ID generator which uses local in-memory counters
 * Suitable for use in a single-node and/or non-clustered Zimbra environment
 *
 */
public class LocalTxnIdGenerator implements TxnIdGenerator {
    private int mTime;
    private int mCounter;

    public LocalTxnIdGenerator() {
        init();
    }

    private void init() {
        mTime = (int) (System.currentTimeMillis() / 1000);
        mCounter = 1;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.redolog.txn.TxnIdGenerator#getNext()
     */
    @Override
    public synchronized TransactionId getNext() {
        TransactionId tid = new TransactionId(mTime, mCounter);
        if (mCounter < 0x7fffffffL)
            mCounter++;
        else
            init();
        return tid;
    }

    @Override
    @VisibleForTesting
    public void setPreviousTransactionId(TransactionId txnId) {
        mTime = txnId.getTime();
        mCounter = txnId.getCounter();
        getNext();
    }
}
