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
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {"metadatas", "attrs"})
public class AutoCompleteGalContactInfo {

    @XmlAttribute(name=MailConstants.A_SORT_FIELD, required=true)
    private final String sortField;

    @XmlAttribute(name=AccountConstants.A_EXP, required=false)
    private Boolean canExpand;

    @XmlAttribute(name=MailConstants.A_ID, required=true)
    private final String id;

    @XmlAttribute(name=MailConstants.A_FOLDER, required=false)
    private String folder;

    @XmlAttribute(name=MailConstants.A_FLAGS, required=false)
    private String flags;

    @XmlAttribute(name=MailConstants.A_TAGS, required=false)
    private String tags;

    @XmlAttribute(name=MailConstants.A_CHANGE_DATE, required=false)
    private Long changeDate;

    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE, required=false)
    private Integer modifiedSequenceId;

    @XmlAttribute(name=MailConstants.A_DATE, required=false)
    private Long date;

    @XmlAttribute(name=MailConstants.A_REVISION, required=false)
    private Integer revisionId;

    @XmlAttribute(name=MailConstants.A_FILE_AS_STR, required=false)
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

    @XmlElement(name=MailConstants.E_METADATA, required=false)
    private List<ContactMetaData> metadatas = Lists.newArrayList();

    @XmlElement(name=MailConstants.E_A, required=false)
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
    public void setMetadatas(Iterable <ContactMetaData> metadatas) {
        this.metadatas.clear();
        if (metadatas != null) {
            Iterables.addAll(this.metadatas,metadatas);
        }
    }

    public AutoCompleteGalContactInfo addMetadata(ContactMetaData metadata) {
        this.metadatas.add(metadata);
        return this;
    }

    public void setAttrs(Iterable <ContactAttr> attrs) {
        this.attrs.clear();
        if (attrs != null) {
            Iterables.addAll(this.attrs,attrs);
        }
    }

    public AutoCompleteGalContactInfo addAttr(ContactAttr attr) {
        this.attrs.add(attr);
        return this;
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
    public List<ContactMetaData> getMetadatas() {
        return Collections.unmodifiableList(metadatas);
    }
    public List<ContactAttr> getAttrs() {
        return Collections.unmodifiableList(attrs);
    }
}
