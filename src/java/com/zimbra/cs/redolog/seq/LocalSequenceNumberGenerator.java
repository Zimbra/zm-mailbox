package com.zimbra.cs.redolog.seq;

public class LocalSequenceNumberGenerator implements SequenceNumberGenerator {

    // Monotonically increasing sequence number for redolog files.
    // Sequence starts at 0 and increments without gap, and may
    // eventually wraparound.
    private long seqNum;

    @Override
    public long getCurrentSequence() {
        return seqNum;
    }

    @Override
    public synchronized void initSequence(long seq) {
        seqNum = seq;
    }

    @Override
    public synchronized long incrementSequence() {
        if (seqNum < Long.MAX_VALUE) {
            ++seqNum;
        } else {
            seqNum = 0;
        }
        return seqNum;
    }

}
