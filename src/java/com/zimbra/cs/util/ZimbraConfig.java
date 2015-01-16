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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Lazy;

import redis.clients.jedis.JedisPool;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.util.memcached.ZimbraMemcachedClient;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.consul.ConsulClient;
import com.zimbra.cs.consul.ConsulServiceLocator;
import com.zimbra.cs.consul.ServiceLocator;
import com.zimbra.cs.extension.ExtensionUtil;
import com.zimbra.cs.index.DefaultIndexingQueueAdapter;
import com.zimbra.cs.index.IndexingQueueAdapter;
import com.zimbra.cs.mailbox.FoldersAndTagsCache;
import com.zimbra.cs.mailbox.LocalSharedDeliveryCoordinator;
import com.zimbra.cs.mailbox.MailboxLockFactory;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxPubSubAdapter;
import com.zimbra.cs.mailbox.MemcachedFoldersAndTagsCache;
import com.zimbra.cs.mailbox.RedisMailboxPubSubAdapter;
import com.zimbra.cs.mailbox.RedisSharedDeliveryCoordinator;
import com.zimbra.cs.mailbox.SharedDeliveryCoordinator;
import com.zimbra.cs.mailbox.acl.EffectiveACLCache;
import com.zimbra.cs.mailbox.acl.MemcachedEffectiveACLCache;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.cs.memcached.ZimbraMemcachedClientConfigurer;
import com.zimbra.cs.redolog.DefaultRedoLogProvider;
import com.zimbra.cs.redolog.RedoLogProvider;
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

    @Bean(name="calendarCacheManager")
    public CalendarCacheManager calendarCacheManagerBean() throws ServiceException {
        return new CalendarCacheManager();
    }

    @Bean
    public ConsulClient consulClient() throws IOException, ServiceException {
        Server server = Provisioning.getInstance().getLocalServer();
        String url = server.getConsulURL();
        return new ConsulClient(url);
    }

    @Bean(name="effectiveACLCache")
    public EffectiveACLCache effectiveACLCacheBean() throws ServiceException {
        return new MemcachedEffectiveACLCache();
    }

    @Bean(name="foldersAndTagsCache")
    public FoldersAndTagsCache foldersAndTagsCacheBean() throws ServiceException {
        return new MemcachedFoldersAndTagsCache();
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

    public boolean isRedisAvailable() throws ServiceException {
        return false; // TODO redisUri() != null;
    }

    @Bean(name="jedisPool")
    public JedisPool jedisPoolBean() throws ServiceException {
        if (!isRedisAvailable()) {
            return null;
        }
        URI uri = redisUri();
        return new JedisPool(uri.getHost(), uri.getPort());
    }

    @Bean(name="mailboxLockFactory")
    public MailboxLockFactory mailboxLockFactoryBean() throws ServiceException {
        return new MailboxLockFactory();
    }

    @Bean(name="mailboxManager")
    public MailboxManager mailboxManagerBean() throws ServiceException {
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

    /**
     * Redis or AMQP pub/sub adapter, which is used to coordinate cache invalidations and other
     * important mailbox data changes across multiple mailstores.
     */
    @Bean(name="mailboxPubSubAdapter")
    public MailboxPubSubAdapter mailboxPubSubAdapter() throws Exception {
        if (isRedisAvailable()) {
            return new RedisMailboxPubSubAdapter();
        } else {
            return null;
        }
    }

    @Bean(name="memcachedClient")
    public ZimbraMemcachedClient memcachedClientBean() throws Exception {
        return new ZimbraMemcachedClient();
    }

    @Bean(name="memcachedClientConfigurer")
    public ZimbraMemcachedClientConfigurer memcachedClientConfigurerBean() throws Exception {
        return new ZimbraMemcachedClientConfigurer();
    }

    @Bean(name="qlessClient")
    public QlessClient qlessClient() throws Exception {
        if (!isRedisAvailable()) {
            return null;
        }
        JedisPool jedisPool = jedisPoolBean();
        QlessClient instance = new QlessClient(jedisPool);
        return instance;
    }

    public URI redisUri() throws ServiceException {
        try {
            return new URI("redis://localhost:6379"); // TODO
        } catch (URISyntaxException e) {
            throw ServiceException.PARSE_ERROR("Invalid Redis URI", e);
        }
    }

	@Bean(name="redologProvider")
    public RedoLogProvider redoLogProviderBean() throws Exception {
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
	    return new ConsulServiceLocator();
	}

    @Bean(name="sharedDeliveryCoordinator")
    public SharedDeliveryCoordinator sharedDeliveryCoordinatorBean() throws Exception {
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
            if (isRedisAvailable()) {
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

    @Bean(name="soapSessionFactory")
    public SoapSessionFactory soapSessionFactoryBean() {
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

	@Bean(name="storeManager")
    public StoreManager storeManagerBean() throws Exception {
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

	@Bean(name="zimbraApplication")
	public ZimbraApplication zimbraApplicationBean() throws Exception {
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

	@Bean(name="indexingQueueAdapter")
	public IndexingQueueAdapter indexingQueueAdapterBean() throws Exception {
	    IndexingQueueAdapter instance = null;
        String className = LC.zimbra_class_indexing_queue_adapter.value();
        if (className != null && !className.equals("")) {
            try {
                instance = (IndexingQueueAdapter) Class.forName(className).newInstance();
            } catch (ClassNotFoundException e) {
                instance = (IndexingQueueAdapter) ExtensionUtil.findClass(className).newInstance();
            }
        }
        if (instance == null) {
            //TODO: instantiate Redis adapter if Redis is available
            instance = new DefaultIndexingQueueAdapter();
        }
        return instance;
	}
}
