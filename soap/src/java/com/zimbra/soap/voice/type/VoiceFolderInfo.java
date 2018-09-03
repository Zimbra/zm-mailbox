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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("phoneNumber", phoneNumber)
            .add("hasVoiceMail", hasVoiceMail)
            .add("virtualRootFolder", virtualRootFolder);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
