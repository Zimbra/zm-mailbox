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

import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.VoiceConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class VoiceMailCallParty {

    /**
     * @zm-api-field-tag address-type
     * @zm-api-field-description Type of the address in <b>{personal-name}</b> and <b>{phone-number}</b>
     * <br />
     * The supported values are '<b>f</b>'(rom) or '<b>t</b>'(o) (for voice mails it should always be '<b>f</b>'
     * because we only return caller info)
     */
    @XmlAttribute(name=MailConstants.A_ADDRESS_TYPE /* t */, required=true)
    private String addressType;

    /**
     * @zm-api-field-tag personal-name
     * @zm-api-field-description Personal name
     */
    @XmlAttribute(name=MailConstants.A_PERSONAL /* p */, required=true)
    private String personalName;

    /**
     * @zm-api-field-tag phone-number
     * @zm-api-field-description Phone number
     */
    @XmlAttribute(name=VoiceConstants.A_PHONENUM /* n */, required=true)
    private String phoneNumber;

    public VoiceMailCallParty() {
    }

    public void setAddressType(String addressType) { this.addressType = addressType; }
    public void setPersonalName(String personalName) { this.personalName = personalName; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAddressType() { return addressType; }
    public String getPersonalName() { return personalName; }
    public String getPhoneNumber() { return phoneNumber; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("addressType", addressType)
            .add("personalName", personalName)
            .add("phoneNumber", phoneNumber);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
