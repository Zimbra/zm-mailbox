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

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.EmailInfoInterface;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class EmailInfo
implements EmailInfoInterface {

    @XmlAttribute(name=MailConstants.A_ADDRESS /* a */, required=true)
    private final String address;

    @XmlAttribute(name=MailConstants.A_DISPLAY /* d */, required=true)
    private final String display;

    @XmlAttribute(name=MailConstants.A_PERSONAL /* p */, required=true)
    private final String personal;

    @XmlAttribute(name=MailConstants.A_ADDRESS_TYPE /* t */, required=true)
    private final String addressType;

    @XmlAttribute(name=MailConstants.A_IS_GROUP /* isGroup */, required=false)
    private ZmBoolean group;

    @XmlAttribute(name=MailConstants.A_EXP /* exp */, required=false)
    private ZmBoolean canExpandGroupMembers;

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

    @Override
    public EmailInfoInterface create(String address, String display,
            String personal, String addressType) {
        return new EmailInfo (address, display, personal, addressType);
    }

    @Override
    public void setGroup(Boolean group) { this.group = ZmBoolean.fromBool(group); }
    @Override
    public void setCanExpandGroupMembers(Boolean canExpandGroupMembers) {
        this.canExpandGroupMembers = ZmBoolean.fromBool(canExpandGroupMembers);
    }

    @Override
    public String getAddress() { return address; }
    @Override
    public String getDisplay() { return display; }
    @Override
    public String getPersonal() { return personal; }
    @Override
    public String getAddressType() { return addressType; }
    @Override
    public Boolean getGroup() { return ZmBoolean.toBool(group); }
    @Override
    public Boolean getCanExpandGroupMembers() { return ZmBoolean.toBool(canExpandGroupMembers); }

    public static Iterable <EmailInfo> fromInterfaces(Iterable <EmailInfoInterface> ifs) {
        if (ifs == null)
            return null;
        List <EmailInfo> newList = Lists.newArrayList();
        for (EmailInfoInterface listEnt : ifs) {
            newList.add((EmailInfo) listEnt);
        }
        return newList;
    }

    public static List <EmailInfoInterface> toInterfaces(Iterable <EmailInfo> params) {
        if (params == null)
            return null;
        List <EmailInfoInterface> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return newList;
    }

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
