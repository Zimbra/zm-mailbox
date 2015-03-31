package com.zimbra.cs.redolog.txn;

public class LocalTxnIdGeneratorTest extends AbstractTxnIdGeneratorTest {

    @Override
    public TxnIdGenerator getGenerator() {
        return new LocalTxnIdGenerator();
    }

}
