/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014, 2015 Zimbra, Inc.
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
package com.zimbra.cs.mailbox;

import redis.clients.jedis.JedisPool;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.Zimbra;
import com.zimbra.cs.util.ZimbraConfig;




public class MailboxLockFactory {

    public MailboxLock create(String accountId, Mailbox mbox) throws ServiceException {
        MailboxLock mailboxLock = null;

        try {
            ZimbraConfig config = Zimbra.getAppContext().getBean(ZimbraConfig.class);
            if (config.isRedisAvailable()) {
                if (config.isRedisClusterAvailable()) {
                    // TODO
                } else {
                    JedisPool jedisPool = Zimbra.getAppContext().getBean(JedisPool.class);
                    mailboxLock = new RedisCoordinatedLocalMailboxLock(jedisPool, mbox);
                }
            }
        } catch (ServiceException e) {
            ZimbraLog.mailbox.error("Failed determining whether Redis is available; falling back on local mailbox locks", e);
        }

        if (mailboxLock == null) {
            mailboxLock = new LocalMailboxLock(accountId, mbox);
        }
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(mailboxLock);
        return mailboxLock;
    }
}
