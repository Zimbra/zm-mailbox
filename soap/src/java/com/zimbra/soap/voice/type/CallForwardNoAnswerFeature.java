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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("forwardTo", forwardTo)
            .add("numRingCycles", numRingCycles);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
