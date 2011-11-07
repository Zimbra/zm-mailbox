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
package com.zimbra.soap.account.type;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;

public class DiscoverRightsTarget {
    
    @XmlAttribute(name=AccountConstants.A_TYPE, required=true)
    private String type;
    
    @XmlAttribute(name=AccountConstants.A_ID, required=false)
    private String id;
    
    @XmlAttribute(name=AccountConstants.A_NAME, required=false)
    private String name;
    
    @XmlElement(name=AccountConstants.E_EMAIL, required=false)
    private List<DiscoverRightsEmail> emails;
    
    public DiscoverRightsTarget() {
        this(null, null, null);
    }
    
    public DiscoverRightsTarget(String type) {
        this(type, null, null);
    }
    
    public DiscoverRightsTarget(String type, String id, String name) {
        setType(type);
        setId(id);
        setName(name);
    }
    
    public String getType() { return type; }
    public String getName() { return name; }
    public String getId() { return id; }
    public List<DiscoverRightsEmail> getAddrs() { return emails; }
     
    public void setType(String type) {
        this.type = type;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public void setEmails(Iterable<DiscoverRightsEmail> emails) {
        this.emails = Lists.newArrayList(emails);
    }
    
    public void addEmail(DiscoverRightsEmail email) {
        if (emails == null) {
            emails = Lists.newArrayList();
        }
        emails.add(email);
    }
}
