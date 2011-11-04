/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.sync.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.SyncConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class DeviceStatusInfo {

    @XmlAttribute(name=SyncConstants.A_ID /* id */, required=true)
    private final String id;

    @XmlAttribute(name=SyncConstants.A_TYPE /* type */, required=true)
    private final String type;

    @XmlAttribute(name=SyncConstants.A_UA /* ua */, required=false)
    private String userAgent;

    @XmlAttribute(name=SyncConstants.A_PROTOCOL /* protocol */, required=false)
    private String protocol;

    @XmlAttribute(name=SyncConstants.A_MODEL /* model */, required=false)
    private String model;

    @XmlAttribute(name=SyncConstants.A_IMEI /* imei */, required=false)
    private String IMEI;

    @XmlAttribute(name=SyncConstants.A_FRIENDLYNAME /* friendly_name */, required=false)
    private String friendlyName;

    @XmlAttribute(name=SyncConstants.A_OS /* os */, required=false)
    private String os;

    @XmlAttribute(name=SyncConstants.A_OSLANGUAGE /* os_language */, required=false)
    private String osLanguage;

    @XmlAttribute(name=SyncConstants.A_PHONENUMBER /* phone_number */, required=false)
    private String phoneNumber;

    // For JSON treat as Attribute
    @XmlElement(name=SyncConstants.E_PROVISIONABLE /* provisionable */, required=true)
    private ZmBoolean provisionable;

    // For JSON treat as Attribute
    @XmlElement(name=SyncConstants.E_STATUS /* status */, required=true)
    private Byte status;

    // For JSON treat as Attribute
    @XmlElement(name=SyncConstants.E_FIRST_REQ_RECEIVED /* firstReqReceived */, required=true)
    private Integer firstReqReceived;

    // For JSON treat as Attribute
    @XmlElement(name=SyncConstants.E_LAST_POLICY_UPDATE /* lastPolicyUpdate */, required=false)
    private Integer lastPolicyUpdate;

    // For JSON treat as Attribute
    @XmlElement(name=SyncConstants.E_REMOTE_WIPE_REQ_TIME /* remoteWipeReqTime */, required=false)
    private Integer remoteWipeReqTime;

    // For JSON treat as Attribute
    @XmlElement(name=SyncConstants.E_REMOTE_WIPE_ACK_TIME /* remoteWipeAckTime */, required=false)
    private Integer remoteWipeAckTime;

    // For JSON treat as Attribute
    @XmlElement(name=SyncConstants.E_RECOVERY_PASSWORD /* recoveryPassword */, required=false)
    private String recoveryPassword;

    // For JSON treat as Attribute
    @XmlElement(name=SyncConstants.E_LAST_USED_DATE /* lastUsedDate */, required=false)
    private String lastUsedDate;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DeviceStatusInfo() {
        this((String) null, (String) null);
    }

    public DeviceStatusInfo(String id, String type) {
        this.id = id;
        this.type = type;
    }

    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public void setModel(String model) { this.model = model; }
    public void setIMEI(String IMEI) { this.IMEI = IMEI; }
    public void setFriendlyName(String friendlyName) {
        this.friendlyName = friendlyName;
    }
    public void setOs(String os) { this.os = os; }
    public void setOsLanguage(String osLanguage) {
        this.osLanguage = osLanguage;
    }
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    public void setProvisionable(Boolean provisionable) {
        this.provisionable = ZmBoolean.fromBool(provisionable);
    }
    public void setStatus(Byte status) { this.status = status; }
    public void setFirstReqReceived(Integer firstReqReceived) {
        this.firstReqReceived = firstReqReceived;
    }
    public void setLastPolicyUpdate(Integer lastPolicyUpdate) {
        this.lastPolicyUpdate = lastPolicyUpdate;
    }
    public void setRemoteWipeReqTime(Integer remoteWipeReqTime) {
        this.remoteWipeReqTime = remoteWipeReqTime;
    }
    public void setRemoteWipeAckTime(Integer remoteWipeAckTime) {
        this.remoteWipeAckTime = remoteWipeAckTime;
    }
    public void setRecoveryPassword(String recoveryPassword) {
        this.recoveryPassword = recoveryPassword;
    }
    public void setLastUsedDate(String lastUsedDate) {
        this.lastUsedDate = lastUsedDate;
    }

    public String getId() { return id; }
    public String getType() { return type; }
    public String getUserAgent() { return userAgent; }
    public String getProtocol() { return protocol; }
    public String getModel() { return model; }
    public String getIMEI() { return IMEI; }
    public String getFriendlyName() { return friendlyName; }
    public String getOs() { return os; }
    public String getOsLanguage() { return osLanguage; }
    public String getPhoneNumber() { return phoneNumber; }
    public Boolean getProvisionable() { return ZmBoolean.toBool(provisionable); }
    public Byte getStatus() { return status; }
    public Integer getFirstReqReceived() { return firstReqReceived; }
    public Integer getLastPolicyUpdate() { return lastPolicyUpdate; }
    public Integer getRemoteWipeReqTime() { return remoteWipeReqTime; }
    public Integer getRemoteWipeAckTime() { return remoteWipeAckTime; }
    public String getRecoveryPassword() { return recoveryPassword; }
    public String getLastUsedDate() { return lastUsedDate; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("type", type)
            .add("userAgent", userAgent)
            .add("protocol", protocol)
            .add("model", model)
            .add("IMEI", IMEI)
            .add("friendlyName", friendlyName)
            .add("os", os)
            .add("osLanguage", osLanguage)
            .add("phoneNumber", phoneNumber)
            .add("provisionable", provisionable)
            .add("status", status)
            .add("firstReqReceived", firstReqReceived)
            .add("lastPolicyUpdate", lastPolicyUpdate)
            .add("remoteWipeReqTime", remoteWipeReqTime)
            .add("remoteWipeAckTime", remoteWipeAckTime)
            .add("recoveryPassword", recoveryPassword)
            .add("lastUsedDate", lastUsedDate);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
