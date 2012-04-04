/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012 Zimbra, Inc.
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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public class MsgToSend
extends Msg {

    /**
     * @zm-api-field-tag saved-draft-id
     * @zm-api-field-description Saved draft ID
     */
    @XmlAttribute(name=MailConstants.A_DRAFT_ID /* did */, required=false)
    private String draftId;
    
    /**
     * @zm-api-field-tag send-from-draft
     * @zm-api-field-description If set, message gets constructed based on the "did" (id of the draft).
     */
    @XmlAttribute(name=MailConstants.A_SEND_FROM_DRAFT /* sfd */, required=false)
    private ZmBoolean sendFromDraft;

    public MsgToSend() {
    }

    public void setDraftId(String draftId) { this.draftId = draftId; }
    public String getDraftId() { return draftId; }
    
    public void setSendFromDraft(Boolean sendFromDraft) {
        this.sendFromDraft = ZmBoolean.fromBool(sendFromDraft);
    }
    public Boolean getSendFromDraft() { return ZmBoolean.toBool(sendFromDraft); }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("draftId", draftId)
            .add("sendFromDraft", sendFromDraft);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
