package com.zimbra.cs.redolog;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.redolog.seq.SequenceNumberGenerator;
import com.zimbra.cs.util.Zimbra;

/**
 * RolloverManager abstract implementation
 * Default constructor wraps the SequenceNumberGenerator provided by Spring, allowing simulated multi-inheritence
 *
 */
public abstract class AbstractRolloverManager implements RolloverManager {

    private final SequenceNumberGenerator generator;

    public AbstractRolloverManager() {
        generator = (SequenceNumberGenerator) Zimbra.getAppContext().getBean("redologSeqNumGenerator");
    }

    public AbstractRolloverManager(SequenceNumberGenerator generator) {
        this.generator = generator;
    }

    @Override
    public long getCurrentSequence() {
        return generator.getCurrentSequence();
    }

    @Override
    public void initSequence(long seq) {
        generator.initSequence(seq);
        ZimbraLog.redolog.trace("init sequence to %d for generator %s", seq, generator);
    }

    @Override
    public long incrementSequence() {
        long seq = generator.incrementSequence();
        ZimbraLog.redolog.trace("inc sequence to %d for generator %s", seq, generator);
        return seq;
    }
}
