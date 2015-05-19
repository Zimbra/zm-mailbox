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

import org.json.JSONException;
import org.json.JSONObject;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.ZmgDevice;

public class ContentAvailablePushNotification implements PushNotification {

    private ZmgDevice device;

    public ContentAvailablePushNotification(ZmgDevice device) {
        this.device = device;
    }

    private String getPayloadForApns() {
        JSONObject aps = new JSONObject();
        JSONObject payload = new JSONObject();
        try {
            aps.put(CONTENT_AVAILABLE, 1);

            payload.put(APNS_APS, aps);
            payload.put(ACTION, CONTENT_AVAILABLE);
        } catch (JSONException e) {
            ZimbraLog.mailbox.warn(
                "ZMG: Exception in creating APNS payload for content available notification", e);
            return "";
        }
        return payload.toString();
    }

    private String getPayloadForGcm() {
        JSONObject gcmData = new JSONObject();
        JSONObject payload = new JSONObject();
        try {
            gcmData.put(ACTION, CONTENT_AVAILABLE);

            Collection<String> registrationIds = new ArrayList<String>();
            registrationIds.add(device.getRegistrationId());
            payload.put(GCM_COLLAPSE_KEY, GCM_COLLAPSE_KEY_VALUE);
            payload.put(GCM_REGISTRATION_IDS, registrationIds);
            payload.put(GCM_DATA, gcmData);
        } catch (JSONException e) {
            ZimbraLog.mailbox.warn(
                "ZMG: Exception in creating GCM payload for content available notification", e);
            return "";
        }
        return payload.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.zimbra.cs.pushnotifications.PushNotification#getPayload()
     */
    @Override
    public String getPayload() {
        switch (device.getPushProvider()) {
        case PROVIDER_IDENTIFIER_GCM:
            return getPayloadForGcm();

        case PROVIDER_IDENTIFIER_APNS:
            return getPayloadForApns();

        default:
            return "";
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.zimbra.cs.pushnotifications.PushNotification#getDevice()
     */
    @Override
    public ZmgDevice getDevice() {
        return device;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.zimbra.cs.pushnotifications.PushNotification#setDevice(com.zimbra
     * .cs.account.ZmgDevice)
     */
    @Override
    public void setDevice(ZmgDevice device) {
        this.device = device;
    }

}
