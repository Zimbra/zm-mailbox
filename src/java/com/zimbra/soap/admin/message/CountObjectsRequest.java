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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.soap.admin.type.CountObjectsType;
import com.zimbra.soap.admin.type.DomainSelector;

@XmlRootElement(name=AdminConstants.E_COUNT_OBJECTS_REQUEST)
public class CountObjectsRequest {

    @XmlAttribute(name=AdminConstants.A_TYPE, required=true)
    private CountObjectsType type;
    
    @XmlElement(name=AdminConstants.E_DOMAIN, required=false)
    private DomainSelector domain;
    
    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    public CountObjectsRequest() {
        this((CountObjectsType) null, (DomainSelector) null);
    }
    
    public CountObjectsRequest(CountObjectsType type) {
        this(type, (DomainSelector) null);
    }
    
    public CountObjectsRequest(CountObjectsType type, DomainSelector domain) {
        setType(type);
        setDomain(domain);
    }
    
    public void setType(CountObjectsType type) {
        this.type = type;
    }
    
    public void setDomain(DomainSelector domain) {
        this.domain = domain;
    }
    
    public CountObjectsType getType() {
        return type;
    }
    
    public DomainSelector getDomain() {
        return domain;
    }
}
