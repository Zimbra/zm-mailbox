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
public class ResetPhoneVoiceFeaturesSpec {

    // Similar structure to PhoneVoiceFeaturesSpec but can't specify VoiceMailPrefs

    /**
     * @zm-api-field-description Phone
     */
    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-description Specification of features to reset
     */
    @XmlElements({
        @XmlElement(name=VoiceConstants.E_ANON_CALL_REJECTION /* anoncallrejection */, type=PhoneVoiceFeaturesSpec.AnonCallRejectionReq.class),
        @XmlElement(name=VoiceConstants.E_CALLER_ID_BLOCKING /* calleridblocking */, type=PhoneVoiceFeaturesSpec.CallerIdBlockingReq.class),
        @XmlElement(name=VoiceConstants.E_CALL_FORWARD /* callforward */, type=PhoneVoiceFeaturesSpec.CallForwardReq.class),
        @XmlElement(name=VoiceConstants.E_CALL_FORWARD_BUSY_LINE /* callforwardbusyline */, type=PhoneVoiceFeaturesSpec.CallForwardBusyLineReq.class),
        @XmlElement(name=VoiceConstants.E_CALL_FORWARD_NO_ANSWER /* callforwardnoanswer */, type=PhoneVoiceFeaturesSpec.CallForwardNoAnswerReq.class),
        @XmlElement(name=VoiceConstants.E_CALL_WAITING /* callwaiting */, type=PhoneVoiceFeaturesSpec.CallWaitingReq.class),
        @XmlElement(name=VoiceConstants.E_SELECTIVE_CALL_FORWARD /* selectivecallforward */, type=PhoneVoiceFeaturesSpec.SelectiveCallForwardReq.class),
        @XmlElement(name=VoiceConstants.E_SELECTIVE_CALL_ACCEPTANCE /* selectivecallacceptance */, type=PhoneVoiceFeaturesSpec.SelectiveCallAcceptanceReq.class),
        @XmlElement(name=VoiceConstants.E_SELECTIVE_CALL_REJECTION /* selectivecallrejection */, type=PhoneVoiceFeaturesSpec.SelectiveCallRejectionReq.class)
    })
    private List<PhoneVoiceFeaturesSpec.CallFeatureReq> callFeatures = Lists.newArrayList();

    public ResetPhoneVoiceFeaturesSpec() {
    }

    public void setName(String name) { this.name = name; }
    public void setCallFeatures(Iterable <PhoneVoiceFeaturesSpec.CallFeatureReq> callFeatures) {
        this.callFeatures.clear();
        if (callFeatures != null) {
            Iterables.addAll(this.callFeatures, callFeatures);
        }
    }

    public void addCallFeature(PhoneVoiceFeaturesSpec.CallFeatureReq callFeature) {
        this.callFeatures.add(callFeature);
    }

    public String getName() { return name; }
    public List<PhoneVoiceFeaturesSpec.CallFeatureReq> getCallFeatures() {
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
