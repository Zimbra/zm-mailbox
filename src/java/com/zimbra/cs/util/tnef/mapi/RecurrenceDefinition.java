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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.WeekDayList;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.util.TimeZones;
import net.freeutils.tnef.RawInputStream;
import net.freeutils.tnef.TNEFUtils;

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
    private String oemCodePage;

    private long readerVersion;
    private long writerVersion;
    public RecurrenceFrequency recurrenceFrequency;
    public PatternType patternType;
    public MsCalScale calScale;
    private long firstDateTime;
    private long period;
    private long slidingFlag;
    private long dayMask;
    private boolean haveDayMask;
    private long dayOfMonth;
    private boolean haveDayOfMonth;
    private long weekDayOccurrenceNumber;
    private boolean haveWeekDayOccurNum;
    private long occurrenceCount;
    private EndType endType;
    private DayOfWeek firstDayOfWeek;
    private EnumSet <DayOfWeek> dayOfWeekMask;
    int deletedInstanceCount;
    long delMidnightMinsSince1601[];
    int modifiedInstanceCount;
    long modMidnightMinsSince1601[];
    // Minutes since 1601 Date portion of DTSTART in local time
    private long startMinsSince1601;
    // Minutes since 1601 Date portion of Start of LAST instance in local time
    // Infinite if set to 0x5AE980DF.
    private long endMinsSince1601;
    private long readerVersion2;
    // writerVersion2 relates approximately to version of Outlook
    // (or equivalent support level) that created this.
    // e.g. Outlook 2000 --> 0x3006, XP -->0x3007, 2003 --> 0x3008
    private long writerVersion2;

    // The number of minutes, since day start 00:00, after which each occurrence starts.
    // e.g. the value for midnight is 0 (zero) and the value for 12:00 P.M. is 720.
    private long startTimeOffset;             // localtime minutes since start of current day
    private long endTimeOffset;               // localtime minutes since start of current day
    private int exceptionCount;               // Should be same as modifiedInstanceCount?
    private long rsrvdBlock1Size;
    private byte [] rsrvdBlock1;
    private long rsrvdBlock2Size;
    private byte [] rsrvdBlock2;
    private long unprocessedByteCount;
    private byte [] unprocessedBytes;
    private  List <DateTime> exdateTimes;
    private  List <DateTime> rdateTimes;
    private  List <ChangedInstanceInfo> changedInstances;

    /**
     *
     * @param ris
     * @param tz
     * @throws IOException
     */
    public RecurrenceDefinition(RawInputStream ris,
            TimeZoneDefinition tz, String oemCP) throws IOException {
        oemCodePage = oemCP;
        exdateTimes = null;
        rsrvdBlock1Size = -1;
        rsrvdBlock2Size = -1;
        haveDayMask = false;
        haveDayOfMonth = false;
        haveWeekDayOccurNum = false;
        this.tzDef = tz;
        try {
            readerVersion = ris.readU16();  // Should be 0x3004
            writerVersion = ris.readU16();  // Should be 0x3004
            readRecurrenceFrequency(ris);
            readPatternType(ris);
            readMsCalScale(ris);
            readFirstDateTime(ris);
    
            // This field is the interval at which the meeting pattern specified in PatternTypeSpecific field
            // repeats. The Period value MUST be between 0 (zero) and the MaximumRecurrenceInterval, which is
            // 999 days for daily recurrences, 99 weeks for weekly recurrences, and 99 months for monthly
            // recurrences. The following table lists the values for this field based on recurrence type.
            period = ris.readU32();
            slidingFlag = ris.readU32(); // Should be 0 unless this is a task
            dayMask = 0;
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
                    // TODO: Use custom ServiceException?
                    throw new IOException("Unexpected PatternType in Recurrence Definition");
            }
    
            readEndType(ris);
            occurrenceCount = ris.readU32();
            readFirstDayOfWeek(ris);
            deletedInstanceCount = (int) ris.readU32();
            delMidnightMinsSince1601 = new long[deletedInstanceCount];
            for (int cnt = 0;cnt < deletedInstanceCount; cnt++) {
                delMidnightMinsSince1601[cnt] = ris.readU32();
            }
            modifiedInstanceCount = (int) ris.readU32();
            modMidnightMinsSince1601 = new long[modifiedInstanceCount];
            for (int cnt = 0;cnt < modifiedInstanceCount; cnt++) {
                modMidnightMinsSince1601[cnt] = ris.readU32();
            }
            
            startMinsSince1601 = ris.readU32();
            endMinsSince1601 = ris.readU32();

            unprocessedByteCount = ris.available();
            if (unprocessedByteCount == 0L) {
                // Task recurrence info ends at this point.
                exceptionCount = 0;
                startTimeOffset = 0;
                endTimeOffset = 0;
                exceptionCount = 0;
                changedInstances = new ArrayList <ChangedInstanceInfo>();
                return;
            }
            readerVersion2 = ris.readU32();
            writerVersion2 = ris.readU32();
            
            startTimeOffset = ris.readU32();
            endTimeOffset = ris.readU32();
            exceptionCount = ris.readU16();  // Should be same as modifiedInstanceCount?
            //        For each modified instance, expect to find an ExceptionInfo structure.
    
            changedInstances = new ArrayList <ChangedInstanceInfo>();
            for (int cnt = 1; cnt <= modifiedInstanceCount; cnt++) {
                ChangedInstanceInfo cInst = new ChangedInstanceInfo(cnt, tzDef, oemCodePage);
                cInst.readExceptionInfo(ris);
                unprocessedByteCount = ris.available();
                changedInstances.add(cInst);
            }
            unprocessedByteCount = ris.available();
            rsrvdBlock1 = null;
            if (unprocessedByteCount > 0) {
                rsrvdBlock1Size = ris.readU32();
                rsrvdBlock1 = ris.readBytes((int)rsrvdBlock1Size);
            }
            unprocessedByteCount = ris.available();
            if (unprocessedByteCount == 0L) {
                return;
            }
            boolean hasChangeHL = (writerVersion2 >= 0x00003009L);
            for (ChangedInstanceInfo cInst : changedInstances) {
                cInst.readExtendedException(ris, hasChangeHL);
            }
            unprocessedByteCount = ris.available();
            if (unprocessedByteCount > 0) {
                rsrvdBlock2Size = ris.readU32();
                rsrvdBlock2 = ris.readBytes((int)rsrvdBlock2Size);
            }
            unprocessedByteCount = ris.available();
            if (unprocessedByteCount > 0) {
                unprocessedBytes = ris.readBytes((int)unprocessedByteCount);
            }
        } catch (IOException e) {
            sLog.debug("Problem processing PidLidAppointmentRecur property", e);
            throw e;
        }
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
     * @return the recurrenceFrequency
     */
    public RecurrenceFrequency getRecurrenceFrequency() {
        return recurrenceFrequency;
    }

    /**
     * @return the patternType
     */
    public PatternType getPatternType() {
        return patternType;
    }

    /**
     * @return the calendarType
     */
    public MsCalScale getMsCalScale() {
        return calScale;
    }

    /**
     * @return the endType
     */
    public EndType getEndType() {
        return endType;
    }

    /**
     * @return the firstDayOfWeek
     */
    public DayOfWeek getFirstDayOfWeek() {
        return firstDayOfWeek;
    }

    public DateTime getStartDate() {
        return IcalUtil.localMinsSince1601toDate(startMinsSince1601, tzDef);
    }

    public EnumSet <DayOfWeek> getDayOfWeekMask() {
        if (dayOfWeekMask != null) {
            return dayOfWeekMask;
        }
        // valid combinations?  Single bit/ weekend bits/ weekday bits
        dayOfWeekMask = EnumSet.noneOf(DayOfWeek.class);
        if (haveDayMask) {
            if ( (dayMask & 0x00000001) == 0x00000001) {
                dayOfWeekMask.add(DayOfWeek.SU);
            }
            if ( (dayMask & 0x00000002) == 0x00000002) {
                dayOfWeekMask.add(DayOfWeek.MO);
            }
            if ( (dayMask & 0x00000004) == 0x00000004) {
                dayOfWeekMask.add(DayOfWeek.TU);
            }
            if ( (dayMask & 0x00000008) == 0x00000008) {
                dayOfWeekMask.add(DayOfWeek.WE);
            }
            if ( (dayMask & 0x00000010) == 0x00000010) {
                dayOfWeekMask.add(DayOfWeek.TH);
            }
            if ( (dayMask & 0x00000020) == 0x00000020) {
                dayOfWeekMask.add(DayOfWeek.FR);
            }
            if ( (dayMask & 0x00000040) == 0x00000040) {
                dayOfWeekMask.add(DayOfWeek.SA);
            }
        }
        return dayOfWeekMask;
    }

    /**
     * @return the firstDateTime
     */
    public long getFirstDateTime() {
        return firstDateTime;
    }

    /**
     * @return the dates suitable for use as EXDATEs
     */
    public List <DateTime> getExdates() {
        if (exdateTimes != null) {
            return exdateTimes;
        }
        exdateTimes = new ArrayList <DateTime>();
        for (long delSince1601 : delMidnightMinsSince1601) {
            //  Outlook XP uses NEW times in modMidnightMinsSince1601
            // rather than original times - so, cannot mine that
            // to prune modifications from delMidnightMinsSince1601
            long minsSince1601 = delSince1601 + startTimeOffset;
            boolean canceledInstance = true;
            for (ChangedInstanceInfo cInst : changedInstances) {
                if (minsSince1601 == cInst.getOrigStartMinsSince1601()) {
                    EnumSet <ExceptionInfoOverrideFlag> overrideFlags = cInst.getOverrideFlags();
                    // Note that modifications which are just a new time are represented
                    // in the ICALENDAR as an EXDATE/RDATE pair
                    if ( (overrideFlags != null) && (!overrideFlags.isEmpty()) ) {
                        canceledInstance = false;
                        break;
                    }
                }
            }
            if (canceledInstance) {
                exdateTimes.add(IcalUtil.localMinsSince1601toDate(
                            minsSince1601, tzDef));
            }
        }
        return exdateTimes;
    }

    /**
     * Note that according to MS-OXCICAL, recurrences only support RDATEs
     * if there is a corresponding EXDATE for an item in the series.
     * @return the dates suitable for use as RDATEs
     */
    public List <DateTime> getRdates() {
        if (rdateTimes != null) {
            return rdateTimes;
        }
        rdateTimes = new ArrayList <DateTime>();
        for (ChangedInstanceInfo cInst : changedInstances) {
            EnumSet <ExceptionInfoOverrideFlag> overrideFlags = cInst.getOverrideFlags();
            // Note that modifications which are just a new time are represented
            // in the ICALENDAR as an EXDATE/RDATE pair
            if ( (overrideFlags == null) || (overrideFlags.isEmpty()) ) {
                rdateTimes.add(cInst.getStartDate());
            }
        }
        return rdateTimes;
    }

    /**
     * @return the changedInstances
     */
    public List <ChangedInstanceInfo> getChangedInstances() {
        return changedInstances;
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
        // According to RFC, only absolutely NEED WKST of have a
        // weekly recurrence with interval greater than 1 OR using
        // BYWEEKNO - however, does no harm to always include it.
        String weekStartDay = firstDayOfWeek.toString();
        boolean hasBYDAY = false;
        boolean isYearly = false;
        int interval = 1;
        switch (patternType) {
            case DAY:
                recurrenceRule.append(Recur.DAILY);
                interval = new Long(period).intValue() / 1440;
                break;
            case WEEK:
                recurrenceRule.append(Recur.WEEKLY);
                interval = new Long(period).intValue();
                hasBYDAY = true;
                break;
            case MONTH:
                interval = new Long(period).intValue();
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
                interval = new Long(period).intValue();
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
                //    set to (EndDate + startTimeOffset), converted from the
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
                long minsSince1601 = this.endMinsSince1601 + this.startTimeOffset;
                DateTime untilDateTime = IcalUtil.localMinsSince1601toDate(minsSince1601, tzDef);
                recurrenceRule.append(";UNTIL=");
                java.util.TimeZone untilTZ = null;
                if (this.tzDef != null) {
                    untilTZ = this.tzDef.getTimeZone();
                }
                if (isFloating) {
                    // Use localtime.
                    recurrenceRule.append(IcalUtil.iCalDateTimeValue(
                            untilDateTime, untilTZ, isAllDay));
                } else {
                    if (isAllDay) {
                        recurrenceRule.append(IcalUtil.iCalDateTimeValue(
                                untilDateTime, untilTZ, isAllDay));
                    } else {
                        // MUST be UTC time
                        recurrenceRule.append(IcalUtil.icalUtcTime(minsSince1601, tzDef));
                    }
                }
            }
            if (hasBYDAY) {
                WeekDayList weekDayList = new WeekDayList();
                dayOfWeekMask = getDayOfWeekMask();
                for (DayOfWeek dow : dayOfWeekMask) {
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
                this.recurrenceFrequency = curr;
                return;
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
                this.patternType = curr;
                return;
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
                this.calScale = 
                MsCalScale.HIJRI;
                return;
            }
            if (patternType.equals(PatternType.HJ_MONTH_NTH)) {
                this.calScale = MsCalScale.HIJRI;
                return;
            }
        }
        for (MsCalScale curr : MsCalScale.values()) {
            if (curr.mapiPropValue() == calType) {
                this.calScale = curr;
                return;
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
                    this.endType = EndType.NEVER_END;
                } else {
                    this.endType = curr;
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
                firstDayOfWeek = curr;
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
        this.firstDateTime = ris.readU32();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer("PidLidAppointmentRecur\n");
        if (readerVersion != 0x3004) {
            buf.append("    Unexpected ReaderVersion=")
                    .append(readerVersion).append("\n");
        }
        if (writerVersion != 0x3004) {
            buf.append("    Unexpected WriterVersion=")
                    .append(writerVersion).append("\n");
        }
        buf.append("    RecurFrequency=")
                .append(getRecurrenceFrequency()).append("\n");
        buf.append("    PatternType=")
                .append(getPatternType()).append("\n");
        buf.append("    MsCalScale=")
                .append(getMsCalScale()).append("\n");
        buf.append("    FirstDateTime=")
                .append(getFirstDateTime()).append("\n");
        buf.append("    Period=")
                .append(period).append("\n");
        buf.append("    SlidingFlag=")
                .append(slidingFlag).append("\n");
        if (haveDayMask) {
            dayOfWeekMask = getDayOfWeekMask();
            buf.append("    dayMask=0x")
                    .append(Long.toHexString(dayMask)).append(" - ")
                    .append(dayOfWeekMask).append("\n");
        }
        if (haveDayOfMonth) {
                buf.append("    dayOfMonth=")
                        .append(dayOfMonth).append("\n");
        }
        if (haveWeekDayOccurNum) {
            buf.append("    weekDayOccurrenceNumber=")
                    .append(weekDayOccurrenceNumber).append("\n");
        }
        buf.append("    EndType=")
                .append(getEndType()).append("\n");
        buf.append("    occurrenceCount=")
                .append(occurrenceCount).append("\n");
        buf.append("    firstDayOfWeek=")
                .append(getFirstDayOfWeek()).append("\n");
        buf.append("    DeletedInstanceCount=")
                .append(deletedInstanceCount).append("\n");
        for (long since1601 : delMidnightMinsSince1601) {
            long timeSince1601 = since1601 + startTimeOffset;
            String suffix = new String("");
            if (changedInstances != null) {
                for (ChangedInstanceInfo cInst : changedInstances) {
                    if (cInst.getOrigStartMinsSince1601() == timeSince1601) {
                        suffix = new String(" [changed]");
                        break;
                    }
                }
            }
            infoOnLocalTimeSince1601ValWithOffset(buf,
                "        DeletedInstance=", since1601, startTimeOffset, suffix);
        }
        buf.append("    ModifedInstanceCount=")
                .append(modifiedInstanceCount).append("\n");
        for (long since1601 : modMidnightMinsSince1601) {
            infoOnLocalTimeSince1601ValWithOffset(buf,
                "        ModifiedInstance=", since1601, startTimeOffset, "");
        }
        infoOnLocalTimeSince1601Val(buf,
                "    StartDate=", startMinsSince1601);
        infoOnLocalTimeSince1601Val(buf,
                "    Last Instance's start=", endMinsSince1601);
        buf.append("    ReaderVersion2=0x")
                .append(Long.toHexString(readerVersion2)).append("\n");
        buf.append("    WriterVersion2=0x")
                .append(Long.toHexString(writerVersion2)).append("\n");
        buf.append("    StartTimeOffset=").append(startTimeOffset).append("\n");
        buf.append("    EndTimeOffset=").append(endTimeOffset).append("\n");
        buf.append("    ExceptionCount=").append(exceptionCount).append("\n");
        if (changedInstances != null) {
            for (ChangedInstanceInfo cInst : changedInstances) {
                buf.append(cInst.toString());
            }
        }
        if (rsrvdBlock1Size != -1) {
            buf.append("    rsrvdBlock1Size=").append(rsrvdBlock1Size).append("\n");
            buf.append("    rsrvdBlock1=")
                .append(TNEFUtils.toHexString((byte[])rsrvdBlock1, (int)rsrvdBlock1Size))
                .append("\n");
        }
        if (rsrvdBlock2Size != -1) {
            buf.append("    rsrvdBlock2Size=").append(rsrvdBlock2Size).append("\n");
            buf.append("    rsrvdBlock2=")
                .append(TNEFUtils.toHexString((byte[])rsrvdBlock2, (int)rsrvdBlock2Size))
                .append("\n");
        }
        if (unprocessedByteCount != 0L) {
            buf.append("    unprocessedByteCount=").append(unprocessedByteCount).append("\n");
            buf.append("    unprocessedBytes=")
                .append(TNEFUtils.toHexString((byte[])unprocessedBytes, (int)unprocessedByteCount))
                .append("\n");
        }
        return buf.toString();
    }
    
    private StringBuffer infoOnLocalTimeSince1601Val(StringBuffer buf, String desc,
            long localTimeSince1601) {
        buf.append(desc);
        buf.append(IcalUtil.friendlyLocalTime(localTimeSince1601, tzDef));
        buf.append(" (").append(IcalUtil.icalUtcTime(localTimeSince1601, tzDef));
        buf.append(") [");
        buf.append(localTimeSince1601);
        buf.append(" (0x");
        buf.append(Long.toHexString(localTimeSince1601));
        buf.append(")]\n");
        return buf;
    }
    
    private StringBuffer infoOnLocalTimeSince1601ValWithOffset(StringBuffer buf, String desc,
            long localTimeSince1601, long startOffset, String suffix) {
        long since1601 = localTimeSince1601 + startOffset;
        buf.append(desc);
        buf.append(IcalUtil.friendlyLocalTime(since1601, tzDef));
        buf.append(" (").append(IcalUtil.icalUtcTime(since1601, tzDef));
        buf.append(") [");
        buf.append(localTimeSince1601);
        buf.append(" + ");
        buf.append(startOffset);
        buf.append("]");
        buf.append(suffix);
        buf.append("\n");
        return buf;
    }

}
