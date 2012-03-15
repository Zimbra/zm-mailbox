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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.google.common.collect.Lists;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.soap.type.TargetType;

@XmlAccessorType(XmlAccessType.NONE)
public class DiscoverRightsTarget {

    /**
     * @zm-api-field-tag target-type
     * @zm-api-field-description Target type
     */
    @XmlAttribute(name=AccountConstants.A_TYPE /* type */, required=true)
    private TargetType type;

    /**
     * @zm-api-field-tag target-id
     * @zm-api-field-description Target ID
     */
    @XmlAttribute(name=AccountConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag target-name
     * @zm-api-field-description Target name
     */
    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=false)
    private String name;

    /**
     * @zm-api-field-tag target-display-name
     * @zm-api-field-description If a discovered target is an account or a group and the entry has a display name set
     * then this is set to that display name.
     */
    @XmlAttribute(name=AccountConstants.A_DISPLAY /* d */, required=false)
    private String displayName;

    /**
     * @zm-api-field-description Email addresses
     */
    @XmlElement(name=AccountConstants.E_EMAIL /* email */, required=false)
    private List<DiscoverRightsEmail> emails;

    public DiscoverRightsTarget() {
        this(null, null, null, null);
    }

    public DiscoverRightsTarget(TargetType type) {
        this(type, null, null, null);
    }

    public DiscoverRightsTarget(TargetType type, String id, String name, String displayName) {
        setType(type);
        setId(id);
        setName(name);
        setDisplayName(displayName);
    }

    public TargetType getType() { return type; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getId() { return id; }
    public List<DiscoverRightsEmail> getAddrs() { return emails; }

    public void setType(TargetType type) {
        this.type = type;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
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
