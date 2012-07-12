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

import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.NONE)
public class VoiceFolder {

    /**
     * @zm-api-field-tag folder-name
     * @zm-api-field-description Folder name
     */
    @XmlAttribute(name=AccountConstants.A_NAME /* name */, required=true)
    private String name;

    /**
     * @zm-api-field-tag phone-ID
     * @zm-api-field-description Phone ID
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private String phoneId;

    /**
     * @zm-api-field-tag folder-ID
     * @zm-api-field-description Folder ID
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=true)
    private String folderId;

    /**
     * @zm-api-field-tag view
     * @zm-api-field-description View
     */
    @XmlAttribute(name=MailConstants.A_DEFAULT_VIEW /* view */, required=false)
    private String view;

    /**
     * @zm-api-field-tag num-unread-voice-msgs
     * @zm-api-field-description Number of unread voice messages
     * <br />
     * Only present for Trash and Voicemail Inbox
     */
    @XmlAttribute(name=MailConstants.A_UNREAD /* u */, required=false)
    private Long numUnreadMsgs;

    /**
     * @zm-api-field-tag total-num-voice-msgs
     * @zm-api-field-description Total number of voice messages
     * <br />
     * Only present for Trash and Voicemail Inbox
     */
    @XmlAttribute(name=MailConstants.A_NUM /* n */, required=false)
    private Long numTotalMsgs;

    public VoiceFolder() {
    }

    public void setName(String name) { this.name = name; }
    public void setPhoneId(String phoneId) { this.phoneId = phoneId; }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setView(String view) { this.view = view; }
    public void setNumUnreadMsgs(Long numUnreadMsgs) { this.numUnreadMsgs = numUnreadMsgs; }
    public void setNumTotalMsgs(Long numTotalMsgs) { this.numTotalMsgs = numTotalMsgs; }
    public String getName() { return name; }
    public String getPhoneId() { return phoneId; }
    public String getFolderId() { return folderId; }
    public String getView() { return view; }
    public Long getNumUnreadMsgs() { return numUnreadMsgs; }
    public Long getNumTotalMsgs() { return numTotalMsgs; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("name", name)
            .add("phoneId", phoneId)
            .add("folderId", folderId)
            .add("view", view)
            .add("numUnreadMsgs", numUnreadMsgs)
            .add("numTotalMsgs", numTotalMsgs);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
