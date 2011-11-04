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

package com.zimbra.soap.admin.message;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_ALL_RIGHTS_REQUEST)
public class GetAllRightsRequest {

    @XmlAttribute(name=AdminConstants.A_TARGET_TYPE, required=false)
    private final String targetType;
    @XmlAttribute(name=AdminConstants.A_EXPAND_ALL_ATTRS, required=false)
    private final ZmBoolean expandAllAttrs;
    @XmlAttribute(name=AdminConstants.A_RIGHT_CLASS, required=false)
    private final String rightClass;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetAllRightsRequest() {
        this((String) null, (Boolean) null, (String) null);
    }

    public GetAllRightsRequest(String targetType, Boolean expandAllAttrs,
            String rightClass) {
        this.targetType = targetType;
        this.expandAllAttrs = ZmBoolean.fromBool(expandAllAttrs);
        this.rightClass = rightClass;
    }

    public String getTargetType() { return targetType; }
    public Boolean isExpandAllAttrs() { return ZmBoolean.toBool(expandAllAttrs); }
    public String getRightClass() { return rightClass; }
}
