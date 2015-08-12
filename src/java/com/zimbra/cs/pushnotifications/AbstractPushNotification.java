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

public abstract class AbstractPushNotification implements PushNotification {

    protected ZmgDevice device;
    protected String dataSourceName;
    protected String accountName;

    protected abstract String getPayloadForGcm();

    protected abstract String getPayloadForApns();

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

    /*
     * (non-Javadoc)
     *
     * @see com.zimbra.cs.pushnotifications.PushNotification#getItemId()
     */
    @Override
    public int getItemId() {
        return -1;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.zimbra.cs.pushnotifications.PushNotification#getAccountName()
     */
    @Override
    public String getAccountName() {
        if (accountName == null) {
            return "";
        }
        return accountName;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.zimbra.cs.pushnotifications.PushNotification#getDataSourceName()
     */
    @Override
    public String getDataSourceName() {
        if (dataSourceName == null) {
            return "";
        }
        return dataSourceName;
    }

}
