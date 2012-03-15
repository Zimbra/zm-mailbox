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
