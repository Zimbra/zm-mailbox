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

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.VoiceConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class VoiceMsgUploadSpec {

    /**
     * @zm-api-field-tag voicemail-id
     * @zm-api-field-description Message id of the voice mail.  It can only be a voice mail in the INBOX, not the
     * trash folder.
     */
    @XmlAttribute(name=AccountConstants.A_ID /* id */, required=true)
    private String voiceMailId;

    /**
     * @zm-api-field-tag phone-number
     * @zm-api-field-description Phone number of the voice mail
     */
    @XmlAttribute(name=VoiceConstants.A_PHONE /* phone */, required=true)
    private String phoneNumber;

    public VoiceMsgUploadSpec() {
    }

    public void setVoiceMailId(String voiceMailId) { this.voiceMailId = voiceMailId; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getVoiceMailId() { return voiceMailId; }
    public String getPhoneNumber() { return phoneNumber; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("voiceMailId", voiceMailId)
            .add("phoneNumber", phoneNumber);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
