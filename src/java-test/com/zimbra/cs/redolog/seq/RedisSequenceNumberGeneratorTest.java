package com.zimbra.cs.redolog.seq;

import org.junit.Assume;
import org.junit.BeforeClass;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.RedisOnLocalhostZimbraConfig;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;


public class RedisSequenceNumberGeneratorTest extends AbstractSequenceNumberGeneratorTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", RedisOnLocalhostZimbraConfig.class);
    }

    @Override
    public SequenceNumberGenerator getGenerator() {
        try {
            JedisPool pool = (JedisPool) Zimbra.getAppContext().getBean("jedisPool");
            try (Jedis jedis = pool.getResource()) {
                jedis.ping();
            }
            RedisSequenceNumberGenerator generator = new RedisSequenceNumberGenerator(pool);
            generator.clear();
            return generator;
        } catch (Exception e) {
            Assume.assumeNoException(e);
            return null;
        }
    }

}
