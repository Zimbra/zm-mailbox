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
import com.zimbra.soap.mail.type.MsgPartIds;

/**
 * @zm-api-command-description Remove attachments from a message body
 * <br />
 * <b>NOTE</b> that this operation is effectively a create and a delete, and thus the message's item ID will change
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_REMOVE_ATTACHMENTS_REQUEST)
public class RemoveAttachmentsRequest {

    /**
     * @zm-api-field-description Specification of parts to remove
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=true)
    private final MsgPartIds msg;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private RemoveAttachmentsRequest() {
        this((MsgPartIds) null);
    }

    public RemoveAttachmentsRequest(MsgPartIds msg) {
        this.msg = msg;
    }

    public MsgPartIds getMsg() { return msg; }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("msg", msg);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
