package com.zimbra.cs.index;

import java.util.HashMap;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.HostAndPort;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.LocalCachingZimbraConfig;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.RedisTestHelper;
import com.zimbra.cs.store.MockStoreManager;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraConfig;
public class RedisIndexingQueueAdapterTest extends AbstractIndexingQueueAdapterTest {

    @BeforeClass
    public static void init() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", MyZimbraConfig.class);
        Provisioning prov = Provisioning.getInstance();
        prov.createAccount("test@zimbra.com", "secret", new HashMap<String, Object>());
        Provisioning.getInstance().getLocalServer().setIndexingQueueProvider("");
        Zimbra.getAppContext().getBean(IndexingService.class).shutDown();
        Provisioning.getInstance().getLocalServer().setIndexManualCommit(true);
    }

    @Configuration
    static class MyZimbraConfig extends LocalCachingZimbraConfig {

        @Override
        public Set<HostAndPort> redisUris() throws ServiceException {
            return RedisTestHelper.getRedisUris();
        }
    }

    @Before
    public void setUp() throws Exception {
        MailboxTestUtil.clearData();
        //instantiate and autowire IndexingQueueAdapter
        adapter = new RedisIndexingQueueAdapter();
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(adapter);
        adapter.drain();
        adapter.clearAllTaskCounts();
    }
    
    @Override
    protected boolean isQueueSourceAvailable() throws Exception {
        if (Zimbra.getAppContext().getBean(ZimbraConfig.class).isRedisClusterAvailable()) {
            return false;
        }
        return Zimbra.getAppContext().getBean(ZimbraConfig.class).isRedisAvailable();
    }
}
