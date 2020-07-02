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

package com.zimbra.soap.account.message;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.account.type.ContactInfo;
import com.zimbra.soap.type.ZmBoolean;

import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_SEARCH_GAL_RESPONSE)
@XmlType(propOrder = {})
@GraphQLType(name=GqlConstants.CLASS_GAL_SEARCH_RESPONSE, description="search GAL response")
public class SearchGalResponse {

    /**
     * @zm-api-field-tag sort-by
     * @zm-api-field-description Name of attribute sorted on.
     */
    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    /**
     * @zm-api-field-description The 0-based offset into the results list returned as the first result for this
     * search operation.
     */
    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer offset;

    /**
     * @zm-api-field-description Flags whether there are more results
     */
    @XmlAttribute(name=MailConstants.A_QUERY_MORE /* more */, required=false)
    private ZmBoolean more;

    /**
     * @zm-api-field-tag pagination-supported
     * @zm-api-field-description Flag whether the underlying search supported pagination.
     * <ul>
     * <li> <b>1 (true)</b> - limit and offset in the request was honored
     * <li> <b>0 (false)</b> - the underlying search does not support pagination. <b>limit</b> and <b>offset</b> in
     *      the request was not honored
     * </ul>
     */
    @XmlAttribute(name=AccountConstants.A_PAGINATION_SUPPORTED /* paginationSupported */, required=false)
    private ZmBoolean pagingSupported;

    // TODO:Documented in soap.txt - not sure if this is still used
    /**
     * @zm-api-field-tag tokenize-key-op
     * @zm-api-field-description Valid values: and|or
     * <ul>
     * <li> Not present if the search key was not tokenized.
     * <li> Some clients backtrack on GAL results assuming the results of a more specific key is the subset of a more
     *      generic key, and it checks cached results instead of issuing another SOAP request to the server.  
     *      If search key was tokenized and expanded with AND or OR, this cannot be assumed.
     * </ul>
     */
    @XmlAttribute(name=AccountConstants.A_TOKENIZE_KEY /* tokenizeKey */, required=false)
    private ZmBoolean tokenizeKey;

    /**
     * @zm-api-field-description Matching contacts
     */
    @XmlElement(name=MailConstants.E_CONTACT /* cn */, required=false)
    private List<ContactInfo> contacts = Lists.newArrayList();

    public SearchGalResponse() {
    }

    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setOffset(Integer offset) { this.offset = offset; }
    public void setMore(Boolean more) { this.more = ZmBoolean.fromBool(more); }
    public void setPagingSupported(Boolean pagingSupported) {
        this.pagingSupported = ZmBoolean.fromBool(pagingSupported);
    }
    public void setTokenizeKey(Boolean tokenizeKey) { this.tokenizeKey = ZmBoolean.fromBool(tokenizeKey); }
    public void setContacts(Iterable <ContactInfo> contacts) {
        this.contacts.clear();
        if (contacts != null) {
            Iterables.addAll(this.contacts,contacts);
        }
    }

    public void addContact(ContactInfo contact) {
        this.contacts.add(contact);
    }

    @GraphQLQuery(name=GqlConstants.SORT_BY, description="Name of attribute sorted on")
    public String getSortBy() { return sortBy; }
    @GraphQLQuery(name=GqlConstants.OFFSET, description="The 0-based offset into the results list returned as the first result for this search operation.")
    public Integer getOffset() { return offset; }
    @GraphQLQuery(name=GqlConstants.QUERY_MORE, description="Flags whether there are more results")
    public Boolean getMore() { return ZmBoolean.toBool(more); }
    @GraphQLQuery(name=GqlConstants.PAGINATION_SUPPORTED, description="Flag whether the underlying search supported pagination. 1 (true) : limit and offset in the request was honored." + 
            " 0 (false) - the underlying search does not support pagination.")
    public Boolean getPagingSupported() { return ZmBoolean.toBool(pagingSupported); }
    @GraphQLQuery(name=GqlConstants.TOKENIZE_KEY, description="Valid values: and|or\n" + 
            "      Not present if the search key was not tokenized.\n" + 
            "      Some clients backtrack on GAL results assuming the results of a more specific key is the subset of a more\n" + 
            "      generic key, and it checks cached results instead of issuing another SOAP request to the server.  \n" + 
            "      If search key was tokenized and expanded with AND or OR, this cannot be assumed.")
    public Boolean getTokenizeKey() { return ZmBoolean.toBool(tokenizeKey); }
    @GraphQLQuery(name=GqlConstants.CONTACTS, description="Matching contacts")
    public List<ContactInfo> getContacts() {
        return Collections.unmodifiableList(contacts);
    }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("sortBy", sortBy)
            .add("offset", offset)
            .add("more", more)
            .add("pagingSupported", pagingSupported)
            .add("tokenizeKey", tokenizeKey)
            .add("contacts", contacts);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
