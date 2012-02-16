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

/**
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
     * @zm-api-field-tag
     * @zm-api-field-description If 'needExp' is set in the request, and when when the 'fetch' attr is set to a
     * message ID, two additional flags may be included in <b>&lt;e></b> elements for the message:
     * <ul>
     * <li> isGroup - set if the email address is a group
     * <li> exp - present only when isGroup="1".
     *      <br />
     *      Set if the authed user can (has permission to) expand members in this group
     *      <br />
     *      Unset if the authed user does not have permission to expand group members
     * </ul>
     */
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

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("nestMessages", nestMessages)
            .add("conversationId", conversationId)
            .add("needCanExpand", needCanExpand);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
