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
public class EmailInfo {

    @XmlAttribute(name=MailConstants.A_ADDRESS /* a */, required=true)
    private final String address;

    @XmlAttribute(name=MailConstants.A_DISPLAY /* d */, required=true)
    private final String display;

    @XmlAttribute(name=MailConstants.A_PERSONAL /* p */, required=true)
    private final String personal;

    @XmlAttribute(name=MailConstants.A_ADDRESS_TYPE /* t */, required=true)
    private final String addressType;

    @XmlAttribute(name=MailConstants.A_IS_GROUP /* isGroup */, required=false)
    private Boolean group;

    @XmlAttribute(name=MailConstants.A_EXP /* exp */, required=false)
    private Boolean canExpandGroupMembers;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private EmailInfo() {
        this((String) null, (String) null, (String) null, (String) null);
    }

    public EmailInfo(String address, String display, String personal,
            String addressType) {
        this.address = address;
        this.display = display;
        this.personal = personal;
        this.addressType = addressType;
    }

    public void setGroup(Boolean group) { this.group = group; }
    public void setCanExpandGroupMembers(Boolean canExpandGroupMembers) { this.canExpandGroupMembers = canExpandGroupMembers; }
    public String getAddress() { return address; }
    public String getDisplay() { return display; }
    public String getPersonal() { return personal; }
    public String getAddressType() { return addressType; }
    public Boolean getGroup() { return group; }
    public Boolean getCanExpandGroupMembers() { return canExpandGroupMembers; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("address", address)
            .add("display", display)
            .add("personal", personal)
            .add("addressType", addressType)
            .add("group", group)
            .add("canExpandGroupMembers", canExpandGroupMembers);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
