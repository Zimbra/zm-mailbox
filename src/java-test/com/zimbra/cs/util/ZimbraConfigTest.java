/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra Software, LLC.
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

import java.lang.reflect.Field;
import java.util.List;

import javax.annotation.PostConstruct;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.Bean;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.util.Pool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.MailboxListenerTransport;
import com.zimbra.cs.mailbox.MailboxTestUtil;
import com.zimbra.cs.mailbox.RedisMailboxListenerTransport;
import com.zimbra.cs.store.MockStoreManager;

/**
 * Unit test for {@link ZimbraConfig}.
 *
 * @author ysasaki
 */
public class ZimbraConfigTest {

    @Test
    @SuppressWarnings("unchecked")
    public void externalMailboxListenersGetAutowired() throws Exception {
        MailboxTestUtil.initServer(MockStoreManager.class, "", ExternalMailboxListenersGetAutowiredConfig.class);
        List<MailboxListenerTransport> externalMailboxListeners = (List<MailboxListenerTransport>)Zimbra.getAppContext().getBean("externalMailboxListeners");
        Assert.assertEquals(1, externalMailboxListeners.size());
        MailboxListenerTransport mailboxListenerTransport = externalMailboxListeners.get(0);
        Field field = RedisMailboxListenerTransport.class.getDeclaredField("jedisPool");
        field.setAccessible(true);
        Assert.assertNotNull("RedisMailboxListenerTransport was not autowired", field.get(mailboxListenerTransport));
    }

    static class ExternalMailboxListenersGetAutowiredConfig extends ZimbraConfig {

        @PostConstruct
        public void init() throws ServiceException {
            Provisioning.getInstance().getLocalServer().addMailboxListenerUrl("redis://localhost:6379");
        }

        @Bean
        @Override
        public Pool<Jedis> jedisPool() throws ServiceException {
            return new JedisPool("localhost", 6379);
        }
    }
}
