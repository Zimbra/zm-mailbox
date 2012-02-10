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
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.admin.type.AdminAttrsImpl;

@XmlAccessorType(XmlAccessType.NONE)
public class XMPPComponentInfo extends AdminAttrsImpl {

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag id
     * @zm-api-field-description ID
     */
    @XmlAttribute(name=AccountConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-description x-domainName
     */
    @XmlAttribute(name="x-domainName", required=false)
    private String domainName;

    /**
     * @zm-api-field-description x-serverName
     */
    @XmlAttribute(name="x-serverName", required=false)
    private String serverName;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private XMPPComponentInfo() {
        this((String) null, (String) null);
    }

    public XMPPComponentInfo(String name, String id) {
        this.name = name;
        this.id = id;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public String getName() { return name; }
    public String getId() { return id; }
    public String getDomainName() { return domainName; }
    public String getServerName() { return serverName; }
}
