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
import com.zimbra.soap.type.TrueOrFalse;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class CallFeatureInfo {

    /**
     * @zm-api-field-tag subscribed-true|false
     * @zm-api-field-description Flag whether subscribed or not - "true" or "false"
     */
    @XmlAttribute(name=VoiceConstants.A_SUBSCRIBED /* s */, required=true)
    private TrueOrFalse subscribed;

    /**
     * @zm-api-field-tag active-true|false
     * @zm-api-field-description Flag whether active or not - "true" or "false"
     */
    @XmlAttribute(name=VoiceConstants.A_ACTIVE /* a */, required=true)
    private TrueOrFalse active;

    public CallFeatureInfo() {
    }

    public void setSubscribed(TrueOrFalse subscribed) { this.subscribed = subscribed; }
    public void setActive(TrueOrFalse active) { this.active = active; }
    public TrueOrFalse getSubscribed() { return subscribed; }
    public TrueOrFalse getActive() { return active; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("subscribed", subscribed)
            .add("active", active);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
