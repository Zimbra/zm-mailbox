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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.ZmgDevice;
import com.zimbra.cs.account.Entry.EntryType;
import com.zimbra.cs.mailbox.MailItem;

public class SyncDataPushNotification extends AbstractPushNotification {

    private String itemId;
    private String itemType;
    private String itemAction;

    public SyncDataPushNotification(Account account, MailItem mailItem, String itemAction, ZmgDevice device) {
        this.accountName = account.getName();
        this.itemId = String.valueOf(mailItem.getId());
        this.itemType = mailItem.getType().name();
        this.itemAction = itemAction;
        this.device = device;
    }

    public SyncDataPushNotification(Account account, DataSource dataSource, String itemAction, ZmgDevice device) {
        this.itemId = dataSource.getId();
        this.dataSourceName = dataSource.getName();
        this.accountName = account.getName();
        this.itemType = dataSource.getEntryType().getName();
        this.itemAction = itemAction;
        this.device = device;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.pushnotifications.AbstractPushNotification#getPayloadForApns()
     */
    @Override
    protected String getPayloadForApns() {
        JSONObject aps = new JSONObject();
        JSONObject payload = new JSONObject();
        try {
            aps.put(CONTENT_AVAILABLE, 1);

            payload.put(APNS_APS, aps);
            payload.put(ID, itemId);
            payload.put(TYPE, itemType);
            payload.put(ACTION, itemAction);
        } catch (JSONException e) {
            ZimbraLog.mailbox.warn(
                "ZMG: Exception in creating APNS payload for sync data notification", e);
            return "";
        }
        return payload.toString();
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.pushnotifications.AbstractPushNotification#getPayloadForGcm()
     */
    @Override
    protected String getPayloadForGcm() {
        JSONObject gcmData = new JSONObject();
        JSONObject payload = new JSONObject();
        try {
            gcmData.put(ID, itemId);
            gcmData.put(TYPE, itemType);
            gcmData.put(ACTION, itemAction);

            Collection<String> registrationIds = new ArrayList<String>();
            registrationIds.add(device.getRegistrationId());
            payload.put(GCM_REGISTRATION_IDS, registrationIds);
            payload.put(GCM_DATA, gcmData);
        } catch (JSONException e) {
            ZimbraLog.mailbox.warn(
                "ZMG: Exception in creating GCM payload for sync data notification", e);
            return "";
        }
        return payload.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see com.zimbra.cs.pushnotifications.PushNotification#getItemId()
     */
    @Override
    public int getItemId() {
        try {
            if (EntryType.DATASOURCE.getName().equals(itemType)) {
                return -1;
            }
            return Integer.parseInt(itemId);
        } catch (NumberFormatException e) {
            ZimbraLog.mailbox.warn("ZMG: Number Format exception while parsing item id", e);
            return -1;
        }
    }

}
