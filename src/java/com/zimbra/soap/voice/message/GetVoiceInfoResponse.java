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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.VoiceConstants;
import com.zimbra.soap.voice.type.StorePrincipalSpec;
import com.zimbra.soap.voice.type.VoiceInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=VoiceConstants.E_GET_VOICE_INFO_RESPONSE)
public class GetVoiceInfoResponse {

    /**
     * @zm-api-field-tag audio-content-type
     * @zm-api-field-description default Content-Type of voice mail audio, e.g. audio/wav, audio/mpeg
     */
    @XmlElement(name=VoiceConstants.E_AUDIO_TYPE /* audioType */, required=true)
    private String audioContentType;

    /**
     * @zm-api-field-description <b>storeprincipal</b> contains principal information of the user in the backing store.
     * For some stores (e.g. the Velodrome store), this principal information is required for subsequent requests.
     * <b>storeprincipal</b> will always be present in the <b>GetVoiceInfoResponse</b>, but the <b>id</b> and
     * <b>name</b> attributes may not always be available.
     * Client should pass all the available store principal information to subsequent voice SOAP requests.
     */
    @XmlElement(name=VoiceConstants.E_STOREPRINCIPAL /* storeprincipal */, required=true)
    private StorePrincipalSpec storePrincipal;

    /**
     * @zm-api-field-description Information related to phone numbers.
     * <br />Note that multiple phone numbers can be associated with the same account.
     */
    @XmlElement(name=VoiceConstants.E_PHONE /* phone */, required=false)
    private List<VoiceInfo> voiceInfoForPhones = Lists.newArrayList();

    public GetVoiceInfoResponse() {
    }

    public void setAudioContentType(String audioContentType) { this.audioContentType = audioContentType; }
    public void setStorePrincipal(StorePrincipalSpec storePrincipal) { this.storePrincipal = storePrincipal; }
    public void setVoiceInfoForPhones(Iterable <VoiceInfo> voiceInfoForPhones) {
        this.voiceInfoForPhones.clear();
        if (voiceInfoForPhones != null) {
            Iterables.addAll(this.voiceInfoForPhones, voiceInfoForPhones);
        }
    }

    public void addVoiceInfoForPhone(VoiceInfo voiceInfoForPhone) {
        this.voiceInfoForPhones.add(voiceInfoForPhone);
    }

    public String getAudioContentType() { return audioContentType; }
    public StorePrincipalSpec getStorePrincipal() { return storePrincipal; }
    public List<VoiceInfo> getVoiceInfoForPhones() {
        return voiceInfoForPhones;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("audioContentType", audioContentType)
            .add("storePrincipal", storePrincipal)
            .add("voiceInfoForPhones", voiceInfoForPhones);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
