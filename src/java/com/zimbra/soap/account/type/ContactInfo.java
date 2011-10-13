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

package com.zimbra.soap.account.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.ContactGroupMemberInterface;
import com.zimbra.soap.base.ContactInterface;
import com.zimbra.soap.base.CustomMetadataInterface;
import com.zimbra.soap.type.ContactAttr;

@XmlAccessorType(XmlAccessType.NONE)
public class ContactInfo
implements ContactInterface {

    // Added by e.g. GalSearchControl.doLocalGalAccountSearch
    @XmlAttribute(name=MailConstants.A_SORT_FIELD /* sf */, required=false)
    private String sortField;

    @XmlAttribute(name=AccountConstants.A_EXP /* exp */, required=false)
    private Boolean canExpand;

    // id is the only attribute or element that can be required:
    //    GalSearchResultCallback.handleContact(Contact c) and CreateContact.handle
    //    sometimes just create E_CONTACT elements with only an A_ID attribute
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private String id;

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folder;

    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    @Deprecated
    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

    @XmlAttribute(name=MailConstants.A_TAG_NAMES /* tn */, required=false)
    private String tagNames;

    @XmlAttribute(name=MailConstants.A_CHANGE_DATE /* md */, required=false)
    private Long changeDate;

    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=false)
    private Integer modifiedSequenceId;

    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=false)
    private Long date;

    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revisionId;

    @XmlAttribute(name=MailConstants.A_FILE_AS_STR /* fileAsStr */, required=false)
    private String fileAs;

    @XmlAttribute(name="email", required=false)
    private String email;

    @XmlAttribute(name="email2", required=false)
    private String email2;

    @XmlAttribute(name="email3", required=false)
    private String email3;

    @XmlAttribute(name="type", required=false)
    private String type;

    @XmlAttribute(name="dlist", required=false)
    private String dlist;

    // See GalSearchResultCallback.handleContact(Contact c)
    @XmlAttribute(name=AccountConstants.A_REF /* ref */, required=false)
    private String reference;

    @XmlElement(name=MailConstants.E_METADATA /* meta */, required=false)
    private List<AccountCustomMetadata> metadatas = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_A /* a */, required=false)
    private List<ContactAttr> attrs = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_CONTACT_GROUP_MEMBER /* m */, required=false)
    private List<ContactGroupMember> contactGroupMembers = Lists.newArrayList();

    public ContactInfo() {
    }

    public ContactInfo(String id) {
        this.id = id;
    }

    private ContactInfo(String sortField, String id) {
        this.sortField = sortField;
        this.id = id;
    }

    public static ContactInfo createForId(String id) {
        return new ContactInfo(id);
    }

    public static ContactInfo createForSortFieldAndId(String sortField, String id) {
        return new ContactInfo(sortField, id);
    }

    @Override
    public void setSortField(String sortField) { this.sortField = sortField; }
    @Override
    public void setCanExpand(Boolean canExpand) { this.canExpand = canExpand; }
    @Override
    public void setId(String id) { this.id = id; }
    @Override
    public void setFolder(String folder) { this.folder = folder; }
    @Override
    public void setFlags(String flags) { this.flags = flags; }
    @Override
    public void setTags(String tags) { this.tags = tags; }
    @Override
    public void setTagNames(String tagNames) { this.tagNames = tagNames; }
    @Override
    public void setChangeDate(Long changeDate) { this.changeDate = changeDate; }
    @Override
    public void setModifiedSequenceId(Integer modifiedSequenceId) {
        this.modifiedSequenceId = modifiedSequenceId;
    }
    @Override
    public void setDate(Long date) { this.date = date; }
    @Override
    public void setRevisionId(Integer revisionId) {
        this.revisionId = revisionId;
    }
    @Override
    public void setFileAs(String fileAs) { this.fileAs = fileAs; }
    @Override
    public void setEmail(String email) { this.email = email; }
    @Override
    public void setEmail2(String email2) { this.email2 = email2; }
    @Override
    public void setEmail3(String email3) { this.email3 = email3; }
    @Override
    public void setType(String type) { this.type = type; }
    @Override
    public void setDlist(String dlist) { this.dlist = dlist; }
    @Override
    public void setReference(String reference) { this.reference = reference; }
    public void setMetadatas(Iterable <AccountCustomMetadata> metadatas) {
        this.metadatas.clear();
        if (metadatas != null) {
            Iterables.addAll(this.metadatas,metadatas);
        }
    }

    public void addMetadata(AccountCustomMetadata metadata) {
        this.metadatas.add(metadata);
    }

    @Override
    public void setAttrs(Iterable <ContactAttr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs,attrs);
        }
    }

    @Override
    public void addAttr(ContactAttr attr) {
        this.attrs.add(attr);
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

    @Override
    public String getSortField() { return sortField; }
    @Override
    public Boolean getCanExpand() { return canExpand; }
    @Override
    public String getId() { return id; }
    @Override
    public String getFolder() { return folder; }
    @Override
    public String getFlags() { return flags; }
    @Override
    public String getTags() { return tags; }
    @Override
    public String getTagNames() { return tagNames; }
    @Override
    public Long getChangeDate() { return changeDate; }
    @Override
    public Integer getModifiedSequenceId() { return modifiedSequenceId; }
    @Override
    public Long getDate() { return date; }
    @Override
    public Integer getRevisionId() { return revisionId; }
    @Override
    public String getFileAs() { return fileAs; }
    @Override
    public String getEmail() { return email; }
    @Override
    public String getEmail2() { return email2; }
    @Override
    public String getEmail3() { return email3; }
    @Override
    public String getType() { return type; }
    @Override
    public String getDlist() { return dlist; }
    @Override
    public String getReference() { return reference; }

    public List<AccountCustomMetadata> getMetadatas() {
        return metadatas;
    }
    @Override
    public List<ContactAttr> getAttrs() {
        return attrs;
    }

    public List<ContactGroupMember> getContactGroupMembers() {
        return Collections.unmodifiableList(contactGroupMembers);
    }

    // non-JAXB method
    @Override
    public void setMetadataInterfaces(
            Iterable<CustomMetadataInterface> metadatas) {
        this.metadatas = AccountCustomMetadata.fromInterfaces(metadatas);
    }

    // non-JAXB method
    @Override
    public void addMetadataInterfaces(CustomMetadataInterface metadata) {
        addMetadata((AccountCustomMetadata)metadata);
    }

    // non-JAXB method
    @Override
    public List<CustomMetadataInterface> getMetadataInterfaces() {
        return AccountCustomMetadata.toInterfaces(metadatas);
    }

    public static Iterable <ContactInfo> fromInterfaces(Iterable <ContactInterface> params) {
        if (params == null)
            return null;
        List <ContactInfo> newList = Lists.newArrayList();
        for (ContactInterface param : params) {
            newList.add((ContactInfo) param);
        }
        return newList;
    }

    public static List <ContactInterface> toInterfaces(Iterable <ContactInfo> params) {
        if (params == null)
            return null;
        List <ContactInterface> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return newList;
    }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("sortField", sortField)
            .add("canExpand", canExpand)
            .add("id", id)
            .add("folder", folder)
            .add("flags", flags)
            .add("tags", tags)
            .add("tagNames", tagNames)
            .add("changeDate", changeDate)
            .add("modifiedSequenceId", modifiedSequenceId)
            .add("date", date)
            .add("revisionId", revisionId)
            .add("fileAs", fileAs)
            .add("email", email)
            .add("email2", email2)
            .add("email3", email3)
            .add("type", type)
            .add("dlist", dlist)
            .add("reference", reference)
            .add("metadatas", metadatas)
            .add("attrs", attrs)
            .add("contactGroupMembers", contactGroupMembers);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }

    @Override
    public void setContactGroupMemberInterfaces(Iterable<ContactGroupMemberInterface> contactGroupMembers) {
        this.setContactGroupMembers(ContactGroupMember.fromInterfaces(contactGroupMembers));
    }

    @Override
    public void addContactGroupMember(ContactGroupMemberInterface contactGroupMember) {
        this.addContactGroupMember((ContactGroupMember) contactGroupMember);
    }

    @Override
    public List<ContactGroupMemberInterface> getContactGroupMemberInterfaces() {
        return ContactGroupMember.toInterfaces(contactGroupMembers);
    }
}
