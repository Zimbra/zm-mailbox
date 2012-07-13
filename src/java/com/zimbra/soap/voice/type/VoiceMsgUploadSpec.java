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

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("voiceMailId", voiceMailId)
            .add("phoneNumber", phoneNumber);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
