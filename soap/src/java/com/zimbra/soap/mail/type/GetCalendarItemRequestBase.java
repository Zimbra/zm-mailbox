/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014, 2016 Synacor, Inc.
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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.type.ZmBoolean;

@XmlAccessorType(XmlAccessType.NONE)
public abstract class GetCalendarItemRequestBase {

    /**
     * @zm-api-field-tag return-mod-date
     * @zm-api-field-description Set this to return the modified date (md) on the appointment.
     */
    @XmlAttribute(name=MailConstants.A_SYNC /* sync */, required=false)
    private ZmBoolean sync;

    /**
     * @zm-api-field-tag include-mime-body-parts
     * @zm-api-field-description If set, MIME parts for body content are returned; <b>default unset</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_INCLUDE_CONTENT /* includeContent */, required=false)
    private ZmBoolean includeContent;

    /**
     * @zm-api-field-tag include-invites
     * @zm-api-field-description If set, information for each invite is included; <b>default set</b>
     */
    @XmlAttribute(name=MailConstants.A_CAL_INCLUDE_INVITES /* includeInvites */, required=false)
    private ZmBoolean includeInvites;
    /**
     * @zm-api-field-tag icalendar-uid
     * @zm-api-field-description iCalendar UID
     * Either id or uid should be specified, but not both
     */
    @XmlAttribute(name=MailConstants.A_UID /* uid */, required=false)
    private String uid;

    /**
     * @zm-api-field-tag appointment-id
     * @zm-api-field-description Appointment ID.
     * Either id or uid should be specified, but not both
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    public void setSync(Boolean sync) { this.sync = ZmBoolean.fromBool(sync); }
    public void setIncludeContent(Boolean includeContent) { this.includeContent = ZmBoolean.fromBool(includeContent); }
    public void setIncludeInvites(Boolean includeInvites) { this.includeInvites = ZmBoolean.fromBool(includeInvites); }
    public void setUid(String uid) { this.uid = uid; }
    public void setId(String id) { this.id = id; }
    public Boolean getSync() { return ZmBoolean.toBool(sync); }
    public Boolean getIncludeContent() { return ZmBoolean.toBool(includeContent); }
    public Boolean getIncludeInvites() { return ZmBoolean.toBool(includeInvites); }
    public String getUid() { return uid; }
    public String getId() { return id; }

    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        return helper
            .add("sync", sync)
            .add("includeContent", includeContent)
            .add("includeInvites", includeContent)
            .add("uid", uid)
            .add("id", id);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
