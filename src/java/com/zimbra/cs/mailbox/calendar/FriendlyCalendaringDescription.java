package com.zimbra.cs.mailbox.calendar;

/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011 Zimbra, Inc.
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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.Recurrence;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZRecur;
import com.zimbra.cs.mailbox.calendar.Recurrence.IRecurrence;
import com.zimbra.cs.mailbox.calendar.ZRecur.ZWeekDayNum;

/**
 * Class FriendlyCalendaringDescription represents the text used in text/plain
 * and text/html alternative descriptions of a calendaring message
 *
 * @author gren
 */
public class FriendlyCalendaringDescription {
    static Log sLog = ZimbraLog.calendar;
    private boolean mDoneGenerate;
    private boolean mDefinitelyModified;
    private StringBuilder mPlainText;
    private StringBuilder mHtml;
    private Account mAccount;
    private Locale mLc;
    private List<Invite> mInviteComponents;

    public FriendlyCalendaringDescription(List <Invite> inviteComponents, Account account) throws ServiceException {
        mDoneGenerate = false;
        mDefinitelyModified = false;
        mPlainText= null;
        mHtml= null;
        mAccount = account;
        mLc = mAccount.getLocale();
        mInviteComponents = inviteComponents;
    }

    public void setDefinitelyModified(boolean val) {
        mDefinitelyModified = val;
    }

    public String getAsPlainText() {
        generateDescriptions();
        return mPlainText == null ? "" : mPlainText.toString();
    }
    public String getAsHtml() {
        generateDescriptions();
        return mHtml == null ? "" : mHtml.toString();
    }

    /**
     * Build up <code>mPlainText</code> and <code>mHtml</code> from scratch
     */
    private void generateDescriptions() {
        if (mDoneGenerate)
            return;
        mDoneGenerate = true;
        if ((mInviteComponents == null) || (mInviteComponents.size() == 0))
            return;
        mPlainText = new StringBuilder();
        mHtml = new StringBuilder("<html>\n<body>\n");
        mHtml.append("<div style=\"font-family: monospace; font-size: 14px\">\n");
        Invite invite = mInviteComponents.get(0);
        if (invite.isEvent())
            addEventDetails(invite);
        String description = null;
        try {
            description = invite.getDescription();
        } catch (ServiceException e) {
            sLog.debug("Resetting descriptions due to ServiceException", e);
            description = null;
        }
        if (description != null) {
            mPlainText.append(description).append("\n");
            mHtml.append("<pre>").append(description).append("</pre>\n");
        }
        mHtml.append("</div>\n");
        mHtml.append("</body>\n</html>\n");
    }

    private void addEventDetails(Invite invite) {
        if (!invite.isEvent())
            return;
        int origPlainLen = mPlainText.length();
        try {
            String uid = invite.getUid();
            String method = invite.getMethod();
            List<ZAttendee> attendees = invite.getAttendees();
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);
            // Would be nice to be able to discover what modifications were made by the CalDAV
            // client.  Currently, this information is not stored in the Calendar.
            // Might be able to extract the information from messages in the Sent folder at
            // some point in the future?
            CalendarItem calItem = mbox.getCalendarItemByUid(uid);
            if (calItem != null) {
                Invite [] calInvites = calItem.getInvites();
                if ((calInvites != null) && (calInvites.length > 1))
                    mDefinitelyModified = true; // At least 1 exception, so this is not a new item
            }
            L10nUtil.MsgKey hdrKey = null;
            if (method.equals("REQUEST")) {
                if (invite.getRecurId() != null)
                    hdrKey = L10nUtil.MsgKey.zsApptInstanceModified;
                else if (mDefinitelyModified)
                    hdrKey = L10nUtil.MsgKey.zsApptModified;
                else
                    hdrKey = L10nUtil.MsgKey.zsApptNew;
            } else if (method.equals("CANCEL")) {
                if (invite.getRecurId() != null)
                    hdrKey = L10nUtil.MsgKey.calendarCancelAppointmentInstance;
                else
                    hdrKey = L10nUtil.MsgKey.calendarCancelAppointment;
            } else if (method.equals("REPLY")) {
                if ((attendees != null) && !attendees.isEmpty()) {
                    ZAttendee replier = attendees.get(0);
                    String partStat = replier.getPartStat();
                    String replierName = replier.getFriendlyAddress().toString();
                    if (partStat.equals(IcalXmlStrMap.PARTSTAT_ACCEPTED)) {
                        hdrKey = L10nUtil.MsgKey.calendarDefaultReplyAccept;
                    } else if (partStat.equals(IcalXmlStrMap.PARTSTAT_TENTATIVE)) {
                        hdrKey = L10nUtil.MsgKey.calendarDefaultReplyAccept;
                    } else if (partStat.equals(IcalXmlStrMap.PARTSTAT_DECLINED)) {
                        hdrKey = L10nUtil.MsgKey.calendarDefaultReplyDecline;
                    }
                    if (hdrKey != null) {
                        mPlainText.append(L10nUtil.getMessage(hdrKey, mLc, replierName)).append("\n\n");
                        mHtml.append("<h3>").append(L10nUtil.getMessage(hdrKey, mLc, replierName)).append("</h3>");
                        hdrKey = null;
                    }
                }
            }
            if (hdrKey != null) {
                mPlainText.append(L10nUtil.getMessage(hdrKey, mLc)).append("\n\n");
                mHtml.append("<h3>").append(L10nUtil.getMessage(hdrKey, mLc)).append("</h3>");
            }
            mHtml.append("\n\n<p>\n<table border=\"0\">\n");

            addSimpleRow(L10nUtil.MsgKey.zsSubject, invite.getName());
            if (invite.hasOrganizer())
                addSimpleRow(L10nUtil.MsgKey.zsOrganizer,
                        invite.getOrganizer().getFriendlyAddress().toString());
            mHtml.append("</table>\n<p>\n<table border=\"0\">\n");
            addSimpleRow(L10nUtil.MsgKey.zsLocation, invite.getLocation());

            addSimpleRow(L10nUtil.MsgKey.zsTime, 
                getTimeDisplayString(invite.getStartTime(), invite.getEndTime(),
                        invite.isRecurrence(), invite.isAllDayEvent()));
            ZRecur zr = getRecur(invite);
            if (zr != null) {
                addSimpleRow(L10nUtil.MsgKey.zsRecurrence, 
                    getRecurrenceDisplayString(zr, invite.getStartTime().getCalendarCopy(), mLc));
            }
            if (!method.equals("REPLY") && (attendees != null) && !attendees.isEmpty()) {
                mHtml.append("</table>\n<p>\n<table border=\"0\">\n");
                StringBuilder attendeeList = new StringBuilder();
                boolean firstAdded = false;
                for (ZAttendee attendee : attendees) {
                    if (firstAdded) {
                        attendeeList.append(", ");
                    } else {
                        firstAdded = true;
                    }
                    attendeeList.append(attendee.getFriendlyAddress().toString());
                }
                addSimpleRow(L10nUtil.MsgKey.zsInvitees, attendeeList.toString());
            }
            mPlainText.append("*~*~*~*~*~*~*~*~*~*\n");
            mHtml.append("</table>\n");
            mHtml.append("<div>*~*~*~*~*~*~*~*~*~*</div><br>\n");
        } catch (ServiceException e) {
            sLog.debug("Resetting descriptions due to ServiceException", e);
            mPlainText.setLength(origPlainLen);
            mHtml.setLength(0);
        }
    }

    /**
     * If <code>value</code> has a non-zero length value, append a line of the form:
     *     <code>msgKey</code>:<code>value</code>
     * to <code>mPlainText</code> and <code>mHtml</code>
     */
    private void addSimpleRow(L10nUtil.MsgKey msgKey, String value) {
        if ((value == null) || (value.length() == 0))
            return;
        String key = L10nUtil.getMessage(msgKey, mLc);
        mPlainText.append(key).append(": ").append(value).append("\n");
        mHtml.append("<tr><th align=\"left\">");
        mHtml.append(StringUtil.escapeHtml(key));
        mHtml.append(":</th><td>");
        mHtml.append(StringUtil.escapeHtml(value));
        mHtml.append("</td></tr>\n");
    }

    public String getTimeDisplayString(ParsedDateTime start, ParsedDateTime end,
            boolean isRecurrence, boolean isAllDayEvent) throws ServiceException {
        StringBuilder sb = new StringBuilder();
        if (!isRecurrence) {
            sb.append(DateFormat.getDateInstance(DateFormat.FULL, mLc).format(start.getDate())).append(", ");
        }
        if (isAllDayEvent) {
            sb.append(L10nUtil.getMessage(L10nUtil.MsgKey.zsAllDay, mLc));
        } else {
            DateFormat timeFormat = DateFormat.getTimeInstance(DateFormat.LONG, mLc);
            sb.append(timeFormat.format(start.getDate())).append(" - ");
            if (!isRecurrence) {
                Calendar calStart = start.getCalendarCopy();
                Calendar calEnd = end.getCalendarCopy();
                boolean isSameday = calStart.get(Calendar.YEAR) == calEnd.get(Calendar.YEAR) &&
                                    calStart.get(Calendar.MONTH) == calEnd.get(Calendar.MONTH) &&
                                    calStart.get(Calendar.DATE) == calEnd.get(Calendar.DATE);
                if (!isSameday) {
                    sb.append(DateFormat.getDateInstance(DateFormat.FULL, mLc).format(end.getDate())).append(", ");
                }
            }
            sb.append(timeFormat.format(end.getDate()));
        }
        return sb.toString();
    }
    
    public static String getRecurrenceDisplayString(ZRecur zr, Calendar dtStart, Locale lc) {
        String repeat = null;
        if (zr.getFrequency().equals(ZRecur.Frequency.DAILY)) {
            List<ZWeekDayNum> dayList = zr.getByDayList();
            if (dayList != null && !dayList.isEmpty()) { //we assume this is every weekday
                repeat = L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurDailyEveryWeekday, lc);
            } else if (zr.getInterval() > 1) {
                repeat = L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurDailyEveryNumDays, lc, zr.getInterval());
            } else {
                repeat = L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurDailyEveryDay, lc);
            }
        } else if (zr.getFrequency().equals(ZRecur.Frequency.WEEKLY)) {
            List<ZWeekDayNum> dayList = zr.getByDayList();
            int dayOfWeek = 0;
            if (dayList == null || dayList.isEmpty()) {
                dayOfWeek = dtStart.get(Calendar.DAY_OF_WEEK);
            } else {
                dayOfWeek = dayList.get(0).mDay.getCalendarDay();
            }
            Calendar cal = Calendar.getInstance(lc);
            cal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
            if (zr.getInterval() == 1) {
                repeat = L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurWeeklyEveryWeekday, lc, cal.getTime());
            } else {
                repeat = L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurWeeklyEveryNumWeeksDate, lc, zr.getInterval(), cal.getTime());
            }
        } else if (zr.getFrequency().equals(ZRecur.Frequency.MONTHLY)) {
            List<Integer> monthdayList = zr.getByMonthDayList();
            if (monthdayList != null && !monthdayList.isEmpty()) {
                repeat = L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurMonthlyEveryNumMonthsDate, lc, monthdayList.get(0), zr.getInterval());
            } else {
                List<ZRecur.ZWeekDayNum> weekdayList = zr.getByDayList();
                if (weekdayList != null && !weekdayList.isEmpty()) {
                    Calendar cal = Calendar.getInstance(lc);
                    cal.set(Calendar.DAY_OF_WEEK, weekdayList.get(0).mDay.getCalendarDay());
                    repeat = L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurMonthlyEveryNumMonthsNumDay, lc, weekOfMonth(zr), cal.getTime(), zr.getInterval());
                } else { //assume by month day, use DTSTART to figure out which one
                    repeat = L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurMonthlyEveryNumMonthsDate, lc, dtStart.get(Calendar.DAY_OF_MONTH), zr.getInterval());
                }
            }
        } else if (zr.getFrequency().equals(ZRecur.Frequency.YEARLY)) {
            Calendar cal = Calendar.getInstance(lc);
            List<Integer> monthList = zr.getByMonthList();
            int month = 0;
            if (monthList != null && !monthList.isEmpty()) {
                month = monthList.get(0).intValue() - 1; //java Calendar month starts from 0
            } else {
                month = dtStart.get(Calendar.DAY_OF_MONTH);
            }
            cal.set(Calendar.MONTH, month);
            List<Integer> monthdayList = zr.getByMonthDayList();
            if (monthdayList != null && !monthdayList.isEmpty()) {
                repeat = L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurYearlyEveryDate, lc, cal.getTime(), monthdayList.get(0));
            } else { //could be by weekday
                List<ZRecur.ZWeekDayNum> weekdayList = zr.getByDayList();
                if (weekdayList != null && !weekdayList.isEmpty()) {
                    cal.set(Calendar.DAY_OF_WEEK, weekdayList.get(0).mDay.getCalendarDay());
                    repeat = L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurYearlyEveryMonthNumDay, lc, weekOfMonth(zr), cal.getTime(), cal.getTime());
                } else { //assume by month day, use DTSTART to figure out which one
                    repeat = L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurYearlyEveryDate, lc, cal.getTime(), dtStart.get(Calendar.DAY_OF_MONTH));
                }
            }
        } else {
            assert false;
        }
        
        String start =  L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurStart, lc, dtStart.getTime());

        String end = null;
        if (zr.getCount() > 0) {
            end = L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurEndNumber, lc, zr.getCount());
        } else if (zr.getUntil() != null) {
            end = L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurEndByDate, lc, zr.getUntil().getDate());
        } else {
            end = L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurEndNone, lc);
        }

        return L10nUtil.getMessage(L10nUtil.MsgKey.zsRecurBlurb, lc, repeat, start, end);
    }

    public static ZRecur getRecur(Invite invite) {
        ZRecur zr = null;

        Recurrence.IRecurrence r = invite.getRecurrence();
        if (r != null) {
            //The Appointment object's IRecurrence is always a RecurrenceRule,
            //which is a container of everything.
            assert r.getType() == Recurrence.TYPE_RECURRENCE;
            Recurrence.RecurrenceRule masterRule = (Recurrence.RecurrenceRule)r;

            //We then go through the RRULE, RDATE, EXRULE and EXDATE.
            //We only support a single RRULE.

            for (Iterator<IRecurrence> iter = masterRule.addRulesIterator(); iter!=null && iter.hasNext();) {
                r = iter.next();

                switch (r.getType()) {
                case Recurrence.TYPE_SINGLE_DATES:
                    //Recurrence.SingleInstanceRule sir = (Recurrence.SingleInstanceRule)r;
                    assert false; //We don't support single instance.
                    break;
                case Recurrence.TYPE_REPEATING:
                    Recurrence.SimpleRepeatingRule srr = (Recurrence.SimpleRepeatingRule)r;
                    zr = srr.getRule();
                    break;
                default:
                    assert false; //can't be anything else
                    break;
                }

                assert !iter.hasNext(); //we don't support more than one RRULE/RDATE
                break;
            }
            assert zr != null;
        }

        return zr;
    }

    public static int weekOfMonth(ZRecur recur) {
        List<ZRecur.ZWeekDayNum> weekdayList = recur.getByDayList();
        assert weekdayList.size() == 1; //can only handle one
        int offset = weekdayList.get(0).mOrdinal;
        if (offset == 0) {
            List<Integer> setposList = recur.getBySetPosList();
            if (setposList != null && !setposList.isEmpty()) {
                assert setposList.size() == 1;
                offset = setposList.get(0).intValue();
            }
        }
        switch (offset) {
        case -1: case 1: case 2: case 3: case 4:
            break;
        default:
            sLog.warn("Invalid RECUR pattern: " + recur.toString());
            break;
        }
        return offset;
    }
}
