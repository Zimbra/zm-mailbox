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

import java.util.Collection;

import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ThreadPool;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Provisioning;

public class DefaultNotificationsQueue implements NotificationsQueue {

    private final static NotificationsQueue INSTANCE = new DefaultNotificationsQueue();
    private int poolSize;
    private final ThreadPool pool;

    private DefaultNotificationsQueue() {
        try {
            poolSize = Provisioning.getInstance().getLocalServer()
                .getMobileGatewayPushNotificationsQueueThreadPoolSize();
        } catch (ServiceException e) {
            ZimbraLog.mailbox.warn("Exception in getting thread pool size from server config", e);
            poolSize = 10;
        }
        pool = new ThreadPool("PushNotificationsQueue", poolSize);
    }

    public static NotificationsQueue getInstance() {
        return INSTANCE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.zimbra.cs.pushnotifications.NotificationsQueue#put(com.zimbra.cs.
     * pushnotifications.PushNotification)
     */
    @Override
    public void put(PushNotification notification) {
        pool.execute(new NotificationProcessor(notification));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.zimbra.cs.pushnotifications.NotificationsQueue#take()
     */
    @Override
    public PushNotification take() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.zimbra.cs.pushnotifications.NotificationsQueue#putAll(java.util.
     * Collection)
     */
    public void putAll(Collection<PushNotification> notifications) {
        for (PushNotification pushNotification : notifications) {
            put(pushNotification);
        }
    }
}
