/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AccountConstants.E_COS)
@XmlType(propOrder = {})
public class DLInfo extends ObjectInfo {

    @XmlAttribute(name=AccountConstants.A_DISPLAY, required=false)
    private final String displayName;
    @XmlAttribute(name=AccountConstants.A_DYNAMIC, required=false)
    ZmBoolean dynamic;
    @XmlAttribute(name=AccountConstants.A_VIA, required=false)
    private final String via;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DLInfo() {
        this(null, null, null, null, null);
    }

    public DLInfo(String id, String name, String displayName, Boolean dynamic, String via) {
        super(id, name, null);
        this.displayName = displayName;
        this.dynamic = ZmBoolean.fromBool(dynamic);
        this.via = via;
    }

    public String getDisplayName() {
        return displayName;
    }
    
    public String getVia() {
        return via;
    }

    public Boolean isDynamic() {
        return ZmBoolean.toBool(dynamic, false);
    }
}
