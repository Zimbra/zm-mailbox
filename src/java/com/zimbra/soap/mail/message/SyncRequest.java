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
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-description Sync
 * <ul>
 * <li> Ssync on other mailbox is done via specifying target account in SOAP header
 * <li> If we're delta syncing on another user's mailbox and any folders have changed:
 *      <ul>
 *      <li> If there are now no visible folders, you'll get an empty <b>&lt;folder/></b> element
 *      <li> If there are any visible folders, you'll get the full visible folder hierarchy
 *      </ul>
 * <li> If a {root-folder-id} other than the mailbox root (folder 1) is requested or if not all folders are visible
 *      when syncing to another user's mailbox, all changed items in other folders are presented as deletes
 * <li> If the response is a mail.MUST_RESYNC fault, client has fallen too far out of date and must re-initial sync
 * </ul>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_SYNC_REQUEST)
public class SyncRequest {

    /**
     * @zm-api-field-tag sync-token
     * @zm-api-field-description Token - not provided for initial sync
     */
    @XmlAttribute(name=MailConstants.A_TOKEN /* token */, required=false)
    private String token;

    /**
     * @zm-api-field-tag earliest-calendar-date
     * @zm-api-field-description Earliest Calendar date.  If present, omit all appointments and tasks that don't have
     * a recurrence ending after that time (specified in ms)
     */
    @XmlAttribute(name=MailConstants.A_CALENDAR_CUTOFF /* calCutoff */, required=false)
    private Long calendarCutoff;

    /**
     * @zm-api-field-tag root-folder-id
     * @zm-api-field-description Root folder ID.  If present, we start sync there rather than at folder 11
     */
    @XmlAttribute(name=MailConstants.A_FOLDER /* l */, required=false)
    private String folderId;

    /**
     * @zm-api-field-tag break-down-deletes-by-item-type
     * @zm-api-field-description If specified and set, deletes are also broken down by item type
     */
    @XmlAttribute(name=MailConstants.A_TYPED_DELETES /* typed */, required=false)
    private ZmBoolean typedDeletes;

    public SyncRequest() {
    }

    public void setToken(String token) { this.token = token; }
    public void setCalendarCutoff(Long calendarCutoff) {
        this.calendarCutoff = calendarCutoff;
    }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setTypedDeletes(Boolean typedDeletes) {
        this.typedDeletes = ZmBoolean.fromBool(typedDeletes);
    }
    public String getToken() { return token; }
    public Long getCalendarCutoff() { return calendarCutoff; }
    public String getFolderId() { return folderId; }
    public Boolean getTypedDeletes() { return ZmBoolean.toBool(typedDeletes); }

    public Objects.ToStringHelper addToStringInfo(Objects.ToStringHelper helper) {
        return helper
            .add("token", token)
            .add("calendarCutoff", calendarCutoff)
            .add("folderId", folderId)
            .add("typedDeletes", typedDeletes);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this)).toString();
    }
}
