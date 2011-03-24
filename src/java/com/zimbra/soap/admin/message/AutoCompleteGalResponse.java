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

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.admin.type.AutoCompleteGalContactInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=AdminConstants.E_AUTO_COMPLETE_GAL_RESPONSE)
public class AutoCompleteGalResponse {

    @XmlAttribute(name=MailConstants.A_SORTBY, required=false)
    private final String sortBy;

    @XmlAttribute(name=MailConstants.A_QUERY_OFFSET, required=false)
    private final Integer offset;

    @XmlAttribute(name=MailConstants.A_QUERY_MORE, required=false)
    private final Boolean more;

    // Probably not actually used for AutoCompleteGal
    @XmlAttribute(name=MailConstants.A_TOKEN, required=false)
    private String token;

    // TODO:Is this actually set anywhere in the server?
    @XmlAttribute(name=AccountConstants.A_TOKENIZE_KEY, required=false)
    private final Boolean tokenizeKey;

    @XmlAttribute(name=AccountConstants.A_PAGINATION_SUPPORTED, required=false)
    private Boolean pagingSupported;

    @XmlElement(name=MailConstants.E_CONTACT, required=false)
    private List<AutoCompleteGalContactInfo> contacts = Lists.newArrayList();

    // Believe that GalSearchResultCallback.handleDeleted(ItemId id) is not
    // used for AutoCompleteGal
    // If it were used - it would matter what order as it would probably
    // be a list of Id

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AutoCompleteGalResponse() {
        this((String) null, (Integer) null, (Boolean) null, (Boolean) null);
    }

    public AutoCompleteGalResponse(String sortBy, Integer offset,
                            Boolean more, Boolean tokenizeKey) {
        this.sortBy = sortBy;
        this.offset = offset;
        this.more = more;
        this.tokenizeKey = tokenizeKey;
    }

    public void setToken(String token) { this.token = token; }

    public void setPagingSupported(Boolean pagingSupported) {
        this.pagingSupported = pagingSupported;
    }

    public void setContacts(Iterable <AutoCompleteGalContactInfo> contacts) {
        this.contacts.clear();
        if (contacts != null) {
            Iterables.addAll(this.contacts,contacts);
        }
    }

    public AutoCompleteGalResponse addContact(
                            AutoCompleteGalContactInfo contact) {
        this.contacts.add(contact);
        return this;
    }

    public String getSortBy() { return sortBy; }
    public Integer getOffset() { return offset; }
    public Boolean getMore() { return more; }
    public String getToken() { return token; }
    public Boolean getTokenizeKey() { return tokenizeKey; }
    public Boolean getPagingSupported() { return pagingSupported; }
    public List<AutoCompleteGalContactInfo> getContacts() {
        return Collections.unmodifiableList(contacts);
    }
}
