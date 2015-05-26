/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.pushnotifications;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import javax.mail.internet.AddressException;

import com.zimbra.common.mime.shim.JavaMailInternetAddress;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.ZmgDevice;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.MailboxOperation;
import com.zimbra.cs.mailbox.Message;

public class NotificationsManager {

    private volatile static NotificationsManager INSTANCE = null;
    private NotificationsQueue queue = null;

    private NotificationsManager() {
    }

    public static NotificationsManager getInstance() {
        if (INSTANCE == null) {
            synchronized (NotificationsManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new NotificationsManager();
                    INSTANCE.init();
                }
            }
        }
        return INSTANCE;
    }

    private void init() {
        if (queue == null) {
            queue = DefaultNotificationsQueue.getInstance();
        }
    }

    public Collection<PushNotification> buildNewMessageNotification(Account account, Mailbox mbox,
        String recipient, Message message, MailboxOperation op) {
        Collection<PushNotification> notifications = new ArrayList<PushNotification>();
        Collection<ZmgDevice> devices = getDevices(mbox);
        for (ZmgDevice device : devices) {
            PushNotification notification = createNotification(mbox, message, recipient, device, op);
            notifications.add(notification);
        }
        return notifications;
    }

    public Collection<PushNotification> buildSyncDataNotification(Mailbox mbox, MailItem mailItem,
        MailboxOperation op) {
        Collection<PushNotification> notifications = new ArrayList<PushNotification>();
        Collection<ZmgDevice> devices = getDevices(mbox);
        for (ZmgDevice device : devices) {
            PushNotification notification = createNotification(mbox, mailItem, op, device);
            notifications.add(notification);
        }
        return notifications;
    }

    public Collection<PushNotification> buildSyncDataNotification(Account account,
        DataSource dataSource, String action) {
        Mailbox mbox;
        try {
            mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            Collection<PushNotification> notifications = new ArrayList<PushNotification>();
            Collection<ZmgDevice> devices = getDevices(mbox);
            for (ZmgDevice device : devices) {
                PushNotification notification = createNotification(mbox, dataSource, action, device);
                notifications.add(notification);
            }
            return notifications;
        } catch (ServiceException e) {
            return Collections.<PushNotification> emptyList();
        }

    }

    public Collection<PushNotification> buildContentAvailableNotification(Account account) {
        Mailbox mbox;
        try {
            mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            Collection<PushNotification> notifications = new ArrayList<PushNotification>();
            Collection<ZmgDevice> devices = getDevices(mbox);
            for (ZmgDevice device : devices) {
                PushNotification notification = createNotification(device);
                notifications.add(notification);
            }
            return notifications;
        } catch (ServiceException e) {
            return Collections.<PushNotification> emptyList();
        }
    }

    public void pushNewMessageNotification(Account account, Mailbox mbox, String recipient,
        Message message, MailboxOperation op) {
        queue.putAll(buildNewMessageNotification(account, mbox, recipient, message, op));
    }

    public void pushSyncDataNotification(Mailbox mbox, MailItem mailItem, MailboxOperation op) {
        queue.putAll(buildSyncDataNotification(mbox, mailItem, op));
    }

    public void pushSyncDataNotification(Account account, DataSource dataSource, String action) {
        queue.putAll(buildSyncDataNotification(account, dataSource, action));
    }

    public void pushContentAvailableNotification(Account account) {
        queue.putAll(buildContentAvailableNotification(account));
    }

    public void push(Collection<PushNotification> notifications) {
        queue.putAll(notifications);
    }

    private PushNotification createNotification(Mailbox mbox, Message message, String recipient,
        ZmgDevice device, MailboxOperation op) {
        String fragment = message.getFragment();
        int unreadCount = 0;
        JavaMailInternetAddress sender = null;
        try {
            unreadCount = mbox.getFolderById(null, message.getFolderId()).getUnreadCount();
            sender = new JavaMailInternetAddress(message.getSender());
        } catch (ServiceException e) {
            ZimbraLog.mailbox.debug("ZMG: Exception in getting unread message count", e);
        } catch (AddressException e) {
            ZimbraLog.mailbox.debug("ZMG: Exception in getting sender email address", e);
        }
        String senderEmailAddress = "";
        String senderDisplayName = "";
        if (sender != null) {
            senderEmailAddress = (sender.getAddress() != null) ? sender.getAddress() : "";
            senderDisplayName = (sender.getPersonal() != null) ? sender.getPersonal() : "";
        }
        return new NewMessagePushNotification(message.getConversationId(), message.getId(),
            message.getSubject(), senderEmailAddress, senderDisplayName, recipient, device,
            fragment, unreadCount, message.getType().name(), op.name());
    }

    private PushNotification createNotification(Mailbox mbox, MailItem mailItem,
        MailboxOperation op, ZmgDevice device) {
        return new SyncDataPushNotification(mailItem, op.name(), device);
    }

    private PushNotification createNotification(Mailbox mbox, DataSource dataSource, String action,
        ZmgDevice device) {
        return new SyncDataPushNotification(dataSource, action, device);
    }

    private PushNotification createNotification(ZmgDevice device) {
        return new ContentAvailablePushNotification(device);
    }

    private Collection<ZmgDevice> getDevices(Mailbox mbox) {
        return ZmgDevice.getDevices(mbox);
    }
}
