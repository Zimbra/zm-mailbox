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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.ZimletConstants;
import com.zimbra.soap.base.ZimletInclude;

/**
 * Implemented as an object rather than using String with @XmlElement because when constructing a JAXB
 * object containing this and other "Strings" there needs to be a way of differentiating them when
 * marshaling to XML.
 *
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=ZimletConstants.ZIMLET_TAG_SCRIPT)
public class AccountZimletInclude
implements ZimletInclude {

    @XmlValue
    private String value;

    @SuppressWarnings("unused")
    private AccountZimletInclude() { }

    public AccountZimletInclude(String value) { setValue(value); }

    @Override
    public void setValue(String value) { this.value = value; }
    @Override
    public String getValue() { return value; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("value", value);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
