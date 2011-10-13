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

package com.zimbra.soap.mail.message;

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

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.AttributeName;
import com.zimbra.soap.type.Id;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_GET_CONTACTS_REQUEST)
public class GetContactsRequest {

    @XmlAttribute(name=MailConstants.A_SYNC /* sync */, required=false)
    private Boolean sync;

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    // Valid values are case insensitive "names" from enum com.zimbra.cs.index.SortBy
    @XmlAttribute(name=MailConstants.A_SORTBY /* sortBy */, required=false)
    private String sortBy;

    @XmlAttribute(name=MailConstants.A_DEREF_CONTACT_GROUP_MEMBER /* derefGroupMember */, required=false)
    private Boolean derefGroupMember;

    @XmlAttribute(name=MailConstants.A_RETURN_HIDDEN_ATTRS /* returnHiddenAttrs */, required=false)
    private Boolean returnHiddenAttrs;

    @XmlElement(name=MailConstants.E_ATTRIBUTE /* a */, required=false)
    private List<AttributeName> attributes = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CONTACT_GROUP_MEMBER_ATTRIBUTE /* ma */, required=false)
    private List<AttributeName> memberAttributes = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CONTACT /* cn */, required=false)
    private List<Id> contacts = Lists.newArrayList();

    public GetContactsRequest() {
    }

    public void setSync(Boolean sync) { this.sync = sync; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
    public void setDerefGroupMember(Boolean derefGroupMember) { this.derefGroupMember = derefGroupMember; }
    public void setReturnHiddenAttrs(Boolean returnHiddenAttrs) { this.returnHiddenAttrs = returnHiddenAttrs; }
    public void setAttributes(Iterable <AttributeName> attributes) {
        this.attributes.clear();
        if (attributes != null) {
            Iterables.addAll(this.attributes,attributes);
        }
    }

    public void addAttribute(AttributeName attribute) {
        this.attributes.add(attribute);
    }

    public void setMemberAttributes(Iterable <AttributeName> memberAttributes) {
        this.memberAttributes.clear();
        if (memberAttributes != null) {
            Iterables.addAll(this.memberAttributes,memberAttributes);
        }
    }

    public void addMemberAttribute(AttributeName memberAttribute) {
        this.memberAttributes.add(memberAttribute);
    }

    public void setContacts(Iterable <Id> contacts) {
        this.contacts.clear();
        if (contacts != null) {
            Iterables.addAll(this.contacts,contacts);
        }
    }

    public void addContact(Id contact) {
        this.contacts.add(contact);
    }

    public Boolean getSync() { return sync; }
    public String getFolderId() { return folderId; }
    public String getSortBy() { return sortBy; }
    public Boolean getDerefGroupMember() { return derefGroupMember; }
    public Boolean getReturnHiddenAttrs() { return returnHiddenAttrs; }
    public List<AttributeName> getAttributes() {
        return Collections.unmodifiableList(attributes);
    }
    public List<AttributeName> getMemberAttributes() {
        return Collections.unmodifiableList(memberAttributes);
    }
    public List<Id> getContacts() {
        return Collections.unmodifiableList(contacts);
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("sync", sync)
            .add("folderId", folderId)
            .add("sortBy", sortBy)
            .add("attributes", getAttributes())
            .add("memberAttributes", getMemberAttributes())
            .add("contacts", getContacts());
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
