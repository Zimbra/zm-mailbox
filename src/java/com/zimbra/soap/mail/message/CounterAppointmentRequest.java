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
import com.zimbra.soap.mail.type.Msg;

/**
 * @zm-api-command-description Propose a new time/location.  Sent by meeting attendee to organizer.
 * <br />
 * The syntax is very similar to CreateAppointmentRequest.
 * <br />
 * <br />
 * Should include an <b>&lt;inv></b> element which encodes an iCalendar COUNTER object
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name=MailConstants.E_COUNTER_APPOINTMENT_REQUEST)
public class CounterAppointmentRequest {

    /**
     * @zm-api-field-description Details of counter proposal.
     */
    @XmlElement(name=MailConstants.E_MSG /* m */, required=false)
    private Msg msg;

    public CounterAppointmentRequest() {
    }

    public void setMsg(Msg msg) { this.msg = msg; }
    public Msg getMsg() { return msg; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        return helper
            .add("msg", msg);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
