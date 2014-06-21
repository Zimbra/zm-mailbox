/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.type;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.Objects;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class ConversationMsgHitInfo {

    /**
     * @zm-api-field-tag message-id
     * @zm-api-field-description Message ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag size
     * @zm-api-field-description Size
     */
    @XmlAttribute(name=MailConstants.A_SIZE /* s */, required=false)
    private Long size;

    /**
     * @zm-api-field-tag folder-id
     * @zm-api-field-description Folder ID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    /**
     * @zm-api-field-tag auto-send-time
     * @zm-api-field-description Can optionally set autoSendTime to specify the time at which the draft should be
     * automatically sent by the server
     */
    @XmlAttribute(name=MailConstants.A_AUTO_SEND_TIME /* autoSendTime */, required=false)
    private Long autoSendTime;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ConversationMsgHitInfo() {
        this((String) null);
    }

    public ConversationMsgHitInfo(String id) {
        this.id = id;
    }

    public static ConversationMsgHitInfo fromIdAndFolderId(String id, String fId) {
        ConversationMsgHitInfo hit = new ConversationMsgHitInfo(id);
        hit.setFolderId(fId);
        return hit;
    }

    public void setSize(Long size) { this.size = size; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setAutoSendTime(Long autoSendTime) {
        this.autoSendTime = autoSendTime;
    }
    public String getId() { return id; }
    public Long getSize() { return size; }
    public String getFolderId() { return folderId; }
    public Long getAutoSendTime() { return autoSendTime; }

    /** Done like this rather than using JAXB for performance reasons */
    public Element toElement(Element parent) {
        Element mel = parent.addNonUniqueElement(MailConstants.E_MSG).addAttribute(MailConstants.A_ID, id);
        if (size != null) {
            mel.addAttribute(MailConstants.A_SIZE, size);
        }
        if (folderId != null) {
            mel.addAttribute(MailConstants.A_FOLDER, folderId);
        }
        if (autoSendTime != null) {
            mel.addAttribute(MailConstants.A_AUTO_SEND_TIME, autoSendTime);
        }
        return mel;
    }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("id", id)
            .add("size", size)
            .add("folderId", folderId)
            .add("autoSendTime", autoSendTime);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
