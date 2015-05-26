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

import com.zimbra.cs.account.ZmgDevice;

public interface PushNotification {

    String APNS_ALERT = "alert";
    String APNS_SOUND = "sound";
    String APNS_BADGE = "badge";
    String APNS_APS = "aps";

    String GCM_REGISTRATION_IDS = "registration_ids";
    String GCM_AUTHORIZATION = "Authorization";
    String GCM_COLLAPSE_KEY = "collapse_key";
    String GCM_COLLAPSE_KEY_VALUE = "zmg_content_available";
    String GCM_DATA = "data";

    String CONTENT_AVAILABLE = "content-available";

    String CID = "cid";
    String SUBJECT = "su";
    String SENDER_ADDRESS = "sa";
    String SENDER_DISPLAY_NAME = "sdn";
    String FRAGMENT = "fr";
    String RECIPIENT_ADDRESS = "ra";
    String UNREAD_COUNT = "uc";

    String ID = "id";
    String TYPE = "ty";
    String ACTION = "ac";

    // Actions that can be performed on a data source
    String CREATE_DATASOURCE = "CreateDataSource";
    String DELETE_DATASOURCE = "DeleteDataSource";

    // push notification providers
    String PROVIDER_IDENTIFIER_GCM = "gcm";
    String PROVIDER_IDENTIFIER_APNS = "apns";

    int MAX_PUSH_NOTIFICATIONS = 10;
    long OLD_MESSAGE_TIME = 24 * 60 * 60 * 1000;

    public String getPayload();

    public ZmgDevice getDevice();

    public void setDevice(ZmgDevice device);
}
