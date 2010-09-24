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

import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;
import java.io.IOException;
import java.util.GregorianCalendar;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.ZimbraLog;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.UtcOffset;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.RRule;
import net.freeutils.tnef.RawInputStream;

public class TZRule {

    static Log sLog = ZimbraLog.tnef;

    private boolean isEffectiveRule;
    private boolean isAssociatedWithRecurrence;
    private int StartYear;
    private int Bias;
    private int StandardBias;
    private int DaylightBias;
    private SYSTEMTIME StandardDate;
    private SYSTEMTIME DaylightDate;

    public TZRule(RawInputStream ris) throws IOException {
        int MajorVersion = ris.readU8();  // 0x02
        int MinorVersion = ris.readU8();  // 0x01
        int ReservedInt = ris.readU16();  // 0x003E
        
        // TZRuleFlags Little Endian 000000ER000000000
        //     R (TZRULE_FLAG_RECUR_CURRENT_TZREG, 0x0001): rule is associated with a recurring series.
        //     E (TZRULE_FLAG_EFFECTIVE_TZREG, 0x0002): This is the effective rule.
        // If this rule represents the time zone rule that will be used to convert to and from UTC, both of these
        // flags are set (for example, the value is 0x0003). If this is not the active time zone rule, neither of
        // these flags are set. These flags are set on exactly one TZRule that is contained in this property, and
        // the flags for all other rules MUST be set to 0.
        int TZRuleFlags = ris.readU16();  
        isAssociatedWithRecurrence = ( (TZRuleFlags & 0x0001) == 0x0001);
        isEffectiveRule = ( (TZRuleFlags & 0x0002) == 0x0002);
        setStartYear(ris.readU16());
        ris.skip(14);
        setBias(readI32(ris));
        setStandardBias(readI32(ris));
        setDaylightBias(readI32(ris));
        setStandardDate(new SYSTEMTIME(ris));
        setDaylightDate(new SYSTEMTIME(ris));

        if (sLog.isDebugEnabled()) {
            StringBuffer debugInfo = new StringBuffer();
            debugInfo.append("TZRule: effective=");
            debugInfo.append(isEffectiveRule);
            debugInfo.append(" AssociatedWithRecurrence=");
            debugInfo.append(isAssociatedWithRecurrence);
            debugInfo.append("\n");
            if (MajorVersion != 0x2) {
                debugInfo.append("    Unexpected MajorVersion=");
                debugInfo.append(MajorVersion);
                debugInfo.append("\n");
            }
            if (MinorVersion != 0x1) {
                debugInfo.append("    Unexpected MinorVersion=");
                debugInfo.append(MinorVersion);
                debugInfo.append("\n");
            }
            if (ReservedInt != 0x3E) {
                debugInfo.append("    Unexpected Reserved=0x");
                debugInfo.append(Integer.toHexString(ReservedInt));
                debugInfo.append("\n");
            }
            debugInfo.append("    Start Year=");
            debugInfo.append(getStartYear());
            debugInfo.append(" Bias=");
            debugInfo.append(Bias);
            debugInfo.append(" StandardBias=");
            debugInfo.append(getStandardBias());
            debugInfo.append(" DaylightBias=");
            debugInfo.append(getDaylightBias());
            debugInfo.append("\n");
            debugInfo.append("    standard info:");
            if (getStandardDate() != null) {
                debugInfo.append(getStandardDate());
                debugInfo.append("\n");
            }
            if (getDaylightDate() != null) {
                debugInfo.append("    daylight info:");
                debugInfo.append(getDaylightDate());
            }
            sLog.debug(debugInfo);
        }
    }

    public TZRule() throws IOException {
        isAssociatedWithRecurrence = true;
        isEffectiveRule = true;
        setStartYear(1971);
    }
    /**
     *
     * @return The RRrule for use in an ICALENDAR STANDARD component
     *         or null if not applicable
     */
    public RRule icalStandardRRule() {
        RRule theRule = null;
        try {
            theRule = new RRule(null, icalStandardRRuleString());
        } catch (ParseException e) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("Parse problem processing STANDARD rule", e);
            }
            theRule = null;
        }
        return theRule;
    }

    /**
     *
     * @return The RRrule for use in an ICALENDAR DAYLIGHT component
     *         or null if not applicable
     */
    public RRule icalDaylightRRule() {
        RRule theRule = null;
        try {
            theRule = new RRule(null, icalDaylightRRuleString());
        } catch (ParseException e) {
            if (sLog.isDebugEnabled()) {
                sLog.debug("Parse problem processing DAYLIGHT rule", e);
            }
            theRule = null;
        }
        return theRule;
    }

    /**
     *
     * @return The RRULE String for use in an ICALENDAR STANDARD component
     *         or null if not applicable
     */
    private String icalStandardRRuleString() {
        if (!hasDaylightSaving()) {
            return null;
        }
        // If getYear() is non-zero, rule only applies to this year
        // Zimbra only supports timezones with 1 yearly standard/daylight
        // rule or just a standard rule.  This probably maps to the best
        // behavior in this situation.
        StringBuffer rrule = new StringBuffer("FREQ=YEARLY;BYDAY=");
        DayOfWeek dow = getStandardDate().getDayOfWeek();
        int occurNum = getStandardDate().getDay();
        rrule.append(rruleDayInfo(dow, occurNum));
        rrule.append(";BYMONTH=");
        rrule.append(getStandardDate().getMonth());
        return rrule.toString();
    }

    /**
     *
     * @return The RRULE String for use in an ICALENDAR DAYLIGHT component
     *         or null if not applicable
     */
    private String icalDaylightRRuleString() {
        if (!hasDaylightSaving()) {
            return null;
        }
        StringBuffer rrule = new StringBuffer("FREQ=YEARLY;BYDAY=");
        DayOfWeek dow = getDaylightDate().getDayOfWeek();
        int occurNum = getDaylightDate().getDay();
        rrule.append(rruleDayInfo(dow, occurNum));
        rrule.append(";BYMONTH=");
        rrule.append(getDaylightDate().getMonth());
        return rrule.toString();
    }

    public boolean daylightStartsOnFinalDowInMonth() {
        return (getDaylightDate().getDay() == 5);
    }

    public boolean standardStartsOnFinalDowInMonth() {
        return (getStandardDate().getDay() == 5);
    }

    /**
     *
     * @param dow
     * @param occurNum
     * @return
     */
    private String rruleDayInfo(DayOfWeek dow, int occurNum) {
        StringBuffer dayInfo = new StringBuffer();
        if (occurNum == 5)
            occurNum = -1;  // Means "last"
        dayInfo.append(occurNum);
        dayInfo.append(dow);
        return dayInfo.toString();
    }

    /**
     * Missing method from RawInputStream
     * @param ris
     * @return
     * @throws IOException
     */
    private Long readI32(RawInputStream ris) throws IOException {
        return (ris.readU8() | (ris.readU8() << 8) | (ris.readU8() << 16) | (ris.readU8() << 24)) & 0xFFFFFFFFFFFFFFFFL;
    }

    /**
     * @return the isEffectiveRule
     */
    public boolean isEffectiveRule() {
        return isEffectiveRule;
    }

    /**
     * @return the isAssociatedWithRecurrence
     */
    public boolean isAssociatedWithRecurrence() {
        return isAssociatedWithRecurrence;
    }

    /**
     * @param startYear the startYear to set
     */
    public void setStartYear(int startYear) {
        StartYear = startYear;
    }

    /**
     * @return the startYear
     */
    public int getStartYear() {
        return StartYear;
    }

    /**
     * @param bias the time zone's offset in minutes from UTC.
     */
    public void setBias(Long bias) {
        Bias = bias.intValue();
    }
    
    /**
     * @param bias the time zone's offset in minutes from UTC.
     */
    public void setBias(int bias) {
        Bias = bias;
    }

    /**
     * @return the time zone's offset in minutes from UTC.
     */
    public int getBias() {
        return Bias;
    }

    /**
     * @param standardBias the offset in minutes from Bias during standard time.
     */
    public void setStandardBias(Long standardBias) {
        StandardBias = standardBias.intValue();
    }
    
    /**
     * 
     * @param standardBias the offset in minutes from Bias during standard time.
     */
    public void setStandardBias(int standardBias) {
        StandardBias = standardBias;
    }

    /**
     * @return the offset in minutes from Bias during standard time.
     */
    public int getStandardBias() {
        return StandardBias;
    }

    /**
     * @param the offset in minutes from Bias during daylight saving time.
     */
    public void setDaylightBias(Long daylightBias) {
        DaylightBias = daylightBias.intValue();
    }
    
    /**
     * @param the offset in minutes from Bias during daylight saving time.
     */
    public void setDaylightBias(int daylightBias) {
        DaylightBias = daylightBias;
    }

    /**
     * @return the offset in minutes from Bias during daylight saving time.
     */
    public int getDaylightBias() {
        return DaylightBias;
    }

    /**
     * @param standardDate the standardDate to set
     */
    public void setStandardDate(SYSTEMTIME standardDate) {
        StandardDate = standardDate;
    }

    /**
     * @return the standardDate
     */
    public SYSTEMTIME getStandardDate() {
        return StandardDate;
    }

    /**
     * @param daylightDate the daylightDate to set
     */
    public void setDaylightDate(SYSTEMTIME daylightDate) {
        DaylightDate = daylightDate;
    }

    /**
     * @return the daylightDate
     */
    public SYSTEMTIME getDaylightDate() {
        return DaylightDate;
    }

    /**
     *
     * @return Offset of STANDARD time from GMT (based on milliseconds)
     */
    public int getStandardUtcOffsetMillis() {
        int offset = -1 * ((getBias() + getStandardBias()) * 60 * 1000);
        return (offset);
    }

    /**
     *
     * @return Offset of DAYLIGHT time from GMT (based on milliseconds)
     */
    public int getDaylightUtcOffsetMillis() {
        int offset = -1 * ((getBias() + getDaylightBias()) * 60 * 1000);
        return (offset);
    }

    /**
     *
     * @return Offset of STANDARD time from GMT (based on milliseconds)
     */
    public UtcOffset getStandardUtcOffset() {
        long offset = -1 * ((getBias() + getStandardBias()) * 60 * 1000);
        return new UtcOffset(offset);
    }

    /**
     *
     * @return Offset of DAYLIGHT time from GMT (based on milliseconds)
     */
    public UtcOffset getDaylightUtcOffset() {
        long offset = -1 * ((getBias() + getDaylightBias()) * 60 * 1000);
        return new UtcOffset(offset);
    }

    public boolean hasDaylightSaving() {
        if (getStandardDate().getMonth() == 0) {
            return false;
        } else {
            return true;
        }
    }

    public DtStart getStandardDtStart() {
        return getDtStart(getStandardDate().getHour(),
                    getStandardDate().getMinute());
    }

    public DtStart getDaylightDtStart() {
        return getDtStart(getDaylightDate().getHour(),
                    getDaylightDate().getMinute());
    }

    /**
     *
     *   From initial TNEF to iCalendar Spec.
     *       The DTSTART property can be hard-coded to 19710101T000000. This is a value that
     *       works well across many calendar client apps.
     *   Note: Some examples seen are similar to this but include the hour of the transition.
     *         Suspect that is useful - so including it.
     *   Zimbra replaces our timezones with closest matchin known ones, so not worth trying
     *   to improve this to see if can choose an accurate start date/time in 1971.
     *
     * @param hr
     * @param min
     * @return
     */
    private DtStart getDtStart(int hr, int min) {
        TimeZone utcZone = TimeZone.getTimeZone("UTC");
        GregorianCalendar gc = new GregorianCalendar(1971, 0 /* zero based */, 1, hr, min);
        gc.setTimeZone(utcZone);
        Date startDate = gc.getTime();
        DateTime startDateTime = new DateTime(startDate);
        return new DtStart(startDateTime);
    }

    public boolean equivalentRule(TZRule other) {
        if (other == null) {
            return false;
        }
        if (Bias != other.getBias()) {
            return false;
        }
        if (StandardBias != other.getStandardBias()) {
            return false;
        }
        if (DaylightBias != other.getDaylightBias()) {
            return false;
        }
        if (!this.getStandardDate().equivalentInTimeZones(other.getStandardDate())) {
            return false;
        }
        if (!this.getDaylightDate().equivalentInTimeZones(other.getDaylightDate())) {
            return false;
        }
        return true;
    }
}
