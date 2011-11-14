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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.TargetBy;
import com.zimbra.soap.type.TargetType;

@XmlAccessorType(XmlAccessType.NONE)
public class EffectiveRightsTargetSelector {

    @XmlAttribute(name=AdminConstants.A_TYPE, required=true)
    private final TargetType type;

    @XmlAttribute(name=AdminConstants.A_BY, required=false)
    private final TargetBy by;

    @XmlValue
    private final String value;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private EffectiveRightsTargetSelector() {
        this((TargetType) null, (TargetBy) null,
                (String) null);
    }

    public EffectiveRightsTargetSelector(TargetType type, TargetBy by, String value) {
        this.type = type;
        this.by = by;
        this.value = value;
    }

    public TargetType getType() { return type; }
    public TargetBy getBy() { return by; }
    public String getValue() { return value; }
}
