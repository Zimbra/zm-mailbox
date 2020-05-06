/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2020 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.session;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.ZimbraNamespace;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.event.EventListener;
import com.zimbra.cs.mailbox.event.MailboxEvent;
import com.zimbra.cs.service.mail.ToXML;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.service.util.ItemIdFormatter;
import com.zimbra.cs.session.Session.ExternalEventNotification;

public class WatchNotification extends ExternalEventNotification {

    private final MailboxOperation op;
    private final Account account;
    private final String userAgent;
    private final long timestamp;
    private final ItemId itemId;
    private final MailItem item;

    public WatchNotification(MailboxOperation op, Account account, String userAgent, long timestamp, MailItem item) throws ServiceException {
        this.op = op;
        this.account = account;
        this.userAgent = userAgent;
        this.timestamp = timestamp;
        this.item = item;
        this.itemId = new ItemId(item.getMailbox().getAccountId(), item.getId());
    }

    @Override
    public boolean canAccess(Account account) {
        boolean visible = false;
        try {
            item.getMailbox().getItemById(new OperationContext(account), item.getId(), MailItem.Type.UNKNOWN);
            visible = true;
        } catch (ServiceException e) {
        }
        return visible;
    }

    @Override
    public void addElement(Element notify) {
        ToXML.addWatchActivity(notify, account, op.toString(), timestamp, itemId.toString(), userAgent, EventListener.getArgs(op, item, null, null));
        Element modified = notify.getOptionalElement(ZimbraNamespace.E_MODIFIED);
        if (modified == null) {
            modified = notify.addUniqueElement(ZimbraNamespace.E_MODIFIED);
            Element doc = modified.addElement(MailConstants.E_DOC);
            ItemIdFormatter fmt = new ItemIdFormatter(account.getId());
            doc.addAttribute(MailConstants.A_ID, fmt.formatItemId(itemId));
            ToXML.encodeDocumentWatchAttribute(doc, op == MailboxOperation.Watch);
        }
    }

    public MailboxEvent toActivity() {
        return new MailboxEvent(account.getId(), op, item.getId(), 0, item.getFolderId(), timestamp, userAgent, EventListener.getArgs(op, item, null, null));
    }
}
