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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("phoneNumber", phoneNumber)
            .add("active", active);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
