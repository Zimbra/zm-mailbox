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
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

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
import com.zimbra.soap.mail.type.SyncDeletedInfo;
import com.zimbra.soap.mail.type.TagInfo;
import com.zimbra.soap.mail.type.TaskItemInfo;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_SYNC_RESPONSE)
@XmlType(propOrder = {"deleted", "items"})
public class SyncResponse {

    @XmlAttribute(name=MailConstants.A_CHANGE_DATE, required=true)
    private final long changeDate;

    @XmlAttribute(name=MailConstants.A_TOKEN, required=false)
    private String token;

    @XmlAttribute(name=MailConstants.A_SIZE, required=false)
    private Long size;

    @XmlAttribute(name=MailConstants.A_QUERY_MORE, required=false)
    private Boolean more;

    @XmlElement(name=MailConstants.E_DELETED, required=false)
    private SyncDeletedInfo deleted;

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
    private List<Object> items = Lists.newArrayList();

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SyncResponse() {
        this(-1L);
    }

    public SyncResponse(long changeDate) {
        this.changeDate = changeDate;
    }

    public void setToken(String token) { this.token = token; }
    public void setSize(Long size) { this.size = size; }
    public void setMore(Boolean more) { this.more = more; }
    public void setDeleted(SyncDeletedInfo deleted) { this.deleted = deleted; }
    public void setItems(Iterable <Object> items) {
        this.items.clear();
        if (items != null) {
            Iterables.addAll(this.items,items);
        }
    }

    public SyncResponse addItem(Object item) {
        this.items.add(item);
        return this;
    }

    public long getChangeDate() { return changeDate; }
    public String getToken() { return token; }
    public Long getSize() { return size; }
    public Boolean getMore() { return more; }
    public SyncDeletedInfo getDeleted() { return deleted; }
    public List<Object> getItems() {
        return Collections.unmodifiableList(items);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("changeDate", changeDate)
            .add("token", token)
            .add("size", size)
            .add("more", more)
            .add("deleted", deleted)
            .add("items", items)
            .toString();
    }
}
