/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
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
    public void setTooManyMembers(Boolean tooManyMembers);
    public void setMetadataInterfaces(Iterable <CustomMetadataInterface> metadatas);
    public void addMetadataInterfaces(CustomMetadataInterface metadata);
    // ContactAttr extends KeyValuePair.
    // com.zimbra.cs.service.mail.ToXML.encodeContactAttachment decorates KeyValuePairs with additional attributes
    public void setAttrs(Iterable <ContactAttr> attrs);
    public void addAttr(ContactAttr attr);
    public void setContactGroupMemberInterfaces(Iterable <ContactGroupMemberInterface> contactGroupMembers);
    public void addContactGroupMember(ContactGroupMemberInterface contactGroupMember);


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
    public Boolean getTooManyMembers();
    public List<CustomMetadataInterface> getMetadataInterfaces();
    public List<ContactAttr> getAttrs();
    public List<ContactGroupMemberInterface> getContactGroupMemberInterfaces();
}
