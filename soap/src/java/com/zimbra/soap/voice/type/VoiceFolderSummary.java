/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.voice.type;

import com.google.common.base.MoreObjects;
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

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("folderId", folderId)
            .add("unreadCount", unreadCount)
            .add("msgCount", msgCount);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
