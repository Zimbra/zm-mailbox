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

package com.zimbra.soap.account.message;

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
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.account.type.ContactInfo;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_SEARCH_GAL_RESPONSE)
@XmlType(propOrder = {})
public class SearchGalResponse {

    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer offset;

    @XmlAttribute(name=MailConstants.A_QUERY_MORE /* more */, required=false)
    private Boolean more;

    @XmlAttribute(name=MailConstants.A_TOKEN /* token */, required=false)
    private String token;

    @XmlAttribute(name=AccountConstants.A_PAGINATION_SUPPORTED /* paginationSupported */, required=false)
    private Boolean pagingSupported;

    // TODO:Documented in soap.txt - not sure if this is still used
    @XmlAttribute(name=AccountConstants.A_TOKENIZE_KEY /* tokenizeKey */, required=false)
    private Boolean tokenizeKey;

    @XmlElement(name=MailConstants.E_CONTACT /* cn */, required=false)
    private List<ContactInfo> contacts = Lists.newArrayList();

    public SearchGalResponse() {
    }

    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setOffset(Integer offset) { this.offset = offset; }
    public void setMore(Boolean more) { this.more = more; }
    public void setToken(String token) { this.token = token; }
    public void setPagingSupported(Boolean pagingSupported) { this.pagingSupported = pagingSupported; }
    public void setTokenizeKey(Boolean tokenizeKey) { this.tokenizeKey = tokenizeKey; }
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
    public Boolean getMore() { return more; }
    public String getToken() { return token; }
    public Boolean getPagingSupported() { return pagingSupported; }
    public Boolean getTokenizeKey() { return tokenizeKey; }
    public List<ContactInfo> getContacts() {
        return Collections.unmodifiableList(contacts);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("sortBy", sortBy)
            .add("offset", offset)
            .add("more", more)
            .add("token", token)
            .add("pagingSupported", pagingSupported)
            .add("tokenizeKey", tokenizeKey)
            .add("contacts", contacts);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
