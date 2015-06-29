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
import com.zimbra.soap.account.type.ZmgDeviceSpec;

public class ZmgDevice {

    private int mailboxId;
    private String deviceId;
    private String registrationId;
    private String pushProvider;
    private String osName;
    private String osVersion;
    private int maxPayloadSize;

    public ZmgDevice(int mailboxId, String deviceId, String registrationId, String pushProvider,
                     String osName, String osVersion, int maxPayloadSize) {
        this.mailboxId = mailboxId;
        this.deviceId = deviceId;
        this.registrationId = registrationId;
        this.pushProvider = pushProvider;
        this.osName = osName;
        this.osVersion = osVersion;
        this.maxPayloadSize = maxPayloadSize;
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

    /**
     * @return the osName
     */
    public String getOSName() {
        return osName;
    }

    /**
     * @param osName
     *            the osName to set
     */
    public void setOSName(String osName) {
        this.osName = osName;
    }

    /**
     * @return the osVersion
     */
    public String getOSVersion() {
        return osVersion;
    }

    public double getOSVersionAsDouble() {
        return parseOSVersion(osVersion);
    }

    /**
     * @param osVersion
     *            the osVersion to set
     */
    public void setOSVersion(String osVersion) {
        this.osVersion = osVersion;
    }

    /**
     * @return the maxPayloadSize
     */
    public int getMaxPayloadSize() {
        return maxPayloadSize;
    }

    /**
     * @param maxPayloadSize
     *            the maxPayloadSize to set
     */
    public void setMaxPayloadSize(int maxPayloadSize) {
        this.maxPayloadSize = maxPayloadSize;
    }

    public static int add(int mailboxId, ZmgDeviceSpec deviceSpec) throws ServiceException {
        ZmgDevice device = new ZmgDevice(mailboxId, deviceSpec.getAppId(),
            deviceSpec.getRegistrationId(), deviceSpec.getPushProvider(), deviceSpec.getOSName(),
            deviceSpec.getOSVersion(), deviceSpec.getMaxPayloadSize());
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

    public static double parseOSVersion(String version) {
        double osVersion;
        if (version == null) {
            return 0;
        }

        try {
            int endIndex = version.indexOf(".") + 2;
            osVersion = Double.valueOf(version.substring(0, endIndex).intern());
        } catch (Exception e) {
            ZimbraLog.mailbox.info("ZMG: Exception in parsing OS Version", e);
            return 0;
        }
        return osVersion;
    }
}
