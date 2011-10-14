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

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.CalendarReplyInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class CalendarReply
extends RecurIdInfo
implements CalendarReplyInterface {

    // zdsync
    @XmlAttribute(name=MailConstants.A_SEQ /* seq */, required=true)
    private final String seq;

    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=true)
    private final String date;

    @XmlAttribute(name=MailConstants.A_CAL_ATTENDEE /* at */, required=true)
    private final String attendee;

    @XmlAttribute(name=MailConstants.A_CAL_SENTBY /* sentBy */, required=false)
    private String sentBy;

    @XmlAttribute(name=MailConstants.A_CAL_PARTSTAT /* ptst */, required=false)
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

    @Override
    public CalendarReplyInterface createFromSeqDateAttendee(String seq,
            String date, String attendee) {
        return new CalendarReply(seq, date, attendee);
    }

    @Override
    public void setSentBy(String sentBy) { this.sentBy = sentBy; }
    @Override
    public void setPartStat(String partStat) { this.partStat = partStat; }
    @Override
    public String getSeq() { return seq; }
    @Override
    public String getDate() { return date; }
    @Override
    public String getAttendee() { return attendee; }
    @Override
    public String getSentBy() { return sentBy; }
    @Override
    public String getPartStat() { return partStat; }

    public static Iterable <CalendarReply> fromInterfaces(
                    Iterable <CalendarReplyInterface> params) {
        if (params == null)
            return null;
        List <CalendarReply> newList = Lists.newArrayList();
        for (CalendarReplyInterface param : params) {
            newList.add((CalendarReply) param);
        }
        return newList;
    }

    public static List <CalendarReplyInterface> toInterfaces(
                    Iterable <CalendarReply> params) {
        if (params == null)
            return null;
        List <CalendarReplyInterface> newList = Lists.newArrayList();
        Iterables.addAll(newList, params);
        return newList;
    }

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
