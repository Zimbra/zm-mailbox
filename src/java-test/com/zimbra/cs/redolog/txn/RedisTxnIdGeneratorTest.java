package com.zimbra.cs.redolog.txn;

import org.junit.Assume;
import org.junit.BeforeClass;

import redis.clients.jedis.Jedis;
import redis.clients.util.Pool;

import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.RedisOnLocalhostZimbraConfig;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;

public class RedisTxnIdGeneratorTest extends AbstractTxnIdGeneratorTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", RedisOnLocalhostZimbraConfig.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public TxnIdGenerator getGenerator() {
        try {
            Pool<Jedis> pool = Zimbra.getAppContext().getBean(Pool.class);
            try (Jedis jedis = pool.getResource()) {
                jedis.ping();
            }
            RedisTxnIdGenerator generator = new RedisTxnIdGenerator(pool);
            generator.clear();
            return generator;
        } catch (Exception e) {
            Assume.assumeNoException(e);
            return null;
        }
    }

}
