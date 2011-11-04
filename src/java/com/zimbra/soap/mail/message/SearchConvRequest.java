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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.MailSearchParams;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SEARCH_CONV_REQUEST)
public class SearchConvRequest extends MailSearchParams {

    @XmlAttribute(name=MailConstants.A_NEST_MESSAGES /* nest */, required=false)
    private ZmBoolean nestMessages;

    @XmlAttribute(name=MailConstants.A_CONV_ID /* cid */, required=true)
    private final String conversationId;

    @XmlAttribute(name=MailConstants.A_NEED_EXP /* needExp */, required=false)
    private ZmBoolean needCanExpand;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SearchConvRequest() {
        this((String) null);
    }

    public SearchConvRequest(String conversationId) {
        this.conversationId = conversationId;
    }

    public void setNestMessages(Boolean nestMessages) { this.nestMessages = ZmBoolean.fromBool(nestMessages); }
    public void setNeedCanExpand(Boolean needCanExpand) { this.needCanExpand = ZmBoolean.fromBool(needCanExpand); }
    public Boolean getNestMessages() { return ZmBoolean.toBool(nestMessages); }
    public String getConversationId() { return conversationId; }
    public Boolean getNeedCanExpand() { return ZmBoolean.toBool(needCanExpand); }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("nestMessages", nestMessages)
            .add("conversationId", conversationId)
            .add("needCanExpand", needCanExpand);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
