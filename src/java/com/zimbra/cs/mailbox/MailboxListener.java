/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
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

import java.util.EnumSet;
import java.util.Set;

import org.springframework.beans.BeansException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailItem.Type;
import com.zimbra.cs.session.PendingModifications;


public interface MailboxListener {
    public static final Set<Type> ALL_ITEM_TYPES = EnumSet.allOf(Type.class);

    /**
     * Listener can indicate specific item types it is interested in,
     * which will reduce the number of notification callbacks.
     *
     * @return Set of MailItem types the listener wants to be notified of
     */
    public Set<MailItem.Type> notifyForItemTypes();

    /**
     * Listeners will be notified at the end of each <code>Mailbox</code>
     * transaction.  The listener must not throw any Exception in this method.
     * The listener must refrain from making synchronous network operation
     * or other long latency operation within notify method.
     */
    public void notify(ChangeNotification notification) throws BeansException, ServiceException;


    public static class ChangeNotification {
        public Account mailboxAccount;
        public OperationContext ctxt;
        public int lastChangeId;
        public PendingModifications mods;
        public MailboxOperation op;
        public long timestamp;

        public ChangeNotification(Account account, PendingModifications mods, OperationContext ctxt, int lastChangeId,
                MailboxOperation op, long timestamp) {
            this.mailboxAccount = account;
            this.mods = mods;
            this.ctxt = ctxt;
            this.lastChangeId = lastChangeId;
            this.op = op;
            this.timestamp = timestamp;
        }
    }
}
