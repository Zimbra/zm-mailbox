/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
@GraphQLType(name="SearchFolder", description="A search for a folder item")
public final class SearchFolder extends Folder {

    /**
     * @zm-api-field-tag query
     * @zm-api-field-description Query
     */
    @XmlAttribute(name=MailConstants.A_QUERY /* query */, required=false)
    @GraphQLQuery(name="query", description="Query")
    private String query;

    /**
     * @zm-api-field-tag sort-by
     * @zm-api-field-description Sort by
     */
    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    @GraphQLQuery(name="sortBy", description="Sort by")
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
    @GraphQLQuery(name="types", description="Set of types")
    private final Set<ItemType> types = EnumSet.noneOf(ItemType.class);

    @GraphQLQuery(name="query", description="The query")
    public String getQuery() {
        return query;
    }

    @GraphQLQuery(name="query", description="The query")
    public void setQuery(String query) {
        this.query = query;
    }

    @GraphQLQuery(name="sortBy", description="Sort by")
    public SearchSortBy getSortBy() {
        return sortBy;
    }

    @GraphQLQuery(name="sortBy", description="Sort by")
    public void setSortBy(SearchSortBy sortBy) {
        this.sortBy = sortBy;
    }

    @GraphQLQuery(name="types", description="Types")
    public Set<ItemType> getTypes() {
        return types;
    }

    @GraphQLQuery(name="types", description="Types")
    public void setTypes(Set<ItemType> set) {
        types.clear();
        types.addAll(set);
    }

    @GraphQLQuery(name="types", description="Types")
    public void addType(ItemType type) {
        types.add(type);
    }
}
