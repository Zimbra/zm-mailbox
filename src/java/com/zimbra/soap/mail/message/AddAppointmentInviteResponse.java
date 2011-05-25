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

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name=MailConstants.E_ADD_APPOINTMENT_INVITE_RESPONSE)
public class AddAppointmentInviteResponse {

    @XmlAttribute(name=MailConstants.A_CAL_ID /* calItemId */, required=false)
    private Integer calItemId;

    @XmlAttribute(name=MailConstants.A_CAL_INV_ID /* invId */, required=false)
    private Integer invId;

    @XmlAttribute(name=MailConstants.A_CAL_COMPONENT_NUM /* compNum */,
                    required=false)
    private Integer numComponents;

    public AddAppointmentInviteResponse() {
    }

    public void setCalItemId(Integer calItemId) { this.calItemId = calItemId; }
    public void setInvId(Integer invId) { this.invId = invId; }
    public void setNumComponents(Integer numComponents) {
        this.numComponents = numComponents;
    }
    public Integer getCalItemId() { return calItemId; }
    public Integer getInvId() { return invId; }
    public Integer getNumComponents() { return numComponents; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("calItemId", calItemId)
            .add("invId", invId)
            .add("numComponents", numComponents);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
