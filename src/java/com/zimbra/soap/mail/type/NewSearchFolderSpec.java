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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class NewSearchFolderSpec {

    @XmlAttribute(name=MailConstants.A_NAME, required=true)
    private final String name;

    @XmlAttribute(name=MailConstants.A_QUERY, required=true)
    private final String query;

    @XmlAttribute(name=MailConstants.A_SEARCH_TYPES, required=false)
    private String searchTypes;

    @XmlAttribute(name=MailConstants.A_SORTBY, required=false)
    private String sortBy;

    @XmlAttribute(name=MailConstants.A_FLAGS, required=false)
    private String flags;

    @XmlAttribute(name=MailConstants.A_COLOR, required=false)
    private Byte color;

    @XmlAttribute(name=MailConstants.A_FOLDER, required=false)
    private String parentFolderId;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private NewSearchFolderSpec() {
        this((String) null, (String) null);
    }

    public NewSearchFolderSpec(String name, String query) {
        this.name = name;
        this.query = query;
    }

    public void setSearchTypes(String searchTypes) {
        this.searchTypes = searchTypes;
    }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setFlags(String flags) { this.flags = flags; }
    public void setColor(Byte color) { this.color = color; }
    public void setParentFolderId(String parentFolderId) {
        this.parentFolderId = parentFolderId;
    }
    public String getName() { return name; }
    public String getQuery() { return query; }
    public String getSearchTypes() { return searchTypes; }
    public String getSortBy() { return sortBy; }
    public String getFlags() { return flags; }
    public Byte getColor() { return color; }
    public String getParentFolderId() { return parentFolderId; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("name", name)
            .add("query", query)
            .add("searchTypes", searchTypes)
            .add("sortBy", sortBy)
            .add("flags", flags)
            .add("color", color)
            .add("parentFolderId", parentFolderId)
            .toString();
    }
}
