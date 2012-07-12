/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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

package com.zimbra.soap.voice.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;

import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class VoiceFolderInfo {

    /**
     * @zm-api-field-tag phone-number
     * @zm-api-field-description Phone number
     */
    @XmlAttribute(name=VoiceConstants.A_NAME /* name */, required=true)
    private String phoneNumber;

    /**
     * @zm-api-field-tag phone-has-voice-mail-service
     * @zm-api-field-description Set if phone has voice mail service
     */
    @XmlAttribute(name=VoiceConstants.A_VM /* vm */, required=true)
    private ZmBoolean hasVoiceMail;

    /**
     * @zm-api-field-description Virtual root folder for the phone
     */
    @XmlElement(name=MailConstants.E_FOLDER /* folder */, required=true)
    private RootVoiceFolder virtualRootFolder;

    public VoiceFolderInfo() {
    }

    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setHasVoiceMail(Boolean hasVoiceMail) { this.hasVoiceMail = ZmBoolean.fromBool(hasVoiceMail); }
    public void setVirtualRootFolder(RootVoiceFolder virtualRootFolder) { this.virtualRootFolder = virtualRootFolder; }
    public String getPhoneNumber() { return phoneNumber; }
    public Boolean getHasVoiceMail() { return ZmBoolean.toBool(hasVoiceMail); }
    public RootVoiceFolder getVirtualRootFolder() { return virtualRootFolder; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("phoneNumber", phoneNumber)
            .add("hasVoiceMail", hasVoiceMail)
            .add("virtualRootFolder", virtualRootFolder);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
