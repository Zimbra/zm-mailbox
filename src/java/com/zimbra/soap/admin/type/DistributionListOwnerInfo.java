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

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.base.DistributionListGranteeInfoInterface;
import com.zimbra.soap.type.DistributionListGranteeType;

@XmlAccessorType(XmlAccessType.FIELD)
public class DistributionListOwnerInfo implements DistributionListGranteeInfoInterface {

    @XmlAttribute(name=AdminConstants.A_TYPE, required=true)
    private final DistributionListGranteeType type;
    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private final String id;
    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DistributionListOwnerInfo() {
        this(null, null, null);
    }

    public DistributionListOwnerInfo(DistributionListGranteeType type, String id, String name) {
        this.type = type;
        this.id = id;
        this.name = name;
    }

    public DistributionListGranteeType getType() { return type; }
    public String getId() { return id; }
    public String getName() { return name; }
}

