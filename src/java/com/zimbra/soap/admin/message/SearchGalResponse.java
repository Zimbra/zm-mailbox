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

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.admin.type.ContactInfo;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AdminConstants.E_SEARCH_GAL_RESPONSE)
@XmlType(propOrder = {})
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
     * @zm-api-field-tag more-flag
     * @zm-api-field-description Set if the results are truncated
     */
    @XmlAttribute(name=MailConstants.A_QUERY_MORE /* more */, required=false)
    private ZmBoolean more;

    // TODO:Documented in soap-admin.txt - not sure if this is still used
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

    public String getSortBy() { return sortBy; }
    public Integer getOffset() { return offset; }
    public Boolean getMore() { return ZmBoolean.toBool(more); }
    public Boolean getTokenizeKey() { return ZmBoolean.toBool(tokenizeKey); }
    public List<ContactInfo> getContacts() {
        return Collections.unmodifiableList(contacts);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("sortBy", sortBy)
            .add("offset", offset)
            .add("more", more)
            .add("tokenizeKey", tokenizeKey)
            .add("contacts", contacts);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
