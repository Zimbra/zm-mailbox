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
public class CallerListEntry {

    /**
     * @zm-api-field-tag phone-number
     * @zm-api-field-description Caller number from which the call should be forwarded to the {forward-to} number
     */
    @XmlAttribute(name=VoiceConstants.A_PHONE_NUMBER /* pn */, required=true)
    private String phoneNumber;

    /**
     * @zm-api-field-tag phone-active
     * @zm-api-field-description Flag whether <b>{phone-number}</b> is active in the list - "true" or "false"
     */
    @XmlAttribute(name=VoiceConstants.A_ACTIVE /* a */, required=true)
    private TrueOrFalse active;

    public CallerListEntry() {
    }

    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public void setActive(TrueOrFalse active) { this.active = active; }
    public String getPhoneNumber() { return phoneNumber; }
    public TrueOrFalse getActive() { return active; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("phoneNumber", phoneNumber)
            .add("active", active);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
