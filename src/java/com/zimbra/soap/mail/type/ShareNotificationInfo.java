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

package com.zimbra.soap.mail.type;

import com.google.common.base.Objects;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = {})
public class ShareNotificationInfo {

    @XmlAttribute(name=MailConstants.A_STATUS /* status */, required=true)
    private final String status;

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=true)
    private final String id;

    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=true)
    private final long date;

    @XmlElement(name=MailConstants.E_GRANTOR /* grantor */, required=true)
    private final Grantor grantor;

    @XmlElement(name=MailConstants.E_MOUNT /* link */, required=true)
    private final LinkInfo link;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private ShareNotificationInfo() {
        this((String) null, (String) null, -1L, (Grantor) null,
                (LinkInfo) null);
    }

    public ShareNotificationInfo(String status, String id, long date,
                    Grantor grantor, LinkInfo link) {
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

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("status", status)
            .add("id", id)
            .add("date", date)
            .add("grantor", grantor)
            .add("link", link);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
