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

package com.zimbra.soap.base;

import java.util.List;

import com.zimbra.soap.type.ContactAttr;

/**
 * 
 * See {@link com.zimbra.cs.service.mail.ToXML} encodeContact, encodeGalContact
 * Note that encodeContactAttachment forces KeyValuePairs to be represented by list of ContactAttr
 */
public interface ContactInterface {
    public void setId(String id);
    public void setSortField(String sortField);
    public void setCanExpand(Boolean canExpand);
    public void setFolder(String folder);
    public void setFlags(String flags);
    @Deprecated
    public void setTags(String tags);
    public void setTagNames(String tagNames);
    public void setChangeDate(Long changeDate);
    public void setModifiedSequenceId(Integer modifiedSequenceId);
    public void setDate(Long date);
    public void setRevisionId(Integer revisionId);
    public void setFileAs(String fileAs);
    public void setEmail(String email);
    public void setEmail2(String email2);
    public void setEmail3(String email3);
    public void setType(String type);
    public void setDlist(String dlist);
    public void setReference(String reference);
    public void setMetadataInterfaces(Iterable <CustomMetadataInterface> metadatas);
    public void addMetadataInterfaces(CustomMetadataInterface metadata);
    // ContactAttr extends KeyValuePair.
    // com.zimbra.cs.service.mail.ToXML.encodeContactAttachment decorates KeyValuePairs with additional attributes
    public void setAttrs(Iterable <ContactAttr> attrs);
    public void addAttr(ContactAttr attr);

    public String getId();
    public String getSortField();
    public Boolean getCanExpand();
    public String getFolder();
    public String getFlags();
    @Deprecated
    public String getTags();
    public String getTagNames();
    public Long getChangeDate();
    public Integer getModifiedSequenceId();
    public Long getDate();
    public Integer getRevisionId();
    public String getFileAs();
    public String getEmail();
    public String getEmail2();
    public String getEmail3();
    public String getType();
    public String getDlist();
    public String getReference();
    public List<CustomMetadataInterface> getMetadataInterfaces();
    public List<ContactAttr> getAttrs();
}
