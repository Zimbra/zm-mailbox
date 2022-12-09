/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.admin.type;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.SyncConstants;
import com.zimbra.soap.type.ZmBoolean;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.NONE)
public class DeviceStatusInfo {

    /**
     * @zm-api-field-tag device-id
     * @zm-api-field-description device ID
     */
    @XmlAttribute(name=SyncConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag device-type
     * @zm-api-field-description Device type
     */
    @XmlAttribute(name=SyncConstants.A_TYPE /* type */, required=true)
    private final String type;

    /**
     * @zm-api-field-tag user-agent
     * @zm-api-field-description User agent
     */
    @XmlAttribute(name=SyncConstants.A_UA /* ua */, required=false)
    private String userAgent;

    /**
     * @zm-api-field-tag protocol-version
     * @zm-api-field-description Protocol version
     */
    @XmlAttribute(name=SyncConstants.A_PROTOCOL /* protocol */, required=false)
    private String protocol;

    /**
     * @zm-api-field-tag device-model
     * @zm-api-field-description Device model
     */
    @XmlAttribute(name=SyncConstants.A_MODEL /* model */, required=false)
    private String model;

    /**
     * @zm-api-field-tag imei
     * @zm-api-field-description IMEI (International Mobile Equipment Identity)
     */
    @XmlAttribute(name=SyncConstants.A_IMEI /* imei */, required=false)
    private String IMEI;

    /**
     * @zm-api-field-tag friendly-name
     * @zm-api-field-description Friendly name of the device
     */
    @XmlAttribute(name=SyncConstants.A_FRIENDLYNAME /* friendly_name */, required=false)
    private String friendlyName;

    /**
     * @zm-api-field-tag os
     * @zm-api-field-description Device running OS (Operating System; e.g. android, ios etc.)
     */
    @XmlAttribute(name=SyncConstants.A_OS /* os */, required=false)
    private String os;

    /**
     * @zm-api-field-tag os-language
     * @zm-api-field-description OS language
     */
    @XmlAttribute(name=SyncConstants.A_OSLANGUAGE /* os_language */, required=false)
    private String osLanguage;

    /**
     * @zm-api-field-tag phone-number
     * @zm-api-field-description Phone number
     */
    @XmlAttribute(name=SyncConstants.A_PHONENUMBER /* phone_number */, required=false)
    private String phoneNumber;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag provisionable
     * @zm-api-field-description Flag whether device is provisionable or not.
     */
    @XmlElement(name=SyncConstants.E_PROVISIONABLE /* provisionable */, required=true)
    private ZmBoolean provisionable;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag device-status
     * @zm-api-field-description Device status
     * <table>
     * <tr> <td> <b>0</b> </td> <td> need provision (same as 1 if provisionable=0)</td> </tr>
     * <tr> <td> <b>1</b> </td> <td> ok </td> </tr>
     * <tr> <td> <b>2</b> </td> <td> suspended </td> </tr>
     * <tr> <td> <b>3</b> </td> <td> remote wipe requested </td> </tr>
     * <tr> <td> <b>4</b> </td> <td> remote wipe complete </td> </tr>
     * </table>
     */
    @XmlElement(name=SyncConstants.E_STATUS /* status */, required=true)
    private Byte status;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag first-req-recv
     * @zm-api-field-description When this device first registered with the server
     */
    @XmlElement(name=SyncConstants.E_FIRST_REQ_RECEIVED /* firstReqReceived */, required=true)
    private Integer firstReqReceived;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag last-policy-update
     * @zm-api-field-description When policy was last updated on this device
     */
    @XmlElement(name=SyncConstants.E_LAST_POLICY_UPDATE /* lastPolicyUpdate */, required=false)
    private Integer lastPolicyUpdate;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag remote-wipe-req-time
     * @zm-api-field-description Time (seconds since epoch) when remote wipe was initiated
     */
    @XmlElement(name=SyncConstants.E_REMOTE_WIPE_REQ_TIME /* remoteWipeReqTime */, required=false)
    private Integer remoteWipeReqTime;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag remote-wipe-ack-time
     * @zm-api-field-description Time (seconds since epoch) when device confirmed the remote wipe
     */
    @XmlElement(name=SyncConstants.E_REMOTE_WIPE_ACK_TIME /* remoteWipeAckTime */, required=false)
    private Integer remoteWipeAckTime;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag recovery-password
     * @zm-api-field-description Recovery password
     */
    @XmlElement(name=SyncConstants.E_RECOVERY_PASSWORD /* recoveryPassword */, required=false)
    private String recoveryPassword;

    // For JSON treat as Attribute
    /**
     * @zm-api-field-tag last-used-date
     * @zm-api-field-description Date when the device was last used (the date is stored in server's time zone)
     */
    @XmlElement(name=SyncConstants.E_LAST_USED_DATE /* lastUsedDate */, required=false)
    private String lastUsedDate;

    /**
     * @zm-api-field-tag email-address
     * @zm-api-field-description email-address of the linked account
     */
    @XmlElement(name=SyncConstants.E_EMAIL_ADDRESS /* emailAddress */, required=false)
    private String emailAddress;

    /**
     * @zm-api-field-tag updateTime
     * @zm-api-field-description status update timestamp of mobile devicet
     */
    @XmlElement(name=SyncConstants.E_UPDATE_TIME /* updateTime */, required=false)
    private String timestamp;
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
    public String getUpdateTime() { return timestamp; }
    public String getEmailAddress() { return emailAddress; }
    public String getTimestamp() { return timestamp; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
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
            .add("lastUsedDate", lastUsedDate)
            .add("timestamp", timestamp);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
