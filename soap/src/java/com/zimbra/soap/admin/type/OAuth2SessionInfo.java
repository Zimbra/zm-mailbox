/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc. All Rights Reserved.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.AdminConstants;

/**
 * One active OAuth2 session.
 */
@XmlAccessorType(XmlAccessType.NONE)
public class OAuth2SessionInfo {

    /**
     * @zm-api-field-tag account-id
     * @zm-api-field-description Account Id
     */
    @XmlAttribute(name = AdminConstants.A_OAUTH2_ACCOUNT_ID, required = true)
    private String accountId;


    /**
     * @zm-api-field-tag device-id
     * @zm-api-field-description Device identifier
     */
    @XmlAttribute(name = AdminConstants.A_OAUTH2_DEVICE_ID, required = false)
    private String deviceId;

    /**
     * @zm-api-field-tag device-name
     * @zm-api-field-description Device name / model
     */
    @XmlAttribute(name = AdminConstants.A_OAUTH2_DEVICE_NAME, required = false)
    private String deviceName;

    /**
     * @zm-api-field-tag created
     * @zm-api-field-description Session creation time, milliseconds since epoch (UTC)
     */
    @XmlAttribute(name = AdminConstants.A_OAUTH2_CREATED, required = false)
    private Long created;

    /**
     * @zm-api-field-tag last-used
     * @zm-api-field-description Last refresh time, milliseconds since epoch (UTC)
     */
    @XmlAttribute(name = AdminConstants.A_OAUTH2_LAST_USED, required = false)
    private Long lastUsed;

    public OAuth2SessionInfo() {
    }

    public OAuth2SessionInfo(String accountId) {
        this.accountId = accountId;
    }

    public String getAccountId() { return accountId; }
    public void setAccountId(String accountId) { this.accountId = accountId; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }


    public Long getCreated() { return created; }
    public void setCreated(Long created) { this.created = created; }

    public Long getLastUsed() { return lastUsed; }
    public void setLastUsed(Long lastUsed) { this.lastUsed = lastUsed; }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("accountName", accountId)
                .add("deviceId", deviceId).add("deviceName", deviceName)
                .add("created", created).add("lastUsed", lastUsed)
                .toString();
    }
}