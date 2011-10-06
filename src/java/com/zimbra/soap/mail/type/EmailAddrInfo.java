/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class EmailAddrInfo {

    @XmlAttribute(name=MailConstants.A_ADDRESS, required=true)
    private final String address;

    @XmlAttribute(name=MailConstants.A_ADDRESS_TYPE, required=false)
    private String addressType;

    @XmlAttribute(name=MailConstants.A_PERSONAL, required=false)
    private String personal;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private EmailAddrInfo() {
        this((String) null);
    }

    public EmailAddrInfo(String address) {
        this.address = address;
    }

    public void setAddressType(String addressType) { this.addressType = addressType; }
    public void setPersonal(String personal) { this.personal = personal; }
    public String getAddress() { return address; }
    public String getAddressType() { return addressType; }
    public String getPersonal() { return personal; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("address", address)
            .add("addressType", addressType)
            .add("personal", personal);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
