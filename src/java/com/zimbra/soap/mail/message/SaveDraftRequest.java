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

package com.zimbra.soap.mail.message;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.SaveDraftMsg;

// Can we have more than one From: address?
// TODO: indicate folder to save in (defaults to Drafts)
/**
 * @zm-api-command-description Save draft
 * <ul> 
 * <li> Only allowed one top-level <b>&lt;mp></b> but can nest <b>&lt;mp></b>s within if multipart/* on reply/forward.
 *      Set origid on <b>&lt;m></b> element and set rt to "r" or "w", respectively.
 * <li> Can optionally set identity-id to specify the identity being used to compose the message.  If updating an
 *      existing draft, set "id" attr on <b>&lt;m></b> element.
 * <li> Can refer to parts of existing draft in <b>&lt;attach></b> block
 * <li> Drafts default to the Drafts folder
 * <li> Setting folder/tags/flags/color occurs <b>after</b> the draft is created/updated, and if it fails the content
 *      <b>WILL STILL BE SAVED</b>
 * <li> Can optionally set autoSendTime to specify the time at which the draft should be automatically sent by the
 *      server
 * <li> The ID of the saved draft is returned in the "id" attribute of the response.
 * <li> The ID referenced in the response's "idnt" attribute specifies the folder where the sent message is saved.
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SAVE_DRAFT_REQUEST)
public class SaveDraftRequest {

    // E_INVITE child is not allowed
    /**
     * @zm-api-field-description Details of Draft to save
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private SaveDraftMsg msg;

    public SaveDraftRequest() {
    }

    public void setMsg(SaveDraftMsg msg) { this.msg = msg; }
    public SaveDraftMsg getMsg() { return msg; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("msg", msg);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
