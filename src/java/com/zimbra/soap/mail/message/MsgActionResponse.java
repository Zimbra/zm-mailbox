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
import com.zimbra.soap.mail.type.IdAndOperation;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_MSG_ACTION_RESPONSE)
public class MsgActionResponse {

    /**
     * @zm-api-field-description The <b>&lt;action></b> element in the response always contains the same id list that
     * the client sent in the request.  In particular, IDs that were ignored due to constraints are included in the
     * id list.
     */
    @XmlElement(name=MailConstants.E_ACTION, required=true)
    private final IdAndOperation action;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private MsgActionResponse() {
        this((IdAndOperation) null);
    }

    public MsgActionResponse(IdAndOperation action) {
        this.action = action;
    }

    public IdAndOperation getAction() { return action; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("action", action)
            .toString();
    }
}
