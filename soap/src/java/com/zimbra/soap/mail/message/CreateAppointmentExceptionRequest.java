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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.mail.type.CalItemRequestBase;
import com.zimbra.soap.mail.type.Msg;

/**
 * @zm-api-command-auth-required true
 * @zm-api-command-admin-auth-required false
 * @zm-api-command-description Create Appointment Exception.
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_CREATE_APPOINTMENT_EXCEPTION_REQUEST)
public class CreateAppointmentExceptionRequest extends CalItemRequestBase {

    /**
     * @zm-api-field-tag id-default-invite
     * @zm-api-field-description ID of default invite
     */
    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    /**
     * @zm-api-field-tag comp-default-invite
     * @zm-api-field-description Component of default invite
     */
    @XmlAttribute(name=MailConstants.E_INVITE_COMPONENT /* comp */, required=false)
    private Integer numComponents;

    /**
     * @zm-api-field-tag change-sequence-of-fetched-series
     * @zm-api-field-description Change sequence of fetched series
     */
    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */, required=false)
    private Integer modifiedSequence;

    /**
     * @zm-api-field-tag revision-of-fetched-series
     * @zm-api-field-description Revision of fetched series
     */
    @XmlAttribute(name=MailConstants.A_REVISION /* rev */, required=false)
    private Integer revision;

    public CreateAppointmentExceptionRequest() {
    }

    public static CreateAppointmentExceptionRequest createForModseqRevIdCompMsg(
            Integer modSeq, Integer rev, String theId, Integer numComp, Msg msg) {
        CreateAppointmentExceptionRequest caer = new CreateAppointmentExceptionRequest();
        caer.setModifiedSequence(modSeq);
        caer.setRevision(rev);
        caer.setId(theId);
        caer.setNumComponents(numComp);
        caer.setMsg(msg);
        return caer;
    }

    public void setId(String id) { this.id = id; }
    public void setNumComponents(Integer numComponents) {
        this.numComponents = numComponents;
    }
    public void setModifiedSequence(Integer modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }
    public void setRevision(Integer revision) { this.revision = revision; }
    public String getId() { return id; }
    public Integer getNumComponents() { return numComponents; }
    public Integer getModifiedSequence() { return modifiedSequence; }
    public Integer getRevision() { return revision; }

    @Override
    public MoreObjects.ToStringHelper addToStringInfo(MoreObjects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("id", id)
            .add("numComponents", numComponents)
            .add("modifiedSequence", modifiedSequence)
            .add("revision", revision);
    }

    @Override
    public String toString() {
        return addToStringInfo(MoreObjects.toStringHelper(this)).toString();
    }
}
