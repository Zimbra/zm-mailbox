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
public class ContactSpec {

    // Used when modifying a contact
    /**
     * @zm-api-field-tag id
     * @zm-api-field-description ID - specified when modifying a contact
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private Integer id;

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description ID of folder to create contact in. Un-specified means use the default Contacts folder.
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folder;

    /**
     * @zm-api-field-tag tags
     * @zm-api-field-description Tags - Comma separated list of integers.  DEPRECATED - use "tn" instead
     */
    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    /**
     * @zm-api-field-tag tag-names
     * @zm-api-field-description Comma-separated list of tag names
     */
    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    /**
     * @zm-api-field-description Either a vcard or attributes can be specified but not both.
     */
    @XmlElement(name=MailConstants.E_VCARD /* vcard */, required=false)
    private VCardInfo vcard;

    /**
     * @zm-api-field-description Contact attributes.  Cannot specify <b>&lt;vcard></b> as well as these
     */
    @XmlElement(name=MailConstants.E_ATTRIBUTE /* a */, required=false)
    private List<NewContactAttr> attrs = Lists.newArrayList();

    /**
     * @zm-api-field-description Contact group members.  Valid only if the contact being created is a contact group
     * (has attribute type="group")
     */
    @XmlElement(name=MailConstants.E_CONTACT_GROUP_MEMBER /* m */, required=false)
    private List<ContactGroupMember> contactGroupMembers = Lists.newArrayList();

    public ContactSpec() {
    }

    public void setId(Integer id) { this.id = id; }
    public void setFolder(String folder) { this.folder = folder; }
    @Deprecated
    public void setTags(String tags) { this.tags = tags; }
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    public void setVcard(VCardInfo vcard) { this.vcard = vcard; }
    public void setAttrs(Iterable <NewContactAttr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs,attrs);
        }
    }

    public ContactSpec addAttr(NewContactAttr attr) {
        this.attrs.add(attr);
        return this;
    }

    public void setContactGroupMembers(Iterable <ContactGroupMember> contactGroupMembers) {
        this.contactGroupMembers.clear();
        if (contactGroupMembers != null) {
            Iterables.addAll(this.contactGroupMembers,contactGroupMembers);
        }
    }

    public void addContactGroupMember(ContactGroupMember contactGroupMember) {
        this.contactGroupMembers.add(contactGroupMember);
    }

    public Integer getId() { return id; }
    public String getFolder() { return folder; }
    @Deprecated
    public String getTags() { return tags; }
    public String getTagNames() { return tagNames; }
    public VCardInfo getVcard() { return vcard; }
    public List<NewContactAttr> getAttrs() {
        return Collections.unmodifiableList(attrs);
    }

    public List<ContactGroupMember> getContactGroupMembers() {
        return contactGroupMembers;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("id", id)
            .add("folder", folder)
            .add("tags", tags)
            .add("tagNames", tagNames)
            .add("vcard", vcard)
            .add("attrs", attrs)
            .toString();
    }
}
