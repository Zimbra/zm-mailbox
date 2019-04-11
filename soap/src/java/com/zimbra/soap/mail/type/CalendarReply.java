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

package com.zimbra.soap.mail.type;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.zimbra.common.gql.GqlConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.soap.base.CalendarReplyInterface;

import io.leangen.graphql.annotations.GraphQLIgnore;
import io.leangen.graphql.annotations.GraphQLInputField;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.annotations.types.GraphQLType;

@XmlAccessorType(XmlAccessType.NONE)
@GraphQLType(name=GqlConstants.CLASS_CALENDAR_REPLY, description="Calendar reply information")
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

    public CalendarReply(
        @GraphQLNonNull @GraphQLInputField int seq,
        @GraphQLNonNull @GraphQLInputField long date,
        @GraphQLNonNull @GraphQLInputField String attendee) {
        this.seq = seq;
        this.date = date;
        this.attendee = attendee;
    }

    public static CalendarReplyInterface createFromSeqDateAttendee(int seq, long date, String attendee) {
        return new CalendarReply(seq, date, attendee);
    }

    @Override
    @GraphQLInputField(name=GqlConstants.SENT_BY, description="iCalendar SENT-BY")
    public void setSentBy(String sentBy) { this.sentBy = sentBy; }
    @Override
    @GraphQLInputField(name=GqlConstants.PARTICIPATION_STATUS, description="iCalendar PTST (Participation status)\n"
        + "* \"NE\"eds-action\n "
        + "* \"TE\"ntative\n "
        + "* \"AC\"cept\n "
        + "* \"DE\"clined\n "
        + "* \"DG\" (delegated)\n "
        + "* \"CO\"mpleted (todo)\n "
        + "* \"IN\"-process (todo)\n "
        + "* \"WA\"iting (custom value only for todo)\n "
        + "* \"DF\" (deferred; custom value only for todo)")
    public void setPartStat(String partStat) { this.partStat = partStat; }
    @Override
    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.SEQUENCE, description="Sequence number")
    public int getSeq() { return seq; }
    @Override
    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.DATE, description="DTSTAMP date in milliseconds")
    public long getDate() { return date; }
    @Override
    @GraphQLNonNull
    @GraphQLQuery(name=GqlConstants.ATTENDEE, description="Attendee address")
    public String getAttendee() { return attendee; }
    @Override
    @GraphQLQuery(name=GqlConstants.SENT_BY, description="iCalendar SENT-BY")
    public String getSentBy() { return sentBy; }
    @Override
    @GraphQLQuery(name=GqlConstants.PARTICIPATION_STATUS, description="iCalendar PTST (Participation status)\n"
        + "* \"NE\"eds-action\n "
        + "* \"TE\"ntative\n "
        + "* \"AC\"cept\n "
        + "* \"DE\"clined\n "
        + "* \"DG\" (delegated)\n "
        + "* \"CO\"mpleted (todo)\n "
        + "* \"IN\"-process (todo)\n "
        + "* \"WA\"iting (custom value only for todo)\n "
        + "* \"DF\" (deferred; custom value only for todo)")
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

    @GraphQLIgnore
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
        return MoreObjects.toStringHelper(this)
            .add("seq", seq)
            .add("date", date)
            .add("attendee", attendee)
            .add("sentBy", sentBy)
            .add("partStat", partStat)
            .toString();
    }
}
