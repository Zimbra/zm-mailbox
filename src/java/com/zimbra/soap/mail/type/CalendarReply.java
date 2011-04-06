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
public class CalendarReply extends RecurIdInfo {

    // zdsync
    @XmlAttribute(name=MailConstants.A_SEQ, required=true)
    private final String seq;

    @XmlAttribute(name=MailConstants.A_DATE, required=true)
    private final String date;

    @XmlAttribute(name=MailConstants.A_CAL_ATTENDEE, required=true)
    private final String attendee;

    @XmlAttribute(name=MailConstants.A_CAL_SENTBY, required=false)
    private String sentBy;

    @XmlAttribute(name=MailConstants.A_CAL_PARTSTAT, required=false)
    private String partStat;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CalendarReply() {
        this((String) null, (String) null, (String) null);
    }

    public CalendarReply(String seq, String date, String attendee) {
        this.seq = seq;
        this.date = date;
        this.attendee = attendee;
    }

    public void setSentBy(String sentBy) { this.sentBy = sentBy; }
    public void setPartStat(String partStat) { this.partStat = partStat; }
    public String getSeq() { return seq; }
    public String getDate() { return date; }
    public String getAttendee() { return attendee; }
    public String getSentBy() { return sentBy; }
    public String getPartStat() { return partStat; }

    @Override
    public String toString() {
        return Objects.toStringHelper(this)
            .add("seq", seq)
            .add("date", date)
            .add("attendee", attendee)
            .add("sentBy", sentBy)
            .add("partStat", partStat)
            .toString();
    }
}
