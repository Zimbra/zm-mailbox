/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.soap.mail.message;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.MailSearchParams;
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Search a conversation
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SEARCH_CONV_REQUEST)
public class SearchConvRequest extends MailSearchParams {

    /**
     * @zm-api-field-tag conversation-id
     * @zm-api-field-description The ID of the conversation to search within.  <b>REQUIRED</b>.
     */
    @XmlAttribute(name=MailConstants.A_CONV_ID /* cid */, required=true)
    private final String conversationId;

    /**
     * @zm-api-field-tag nest-messages-inside-conv
     * @zm-api-field-description If set then the response will contain a top level <b>&lt;c</b> element representing
     * the conversation with child <b>&lt;m></b> elements representing messages in the conversation.
     * <br />
     * If unset, no <b>&lt;c></b> element is included - <b>&lt;m></b> elements will be top level elements.
     */
    @XmlAttribute(name=MailConstants.A_NEST_MESSAGES /* nest */, required=false)
    private ZmBoolean nestMessages;

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
    public Boolean getNestMessages() { return ZmBoolean.toBool(nestMessages); }
    public String getConversationId() { return conversationId; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("nestMessages", nestMessages)
            .add("conversationId", conversationId);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
