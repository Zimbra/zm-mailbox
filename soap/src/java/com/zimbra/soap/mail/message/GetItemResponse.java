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

@XmlRootElement(name=MailConstants.E_GET_ITEM_RESPONSE)
public class GetItemResponse {

    /**
     * @zm-api-field-description Item
     */
    @XmlElements({
        @XmlElement(name=MailConstants.E_FOLDER /* folder */, type=Folder.class),
        @XmlElement(name=MailConstants.E_TAG /* tag */, type=TagInfo.class),
        @XmlElement(name=MailConstants.E_NOTE /* note */, type=NoteInfo.class),
        @XmlElement(name=MailConstants.E_CONTACT /* cn */, type=ContactInfo.class),
        @XmlElement(name=MailConstants.E_APPOINTMENT /* appt */, type=CalendarItemInfo.class),
        @XmlElement(name=MailConstants.E_TASK /* task */, type=TaskItemInfo.class),
        @XmlElement(name=MailConstants.E_CONV /* c */, type=ConversationSummary.class),
        @XmlElement(name=MailConstants.E_WIKIWORD /* w */, type=CommonDocumentInfo.class),
        @XmlElement(name=MailConstants.E_DOC /* doc */, type=DocumentInfo.class),
        @XmlElement(name=MailConstants.E_MSG /* m */, type=MessageSummary.class),
        @XmlElement(name=MailConstants.E_CHAT /* chat */, type=ChatSummary.class)
    })
    private Object item;

    public GetItemResponse() {
    }

    public void setItem(Object item) { this.item = item; }
    public Object getItem() { return item; }

    public MoreObjects.ToStringHelper addToStringInfo(
                MoreObjects.ToStringHelper helper) {
        return helper
            .add("item", item);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this))
                .toString();
    }
}
