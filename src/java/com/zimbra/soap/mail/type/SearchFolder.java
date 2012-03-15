/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011 Zimbra, Inc.
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

import java.util.EnumSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.SearchSortBy;

/*
  <search id="..." name="..." query="..." [types="..."] [sortBy="..."] l="{folder}"/>+

 */
// Root element name needed to differentiate between types of folder
// MailConstants.E_SEARCH == "search"
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SEARCH)
public final class SearchFolder extends Folder {

    /**
     * @zm-api-field-tag query
     * @zm-api-field-description Query
     */
    @XmlAttribute(name=MailConstants.A_QUERY /* query */, required=false)
    private String query;

    /**
     * @zm-api-field-tag sort-by
     * @zm-api-field-description Sort by
     */
    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private SearchSortBy sortBy;

    /**
     * @zm-api-field-tag comma-sep-search-types
     * @zm-api-field-description Comma-separated list.  Legal values in list are:
     * <br />
     * <b>appointment|chat|contact|conversation|document|message|tag|task|wiki</b>
     * (default is &quot;conversation&quot;)
     */
    @XmlAttribute(name=MailConstants.A_SEARCH_TYPES /* types */, required=false)
    @XmlJavaTypeAdapter(ItemType.CSVAdapter.class)
    private final Set<ItemType> types = EnumSet.noneOf(ItemType.class);

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public SearchSortBy getSortBy() {
        return sortBy;
    }

    public void setSortBy(SearchSortBy sortBy) {
        this.sortBy = sortBy;
    }

    public Set<ItemType> getTypes() {
        return types;
    }

    public void setTypes(Set<ItemType> set) {
        types.clear();
        types.addAll(set);
    }

    public void addType(ItemType type) {
        types.add(type);
    }
}
