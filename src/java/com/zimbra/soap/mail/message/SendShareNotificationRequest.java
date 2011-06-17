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
import com.zimbra.soap.mail.type.SendShareNotificationSpec;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_SEND_SHARE_NOTIFICATION_REQUEST)
public class SendShareNotificationRequest {

    @XmlElement(name=MailConstants.E_SHARE /* share */, required=true)
    private final SendShareNotificationSpec share;

    @XmlElement(name=MailConstants.E_NOTES /* notes */, required=false)
    private String notes;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private SendShareNotificationRequest() {
        this((SendShareNotificationSpec) null);
    }

    public SendShareNotificationRequest(SendShareNotificationSpec share) {
        this.share = share;
    }

    public void setNotes(String notes) { this.notes = notes; }
    public SendShareNotificationSpec getShare() { return share; }
    public String getNotes() { return notes; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("share", share)
            .add("notes", notes);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
