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

package com.zimbra.soap.account.type;

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.ZimletConstants;
import com.zimbra.soap.base.ZimletProperty;

@XmlAccessorType(XmlAccessType.NONE)
public class AccountZimletProperty
implements ZimletProperty {

    /**
     * @zm-api-field-description zimlet-property-name
     * @zm-api-field-description Property name
     */
    @XmlAttribute(name=ZimletConstants.ZIMLET_ATTR_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-description zimlet-property-value
     * @zm-api-field-description Property value
     */
    @XmlValue
    private String value;

    private AccountZimletProperty() {
    }

    private AccountZimletProperty(String name, String value) {
        setName(name);
        setValue(value);
    }

    public static AccountZimletProperty createForNameAndValue(String name, String value) {
        return new AccountZimletProperty(name, value);
    }

    @Override
    public void setName(String name) { this.name = name; }
    @Override
    public void setValue(String value) { this.value = value; }
    @Override
    public String getName() { return name; }
    @Override
    public String getValue() { return value; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("value", value);
    }

    public static Iterable <AccountZimletProperty> fromInterfaces(Iterable <ZimletProperty> ifs) {
        if (ifs == null)
            return null;
        List <AccountZimletProperty> newList = Lists.newArrayList();
        for (ZimletProperty listEnt : ifs) {
            newList.add((AccountZimletProperty) listEnt);
        }
        return newList;
    }

    public static List <ZimletProperty> toInterfaces(Iterable <AccountZimletProperty> params) {
        if (params == null)
            return null;
        List <ZimletProperty> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return newList;
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
