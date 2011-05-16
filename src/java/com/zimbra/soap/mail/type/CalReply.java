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

import com.zimbra.common.soap.MailConstants;

@XmlAccessorType(XmlAccessType.FIELD)
public class CalReply extends RecurIdInfo {

    @XmlAttribute(name=MailConstants.A_CAL_ATTENDEE, required=true)
    private final String attendee;

    @XmlAttribute(name=MailConstants.A_CAL_SENTBY, required=false)
    private final String sentBy;

    @XmlAttribute(name=MailConstants.A_CAL_PARTSTAT, required=false)
    private final String partStat;

    @XmlAttribute(name=MailConstants.A_SEQ, required=true)
    private final int sequence;

    @XmlAttribute(name=MailConstants.A_DATE, required=true)
    private final int date;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CalReply() {
        this((String) null, (String) null, (String) null, -1, -1);
    }

    public CalReply(String attendee, String sentBy, String partStat,
            int sequence, int date) {
        this.attendee = attendee;
        this.sentBy = sentBy;
        this.partStat = partStat;
        this.sequence = sequence;
        this.date = date;
    }

    public String getAttendee() { return attendee; }
    public String getSentBy() { return sentBy; }
    public String getPartStat() { return partStat; }
    public int getSequence() { return sequence; }
    public int getDate() { return date; }

    public Objects.ToStringHelper addToStringInfo(
                Objects.ToStringHelper helper) {
        helper = super.addToStringInfo(helper);
        return helper
            .add("attendee", attendee)
            .add("sentBy", sentBy)
            .add("partStat", partStat)
            .add("sequence", sequence)
            .add("date", date);
    }

    @Override
    public String toString() {
        return addToStringInfo(Objects.toStringHelper(this))
                .toString();
    }
}
