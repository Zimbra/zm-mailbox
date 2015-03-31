package com.zimbra.cs.redolog.txn;

import org.junit.Assume;
import org.junit.BeforeClass;

import redis.clients.jedis.JedisPool;

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
    public TxnIdGenerator getGenerator() {
        try {
            JedisPool pool = (JedisPool) Zimbra.getAppContext().getBean("jedisPool");
            pool.getResource().ping();
            return new RedisTxnIdGenerator(pool);
        } catch (Exception e) {
            Assume.assumeNoException(e);
            return null;
        }
    }

}
