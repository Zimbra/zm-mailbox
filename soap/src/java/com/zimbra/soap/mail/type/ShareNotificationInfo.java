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

package com.zimbra.soap.mail.type;

import com.google.common.base.MoreObjects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.json.jackson.annotate.ZimbraUniqueElement;

@XmlAccessorType(XmlAccessType.NONE)
public class ShareNotificationInfo {

    /**
     * @zm-api-field-tag status-new|seen
     * @zm-api-field-description Status - "new" if the message is unread or "seen" if the message is read.
     */
    @XmlAttribute(name=MailConstants.A_STATUS /* status */, required=true)
    private final String status;

    /**
     * @zm-api-field-tag notification-item-id
     * @zm-api-field-description The item ID of the share notification message.
     * The message must be in the Inbox folder.
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    /**
     * @zm-api-field-tag date
     * @zm-api-field-description Date
     */
    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=true)
    private final long date;

    /**
     * @zm-api-field-description Grantor information
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_GRANTOR /* grantor */, required=true)
    private final Grantor grantor;

    /**
     * @zm-api-field-description Link information
     */
    @ZimbraUniqueElement
    @XmlElement(name=MailConstants.E_MOUNT /* link */, required=true)
    private final LinkInfo link;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ShareNotificationInfo() {
        this((String) null, (String) null, -1L, (Grantor) null, (LinkInfo) null);
    }

    public ShareNotificationInfo(String status, String id, long date, Grantor grantor, LinkInfo link) {
        this.status = status;
        this.id = id;
        this.date = date;
        this.grantor = grantor;
        this.link = link;
    }

    public String getStatus() { return status; }
    public String getId() { return id; }
    public long getDate() { return date; }
    public Grantor getGrantor() { return grantor; }
    public LinkInfo getLink() { return link; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("status", status)
            .add("id", id)
            .add("date", date)
            .add("grantor", grantor)
            .add("link", link);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
