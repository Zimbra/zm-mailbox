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
public class ModifySearchFolderSpec {

    /**
     * @zm-api-field-tag id
     * @zm-api-field-description ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag query
     * @zm-api-field-description Query
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
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ModifySearchFolderSpec() {
        this((String) null, (String) null);
    }

    public ModifySearchFolderSpec(String id, String query) {
        this.id = id;
        this.query = query;
    }

    public void setSearchTypes(String searchTypes) { this.searchTypes = searchTypes; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public String getId() { return id; }
    public String getQuery() { return query; }
    public String getSearchTypes() { return searchTypes; }
    public String getSortBy() { return sortBy; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("query", query)
            .add("searchTypes", searchTypes)
            .add("sortBy", sortBy);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
