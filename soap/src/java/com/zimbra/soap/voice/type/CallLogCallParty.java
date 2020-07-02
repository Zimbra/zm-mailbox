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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("city", city)
            .add("state", state)
            .add("country", country);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
