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

package com.zimbra.soap.admin.type;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ContactAttr;

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(propOrder = {"metadatas", "attrs"})
public class AutoCompleteGalContactInfo {

    @XmlAttribute(name=MailConstants.A_SORT_FIELD /* sf */, required=true)
    private final String sortField;

    @XmlAttribute(name=AccountConstants.A_EXP /* exp */, required=false)
    private Boolean canExpand;

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folder;

    @XmlAttribute(name=MailConstants.A_FLAGS /* f */, required=false)
    private String flags;

    @XmlAttribute(name=MailConstants.A_TAGS /* t */, required=false)
    private String tags;

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

    @XmlElement(name=MailConstants.E_METADATA /* meta */, required=false)
    private List<AdminCustomMetadata> metadatas = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_A /* a */, required=false)
    private List<ContactAttr> attrs = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private AutoCompleteGalContactInfo() {
        this((String) null, (String) null);
    }

    public AutoCompleteGalContactInfo(String sortField, String id) {
        this.sortField = sortField;
        this.id = id;
    }

    public void setCanExpand(Boolean canExpand) { this.canExpand = canExpand; }
    public void setFolder(String folder) { this.folder = folder; }
    public void setFlags(String flags) { this.flags = flags; }
    public void setTags(String tags) { this.tags = tags; }
    public void setChangeDate(Long changeDate) { this.changeDate = changeDate; }
    public void setModifiedSequenceId(Integer modifiedSequenceId) { this.modifiedSequenceId = modifiedSequenceId; }
    public void setDate(Long date) { this.date = date; }
    public void setRevisionId(Integer revisionId) { this.revisionId = revisionId; }
    public void setFileAs(String fileAs) { this.fileAs = fileAs; }
    public void setEmail(String email) { this.email = email; }
    public void setEmail2(String email2) { this.email2 = email2; }
    public void setEmail3(String email3) { this.email3 = email3; }
    public void setType(String type) { this.type = type; }
    public void setDlist(String dlist) { this.dlist = dlist; }
    public void setMetadatas(Iterable <AdminCustomMetadata> metadatas) {
        this.metadatas.clear();
        if (metadatas != null) {
            Iterables.addAll(this.metadatas,metadatas);
        }
    }

    public void addMetadata(AdminCustomMetadata metadata) {
        this.metadatas.add(metadata);
    }

    public void setAttrs(Iterable <ContactAttr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs,attrs);
        }
    }

    public void addAttr(ContactAttr attr) {
        this.attrs.add(attr);
    }

    public String getSortField() { return sortField; }
    public Boolean getCanExpand() { return canExpand; }
    public String getId() { return id; }
    public String getFolder() { return folder; }
    public String getFlags() { return flags; }
    public String getTags() { return tags; }
    public Long getChangeDate() { return changeDate; }
    public Integer getModifiedSequenceId() { return modifiedSequenceId; }
    public Long getDate() { return date; }
    public Integer getRevisionId() { return revisionId; }
    public String getFileAs() { return fileAs; }
    public String getEmail() { return email; }
    public String getEmail2() { return email2; }
    public String getEmail3() { return email3; }
    public String getType() { return type; }
    public String getDlist() { return dlist; }
    public List<AdminCustomMetadata> getMetadatas() {
        return Collections.unmodifiableList(metadatas);
    }
    public List<ContactAttr> getAttrs() {
        return Collections.unmodifiableList(attrs);
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
            .add("metadatas", metadatas)
            .add("attrs", attrs);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
