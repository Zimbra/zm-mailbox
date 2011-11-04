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
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_GET_RIGHT_REQUEST)
public class GetRightRequest {

    @XmlAttribute(name=AdminConstants.A_EXPAND_ALL_ATTRS, required=false)
    private final ZmBoolean expandAllAttrs;
    @XmlElement(name=AdminConstants.E_RIGHT, required=true)
    private final String right;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private GetRightRequest() {
        this((String) null, (Boolean) null);
    }

    public GetRightRequest(String right, Boolean expandAllAttrs) {
        this.right = right;
        this.expandAllAttrs = ZmBoolean.fromBool(expandAllAttrs);
    }

    public String getRight() { return right; }
    public Boolean getExpandAllAttrs() { return ZmBoolean.toBool(expandAllAttrs); }
}
