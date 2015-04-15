package com.zimbra.cs.redolog.seq;


public class LocalSequenceNumberGeneratorTest extends AbstractSequenceNumberGeneratorTest {

    @Override
    public SequenceNumberGenerator getGenerator() {
        return new LocalSequenceNumberGenerator();
    }

}
