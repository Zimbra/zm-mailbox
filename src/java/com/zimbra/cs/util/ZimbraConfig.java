/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Lazy;

import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisCluster;
import redis.clients.jedis.JedisPool;

import com.zimbra.common.consul.ConsulClient;
import com.zimbra.common.consul.ConsulServiceLocator;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.servicelocator.ChainedServiceLocator;
import com.zimbra.common.servicelocator.RandomSelector;
import com.zimbra.common.servicelocator.Selector;
import com.zimbra.common.servicelocator.ServiceLocator;
import com.zimbra.common.util.ZimbraHttpClientManager;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.ProvisioningServiceLocator;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.amqp.AmqpConstants;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.index.DefaultIndexingQueueAdapter;
import com.zimbra.cs.index.IndexingQueueAdapter;
import com.zimbra.cs.index.IndexingService;
import com.zimbra.cs.mailbox.AmqpMailboxListenerTransport;
import com.zimbra.cs.mailbox.CacheManager;
import com.zimbra.cs.mailbox.DefaultMailboxLockFactory;
import com.zimbra.cs.mailbox.FoldersAndTagsCache;
import com.zimbra.cs.mailbox.LocalSharedDeliveryCoordinator;
import com.zimbra.cs.mailbox.MailboxListenerManager;
import com.zimbra.cs.mailbox.MailboxListenerTransport;
import com.zimbra.cs.mailbox.MailboxLockFactory;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MemcachedFoldersAndTagsCache;
import com.zimbra.cs.mailbox.RedisClusterMailboxListenerManager;
import com.zimbra.cs.mailbox.RedisClusterSharedDeliveryCoordinator;
import com.zimbra.cs.mailbox.RedisMailboxListenerTransport;
import com.zimbra.cs.mailbox.RedisSharedDeliveryCoordinator;
import com.zimbra.cs.mailbox.SharedDeliveryCoordinator;
import com.zimbra.cs.mailbox.acl.EffectiveACLCache;
import com.zimbra.cs.mailbox.acl.MemcachedEffectiveACLCache;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.memcached.ZimbraMemcachedClientConfigurer;
import com.zimbra.cs.redolog.DefaultRedoLogProvider;
import com.zimbra.cs.redolog.RedoLogProvider;
import com.zimbra.cs.redolog.seq.LocalSequenceNumberGenerator;
import com.zimbra.cs.redolog.seq.RedisSequenceNumberGenerator;
import com.zimbra.cs.redolog.seq.SequenceNumberGenerator;
import com.zimbra.cs.redolog.txn.LocalTxnIdGenerator;
import com.zimbra.cs.redolog.txn.RedisTxnIdGenerator;
import com.zimbra.cs.redolog.txn.TxnIdGenerator;
import com.zimbra.cs.store.StoreManager;
import com.zimbra.cs.store.file.FileBlobStore;
import com.zimbra.qless.QlessClient;
import com.zimbra.soap.DefaultSoapSessionFactory;
import com.zimbra.soap.SoapSessionFactory;

/**
 * Singleton factories for Spring Configuration.
 *
 * To get a reference to a singleton, use:
 *
 * <code>
 * Zimbra.getAppContext().getBean(myclass);
 * </code>
 *
 * To autowire and initialize an object with Spring that you've constructed yourself:
 *
 * <code>
 * Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(myObject);
 * Zimbra.getAppContext().getAutowireCapableBeanFactory().initializeBean(myObject, "myObjectName");
 * </code>
 */
@Configuration
@EnableAspectJAutoProxy
@Lazy
public class ZimbraConfig {

    public AmqpAdmin amqpAdmin(ConnectionFactory amqpConnectionFactory) throws Exception {
        AmqpAdmin amqpAdmin = new RabbitAdmin(amqpConnectionFactory);
        amqpAdminInit(amqpAdmin);
        return amqpAdmin;
    }

    /** One-time setup of AMQP exchanges and queues required at runtime, in case they don't yet exist */
    protected void amqpAdminInit(AmqpAdmin amqpAdmin) throws Exception {
        amqpAdmin.declareExchange(AmqpConstants.EXCHANGE_MBOX);
    }

    protected ConnectionFactory amqpConnectionFactory(URI uri) throws Exception {
        CachingConnectionFactory factory = new CachingConnectionFactory(uri.getHost());
        if (uri.getPort() != -1) {
            factory.setPort(uri.getPort());
        }
        if (uri.getAuthority() != null) {
            int atPos = uri.getAuthority().indexOf('@');
            String auth = uri.getAuthority().substring(0, atPos);
            factory.setUsername(auth.split(":")[0]);
            factory.setPassword(auth.split(":")[1]);
        }
        factory.setVirtualHost(uri.getPath());
        return factory;
    }

    protected RabbitTemplate amqpTemplate(ConnectionFactory amqpConnectionFactory) throws Exception {
        return new RabbitTemplate(amqpConnectionFactory);
    }

    @Bean
    public CacheManager cacheManager() throws ServiceException {
        return new CacheManager();
    }

    @Bean
    public CalendarCacheManager calendarCacheManager() throws ServiceException {
        return new CalendarCacheManager();
    }

    @Bean
    public ConsulClient consulClient() throws IOException, ServiceException {
        Server server = Provisioning.getInstance().getLocalServer();
        String url = server.getConsulURL();
        return new ConsulClient(url);
    }

    @Bean
    public FoldersAndTagsCache foldersAndTagsCache() throws ServiceException {
        return new MemcachedFoldersAndTagsCache();
    }

    @Bean
    public ZimbraHttpClientManager httpClientManager() throws Exception {
        return new ZimbraHttpClientManager();
    }

    @Bean
    public IndexingQueueAdapter indexingQueueAdapter() throws Exception {
        IndexingQueueAdapter instance = null;
        Server localServer = Provisioning.getInstance().getLocalServer();
        String className = localServer.getIndexingQueueProvider();
        if (className != null && !className.isEmpty()) {
            try {
                instance = (IndexingQueueAdapter) Class.forName(className).newInstance();
            } catch (ClassNotFoundException e) {
                instance = (IndexingQueueAdapter) ExtensionUtil.findClass(className).newInstance();
            }
        }
        if (instance == null) {
            //fall back to default (local) queue implementation
            instance = new DefaultIndexingQueueAdapter();
        }
        return instance;
    }

    @Bean
    public IndexingService indexingService()  {
        return new IndexingService();
    }

    @Bean
    public EffectiveACLCache effectiveACLCache() throws ServiceException {
        return new MemcachedEffectiveACLCache();
    }

    /**
     * External mailbox listener managers, which are used to coordinate cache invalidations and other
     * mailbox data changes across multiple mailstores.
     */
    @Bean
    public List<MailboxListenerTransport> externalMailboxListeners() throws Exception {
        String[] uris = Provisioning.getInstance().getLocalServer().getMailboxListenerUrl();
        if (uris.length == 0 && isRedisAvailable()) {
            ZimbraLog.misc.info("No external mailbox listeners are configured; defaulting to Redis");
            uris = new String[] {"redis:default"};
        }
        if (uris.length == 0) {
            ZimbraLog.misc.info("No external mailbox listeners are configured");
            return Collections.emptyList();
        }

        List<MailboxListenerTransport> result = new ArrayList<>();
        for (String uri: uris) {
            if (uri.startsWith("redis:")) {
                if ("redis:default".equals(uri)) {
                    // URI specifies the default pool specified by zimbraRedisUrl attribute
                    if (isRedisClusterAvailable()) {
                        result.add(new RedisClusterMailboxListenerManager(jedisCluster()));
                        ZimbraLog.misc.info("Registered external mailbox listener: %s", uri);
                    } else if (isRedisAvailable()) {
                        result.add(new RedisMailboxListenerTransport(jedisPool()));
                        ZimbraLog.misc.info("Registered external mailbox listener: %s", uri);
                    } else {
                        ZimbraLog.misc.error("Ignoring request to register external mailbox listener: %s because no zimbraRedisUrl URIs are configured", uri);
                    }
                } else {
                    // URI specifies a new Redis endpoint, so it gets its own pool
                    URI uri_ = new URI(uri);
                    JedisPool jedisPool = jedisPool(uri_.getHost(), uri_.getPort());
                    result.add(new RedisMailboxListenerTransport(jedisPool));
                    ZimbraLog.misc.info("Registered external mailbox listener: %s", uri);
                }

            } else if (uri.startsWith("amqp:")) {
                ConnectionFactory connectionFactory = amqpConnectionFactory(new URI(uri));
                AmqpAdmin amqpAdmin = amqpAdmin(connectionFactory);
                AmqpTemplate amqpTemplate = amqpTemplate(connectionFactory);
                result.add(new AmqpMailboxListenerTransport(amqpAdmin, amqpTemplate));
                ZimbraLog.misc.info("Registered external mailbox listener: %s", uri);

            } else {
                ZimbraLog.misc.error("Ignoring unsupported URI scheme for external mailbox listener: %s", uri);
            }
        }
        return result;
    }

    public boolean isMemcachedAvailable() {
        try {
            Server server = Provisioning.getInstance().getLocalServer();
            String[] serverList = server.getMultiAttr(Provisioning.A_zimbraMemcachedClientServerList);
            return serverList.length > 0;
        } catch (ServiceException e) {
            ZimbraLog.system.error("Error reading memcached configuration; proceeding as unavailable", e);
            return false;
        }
    }

    /** Returns whether Redis service is available (either cluster or sentinel/stand-alone) */
    public boolean isRedisAvailable() throws ServiceException {
        Set<HostAndPort> uris = redisUris();
        if (uris.isEmpty()) {
            return false;
        }
        try (Jedis jedis = jedisPool().getResource()) {
            jedis.get("");
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isRedisClusterAvailable() throws ServiceException {
        return isRedisClusterAvailable(jedisCluster());
    }

    public boolean isRedisClusterAvailable(JedisCluster jedisCluster) throws ServiceException {
        if (jedisCluster == null) {
            return false;
        }
        try {
            jedisCluster.get("");
            return true;
        } catch (Exception e) {
            ZimbraLog.misc.info("Failed connecting to a Redis Cluster; defaulting to non-cluster mode Redis access (%s)", e.getLocalizedMessage());
            return false;
        }
    }

    /** Returns a JedisCluster client if possible, or null */
    @Bean
    public JedisCluster jedisCluster() throws ServiceException {
        Set<HostAndPort> uris = redisUris();
        if (uris.isEmpty()) {
            return null;
        }
        try {
            return new JedisCluster(uris);
        } catch (Exception e) {
            ZimbraLog.misc.info("Failed connecting to a Redis Cluster; defaulting to non-cluster mode Redis access (%s)", e.getLocalizedMessage());
            return null;
        }
    }

    /** Returns a JedisPool client if possible, or null */
    public JedisPool jedisPool(String host, int port) throws ServiceException {
        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
        Server server = Provisioning.getInstance().getLocalServer();
        config.setMaxTotal(server.getRedisMaxConnectionCount());
        return new JedisPool(config, host, port == -1 ? 6379 : port);
    }

    /** Returns a JedisPool client if possible, or null */
    @Bean
    public JedisPool jedisPool() throws ServiceException {
        Set<HostAndPort> uris = redisUris();
        if (uris.isEmpty()) {
            return null;
        }
        HostAndPort hostAndPort = uris.iterator().next();
        return jedisPool(hostAndPort.getHost(), hostAndPort.getPort());
    }

    @Bean
    public MailboxListenerManager mailboxListenerManager() {
        return new MailboxListenerManager();
    }

    @Bean
    public MailboxLockFactory mailboxLockFactory() throws ServiceException {
        return new DefaultMailboxLockFactory();
    }

    @Bean
    public MailboxManager mailboxManager() throws ServiceException {
        MailboxManager instance = null;
        String className = LC.zimbra_class_mboxmanager.value();
        if (className != null && !className.equals("")) {
            try {
                try {
                    instance = (MailboxManager) Class.forName(className).newInstance();
                } catch (ClassNotFoundException cnfe) {
                    // ignore and look in extensions
                    instance = (MailboxManager) ExtensionUtil.findClass(className).newInstance();
                }
            } catch (Exception e) {
                ZimbraLog.account.error("could not instantiate MailboxManager interface of class '" + className + "'; defaulting to MailboxManager", e);
            }
        }
        if (instance == null) {
            instance = new MailboxManager();
        }
        return instance;
    }

    @Bean
    public ZimbraMemcachedClient memcachedClient() throws Exception {
        return new ZimbraMemcachedClient();
    }

    @Bean
    public ZimbraMemcachedClientConfigurer memcachedClientConfigurer() throws Exception {
        return new ZimbraMemcachedClientConfigurer();
    }

    @Bean
    public QlessClient qlessClient() throws Exception {
        if (!isRedisAvailable()) {
            return null;
        }
        JedisPool jedisPool = jedisPool();
        QlessClient instance = new QlessClient(jedisPool);
        return instance;
    }

    public Set<HostAndPort> redisUris() throws ServiceException {
        String[] uris = Provisioning.getInstance().getLocalServer().getRedisUrl();
        if (uris.length == 0) {
            return Collections.emptySet();
        }
        Set<HostAndPort> result = new HashSet<>();
        try {
            for (String uriStr: uris) {
                URI uri = new URI(uriStr);
                result.add(new HostAndPort(uri.getHost(), uri.getPort() == -1 ? 6379 : uri.getPort()));
            }
            return result;
        } catch (URISyntaxException e) {
            throw ServiceException.PARSE_ERROR("Invalid Redis URI", e);
        }
    }

    @Bean
    public SequenceNumberGenerator redologSeqNumGenerator() throws Exception
    {
        SequenceNumberGenerator generator = null;
        if (isRedisAvailable()) {
            generator = new RedisSequenceNumberGenerator(jedisPool());
        }

        if (generator == null) {
          if (Zimbra.isAlwaysOn()) {
              throw new Exception("Redis is required in always on environment");
          }
          generator = new LocalSequenceNumberGenerator();
        }
        return generator;
    }

    @Bean
    public TxnIdGenerator redologTxnIdGenerator() throws Exception
    {
        TxnIdGenerator idGenerator = null;
        if (isRedisAvailable()) {
            idGenerator = new RedisTxnIdGenerator(jedisPool());
        }

        if (idGenerator == null) {
          if (Zimbra.isAlwaysOn()) {
              throw new Exception("Redis is required in always on environment");
          }
          idGenerator = new LocalTxnIdGenerator();
        }
        return idGenerator;
    }

    @Bean
    public RedoLogProvider redologProvider() throws Exception {
        RedoLogProvider instance = null;
        Class<?> klass = null;
        Server config = Provisioning.getInstance().getLocalServer();
        String className = config.getAttr(Provisioning.A_zimbraRedoLogProvider);
        if (className != null) {
            klass = Class.forName(className);
        } else {
            klass = DefaultRedoLogProvider.class;
            ZimbraLog.misc.debug("Redolog provider name not specified.  Using default " +
                                 klass.getName());
        }
        instance = (RedoLogProvider) klass.newInstance();
        return instance;
    }

	@Bean
	public ServiceLocator serviceLocator() throws Exception {
	    // First try Consul, then fall back on provisioned service lists (which may be up or down)
	    ServiceLocator sl1 = new ConsulServiceLocator(consulClient());
	    ServiceLocator sl2 = new ProvisioningServiceLocator(Provisioning.getInstance());
	    return new ChainedServiceLocator(sl1, sl2);
	}

    /** Centralized algorithm for selection of a server from a list, for load balancing and/or account reassignment, or picking a SOAP target in a cluster */
    @Bean
    public Selector<ServiceLocator.Entry> serviceLocatorHostSelector() throws ServiceException {
        return new RandomSelector<ServiceLocator.Entry>();
    }

    @Bean
    public SharedDeliveryCoordinator sharedDeliveryCoordinator() throws Exception {
        SharedDeliveryCoordinator instance = null;
        String className = LC.zimbra_class_shareddeliverycoordinator.value();
        if (className != null && !className.equals("")) {
            try {
                instance = (SharedDeliveryCoordinator) Class.forName(className).newInstance();
            } catch (ClassNotFoundException e) {
                instance = (SharedDeliveryCoordinator) ExtensionUtil.findClass(className).newInstance();
            }
        }
        if (instance == null) {
            if (isRedisClusterAvailable()) {
                instance = new RedisClusterSharedDeliveryCoordinator();
            } else if (isRedisAvailable()) {
                instance = new RedisSharedDeliveryCoordinator();
//            } else if (isMemcachedAvailable()) {
//                TODO: Future Memcached-based shared delivery coordination support
//                instance = new MemcachedSharedDeliveryCoordinator();
            } else {
                instance = new LocalSharedDeliveryCoordinator();
            }
        }
        return instance;
    }

    @Bean
    public SoapSessionFactory soapSessionFactory() {
        SoapSessionFactory instance = null;
        String className = LC.zimbra_class_soapsessionfactory.value();
        if (className != null && !className.equals("")) {
            try {
                try {
                    instance = (SoapSessionFactory) Class.forName(className).newInstance();
                } catch (ClassNotFoundException cnfe) {
                    // ignore and look in extensions
                    instance = (SoapSessionFactory) ExtensionUtil.findClass(className).newInstance();
                }
            } catch (Exception e) {
                ZimbraLog.account.error("could not instantiate SoapSessionFactory class '" + className + "'; defaulting to SoapSessionFactory", e);
            }
        }
        if (instance == null) {
            instance = new DefaultSoapSessionFactory();
        }
        return instance;
    }

	@Bean
    public StoreManager storeManager() throws Exception {
		StoreManager instance = null;
        String className = LC.zimbra_class_store.value();
        if (className != null && !className.equals("")) {
            try {
                instance = (StoreManager) Class.forName(className).newInstance();
            } catch (ClassNotFoundException e) {
                instance = (StoreManager) ExtensionUtil.findClass(className).newInstance();
            }
        }
        if (instance == null) {
            instance = new FileBlobStore();
        }
        return instance;
    }

	@Bean
	public ZimbraApplication zimbraApplication() throws Exception {
	    ZimbraApplication instance = null;
        String className = LC.zimbra_class_application.value();
        if (className != null && !className.equals("")) {
            try {
                instance = (ZimbraApplication)Class.forName(className).newInstance();
            } catch (Exception e) {
                ZimbraLog.misc.error(
                    "could not instantiate ZimbraApplication class '"
                        + className + "'; defaulting to ZimbraApplication", e);
            }
        }
        if (instance == null) {
            instance = new ZimbraApplication();
        }
        return instance;
	}
}
