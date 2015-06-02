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

package com.zimbra.cs.account;

import java.util.Collection;
import java.util.Collections;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.db.DbZmgDevices;
import com.zimbra.cs.mailbox.Mailbox;

public class ZmgDevice {

    private int mailboxId;
    private String deviceId;
    private String registrationId;
    private String pushProvider;

    public ZmgDevice(int mailboxId, String deviceId, String registrationId, String pushProvider) {
        this.mailboxId = mailboxId;
        this.deviceId = deviceId;
        this.registrationId = registrationId;
        this.pushProvider = pushProvider;
    }

    /**
     * @return the mailboxId
     */
    public int getMailboxId() {
        return mailboxId;
    }

    /**
     * @param mailboxId
     *            the mailboxId to set
     */
    public void setMailboxId(int mailboxId) {
        this.mailboxId = mailboxId;
    }

    /**
     * @return the deviceId
     */
    public String getDeviceId() {
        return deviceId;
    }

    /**
     * @param deviceId
     *            the deviceId to set
     */
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    /**
     * @return the registrationId
     */
    public String getRegistrationId() {
        return registrationId;
    }

    /**
     * @param registrationId
     *            the registrationId to set
     */
    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    /**
     * @return the pushProvider
     */
    public String getPushProvider() {
        return pushProvider;
    }

    /**
     * @param pushProvider
     *            the pushProvider to set
     */
    public void setPushProvider(String pushProvider) {
        this.pushProvider = pushProvider;
    }

    public static int add(int mailboxId, String deviceId, String registrationId, String pushProvider)
            throws ServiceException {
        ZmgDevice device = new ZmgDevice(mailboxId, deviceId, registrationId, pushProvider);
        return DbZmgDevices.addDevice(device);
    }

    public static Collection<ZmgDevice> getDevices(Mailbox mbox) {
        try {
            return DbZmgDevices.getDevices(mbox);
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("Error in getting registered ZMG devices from db", e);
            return Collections.<ZmgDevice> emptyList();
        }
    }
}
