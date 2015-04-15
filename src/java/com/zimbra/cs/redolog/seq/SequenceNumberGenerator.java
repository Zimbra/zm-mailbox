package com.zimbra.cs.redolog.seq;

public interface SequenceNumberGenerator {

    /**
     * Get the current log sequence number
     */
    public long getCurrentSequence();

    /**
     * Initialize to a given sequence number
     * @param seq
     */
    public void initSequence(long seq);

    /**
     * Increment the sequence number
     * @return the new current number
     */
    public long incrementSequence();
}
