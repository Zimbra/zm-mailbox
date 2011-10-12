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

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.ZimletConstants;
import com.zimbra.soap.base.ZimletGlobalConfigInfo;
import com.zimbra.soap.base.ZimletProperty;

@XmlAccessorType(XmlAccessType.NONE)
public class AccountZimletGlobalConfigInfo
implements ZimletGlobalConfigInfo {

    @XmlElement(name=ZimletConstants.ZIMLET_TAG_PROPERTY /* property */, required=false)
    private List<AccountZimletProperty> properties = Lists.newArrayList();

    public AccountZimletGlobalConfigInfo() {
    }

    public void setProperties(Iterable <AccountZimletProperty> properties) {
        this.properties.clear();
        if (properties != null) {
            Iterables.addAll(this.properties,properties);
        }
    }

    public void addProperty(AccountZimletProperty property) {
        this.properties.add(property);
    }

    public List<AccountZimletProperty> getProperties() {
        return Collections.unmodifiableList(properties);
    }

    @Override
    public void setZimletProperties(Iterable<ZimletProperty> properties) {
        setProperties(AccountZimletProperty.fromInterfaces(properties));
    }

    @Override
    public void addZimletProperty(ZimletProperty property) {
        addProperty((AccountZimletProperty) property);
    }

    @Override
    public List<ZimletProperty> getZimletProperties() {
        return AccountZimletProperty.toInterfaces(properties);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("properties", properties);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
