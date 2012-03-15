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

package com.zimbra.soap.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AccountConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class OpValue {

    /**
     * @zm-api-field-description Operation to apply to an address
     * <br />
     * <ul>
     * <li> <b>+</b> : add, ignored if the value already exists
     * <li> <b>-</b> : remove, ignored if the value does not exist
     * </ul>
     * if not present, replace the entire list with provided values.
     */
    @XmlAttribute(name=AccountConstants.A_OP, required=false)
    private final String op;

    /**
     * @zm-api-field-description Email address
     */
    @XmlValue
    private final String value;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private OpValue() {
        this((String) null, (String) null);
    }

    public OpValue(String op, String value) {
        this.op = op;
        this.value = value;
    }

    public String getOp() { return op; }
    public String getValue() { return value; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("op", op)
            .add("value", value)
            .toString();
    }
}
