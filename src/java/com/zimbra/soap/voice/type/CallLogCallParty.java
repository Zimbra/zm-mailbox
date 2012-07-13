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
public class CallLogCallParty extends VoiceMailCallParty {

    /**
     * @zm-api-field-tag city
     * @zm-api-field-description City
     */
    @XmlAttribute(name=VoiceConstants.A_CITY /* ci */, required=true)
    private String city;

    /**
     * @zm-api-field-tag state
     * @zm-api-field-description State
     */
    @XmlAttribute(name=VoiceConstants.A_STATE /* st */, required=true)
    private String state;

    /**
     * @zm-api-field-tag country
     * @zm-api-field-description Country
     */
    @XmlAttribute(name=VoiceConstants.A_COUNTRY /* co */, required=true)
    private String country;

    public CallLogCallParty() {
    }

    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setCountry(String country) { this.country = country; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getCountry() { return country; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("city", city)
            .add("state", state)
            .add("country", country);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
