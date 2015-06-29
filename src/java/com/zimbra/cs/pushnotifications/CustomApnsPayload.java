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

import org.json.JSONException;

import javapns.notification.PushNotificationPayload;

public class CustomApnsPayload extends PushNotificationPayload {

    private int maxPayloadSize;

    public CustomApnsPayload() {
        super();
    }

    /**
     * @param rawJson
     * @throws JSONException
     */
    public CustomApnsPayload(String rawJson, int maxPayloadSize) throws JSONException {
        super(rawJson);
        this.maxPayloadSize = maxPayloadSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javapns.notification.PushNotificationPayload#getMaximumPayloadSize()
     */
    @Override
    public int getMaximumPayloadSize() {
        return maxPayloadSize;
    }

}
