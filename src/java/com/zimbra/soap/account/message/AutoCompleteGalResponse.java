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
import com.zimbra.soap.base.ContactInterface;
import com.zimbra.soap.base.AutoCompleteGalInterface;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=AccountConstants.E_AUTO_COMPLETE_GAL_RESPONSE)
@XmlType(propOrder = {})
public class AutoCompleteGalResponse implements AutoCompleteGalInterface {

    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET /* offset */, required=false)
    private Integer offset;

    @XmlAttribute(name=MailConstants.A_QUERY_MORE /* more */, required=false)
    private ZmBoolean more;

    // Probably not actually used for AutoCompleteGal
    @XmlAttribute(name=MailConstants.A_TOKEN /* token */, required=false)
    private String token;

    // TODO:Is this actually set anywhere in the server?
    @XmlAttribute(name=AccountConstants.A_TOKENIZE_KEY /* tokenizeKey */, required=false)
    private ZmBoolean tokenizeKey;

    @XmlAttribute(name=AccountConstants.A_PAGINATION_SUPPORTED /* paginationSupported */, required=false)
    private ZmBoolean pagingSupported;

    @XmlElement(name=MailConstants.E_CONTACT /* cn */, required=false)
    private List<ContactInfo> contacts = Lists.newArrayList();

    // Believe that GalSearchResultCallback.handleDeleted(ItemId id) is not used for AutoCompleteGal
    // If it were used - it would matter what order as it would probably be a list of Id

    public AutoCompleteGalResponse() {
        this((String) null, (Integer) null, (Boolean) null, (Boolean) null);
    }

    private AutoCompleteGalResponse(String sortBy, Integer offset,
                            Boolean more, Boolean tokenizeKey) {
        this.setSortBy(sortBy);
        this.setOffset(offset);
        this.setMore(more);
        this.tokenizeKey = ZmBoolean.fromBool(tokenizeKey);
    }

    public static AutoCompleteGalResponse createForSortByOffsetMoreAndTokenizeKey(String sortBy, Integer offset,
                            Boolean more, Boolean tokenizeKey) {
        return new AutoCompleteGalResponse(sortBy, offset, more, tokenizeKey);
    }


    @Override
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    @Override
    public void setOffset(Integer offset) { this.offset = offset; }
    @Override
    public void setMore(Boolean more) { this.more = ZmBoolean.fromBool(more); }
    @Override
    public void setToken(String token) { this.token = token; }
    @Override
    public void setTokenizeKey(Boolean tokenizeKey) { this.tokenizeKey = ZmBoolean.fromBool(tokenizeKey); }
    @Override
    public void setPagingSupported(Boolean pagingSupported) { this.pagingSupported = ZmBoolean.fromBool(pagingSupported); }

    public void setContacts(Iterable <ContactInfo> contacts) {
        this.contacts.clear();
        if (contacts != null) {
            Iterables.addAll(this.contacts,contacts);
        }
    }

    public void addContact(ContactInfo contact) {
        this.contacts.add(contact);
    }

    @Override
    public String getSortBy() { return sortBy; }
    @Override
    public Integer getOffset() { return offset; }
    @Override
    public Boolean getMore() { return ZmBoolean.toBool(more); }
    @Override
    public String getToken() { return token; }
    @Override
    public Boolean getTokenizeKey() { return ZmBoolean.toBool(tokenizeKey); }
    @Override
    public Boolean getPagingSupported() { return ZmBoolean.toBool(pagingSupported); }
    public List<ContactInfo> getContacts() {
        return contacts;
    }

    @Override
    public List<ContactInterface> getContactInterfaces() {
        return Collections.unmodifiableList(ContactInfo.toInterfaces(contacts));
    }

    @Override
    public void setContactInterfaces(
            Iterable<ContactInterface> contacts) {
        setContacts(ContactInfo.fromInterfaces(contacts));
    }

    @Override
    public void addContactInterface(ContactInterface contact) {
        addContact((ContactInfo) contact);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("sortBy", sortBy)
            .add("offset", offset)
            .add("more", more)
            .add("token", token)
            .add("tokenizeKey", tokenizeKey)
            .add("pagingSupported", pagingSupported)
            .add("contacts", contacts);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
