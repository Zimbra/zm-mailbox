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

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.lmtpserver.LmtpCallback;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.pushnotifications.filters.FilterManager;

public class ZmgPushNotificationCallback implements LmtpCallback {

    private static final ZmgPushNotificationCallback INSTANCE = new ZmgPushNotificationCallback();

    private ZmgPushNotificationCallback() {
    }

    public static ZmgPushNotificationCallback getInstance() {
        return INSTANCE;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.zimbra.cs.lmtpserver.LmtpCallback#afterDelivery(com.zimbra.cs.account
     * .Account, com.zimbra.cs.mailbox.Mailbox, java.lang.String,
     * java.lang.String, com.zimbra.cs.mailbox.Message)
     */
    @Override
    public void afterDelivery(Account account, Mailbox mbox, String envelopeSender,
        String recipientEmail, Message newMessage) {
        try {
            if (FilterManager.executeNewMessageFilters(account, newMessage)) {
                NotificationsManager.getInstance().buildAndPush(account, mbox, envelopeSender,
                    recipientEmail, newMessage);
            }
        } catch (Exception e) {
            ZimbraLog.mailbox.warn("Exception in sending new message push notification", e);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.zimbra.cs.lmtpserver.LmtpCallback#forwardWithoutDelivery(com.zimbra
     * .cs.account.Account, com.zimbra.cs.mailbox.Mailbox, java.lang.String,
     * java.lang.String, com.zimbra.cs.mime.ParsedMessage)
     */
    @Override
    public void forwardWithoutDelivery(Account account, Mailbox mbox, String envelopeSender,
        String recipientEmail, ParsedMessage pm) {
    }

}
