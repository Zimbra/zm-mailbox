/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class RightPermission {

    /**
     * @zm-api-field-tag has-right-on-target
     * @zm-api-field-description If set then the authed user has the right <b>{right-name}</b> on the target.
     */
    @XmlAttribute(name=MailConstants.A_ALLOW /* allow */, required=true)
    private final ZmBoolean allow;

    /**
     * @zm-api-field-tag right-name
     * @zm-api-field-description Right name
     */
    @XmlValue
    private String rightName;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RightPermission() {
        this((Boolean) null);
    }

    public RightPermission(Boolean allow) {
        this.allow = ZmBoolean.fromBool(allow);
    }

    public void setRightName(String rightName) { this.rightName = rightName; }
    public ZmBoolean getAllow() { return allow; }
    public String getRightName() { return rightName; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("allow", allow)
            .add("rightName", rightName);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
