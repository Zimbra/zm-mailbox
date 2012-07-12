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

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("subscribed", subscribed)
            .add("active", active);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
