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

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.ZmgDevice;
import com.zimbra.cs.mailbox.Mailbox;
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

    public Collection<PushNotification> build(Account account, Mailbox mbox, String sender,
        String recipientEmail, Message message) {
        Collection<PushNotification> notifications = new ArrayList<PushNotification>();
        Collection<ZmgDevice> devices = getDevices(mbox);
        for (ZmgDevice device : devices) {
            PushNotification notification = createNotification(message, sender, recipientEmail,
                device);
            notifications.add(notification);
        }
        return notifications;
    }

    public void buildAndPush(Account account, Mailbox mbox, String sender, String recipientEmail,
        Message message) {
        queue.putAll(build(account, mbox, sender, recipientEmail, message));
    }

    public void push(Collection<PushNotification> notifications) {
        queue.putAll(notifications);
    }

    private PushNotification createNotification(Message message, String sender,
        String recipientEmail, ZmgDevice device) {
        String msgId = Integer.toString(message.getId());
        String convId = Integer.toString(message.getConversationId());
        String fragment = message.getFragment();
        return new NewMessagePushNotification(convId, msgId, message.getSubject(), sender,
            recipientEmail, device, fragment);
    }

    private Collection<ZmgDevice> getDevices(Mailbox mbox) {
        return ZmgDevice.getDevices(mbox);
    }
}
