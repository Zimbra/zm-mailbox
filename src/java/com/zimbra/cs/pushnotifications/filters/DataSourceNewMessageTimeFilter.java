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

package com.zimbra.cs.pushnotifications.filters;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.pushnotifications.PushNotification;

public class DataSourceNewMessageTimeFilter implements Filter {

    private Message message;

    public DataSourceNewMessageTimeFilter(Message message) {
        this.message = message;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.zimbra.cs.pushnotifications.filters.Filter#apply()
     */
    @Override
    public boolean apply() {
        try {
            /*
             * When a data source is created and all its messages are synced,
             * the mobile client should not receive notifications for syncing
             * any old messages in the data source. Any incoming message in the
             * data source older then 24 hrs is considered as old message.
             */
            if ((System.currentTimeMillis() - message.getDate()) < PushNotification.OLD_MESSAGE_TIME) {
                return true;
            }
        } catch (Exception e) {
            ZimbraLog.mailbox.warn(
                "ZMG: Exception in processing DataSourceNewMessageTimeFilter message id=%d",
                message.getId(), e);
            return false;
        }
        return false;
    }

}
