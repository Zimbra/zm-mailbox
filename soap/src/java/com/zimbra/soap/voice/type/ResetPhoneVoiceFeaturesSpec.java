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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("callFeatures", callFeatures);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
