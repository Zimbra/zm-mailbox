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

package com.zimbra.soap.voice.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class VoiceFolderSummary {

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Folder ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private String folderId;

    /**
     * @zm-api-field-tag unread-count
     * @zm-api-field-description Unread count
     */
    @XmlAttribute(name=MailConstants.A_UNREAD /* u */, required=true)
    private Long unreadCount;

    /**
     * @zm-api-field-tag msg-count
     * @zm-api-field-description Message count
     */
    @XmlAttribute(name=MailConstants.A_NUM /* n */, required=true)
    private Long msgCount;

    public VoiceFolderSummary() {
    }

    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setUnreadCount(Long unreadCount) { this.unreadCount = unreadCount; }
    public void setMsgCount(Long msgCount) { this.msgCount = msgCount; }
    public String getFolderId() { return folderId; }
    public Long getUnreadCount() { return unreadCount; }
    public Long getMsgCount() { return msgCount; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("folderId", folderId)
            .add("unreadCount", unreadCount)
            .add("msgCount", msgCount);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
