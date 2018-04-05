/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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
import com.zimbra.soap.type.ZmBoolean;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
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
     * @zm-api-field-description Earliest Calendar date. If present, omit all appointments and tasks that don't have
     * a recurrence ending after that time (specified in ms)
     */
    @XmlAttribute(name=MailConstants.A_CALENDAR_CUTOFF /* calCutoff */, required=false)
    private Long calendarCutoff;

    /**
     * @zm-api-field-tag earliest-message-date
     * @zm-api-field-description Earliest Message date.  If present, omit all Messages and conversations that
     *  are older than time (specified in seconds) "Note:value in seconds, unlike calCutoff which is in milliseconds"
     */
    @XmlAttribute(name=MailConstants.A_MSG_CUTOFF /* msgCutoff */, required=false)
    private Integer msgCutoff;

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

    /**
     * @zm-api-field-tag delete-limit
     * @zm-api-field-description maximum number of deleted item ids returned in a response.
     */
    @XmlAttribute(name=MailConstants.A_DELETE_LIMIT /* deleteLimit */, required=false)
    private int deleteLimit;

    /**
     * @zm-api-field-tag change-limit
     * @zm-api-field-description maximum number of modified item ids returned in a response.
     */
    @XmlAttribute(name=MailConstants.A_CHANGE_LIMIT /* changeLimit */, required=false)
    private int changeLimit;

    public SyncRequest() {
    }

    public void setToken(String token) { this.token = token; }
    public void setCalendarCutoff(Long calendarCutoff) {
        this.calendarCutoff = calendarCutoff;
    }
    public void setMsgCutoff(Integer msgCutoff) {
        this.msgCutoff = msgCutoff;
    }
    public void setFolderId(String folderId) { this.folderId = folderId; }
    public void setTypedDeletes(Boolean typedDeletes) {
        this.typedDeletes = ZmBoolean.fromBool(typedDeletes);
    }
    public void setDeleteLimit(int deleteLimit) {
        this.deleteLimit = deleteLimit;
    }
    public void setChangeLimit(int changeLimit) {
        this.changeLimit = changeLimit;
    }
    public String getToken() { return token; }
    public Long getCalendarCutoff() { return calendarCutoff; }
    public Integer getMsgCutoff() { return msgCutoff; }
    public String getFolderId() { return folderId; }
    public Boolean getTypedDeletes() { return ZmBoolean.toBool(typedDeletes); }
    public int getDeleteLimit() { return deleteLimit; }
    public int getChangeLimit() { return changeLimit; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("token", token)
            .add("calendarCutoff", calendarCutoff)
            .add("msgCutOff", msgCutoff)
            .add("folderId", folderId)
            .add("typedDeletes", typedDeletes)
            .add("deleteLimit", deleteLimit)
            .add("changeLimit", changeLimit);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
