/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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

package com.zimbra.cs.util.tnef.mapi;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.util.tnef.IcalUtil;
import com.zimbra.cs.util.tnef.TNEFtoIcalendarServiceException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.GregorianCalendar;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.WeekDayList;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.util.TimeZones;
import net.freeutils.tnef.RawInputStream;

/**
 * From MS-OXCOCAL this is used for :
 *     PidLidAppointmentRecur
 *         Includes information related to recurrence patterns, including
 *         summary details for modified and deleted instances.
 *         The structure is an AppointmentRecurrencePattern
 */

public class RecurrenceDefinition {

    static Log sLog = ZimbraLog.tnef;

    public static enum RecurrenceFrequency {
        DAILY (0x200A), WEEKLY (0x200B), MONTHLY (0x200C), YEARLY (0x200D);

        private final int MapiPropValue;

        RecurrenceFrequency(int propValue) {
            this.MapiPropValue = propValue;
        }

        public int mapiPropValue() {
            return MapiPropValue;
        }
    }

    public static enum PatternType {
        DAY (0x0000), WEEK (0x0001),
        MONTH (0x0002), MONTH_NTH (0x0003), MONTH_END (0x0004),
        HJ_MONTH (0x000A), HJ_MONTH_NTH (0x000B), HJ_MONTH_END (0x000C);

        private final int MapiPropValue;

        PatternType(int propValue) {
            this.MapiPropValue = propValue;
        }

        public int mapiPropValue() {
            return MapiPropValue;
        }
    }

    public static enum EndType {
        END_BY_DATE (0x00002021),
        END_AFTER_N_OCCURRENCES (0x00002022),
        NEVER_END (0x00002023),
        NEVER_END_OLD (0xFFFFFFFF);

        private final long MapiPropValue;

        EndType(long propValue) {
            this.MapiPropValue = propValue;
        }

        public long mapiPropValue() {
            return MapiPropValue;
        }
    }

    private TimeZoneDefinition tzDef;
    public RecurrenceFrequency recurrenceFrequency;
    public PatternType patternType;
    public MsCalScale calScale;
    private EndType endType;
    private DayOfWeek FirstDayOfWeek;
    private EnumSet <DayOfWeek> DayOfWeekMask;

    long Period;
    private long firstDateTime;
    private long dayOfMonth;
    private long weekDayOccurrenceNumber;
    private long occurrenceCount;

    // Minutes since 1601 Date portion of DTSTART in local time
    private long StartDate;

    // Minutes since 1601 Date portion of Start of LAST instance in local time
    // Infinite if set to 0x5AE980DF.
    private long EndDate;

    // The number of minutes, since day start 00:00, after which each occurrence starts.
    // e.g. the value for midnight is 0 (zero) and the value for 12:00 P.M.
    // is 720.
    private long StartTimeOffset;  // localtime minutes since start of current day
    private DateTime deletedInstances[];
    private DateTime modifiedInstances[];

    /**
     *
     * @param ris
     * @param tz
     * @throws IOException
     */
    public RecurrenceDefinition(RawInputStream ris,
            TimeZoneDefinition tz) throws IOException {
        boolean haveDayMask = false;
        boolean haveDayOfMonth = false;
        boolean haveWeekDayOccurNum = false;
        this.tzDef = tz;
        int ReaderVersion = ris.readU16();  // Should be 0x3004
        int WriterVersion = ris.readU16();  // Should be 0x3004
        readRecurrenceFrequency(ris);
        readPatternType(ris);
        readMsCalScale(ris);
        readFirstDateTime(ris);

        // This field is the interval at which the meeting pattern specified in PatternTypeSpecific field
        // repeats. The Period value MUST be between 0 (zero) and the MaximumRecurrenceInterval, which is
        // 999 days for daily recurrences, 99 weeks for weekly recurrences, and 99 months for monthly
        // recurrences. The following table lists the values for this field based on recurrence type.
        Period = ris.readU32();
        long SlidingFlag = ris.readU32(); // Should be 0 unless this is a task
        long dayMask = 0;
        dayOfMonth = 0;
        weekDayOccurrenceNumber = 0;

        // Process PatternSpecificType data
        switch (patternType) {
            case DAY:
                break; // no PatternTypeSpecific data in this case
            case WEEK:
                dayMask = ris.readU32();
                haveDayMask = true;
                break;
            case MONTH:
            case MONTH_END:
            case HJ_MONTH:
            case HJ_MONTH_END:
                dayOfMonth = ris.readU32();
                haveDayOfMonth = true;
                break;
            case MONTH_NTH:
            case HJ_MONTH_NTH:
                dayMask = ris.readU32();
                haveDayMask = true;
                weekDayOccurrenceNumber = ris.readU32(); // 5 means last one
                haveWeekDayOccurNum = true;
                break;
            default:
                throw new IOException("Unexpected PatternType in Recurrence Definition");
        }

        // valid combinations?  Single bit/ weekend bits/ weekday bits
        DayOfWeekMask = EnumSet.noneOf(DayOfWeek.class);
        if (haveDayMask) {
            if ( (dayMask & 0x00000001) == 0x00000001) {
                DayOfWeekMask.add(DayOfWeek.SU);
            }
            if ( (dayMask & 0x00000002) == 0x00000002) {
                DayOfWeekMask.add(DayOfWeek.MO);
            }
            if ( (dayMask & 0x00000004) == 0x00000004) {
                DayOfWeekMask.add(DayOfWeek.TU);
            }
            if ( (dayMask & 0x00000008) == 0x00000008) {
                DayOfWeekMask.add(DayOfWeek.WE);
            }
            if ( (dayMask & 0x00000010) == 0x00000010) {
                DayOfWeekMask.add(DayOfWeek.TH);
            }
            if ( (dayMask & 0x00000020) == 0x00000020) {
                DayOfWeekMask.add(DayOfWeek.FR);
            }
            if ( (dayMask & 0x00000040) == 0x00000040) {
                DayOfWeekMask.add(DayOfWeek.SA);
            }
        }

        readEndType(ris);
        occurrenceCount = ris.readU32();
        readFirstDayOfWeek(ris);
        int DeletedInstanceCount = (int) ris.readU32();
        long delMidnightMinsSince1601[] = new long[DeletedInstanceCount];
        for (int cnt = 0;cnt < DeletedInstanceCount; cnt++) {
            delMidnightMinsSince1601[cnt] = ris.readU32();
        }
        int ModifiedInstanceCount = (int) ris.readU32();
        long modMidnightMinsSince1601[] = new long[ModifiedInstanceCount];
        for (int cnt = 0;cnt < ModifiedInstanceCount; cnt++) {
            modMidnightMinsSince1601[cnt] = ris.readU32();
        }
        
        StartDate = ris.readU32();
        EndDate = ris.readU32();
        Date sDate = IcalUtil.localMinsSince1601toDate(StartDate, tzDef);
        Date eDate = IcalUtil.localMinsSince1601toDate(EndDate, tzDef);

        long ReaderVersion2 = ris.readU32();
        // WriterVersion2 relates approximately to version of Outlook
        // (or equivalent support level) that created this.
        // e.g. Outlook 2000 --> 0x3006, XP -->0x3007, 2003 --> 0x3008
        long WriterVersion2 = ris.readU32();
        
        StartTimeOffset = ris.readU32();
        long EndTimeOffset = ris.readU32();
        int ExceptionCount = ris.readU16();  // Should be same as ModifiedInstanceCount?

        deletedInstances = new DateTime[(int) DeletedInstanceCount];
        for (int cnt = 0;cnt < DeletedInstanceCount; cnt++) {
            deletedInstances[cnt] = IcalUtil.localMinsSince1601toDate(
                    delMidnightMinsSince1601[cnt] + StartTimeOffset, tzDef);
        }

        modifiedInstances = new DateTime[(int) ModifiedInstanceCount];
        for (int cnt = 0;cnt < ModifiedInstanceCount; cnt++) {
            modifiedInstances[cnt] = IcalUtil.localMinsSince1601toDate(
                    modMidnightMinsSince1601[cnt] + StartTimeOffset, tzDef);
        }

        if (sLog.isDebugEnabled()) {
            StringBuffer debugInfo = new StringBuffer("RecurrenceDefinition\n");
            if (ReaderVersion != 0x3004) {
                debugInfo.append("    Unexpected ReaderVersion=")
                        .append(ReaderVersion).append("\n");
            }
            if (WriterVersion != 0x3004) {
                debugInfo.append("    Unexpected WriterVersion=")
                        .append(WriterVersion).append("\n");
            }
            debugInfo.append("    RecurFrequency=")
                    .append(getRecurrenceFrequency()).append("\n");
            debugInfo.append("    PatternType=")
                    .append(getPatternType()).append("\n");
            debugInfo.append("    MsCalScale=")
                    .append(getMsCalScale()).append("\n");
            debugInfo.append("    FirstDateTime=")
                    .append(getFirstDateTime()).append("\n");
            debugInfo.append("    Period=")
                    .append(Period).append("\n");
            debugInfo.append("    SlidingFlag=")
                    .append(SlidingFlag).append("\n");
            if (haveDayMask) {
                debugInfo.append("    dayMask=0x")
                        .append(Long.toHexString(dayMask)).append(" - ")
                        .append(DayOfWeekMask).append("\n");
            }
            if (haveDayOfMonth) {
                    debugInfo.append("    dayOfMonth=")
                            .append(dayOfMonth).append("\n");
            }
            if (haveWeekDayOccurNum) {
                debugInfo.append("    weekDayOccurrenceNumber=")
                        .append(weekDayOccurrenceNumber).append("\n");
            }
            debugInfo.append("    EndType=")
                    .append(getEndType()).append("\n");
            debugInfo.append("    occurrenceCount=")
                    .append(occurrenceCount).append("\n");
            debugInfo.append("    firstDayOfWeek=")
                    .append(getFirstDayOfWeek()).append("\n");
            debugInfo.append("    DeletedInstanceCount=")
                    .append(DeletedInstanceCount).append("\n");
            for (Date currDate : deletedInstances) {
                debugInfo.append("        DeletedInstance=")
                        .append(currDate).append("\n");
            }
            debugInfo.append("    ModifedInstanceCount=")
                    .append(ModifiedInstanceCount).append("\n");
            for (Date currDate : modifiedInstances) {
                debugInfo.append("        ModifiedInstance=")
                        .append(currDate).append("\n");
            }
            debugInfo.append("    StartDate=").append(sDate)
                    .append("[").append(StartDate).append("]\n");
            debugInfo.append("    EndDate(Start of last instance)=")
                    .append(eDate).append("[").append(EndDate).append("]\n");
            debugInfo.append("    ReaderVersion2=0x")
                    .append(Long.toHexString(ReaderVersion2)).append("\n");
            debugInfo.append("    WriterVersion2=0x")
                    .append(Long.toHexString(WriterVersion2)).append("\n");
            debugInfo.append("    StartTimeOffset=").append(StartTimeOffset).append("\n");
            debugInfo.append("    EndTimeOffset=").append(EndTimeOffset).append("\n");
            debugInfo.append("    ExceptionCount=").append(ExceptionCount).append("\n");
            sLog.debug(debugInfo);
        }

        //        For each modified instance, expect to find an ExceptionInfo structure.
        //        Then possibly some more info followed by Extended ExceptionInfo structures.
        //        For scheduling messages, we probably do not need to look at any of that.

    }

    /**
     *
     * @return appropriate value for the ICALENDAR X-MICROSOFT-CALSCALE
     *         property or "" if not required.
     */
    public String xMicrosoftCalscale() {
        return calScale.XMicrosoftCalScale();
    }

    /**
     * @param recurrenceFrequency the recurrenceFrequency to set
     */
    public void setRecurrenceFrequency(RecurrenceFrequency recurrenceFrequency) {
        this.recurrenceFrequency = recurrenceFrequency;
    }

    /**
     * @return the recurrenceFrequency
     */
    public RecurrenceFrequency getRecurrenceFrequency() {
        return recurrenceFrequency;
    }

    /**
     * @param patternType the MAPI PatternType to set
     */
    public void setPatternType(PatternType patternType) {
        this.patternType = patternType;
    }

    /**
     * @return the patternType
     */
    public PatternType getPatternType() {
        return patternType;
    }

    /**
     * @param calendarType the calendarType to set
     */
    public void setMsCalScale(MsCalScale calendarType) {
        this.calScale = calendarType;
    }

    /**
     * @return the calendarType
     */
    public MsCalScale getMsCalScale() {
        return calScale;
    }

    /**
     * @param endType the endType to set
     */
    public void setEndType(EndType endType) {
        this.endType = endType;
    }

    /**
     * @return the endType
     */
    public EndType getEndType() {
        return endType;
    }

    /**
     * @param firstDayOfWeek the firstDayOfWeek to set
     */
    public void setFirstDayOfWeek(DayOfWeek firstDayOfWeek) {
        FirstDayOfWeek = firstDayOfWeek;
    }

    /**
     * @return the firstDayOfWeek
     */
    public DayOfWeek getFirstDayOfWeek() {
        return FirstDayOfWeek;
    }

    /**
     * @param firstDateTime the firstDateTime to set
     */
    public void setFirstDateTime(long firstDateTime) {
        this.firstDateTime = firstDateTime;
    }

    /**
     * @return the firstDateTime
     */
    public long getFirstDateTime() {
        return firstDateTime;
    }

    /**
     * @return the deletedInstances
     */
    public DateTime[] getDeletedInstances() {
        return deletedInstances;
    }

    /**
     * @param isAllDay
     * @param isFloating
     * @return The main recurrence property as ICAL - typically an RRULE but
     *         could theoretically be an X-MICROSOFT-RRULE
     * @throws ServiceException
     */
    public Property icalRecurrenceProperty(
            boolean isAllDay, boolean isFloating) throws ServiceException {
        RRule theRule = null;
        if (this.calScale.isSolarCalendar() == false) {
            throw TNEFtoIcalendarServiceException.NON_SOLAR_CALENDAR();
        }
        // iCal4j Recur is a bit basic when it come to building things
        // up from components, for instance, there currently isn't a way
        // to set any BYDAY= value other than in the constructor from a String
        StringBuffer recurrenceRule = new StringBuffer("FREQ=");
        String weekStartDay = null;
        boolean hasBYDAY = false;
        boolean isYearly = false;
        int interval = 1;
        switch (patternType) {
            case DAY:
                recurrenceRule.append(Recur.DAILY);
                interval = new Long(Period).intValue() / 1440;
                break;
            case WEEK:
                recurrenceRule.append(Recur.WEEKLY);
                interval = new Long(Period).intValue();
                hasBYDAY = true;
                if (interval > 1) {
                    weekStartDay = this.getFirstDayOfWeek().toString();
                }
                break;
            case MONTH:
                interval = new Long(Period).intValue();
                if ((interval % 12) == 0) {
                    isYearly = true;
                    recurrenceRule.append(Recur.YEARLY);
                    interval = (interval / 12);
                } else {
                    recurrenceRule.append(Recur.MONTHLY);
                }
                if (dayOfMonth != 0) {
                    recurrenceRule.append(";BYMONTHDAY=");
                    if (dayOfMonth == 31) {
                        recurrenceRule.append("-1");
                    } else {
                        recurrenceRule.append(dayOfMonth);
                    }
                }
                if (isYearly) {
                    java.util.TimeZone javaTZ = null;
                    if (tzDef != null) {
                        javaTZ = tzDef.getTimeZone();
                    } else {
                        javaTZ = TimeZone.getTimeZone(TimeZones.UTC_ID);
                    }
                    Date bymonthDate = IcalUtil.localMinsSince1601toDate(firstDateTime, tzDef);
                    Calendar bymonthCal = new GregorianCalendar(javaTZ);
                    bymonthCal.setTimeInMillis(bymonthDate.getTime());
                    String MONTH_ONLY_PATTERN = "MM";
                    DateFormat monthOnlyFormat = new SimpleDateFormat(MONTH_ONLY_PATTERN);
                    monthOnlyFormat.setCalendar(bymonthCal);
                    recurrenceRule.append(";BYMONTH=");
                    recurrenceRule.append(monthOnlyFormat.format(bymonthDate));
                }
                break;
            case MONTH_NTH:
                interval = new Long(Period).intValue();
                if ((interval % 12) == 0) {
                    isYearly = true;
                    recurrenceRule.append(Recur.YEARLY);
                    interval = (interval / 12);
                } else {
                    recurrenceRule.append(Recur.MONTHLY);
                }
                hasBYDAY = true;
                recurrenceRule.append(";BYSETPOS=");
                if (weekDayOccurrenceNumber == 5) {
                    recurrenceRule.append(-1);
                } else {
                    recurrenceRule.append(weekDayOccurrenceNumber);
                }
                if (isYearly) {
                    java.util.TimeZone javaTZ = null;
                    if (tzDef != null) {
                        javaTZ = tzDef.getTimeZone();
                    } else {
                        javaTZ = TimeZone.getTimeZone(TimeZones.UTC_ID);
                    }
                    Date bymonthDate = IcalUtil.localMinsSince1601toDate(firstDateTime, tzDef);
                    Calendar bymonthCal = new GregorianCalendar(javaTZ);
                    bymonthCal.setTimeInMillis(bymonthDate.getTime());
                    String MONTH_ONLY_PATTERN = "MM";
                    DateFormat monthOnlyFormat = new SimpleDateFormat(MONTH_ONLY_PATTERN);
                    monthOnlyFormat.setCalendar(bymonthCal);
                    recurrenceRule.append(";BYMONTH=");
                    recurrenceRule.append(monthOnlyFormat.format(bymonthDate));
                }
                break;
            case MONTH_END:
            case HJ_MONTH:
            case HJ_MONTH_END:
            case HJ_MONTH_NTH:
                throw TNEFtoIcalendarServiceException.UNSUPPORTED_RECURRENCE_TYPE(patternType.name());
            default:
                throw TNEFtoIcalendarServiceException.UNSUPPORTED_RECURRENCE_TYPE(patternType.name());
        }
        if (recurrenceRule.length() > 5 /* length of "FREQ=" */) {
            if (endType.equals(EndType.END_AFTER_N_OCCURRENCES)) {
                recurrenceRule.append(";COUNT=");
                recurrenceRule.append(occurrenceCount);
            } else if (endType.equals(EndType.END_BY_DATE)) {
                // MS-OXCICAL :
                //    set to (EndDate + StartTimeOffset), converted from the
                //    time zone specified by PidLidTimeZoneStruct to the UTC time zone
                //  From RFC 5545 :
                //    The UNTIL rule part defines a DATE or DATE-TIME value that bounds
                //    the recurrence rule in an inclusive manner.  If the value
                //    specified by UNTIL is synchronized with the specified recurrence,
                //    this DATE or DATE-TIME becomes the last instance of the
                //    recurrence.  The value of the UNTIL rule part MUST have the same
                //    value type as the "DTSTART" property.  Furthermore, if the
                //    "DTSTART" property is specified as a date with local time, then
                //    the UNTIL rule part MUST also be specified as a date with local
                //    time [Gren - i.e. when DTSTART is a floating time].
                //    If the "DTSTART" property is specified as a date with UTC
                //    time or a date with local time and time zone reference, then the
                //    UNTIL rule part MUST be specified as a date with UTC time.
                long minsSince1601 = this.EndDate + this.StartTimeOffset;
                DateTime untilDateTime = null;
                recurrenceRule.append(";UNTIL=");
                if (isFloating) {
                    // use localtime.  TODO: If all day, do we need to remove hrs/mins etc?
                    untilDateTime = IcalUtil.localMinsSince1601toDate(minsSince1601, tzDef);
                } else {
                    untilDateTime = IcalUtil.localMinsSince1601toUtcDate(minsSince1601, tzDef);
                }
                recurrenceRule.append(IcalUtil.iCalDateTimeValue(
                        untilDateTime, this.tzDef.getTimeZone(), isAllDay));
            }
            if (hasBYDAY) {
                WeekDayList weekDayList = new WeekDayList();
                for (DayOfWeek dow : this.DayOfWeekMask) {
                    weekDayList.add(dow.iCal4JWeekDay());
                }
                if (!weekDayList.isEmpty()) {
                    recurrenceRule.append(";BYDAY=");
                    recurrenceRule.append(weekDayList);
                }
            }
            if (interval != 1) {
                recurrenceRule.append(";INTERVAL=");
                recurrenceRule.append(interval);
            }
            if (weekStartDay != null) {
                recurrenceRule.append(";WKST=");
                recurrenceRule.append(weekStartDay);
            }
            Recur recur;
            try {
                recur = new Recur(recurrenceRule.toString());
            } catch (ParseException ex) {
                throw TNEFtoIcalendarServiceException.RRULE_PARSING_PROBLEM(ex);
            }
            theRule = new RRule(recur);
        }
        return theRule;
    }

    private void readRecurrenceFrequency(RawInputStream ris) throws IOException {
        int mapiFrequency = ris.readU16();

        for (RecurrenceFrequency curr : RecurrenceFrequency.values()) {
            if (curr.mapiPropValue() == mapiFrequency) {
                setRecurrenceFrequency(curr); return;
            }
        }

        // TODO: Would some sort of ParseException be more appropriate?
        // TODO: Include the hex value of the property
        throw new IOException("Invalid Frequency value " + mapiFrequency +
                        " in MAPI recurrence definition property");
    }

    private void readPatternType(RawInputStream ris) throws IOException {
        int pattType = ris.readU16();

        for (PatternType curr : PatternType.values()) {
            if (curr.mapiPropValue() == pattType) {
                setPatternType(curr); return;
            }
        }

        // TODO: Would some sort of ParseException be more appropriate?
        // TODO: Include the hex value of the property
        throw new IOException("Invalid PatternType value " + pattType +
                        " in MAPI recurrence definition property");
    }

    private void readMsCalScale(RawInputStream ris) throws IOException {
        int calType = ris.readU16();

        if (calType == 0) {
            if (patternType.equals(PatternType.HJ_MONTH)) {
                setMsCalScale(MsCalScale.HIJRI);
                return;
            }
            if (patternType.equals(PatternType.HJ_MONTH_NTH)) {
                setMsCalScale(MsCalScale.HIJRI);
                return;
            }
        }
        for (MsCalScale curr : MsCalScale.values()) {
            if (curr.mapiPropValue() == calType) {
                setMsCalScale(curr); return;
            }
        }

        // TODO: Would some sort of ParseException be more appropriate?
        // TODO: Include the hex value of the property
        throw new IOException("Invalid MsCalScale value " + calType +
                        " in MAPI recurrence definition property");
    }

    private void readEndType(RawInputStream ris) throws IOException {
        long endTyp = ris.readU32();

        for (EndType curr : EndType.values()) {
            if (curr.mapiPropValue() == endTyp) {
                if (curr.equals(EndType.NEVER_END_OLD)) {
                    setEndType(EndType.NEVER_END);
                } else {
                    setEndType(curr);
                }
                return;
            }
        }

        // TODO: Would some sort of ParseException be more appropriate?
        // TODO: Include the hex value of the property
        throw new IOException("Invalid EndType value " + endTyp +
                        " in MAPI recurrence definition property");
    }

    private void readFirstDayOfWeek(RawInputStream ris) throws IOException {
        long dow = ris.readU32();

        for (DayOfWeek curr : DayOfWeek.values()) {
            if (curr.mapiPropValue() == dow) {
                setFirstDayOfWeek(curr);
                return;
            }
        }

        // TODO: Would some sort of ParseException be more appropriate?
        // TODO: Include the hex value of the property
        throw new IOException("Invalid FirstDayOfWeek value " + dow +
                        " in MAPI recurrence definition property");
    }

    /**
     *
     * Read FristDateTime value from the correct place in a stream representing
     * the PidLidAppointmentRecur property. The value depends on the
     * RecurrenceFrequency value.
     *
     * @param ris
     * @throws IOException
     */
    private void readFirstDateTime(RawInputStream ris) throws IOException {
        setFirstDateTime(ris.readU32());
        // RecurrenceFrequency DAILY -->
    }
}
