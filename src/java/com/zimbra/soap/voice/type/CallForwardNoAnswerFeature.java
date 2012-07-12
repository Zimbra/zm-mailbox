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

import com.zimbra.common.soap.VoiceConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class CallForwardNoAnswerFeature extends CallFeatureInfo {

    /**
     * @zm-api-field-tag forward-to
     * @zm-api-field-description Telephone number to forward calls to
     */
    @XmlAttribute(name=VoiceConstants.A_FORWARD_TO /* ft */, required=false)
    private String forwardTo;

    /**
     * @zm-api-field-tag number-of-ring-cycles
     * @zm-api-field-description Integer, the number of ring cycles before forwarding calls.
     */
    @XmlAttribute(name=VoiceConstants.A_NUM_RING_CYCLES /* nr */, required=false)
    private String numRingCycles;

    public CallForwardNoAnswerFeature() {
    }

    public void setForwardTo(String forwardTo) { this.forwardTo = forwardTo; }
    public void setNumRingCycles(String numRingCycles) { this.numRingCycles = numRingCycles; }
    public String getForwardTo() { return forwardTo; }
    public String getNumRingCycles() { return numRingCycles; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("forwardTo", forwardTo)
            .add("numRingCycles", numRingCycles);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
