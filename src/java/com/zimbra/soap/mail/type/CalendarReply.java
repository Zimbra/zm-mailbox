/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013 Zimbra Software, LLC.
 *
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.soap.mail.type;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.CalendarReplyInterface;

@XmlAccessorType(XmlAccessType.NONE)
public class CalendarReply
extends RecurIdInfo
implements CalendarReplyInterface {

    // zdsync
    /**
     * @zm-api-field-tag sequence-num
     * @zm-api-field-description Sequence number
     */
    @XmlAttribute(name=MailConstants.A_SEQ /* seq */, required=true)
    private int seq;

    /**
     * @zm-api-field-tag dtstamp
     * @zm-api-field-description DTSTAMP date in milliseconds
     */
    @XmlAttribute(name=MailConstants.A_DATE /* d */, required=true)
    private long date;

    /**
     * @zm-api-field-tag attendee
     * @zm-api-field-description Attendee address
     */
    @XmlAttribute(name=MailConstants.A_CAL_ATTENDEE /* at */, required=true)
    private String attendee;

    /**
     * @zm-api-field-tag sent-by
     * @zm-api-field-description iCalendar SENT-BY
     */
    @XmlAttribute(name=MailConstants.A_CAL_SENTBY /* sentBy */, required=false)
    private String sentBy;

    /**
     * @zm-api-field-tag participation-status
     * @zm-api-field-description iCalendar PTST (Participation status)
     * <br />
     * Valid values: <b>NE|AC|TE|DE|DG|CO|IN|WE|DF</b>
     * <br />
     * Meanings:
     * <br />
     * "NE"eds-action, "TE"ntative, "AC"cept, "DE"clined, "DG" (delegated), "CO"mpleted (todo), "IN"-process (todo),
     * "WA"iting (custom value only for todo), "DF" (deferred; custom value only for todo)
     */
    @XmlAttribute(name=MailConstants.A_CAL_PARTSTAT /* ptst */, required=false)
    private String partStat;

    /**
     * no-argument constructor wanted by JAXB
     */
    @SuppressWarnings("unused")
    private CalendarReply() {
    }

    public CalendarReply(int seq, long date, String attendee) {
        this.seq = seq;
        this.date = date;
        this.attendee = attendee;
    }

    public static CalendarReplyInterface createFromSeqDateAttendee(int seq, long date, String attendee) {
        return new CalendarReply(seq, date, attendee);
    }

    @Override
    public void setSentBy(String sentBy) { this.sentBy = sentBy; }
    @Override
    public void setPartStat(String partStat) { this.partStat = partStat; }
    @Override
    public int getSeq() { return seq; }
    @Override
    public long getDate() { return date; }
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

    /** Done like this rather than using JAXB for performance reasons */
    public static void encodeCalendarReplyList(Element parent, List<CalendarReply> replies) {
        if (replies.isEmpty()) {
            return;
        }
        Element repliesElt = parent.addNonUniqueElement(MailConstants.E_CAL_REPLIES);
        for (CalendarReply repInfo : replies) {
            repInfo.toElement(repliesElt);
        }
    }

    /** Done like this rather than using JAXB for performance reasons */
    public Element toElement(Element parent) {
        Element curElt = parent.addNonUniqueElement(MailConstants.E_CAL_REPLY);
        curElt.addAttribute(MailConstants.A_SEQ, getSeq()); //zdsync
        curElt.addAttribute(MailConstants.A_DATE, getDate());
        curElt.addAttribute(MailConstants.A_CAL_ATTENDEE, getAttendee());
        if (!Strings.isNullOrEmpty(getSentBy())) {
            curElt.addAttribute(MailConstants.A_CAL_SENTBY, getSentBy());
        }
        if (!Strings.isNullOrEmpty(getPartStat())) {
            curElt.addAttribute(MailConstants.A_CAL_PARTSTAT, getPartStat());
        }
        curElt.addAttribute(MailConstants.A_CAL_RECURRENCE_RANGE_TYPE, getRecurrenceRangeType());
        if (!Strings.isNullOrEmpty(getRecurrenceId())) {
            curElt.addAttribute(MailConstants.A_CAL_RECURRENCE_ID, getRecurrenceId());
        }
        if (!Strings.isNullOrEmpty(getTimezone())) {
            curElt.addAttribute(MailConstants.A_CAL_TIMEZONE, getTimezone());
        }
        if (!Strings.isNullOrEmpty(getRecurIdZ())) {
            curElt.addAttribute(MailConstants.A_CAL_RECURRENCE_ID_Z, getRecurIdZ());
        }
        return curElt;
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
