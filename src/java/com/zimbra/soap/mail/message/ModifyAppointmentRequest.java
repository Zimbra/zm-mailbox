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
import com.zimbra.soap.mail.type.CalItemRequestBase;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_MODIFY_APPOINTMENT_REQUEST)
public class ModifyAppointmentRequest extends CalItemRequestBase {

    @XmlAttribute(name=MailConstants.A_ID /* id */, required=false)
    private String id;

    @XmlAttribute(name=MailConstants.A_CAL_COMP /* comp */, required=false)
    private Integer numComponents;

    @XmlAttribute(name=MailConstants.A_MODIFIED_SEQUENCE /* ms */,
                        required=false)
    private Integer modifiedSequence;

    public ModifyAppointmentRequest() {
    }

    public void setId(String id) { this.id = id; }
    public void setNumComponents(Integer numComponents) {
        this.numComponents = numComponents;
    }
    public void setModifiedSequence(Integer modifiedSequence) {
        this.modifiedSequence = modifiedSequence;
    }
    public String getId() { return id; }
    public Integer getNumComponents() { return numComponents; }
    public Integer getModifiedSequence() { return modifiedSequence; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("id", id)
            .add("numComponents", numComponents)
            .add("modifiedSequence", modifiedSequence);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
