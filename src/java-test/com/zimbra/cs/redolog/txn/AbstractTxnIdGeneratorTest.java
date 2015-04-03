package com.zimbra.cs.redolog.txn;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.redolog.TransactionId;

public abstract class AbstractTxnIdGeneratorTest {

    public abstract TxnIdGenerator getGenerator();

    @Test
    public void testGenerateIds() {
        TxnIdGenerator generator = getGenerator();
        TransactionId lastTxnId = generator.getNext();
        int trials = 10;
        for (int i = 0; i < trials; i++) {
            TransactionId txnId = generator.getNext();
            Assert.assertTrue(txnId.compareTo(lastTxnId) > 0);
            lastTxnId = txnId;
        }
    }

    @Test
    public void testGenerateConcurrently() {

        Set<TransactionId> generated = new HashSet<TransactionId>();
        Set<AssertionError> errors = new HashSet<AssertionError>();

        TxnIdGenerator generator = getGenerator();

        int numThreads = 10;
        int trials = 10;
        Set<Thread> threads = new HashSet<Thread>();

        for (int i = 0; i < numThreads; i++) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        TransactionId lastTxnId = generator.getNext();
                        Assert.assertTrue(generated.add(lastTxnId));
                        for (int j = 0; j < trials; j++) {
                            TransactionId txnId = generator.getNext();
                            Assert.assertTrue(txnId.compareTo(lastTxnId) > 0);
                            lastTxnId = txnId;
                            Assert.assertTrue(generated.add(lastTxnId));
                        }
                    } catch (AssertionError ae) {
                        ZimbraLog.test.error("multithread failure", ae);
                        errors.add(ae);
                    }
                }
            };
            threads.add(t);
        }

        for (Thread t : threads) {
            t.start();
        }

        long sleepTime = 1000000;
        for (Thread t : threads) {
            try {
                t.join(sleepTime);
            } catch (InterruptedException e) {
            }
            Assert.assertFalse(t.isAlive());
        }

        if (errors.size() > 0) {
            throw errors.iterator().next();
        }
    }

    @Test
    public void testRolloverIds() {
        TxnIdGenerator generator = getGenerator();
        //we basically require concrete implementations to implement this interface for testing
        //but don't want to require or rely on it in real code
        //so separate interface and testing fails if not implemented

        int startTime = (int) (System.currentTimeMillis() / 1000) - 1;
        int startCounter = Integer.MAX_VALUE;
        TransactionId startTxn = new TransactionId(startTime, startCounter);
        generator.setPreviousTransactionId(startTxn);

        TransactionId nextTxn = generator.getNext();
        Assert.assertTrue(nextTxn.getCounter() > 0);
        Assert.assertTrue(startTime < nextTxn.getTime());
        Assert.assertEquals(1, nextTxn.getCounter());
    }

}
