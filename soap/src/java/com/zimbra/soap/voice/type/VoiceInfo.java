/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.voice.type;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;

import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class VoiceInfo {

    /**
     * @zm-api-field-tag phone-number
     * @zm-api-field-description Phone number
     */
    @XmlAttribute(name=VoiceConstants.A_NAME /* name */, required=true)
    private String phoneNumber;

    /**
     * @zm-api-field-tag phone-ID
     * @zm-api-field-description Phone ID
     */
    @XmlAttribute(name=VoiceConstants.A_ID /* id */, required=true)
    private String phoneID;

    /**
     * @zm-api-field-tag phone-name/label
     * @zm-api-field-description Phone name/label
     */
    @XmlAttribute(name=VoiceConstants.A_LABEL /* label */, required=true)
    private String label;

    /**
     * @zm-api-field-description Set if click to call is enabled for the phone number

     */
    @XmlAttribute(name=VoiceConstants.A_CALLABLE /* callable */, required=true)
    private ZmBoolean callable;

    /**
     * @zm-api-field-description Set if the phone number and label can be edited
     */
    @XmlAttribute(name=VoiceConstants.A_EDITABLE /* editable */, required=true)
    private ZmBoolean editable;

    /**
     * @zm-api-field-tag phone-type
     * @zm-api-field-description Phone type (Optional).
     * <br />
     * Currently used in Mitel, it denotes the type of phone. Possible values are:
     * <br /><b>DeskPhone, SoftPhone, Voicemail, MobileExt, EDHU, PRG, OTHER</b>
     */
    @XmlAttribute(name=VoiceConstants.A_TYPE /* type */, required=false)
    private String phoneType;

    /**
     * @zm-api-field-tag click-2-call-device-id
     * @zm-api-field-description click-2-call Device ID
     */
    @XmlAttribute(name=VoiceConstants.A_C2C_DEVICE_ID /* c2cDeviceId */, required=false)
    private String click2callDeviceId;

    /**
     * @zm-api-field-tag phone-has-voice-mail-service
     * @zm-api-field-description Set if phone has voice mail service
     */
    @XmlAttribute(name=VoiceConstants.A_VM /* vm */, required=true)
    private ZmBoolean hasVoiceMail;

    /**
     * @zm-api-field-tag voice-mailbox-quota-used
     * @zm-api-field-description Voice mailbox quota used in bytes (available only if the phone has voicemail service)
     */
    @XmlAttribute(name=AdminConstants.A_QUOTA_USED /* used */, required=false)
    private Long quotaUsed;

    /**
     * @zm-api-field-tag voice-mailbox-quota-limit
     * @zm-api-field-description Voice mailbox quota limit in bytes, or 0 if unlimited (available only if the phone
     * has voicemail service)
     */
    @XmlAttribute(name=AdminConstants.A_QUOTA_LIMIT /* limit */, required=false)
    private Long quotaLimit;

    /**
     * @zm-api-field-description Virtual root folder for the phone
     */
    @XmlElement(name=MailConstants.E_FOLDER /* folder */, required=true)
    private RootVoiceFolder virtualRootFolder;

    /**
     * @zm-api-field-description Subscribed call features
     */
    @XmlElementWrapper(name=VoiceConstants.E_CALL_FEATURES /* callfeatures */, required=false)
    @XmlElement(name=VoiceConstants.E_CALL_FEATURE /* callfeature */, required=false)
    private List<CallFeature> callFeatures = Lists.newArrayList();

    public VoiceInfo() {
    }

    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setPhoneID(String phoneID) { this.phoneID = phoneID; }
    public void setLabel(String label) { this.label = label; }
    public void setCallable(Boolean callable) { this.callable = ZmBoolean.fromBool(callable); }
    public void setEditable(Boolean editable) { this.editable = ZmBoolean.fromBool(editable); }
    public void setPhoneType(String phoneType) { this.phoneType = phoneType; }
    public void setClick2callDeviceId(String click2callDeviceId) { this.click2callDeviceId = click2callDeviceId; }
    public void setHasVoiceMail(Boolean hasVoiceMail) { this.hasVoiceMail = ZmBoolean.fromBool(hasVoiceMail); }
    public void setQuotaUsed(Long quotaUsed) { this.quotaUsed = quotaUsed; }
    public void setQuotaLimit(Long quotaLimit) { this.quotaLimit = quotaLimit; }
    public void setVirtualRootFolder(RootVoiceFolder virtualRootFolder) { this.virtualRootFolder = virtualRootFolder; }
    public void setCallFeatures(Iterable <CallFeature> callFeatures) {
        this.callFeatures.clear();
        if (callFeatures != null) {
            Iterables.addAll(this.callFeatures, callFeatures);
        }
    }

    public void addCallFeature(CallFeature callFeature) {
        this.callFeatures.add(callFeature);
    }

    public String getPhoneNumber() { return phoneNumber; }
    public String getPhoneID() { return phoneID; }
    public String getLabel() { return label; }
    public Boolean getCallable() { return ZmBoolean.toBool(callable); }
    public Boolean getEditable() { return ZmBoolean.toBool(editable); }
    public String getPhoneType() { return phoneType; }
    public String getClick2callDeviceId() { return click2callDeviceId; }
    public Boolean getHasVoiceMail() { return ZmBoolean.toBool(hasVoiceMail); }
    public Long getQuotaUsed() { return quotaUsed; }
    public Long getQuotaLimit() { return quotaLimit; }
    public RootVoiceFolder getVirtualRootFolder() { return virtualRootFolder; }
    public List<CallFeature> getCallFeatures() {
        return callFeatures;
    }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("phoneNumber", phoneNumber)
            .add("phoneID", phoneID)
            .add("label", label)
            .add("callable", callable)
            .add("editable", editable)
            .add("phoneType", phoneType)
            .add("click2callDeviceId", click2callDeviceId)
            .add("hasVoiceMail", hasVoiceMail)
            .add("quotaUsed", quotaUsed)
            .add("quotaLimit", quotaLimit)
            .add("virtualRootFolder", virtualRootFolder)
            .add("callFeatures", callFeatures);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
