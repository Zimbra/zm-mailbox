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

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.VoiceConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class PhoneVoiceFeaturesSpec {

    public interface CallFeatureReq {
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class AnonCallRejectionReq
    implements CallFeatureReq {
        public AnonCallRejectionReq() {}
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class CallerIdBlockingReq
    implements CallFeatureReq {
        public CallerIdBlockingReq() {}
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class CallForwardReq
    implements CallFeatureReq {
        public CallForwardReq() {}
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class CallForwardBusyLineReq
    implements CallFeatureReq {
        public CallForwardBusyLineReq() {}
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class CallForwardNoAnswerReq
    implements CallFeatureReq {
        public CallForwardNoAnswerReq() {}
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class CallWaitingReq
    implements CallFeatureReq {
        public CallWaitingReq() {}
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class SelectiveCallForwardReq
    implements CallFeatureReq {
        public SelectiveCallForwardReq() {}
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class SelectiveCallAcceptanceReq
    implements CallFeatureReq {
        public SelectiveCallAcceptanceReq() {}
    }

    @XmlAccessorType(XmlAccessType.NONE)
    public static class SelectiveCallRejectionReq
    implements CallFeatureReq {
        public SelectiveCallRejectionReq() {}
    }

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
        @XmlElement(name=VoiceConstants.E_VOICE_MAIL_PREFS /* voicemailprefs */, type=VoiceMailPrefsReq.class),
        @XmlElement(name=VoiceConstants.E_ANON_CALL_REJECTION /* anoncallrejection */, type=AnonCallRejectionReq.class),
        @XmlElement(name=VoiceConstants.E_CALLER_ID_BLOCKING /* calleridblocking */, type=CallerIdBlockingReq.class),
        @XmlElement(name=VoiceConstants.E_CALL_FORWARD /* callforward */, type=CallForwardReq.class),
        @XmlElement(name=VoiceConstants.E_CALL_FORWARD_BUSY_LINE /* callforwardbusyline */, type=CallForwardBusyLineReq.class),
        @XmlElement(name=VoiceConstants.E_CALL_FORWARD_NO_ANSWER /* callforwardnoanswer */, type=CallForwardNoAnswerReq.class),
        @XmlElement(name=VoiceConstants.E_CALL_WAITING /* callwaiting */, type=CallWaitingReq.class),
        @XmlElement(name=VoiceConstants.E_SELECTIVE_CALL_FORWARD /* selectivecallforward */, type=SelectiveCallForwardReq.class),
        @XmlElement(name=VoiceConstants.E_SELECTIVE_CALL_ACCEPTANCE /* selectivecallacceptance */, type=SelectiveCallAcceptanceReq.class),
        @XmlElement(name=VoiceConstants.E_SELECTIVE_CALL_REJECTION /* selectivecallrejection */, type=SelectiveCallRejectionReq.class)
    })
    private List<CallFeatureReq> callFeatures = Lists.newArrayList();

    public PhoneVoiceFeaturesSpec() {
    }

    public void setName(String name) { this.name = name; }
    public void setCallFeatures(Iterable <CallFeatureReq> callFeatures) {
        this.callFeatures.clear();
        if (callFeatures != null) {
            Iterables.addAll(this.callFeatures, callFeatures);
        }
    }

    public void addCallFeature(CallFeatureReq callFeature) {
        this.callFeatures.add(callFeature);
    }

    public String getName() { return name; }
    public List<CallFeatureReq> getCallFeatures() {
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
