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

public interface MessageCommonInterface {
    public void setSize(Long size);
    public void setDate(Long date);
    public void setFolder(String folder);
    public void setConversationId(String conversationId);
    public void setFlags(String flags);
    @Deprecated
    public void setTags(String tags);
    public void setTagNames(String tagNames);
    public void setRevision(Integer revision);
    public void setChangeDate(Long changeDate);
    public void setModifiedSequence(Integer modifiedSequence);
    public void setMetadataInterfaces(Iterable <CustomMetadataInterface> metadatas);
    public void addMetadataInterfaces(CustomMetadataInterface metadata);

    public Long getSize();
    public Long getDate();
    public String getFolder();
    public String getConversationId();
    public String getFlags();
    @Deprecated
    public String getTags();
    public String getTagNames();
    public Integer getRevision();
    public Long getChangeDate();
    public Integer getModifiedSequence();

    public List<CustomMetadataInterface> getMetadataInterfaces();
}
