/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.MoreObjects;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.MailConstants;

import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name="NewSearchFolderSpec", description="Input for creating a new search folder")
public class NewSearchFolderSpec {

    /**
     * @zm-api-field-tag name
     * @zm-api-field-description Name
     */
    @XmlAttribute(name=MailConstants.A_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag query
     * @zm-api-field-description query
     */
    @XmlAttribute(name=MailConstants.A_QUERY /* query */, required=true)
    private String query;

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
     * @zm-api-field-tag rgb-color
     * @zm-api-field-description RGB color in format #rrggbb where r,g and b are hex digits
     */
    @XmlAttribute(name=MailConstants.A_RGB /* rgb */, required=false)
    private String rgb;

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
        this((String) null, (String) null, (String) null);
    }

    private NewSearchFolderSpec(String name, String query, String parentFolderId) {
        this.name = name;
        this.query = query;
        this.parentFolderId = parentFolderId;
    }

    public static NewSearchFolderSpec forNameQueryAndFolder(String folderName, String searchQuery, String parentId) {
        NewSearchFolderSpec spec = new NewSearchFolderSpec(folderName, searchQuery, parentId);
        return spec;
    }

    @GraphQLInputField(name=GqlConstants.NAME, description="name of the new folder")
    public void setName(@GraphQLNonNull String name) { this.name = name; }
    @GraphQLInputField(name=GqlConstants.QUERY, description="query to search")
    public void setQuery(@GraphQLNonNull String query) { this.query = query; }
    @GraphQLInputField(name=GqlConstants.SEARCH_TYPES, description="type of the new folder to be created")
    public void setSearchTypes(String searchTypes) { this.searchTypes = searchTypes; }
    @GraphQLInputField(name=GqlConstants.SORT_BY, description="sort order for the search results")
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    @GraphQLInputField(name=GqlConstants.FLAGS, description="Folder flags")
    public void setFlags(String flags) { this.flags = flags; }
    @GraphQLInputField(name=GqlConstants.COLOR, description="color numeric; range 0-127; defaults to 0 if not present; client can display only 0-7")
    public void setColor(Byte color) { this.color = color; }
    @GraphQLInputField(name=GqlConstants.PARENT_FOLDER_ID, description="parent folder id")
    public void setParentFolderId(@GraphQLNonNull String parentFolderId) { this.parentFolderId = parentFolderId; }
    @GraphQLInputField(name=GqlConstants.RGB, description="RGB color in format #rrggbb where r,g and b are hex digits")
    public void setRgb(String rgb) { this.rgb = rgb; }
    @GraphQLQuery(name=GqlConstants.NAME, description="name of the new folder")
    public String getName() { return name; }
    @GraphQLQuery(name=GqlConstants.QUERY, description="query to search")
    public String getQuery() { return query; }
    @GraphQLQuery(name=GqlConstants.SEARCH_TYPES, description="type of the new folder to be created")
    public String getSearchTypes() { return searchTypes; }
    @GraphQLQuery(name=GqlConstants.SORT_BY, description="sort order for the search results")
    public String getSortBy() { return sortBy; }
    @GraphQLQuery(name=GqlConstants.FLAGS, description="Folder flags")
    public String getFlags() { return flags; }
    @GraphQLQuery(name=GqlConstants.COLOR, description="color numeric; range 0-127; defaults to 0 if not present; client can display only 0-7")
    public Byte getColor() { return color; }
    @GraphQLQuery(name=GqlConstants.PARENT_FOLDER_ID, description="parent folder id")
    public String getParentFolderId() { return parentFolderId; }
    @GraphQLQuery(name=GqlConstants.RGB, description="RGB color in format #rrggbb where r,g and b are hex digits")
    public String getRgb() { return rgb; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("query", query)
            .add("searchTypes", searchTypes)
            .add("sortBy", sortBy)
            .add("flags", flags)
            .add("color", color)
            .add("rgb", rgb)
            .add("parentFolderId", parentFolderId);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
