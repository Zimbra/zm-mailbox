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

/**
 * Information about a DL that another particular DL is a member of
 */
@XmlAccessorType(XmlAccessType.FIELD)
public class DistributionListMembershipInfo {

    @XmlAttribute(name=AdminConstants.A_ID, required=true)
    private final String id;
    @XmlAttribute(name=AdminConstants.A_NAME, required=true)
    private final String name;
    @XmlAttribute(name=AdminConstants.A_VIA, required=false)
    private final String via;
    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private DistributionListMembershipInfo() {
        this(null, null, null);
    }

    public DistributionListMembershipInfo(String id, String name) {
        this(id, name, null);
    }

    public DistributionListMembershipInfo(String id, String name, String via) {
        this.id = id;
        this.name = name;
        this.via = via;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getVia() { return via; }
}
