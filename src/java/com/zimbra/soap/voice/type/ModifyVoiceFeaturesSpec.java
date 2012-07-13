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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.VoiceConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ModifyVoiceFeaturesSpec {

    /**
     * @zm-api-field-tag phone
     * @zm-api-field-description Phone
     */
    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-description
     */
    @XmlElements({
        @XmlElement(name=VoiceConstants.E_VOICE_MAIL_PREFS /* voicemailprefs */, type=VoiceMailPrefsFeature.class),
        @XmlElement(name=VoiceConstants.E_ANON_CALL_REJECTION /* anoncallrejection */, type=AnonCallRejectionFeature.class),
        @XmlElement(name=VoiceConstants.E_CALLER_ID_BLOCKING /* calleridblocking */, type=CallerIdBlockingFeature.class),
        @XmlElement(name=VoiceConstants.E_CALL_FORWARD /* callforward */, type=CallForwardFeature.class),
        @XmlElement(name=VoiceConstants.E_CALL_FORWARD_BUSY_LINE /* callforwardbusyline */, type=CallForwardBusyLineFeature.class),
        @XmlElement(name=VoiceConstants.E_CALL_FORWARD_NO_ANSWER /* callforwardnoanswer */, type=CallForwardNoAnswerFeature.class),
        @XmlElement(name=VoiceConstants.E_CALL_WAITING /* callwaiting */, type=CallWaitingFeature.class),
        @XmlElement(name=VoiceConstants.E_SELECTIVE_CALL_FORWARD /* selectivecallforward */, type=SelectiveCallForwardFeature.class),
        @XmlElement(name=VoiceConstants.E_SELECTIVE_CALL_ACCEPTANCE /* selectivecallacceptance */, type=SelectiveCallAcceptanceFeature.class),
        @XmlElement(name=VoiceConstants.E_SELECTIVE_CALL_REJECTION /* selectivecallrejection */, type=SelectiveCallRejectionFeature.class)
    })
    private List<CallFeatureInfo> callFeatures = Lists.newArrayList();

    public ModifyVoiceFeaturesSpec() {
    }

    public void setName(String name) { this.name = name; }
    public void setCallFeatures(Iterable <CallFeatureInfo> callFeatures) {
        this.callFeatures.clear();
        if (callFeatures != null) {
            Iterables.addAll(this.callFeatures, callFeatures);
        }
    }

    public void addCallFeature(CallFeatureInfo callFeature) {
        this.callFeatures.add(callFeature);
    }

    public String getName() { return name; }
    public List<CallFeatureInfo> getCallFeatures() {
        return callFeatures;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("callFeatures", callFeatures);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
