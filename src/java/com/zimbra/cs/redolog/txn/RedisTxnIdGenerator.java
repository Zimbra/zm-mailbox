package com.zimbra.cs.redolog.txn;

import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.redolog.TransactionId;

/**
 * Transaction ID generator which uses Redis transactions to guarantee cross-process uniqueness
 * Suitable for use in a multi-server clustered Zimbra environment
 *
 */
public class RedisTxnIdGenerator implements TxnIdGenerator {

    public RedisTxnIdGenerator(JedisPool jedisPool) {
        super();
        this.jedisPool = jedisPool;
    }

    private static final String KEY = "zmRedoLogTxnId";
    private static final int MAX_TRIES = 100;

    protected JedisPool jedisPool;

    private TransactionId newTransactionId() {
        return new TransactionId((int) System.currentTimeMillis() / 1000, 1);
    }

    @Override
    public TransactionId getNext() {
        Jedis jedis = jedisPool.getResource();
        try {
            boolean good = false;
            int tries = 0;
            TransactionId txnId = null;
            while (!good && ++tries < MAX_TRIES) {
                jedis.watch(KEY);
                String startValue = jedis.get(KEY);
                Transaction transaction = jedis.multi();
                if (startValue == null) {
                    ZimbraLog.redolog.debug("no existing txnId");
                    txnId = newTransactionId();
                } else {
                    ZimbraLog.redolog.debug("existing txnId %s", startValue);
                    try {
                        txnId = TransactionId.decodeFromString(startValue);
                    } catch (ServiceException e) {
                        ZimbraLog.redolog.warn("unable to parse existing transaction id", e);
                        txnId = newTransactionId();
                    }
                    if (txnId.getCounter() < 0x7fffffffL) {
                        txnId = new TransactionId(txnId.getTime(), txnId.getCounter() + 1);
                    } else {
                        txnId = new TransactionId(Math.max((int) System.currentTimeMillis() / 1000, txnId.getTime() + 1), 1);
                    }
                }
                transaction.set(KEY, txnId.encodeToString());
                List<Object> result = transaction.exec();
                if (result != null && result.size() > 0) {
                    //null return when WATCH notices value change by other client
                    good = true;
                }
            }
            if (good) {
                ZimbraLog.redolog.debug("generated txnId %s", txnId);
                return txnId;
            } else {
                throw new RuntimeException("unable to generate new transactionId");
            }
        } finally {
            jedisPool.returnResource(jedis);
        }
    }

}
