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

import com.zimbra.cs.util.Zimbra;




public class MailboxLockFactory {

    public MailboxLock create(String accountId, Mailbox mbox) {
        MailboxLock mailboxLock = null;

// TODO Enable this code before completing bug 85257 -- missing RedisMailboxLock adapter blocked on RedissonLockTest not passing yet
//        try {
//            if (Zimbra.getAppContext().getBean(ZimbraConfig.class).isRedisAvailable()) {
//                mailboxLock = new RedisMailboxLock(mbox);
//            }
//        } catch (ServiceException e) {
//            ZimbraLog.mailbox.error("Failed determining whether Redis is available; falling back on local mailbox locks", e);
//        }

        if (mailboxLock == null) {
            mailboxLock = new LocalMailboxLock(accountId, mbox);
        }
        Zimbra.getAppContext().getAutowireCapableBeanFactory().autowireBean(mailboxLock);
        Zimbra.getAppContext().getAutowireCapableBeanFactory().initializeBean(mailboxLock, "mailboxLock");
        return mailboxLock;
    }
}
