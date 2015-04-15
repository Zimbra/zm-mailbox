package com.zimbra.cs.redolog.seq;

import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.zimbra.common.util.ZimbraLog;

public abstract class AbstractSequenceNumberGeneratorTest {

    public abstract SequenceNumberGenerator getGenerator();

    @Test
    public void testGenerate() {
        SequenceNumberGenerator generator = getGenerator();
        Assert.assertEquals(0L, generator.getCurrentSequence());
        long next = generator.incrementSequence();
        Assert.assertEquals(1L, next);
        next = generator.incrementSequence();
        Assert.assertEquals(2L, next);
    }

    @Test
    public void testInitialize() {
        SequenceNumberGenerator generator = getGenerator();
        long startSeq = 100L;
        generator.initSequence(startSeq);
        long next = generator.incrementSequence();
        Assert.assertEquals(startSeq + 1, next);
        next = generator.incrementSequence();
        Assert.assertEquals(startSeq + 2, next);
    }

    @Test(timeout=100000)
    public void testGenerateConcurrently() {

        Set<Long> generated = new HashSet<Long>();
        Set<AssertionError> errors = new HashSet<AssertionError>();

        SequenceNumberGenerator generator = getGenerator();

        int numThreads = 10;
        int trials = 10;
        Set<Thread> threads = new HashSet<Thread>();

        for (int i = 0; i < numThreads; i++) {
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        long lastSeqNum = generator.incrementSequence();
                        Assert.assertTrue(generated.add(lastSeqNum));
                        for (int j = 0; j < trials; j++) {
                            long nextSeqNum = generator.incrementSequence();
                            Assert.assertTrue(nextSeqNum > lastSeqNum);
                            lastSeqNum = nextSeqNum;
                            Assert.assertTrue(generated.add(lastSeqNum));
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

        for (Thread t : threads) {
            try {
                t.join();
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
        SequenceNumberGenerator generator = getGenerator();
        generator.initSequence(Long.MAX_VALUE);
        long next = generator.incrementSequence();
        Assert.assertEquals(0, next);
    }

    private static class IncrementThread extends Thread {
        private SequenceNumberGenerator generator;
        private boolean good;
        static Set<Long> ids = new HashSet<Long>();

        public IncrementThread(SequenceNumberGenerator generator) {
            super();
            this.generator = generator;
        }

        public boolean isGood() {
            return good;
        }

        @Override
        public void run() {
            long next = generator.incrementSequence();
            try {
                //change this if test expands.
                //currently just used to test that the first two ids after rollover are unique
                Assert.assertTrue(next == 0 || next == 1);
                synchronized(ids) {
                    Assert.assertTrue(ids.add(next));
                }
                good = true;
            } catch (AssertionError ae) {
                ZimbraLog.test.error("assertion error in thread", ae);
                good = false;
            }
        }

    }

    @Test(timeout=20000)
    public void testConcurrentRollover() {
        SequenceNumberGenerator generator = getGenerator();
        generator.initSequence(Long.MAX_VALUE);

        IncrementThread t1 = new IncrementThread(generator);
        IncrementThread t2 = new IncrementThread(generator);
        IncrementThread.ids.clear();
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
        }
        Assert.assertTrue(t1.isGood());
        Assert.assertTrue(t2.isGood());
    }

}
