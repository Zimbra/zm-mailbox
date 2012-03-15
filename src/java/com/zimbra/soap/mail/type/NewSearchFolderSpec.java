/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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

@XmlAccessorType(XmlAccessType.NONE)
public class NewSearchFolderSpec {

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=true)
    private final String name;

    /**
     * @zm-api-field-tag query
     * @zm-api-field-description query
     */
    @XmlAttribute(name=MailConstants.A_QUERY /* query */, required=true)
    private final String query;

    /**
     * @zm-api-field-tag search-types
     * @zm-api-field-description Search types
     */
    @XmlAttribute(name=MailConstants.A_SEARCH_TYPES /* types */, required=false)
    private String searchTypes;

    /**
     * @zm-api-field-tag sort-by
     * @zm-api-field-description Sort by
     */
    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    /**
     * @zm-api-field-tag flags
     * @zm-api-field-description Flags
     */
    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    /**
     * @zm-api-field-tag color
     * @zm-api-field-description color numeric; range 0-127; defaults to 0 if not present; client can display only 0-7
     */
    @XmlAttribute(name=MailConstants.A_COLOR /* color */, required=false)
    private Byte color;

    /**
     * @zm-api-field-tag parent-folder-id
     * @zm-api-field-description Parent folder ID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
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

    public void setSearchTypes(String searchTypes) { this.searchTypes = searchTypes; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setFlags(String flags) { this.flags = flags; }
    public void setColor(Byte color) { this.color = color; }
    public void setParentFolderId(String parentFolderId) { this.parentFolderId = parentFolderId; }
    public String getName() { return name; }
    public String getQuery() { return query; }
    public String getSearchTypes() { return searchTypes; }
    public String getSortBy() { return sortBy; }
    public String getFlags() { return flags; }
    public Byte getColor() { return color; }
    public String getParentFolderId() { return parentFolderId; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("query", query)
            .add("searchTypes", searchTypes)
            .add("sortBy", sortBy)
            .add("flags", flags)
            .add("color", color)
            .add("parentFolderId", parentFolderId);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
