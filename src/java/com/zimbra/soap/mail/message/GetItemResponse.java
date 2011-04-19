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
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalendarItemInfo;
import com.zimbra.soap.mail.type.ChatSummary;
import com.zimbra.soap.mail.type.CommonDocumentInfo;
import com.zimbra.soap.mail.type.ContactInfo;
import com.zimbra.soap.mail.type.ConversationSummary;
import com.zimbra.soap.mail.type.DocumentInfo;
import com.zimbra.soap.mail.type.Folder;
import com.zimbra.soap.mail.type.MessageSummary;
import com.zimbra.soap.mail.type.NoteInfo;
import com.zimbra.soap.mail.type.TagInfo;
import com.zimbra.soap.mail.type.TaskItemInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_GET_ITEM_RESPONSE)
public class GetItemResponse {

    // TODO:Create an interface instead of using Objects?
    @XmlElements({
        @XmlElement(name=MailConstants.E_FOLDER,
            type=Folder.class),
        @XmlElement(name=MailConstants.E_TAG,
            type=TagInfo.class),
        @XmlElement(name=MailConstants.E_NOTE,
            type=NoteInfo.class),
        @XmlElement(name=MailConstants.E_CONTACT,
            type=ContactInfo.class),
        @XmlElement(name=MailConstants.E_APPOINTMENT,
            type=CalendarItemInfo.class),
        @XmlElement(name=MailConstants.E_TASK,
            type=TaskItemInfo.class),
        @XmlElement(name=MailConstants.E_CONV,
            type=ConversationSummary.class),
        @XmlElement(name=MailConstants.E_WIKIWORD,
            type=CommonDocumentInfo.class),
        @XmlElement(name=MailConstants.E_DOC,
            type=DocumentInfo.class),
        @XmlElement(name=MailConstants.E_MSG,
            type=MessageSummary.class),
        @XmlElement(name=MailConstants.E_CHAT,
            type=ChatSummary.class)
    })
    private Object item;

    public GetItemResponse() {
    }

    public void setItem(Object item) { this.item = item; }
    public Object getItem() { return item; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("item", item)
            .toString();
    }
}
