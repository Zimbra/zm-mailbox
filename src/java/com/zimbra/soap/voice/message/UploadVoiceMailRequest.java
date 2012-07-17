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

package com.zimbra.soap.voice.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.soap.voice.type.StorePrincipalSpec;
import com.zimbra.soap.voice.type.VoiceMsgUploadSpec;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Retrieve the voice mail body from the gateway and upload(save) it as an attachment on
 * the server.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=VoiceConstants.E_UPLOAD_VOICE_MAIL_REQUEST)
public class UploadVoiceMailRequest {

    /**
     * @zm-api-field-description Store Principal specification
     */
    @XmlElement(name=VoiceConstants.E_STOREPRINCIPAL /* storeprincipal */, required=false)
    private StorePrincipalSpec storePrincipal;

    /**
     * @zm-api-field-description Specification of voice message to upload
     */
    @XmlElement(name=VoiceConstants.E_VOICEMSG /* vm */, required=false)
    private VoiceMsgUploadSpec voiceMsg;

    public UploadVoiceMailRequest() {
    }

    public void setStorePrincipal(StorePrincipalSpec storePrincipal) { this.storePrincipal = storePrincipal; }
    public void setVoiceMsg(VoiceMsgUploadSpec voiceMsg) { this.voiceMsg = voiceMsg; }
    public StorePrincipalSpec getStorePrincipal() { return storePrincipal; }
    public VoiceMsgUploadSpec getVoiceMsg() { return voiceMsg; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("storePrincipal", storePrincipal)
            .add("voiceMsg", voiceMsg);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
