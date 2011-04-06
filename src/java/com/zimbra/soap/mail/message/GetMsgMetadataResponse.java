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
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.ChatMessageInfo;
import com.zimbra.soap.mail.type.MessageInfo;
import com.zimbra.soap.mail.type.MessageSummaryInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_GET_MSG_METADATA_RESPONSE)
public class GetMsgMetadataResponse {

    @XmlElements({
        @XmlElement(name=MailConstants.E_CHAT,
            type=ChatMessageInfo.class),
        @XmlElement(name=MailConstants.E_MSG,
            type=MessageInfo.class)
    })
    private List<MessageSummaryInfo> messages = Lists.newArrayList();

    public GetMsgMetadataResponse() {
    }

    public void setMessages(Iterable <MessageSummaryInfo> messages) {
        this.messages.clear();
        if (messages != null) {
            Iterables.addAll(this.messages,messages);
        }
    }

    public GetMsgMetadataResponse addMessag(MessageSummaryInfo messag) {
        this.messages.add(messag);
        return this;
    }

    public List<MessageSummaryInfo> getMessages() {
        return Collections.unmodifiableList(messages);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("messages", messages)
            .toString();
    }
}
