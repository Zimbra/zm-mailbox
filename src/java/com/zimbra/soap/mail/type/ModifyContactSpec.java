/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012 Zimbra, Inc.
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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ModifyContactSpec {

    // Used when modifying a contact
    /**
     * @zm-api-field-tag id
     * @zm-api-field-description ID - specified when modifying a contact
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private Integer id;


    /**
     * @zm-api-field-tag tag-names
     * @zm-api-field-description Comma-separated list of tag names
     */
    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    /**
     * @zm-api-field-description Contact attributes.  Cannot specify <b>&lt;vcard></b> as well as these
     */
    @XmlElement(name=MailConstants.E_ATTRIBUTE /* a */, required=false)
    private List<ModifyContactAttr> attrs = Lists.newArrayList();

    /**
     * @zm-api-field-description Contact group members.  Valid only if the contact being created is a contact group
     * (has attribute type="group")
     */
    @XmlElement(name=MailConstants.E_CONTACT_GROUP_MEMBER /* m */, required=false)
    private List<ModifyContactGroupMember> contactGroupMembers = Lists.newArrayList();

    public ModifyContactSpec() {
    }

    public void setId(Integer id) { this.id = id; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    public void setAttrs(Iterable <ModifyContactAttr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs, attrs);
        }
    }

    public void addAttr(ModifyContactAttr attr) {
        this.attrs.add(attr);
    }

    public void setContactGroupMembers(Iterable <ModifyContactGroupMember> contactGroupMembers) {
        this.contactGroupMembers.clear();
        if (contactGroupMembers != null) {
            Iterables.addAll(this.contactGroupMembers, contactGroupMembers);
        }
    }

    public void addContactGroupMember(ModifyContactGroupMember contactGroupMember) {
        this.contactGroupMembers.add(contactGroupMember);
    }

    public Integer getId() { return id; }
    public String getTagNames() { return tagNames; }
    public List<ModifyContactAttr> getAttrs() {
        return attrs;
    }
    public List<ModifyContactGroupMember> getContactGroupMembers() {
        return contactGroupMembers;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("tagNames", tagNames)
            .add("attrs", attrs)
            .add("contactGroupMembers", contactGroupMembers);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
