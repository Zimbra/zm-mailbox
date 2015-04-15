package com.zimbra.cs.redolog.seq;

import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.exceptions.JedisDataException;

import com.google.common.annotations.VisibleForTesting;
import com.zimbra.common.util.ZimbraLog;

public class RedisSequenceNumberGenerator implements SequenceNumberGenerator {

    protected JedisPool jedisPool;
    private static final String KEY = "zmRedoLogSeqNum";
    private static final int MAX_TRIES = 100;

    public RedisSequenceNumberGenerator(JedisPool jedisPool) {
        super();
        this.jedisPool = jedisPool;
    }

    @VisibleForTesting
    void clear() {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del(KEY);
        }
    }

    @Override
    public long getCurrentSequence() {
        try (Jedis jedis = jedisPool.getResource()) {
            String currentVal = jedis.get(KEY);
            return currentVal == null ? 0 : Long.valueOf(currentVal);
        }
    }

    @Override
    public void initSequence(long seq) {
        try (Jedis jedis = jedisPool.getResource()) {
            boolean good = false;
            int tries = 0;
            while (!good && ++tries < MAX_TRIES) {
                jedis.watch(KEY);
                jedis.get(KEY);
                Transaction transaction = jedis.multi();
                transaction.set(KEY, seq+"");
                List<Object> result = transaction.exec();
                if (result != null && result.size() > 0) {
                    //null return when WATCH notices value change by other client
                    good = true;
                }
            }
            if (good) {
                ZimbraLog.redolog.debug("set seqNum to %d", seq);
            } else {
                throw new RuntimeException("unable to generate new sequence number");
            }
        }
    }

    @Override
    public long incrementSequence() {
        try (Jedis jedis = jedisPool.getResource()) {
            try {
                return jedis.incr(KEY);
            } catch (JedisDataException jde) {
                //TODO: maybe a way to get a real error code from Jedis?
                if (jde.getMessage().contains("ERR increment or decrement would overflow")) {
                    jedis.watch(KEY);
                    String val = jedis.get(KEY);
                    if (val != null && Long.valueOf(val) == Long.MAX_VALUE) {
                        Transaction transaction = jedis.multi();
                        transaction.set(KEY, 0 + "");
                        List<Object> result = transaction.exec();
                        if (result != null && result.size() > 0) {
                            //reset correctly
                            return 0;
                        }
                    }
                    //most likely someone else already hit this and recycled
                    //try increment once more on that assumption
                    //if this fails then there is a more serious problem; or unexpected overflow
                    return jedis.incr(KEY);
                } else {
                    throw jde;
                }
            }
        }
    }

}
