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

package com.zimbra.soap.admin.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlValue;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
public class RightModifierInfo {

    @XmlAttribute(name=AdminConstants.A_DENY, required=false)
    private final ZmBoolean deny;

    @XmlAttribute(name=AdminConstants.A_CAN_DELEGATE, required=false)
    private final ZmBoolean canDelegate;

    @XmlAttribute(name=AdminConstants.A_SUB_DOMAIN, required=false)
    private final ZmBoolean subDomain;

    @XmlValue
    private final String value;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RightModifierInfo() {
        this((Boolean) null, (Boolean) null, (Boolean) null, (String) null);
    }

    public RightModifierInfo(Boolean deny, Boolean canDelegate,
                Boolean subDomain, String value) {
        this.deny = ZmBoolean.fromBool(deny);
        this.canDelegate = ZmBoolean.fromBool(canDelegate);
        this.subDomain = ZmBoolean.fromBool(subDomain);
        this.value = value;
    }

    public Boolean getDeny() { return ZmBoolean.toBool(deny); }
    public Boolean getCanDelegate() { return ZmBoolean.toBool(canDelegate); }
    public Boolean getSubDomain() { return ZmBoolean.toBool(subDomain); }
    public String getValue() { return value; }
}
