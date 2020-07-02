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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import com.google.common.base.MoreObjects;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalItemRequestBase;
import com.zimbra.soap.mail.type.Msg;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Modify an appointment, or if the appointment is a recurrence then modify the "default"
 * invites. That is, all instances that do not have exceptions.
 * <br />
 * <br />
 * If the appointment has a <b>&lt;recur></b>, then the following caveats are worth mentioning:
 * <br />
 * <b>If any of: START, DURATION, END or RECUR change, then all exceptions are implicitly canceled!</b>
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_MODIFY_APPOINTMENT_REQUEST)
public class ModifyAppointmentRequest extends CalItemRequestBase {

    /**
     * @zm-api-field-tag invite-id-of-default-invite
     * @zm-api-field-description Invite ID of default invite
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag default-invite-component-num
     * @zm-api-field-description Component number of default component
     */
    @XmlAttribute(name=MailConstants.A_CAL_COMP /* comp */, required=false)
    private Integer componentNum;

    /**
     * @zm-api-field-tag changed-seq-of-fetched-version
     * @zm-api-field-description Changed sequence of fetched version.
     * <br />
     * Used for conflict detection.  By setting this, the request indicates which version of the appointment it is
     * attempting to modify.  If the appointment was updated on the server between the fetch and modify, an
     * <b>INVITE_OUT_OF_DATE exception</b> will be thrown.
     */
    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=false)
    private Integer modifiedSequence;

    /**
     * @zm-api-field-tag
     * @zm-api-field-description
     */
    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revision;

    public ModifyAppointmentRequest() {
    }

    public static ModifyAppointmentRequest createForIdModseqRevCompnumMsg(
            String id, Integer modSeq, Integer rev, Integer compNum, Msg msg) {

        ModifyAppointmentRequest mar = new ModifyAppointmentRequest();
        mar.setId(id);
        mar.setModifiedSequence(modSeq);
        mar.setRevision(rev);
        mar.setComponentNum(compNum);
        mar.setMsg(msg);
        return mar;
    }

    public void setId(String id) { this.id = id; }
    public void setComponentNum(Integer componentNum) { this.componentNum = componentNum; }
    public void setModifiedSequence(Integer modifiedSequence) { this.modifiedSequence = modifiedSequence; }
    public void setRevision(Integer revision) { this.revision = revision; }
    public String getId() { return id; }
    public Integer getComponentNum() { return componentNum; }
    public Integer getModifiedSequence() { return modifiedSequence; }
    public Integer getRevision() { return revision; }

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("id", id)
            .add("componentNum", componentNum)
            .add("modifiedSequence", modifiedSequence)
            .add("revision", revision);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
