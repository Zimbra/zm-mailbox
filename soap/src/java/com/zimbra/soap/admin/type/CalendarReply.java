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

package com.zimbra.soap.admin.type;

import java.util.List;

import com.google.common.base.MoreObjects;
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("seq", seq)
            .add("date", date)
            .add("attendee", attendee)
            .add("sentBy", sentBy)
            .add("partStat", partStat)
            .toString();
    }
}
