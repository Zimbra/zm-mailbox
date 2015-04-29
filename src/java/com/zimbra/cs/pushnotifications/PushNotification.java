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

import java.util.Map;

import com.zimbra.cs.account.ZmgDevice;

public interface PushNotification {

    String APNS_ALERT = "alert";
    String APNS_SOUND = "sound";
    String APNS_BADGE = "badge";
    String APNS_CID = "cid";
    String APNS_MSG_ID = "msgId";
    String APNS_FRAGMENT = "fragment";
    String APNS_RECIPIENT_ADDRESS = "recipientAddress";

    String GCM_CID = "data.cid";
    String GCM_MSG_ID = "data.msgId";
    String GCM_SUBJECT = "data.subject";
    String GCM_SENDER = "data.sender";
    String GCM_FRAGMENT = "data.fragment";
    String GCM_RECIPIENT_ADDRESS = "data.recipientAddress";
    String GCM_UNREAD_COUNT = "data.unreadCount";

    String PROVIDER_IDENTIFIER_GCM = "gcm";
    String PROVIDER_IDENTIFIER_APNS = "apns";

    public Map<String, String> getPayload();

    public ZmgDevice getDevice();

    public void setDevice(ZmgDevice device);
}
