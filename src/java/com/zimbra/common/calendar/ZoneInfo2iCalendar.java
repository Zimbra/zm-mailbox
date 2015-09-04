/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2011, 2012, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.common.calendar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;
import java.util.regex.Pattern;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.ParameterList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.UtcOffset;
import net.fortuna.ical4j.model.component.Daylight;
import net.fortuna.ical4j.model.component.Observance;
import net.fortuna.ical4j.model.component.Standard;
import net.fortuna.ical4j.model.component.VTimeZone;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.LastModified;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.TzId;
import net.fortuna.ical4j.model.property.TzName;
import net.fortuna.ical4j.model.property.TzOffsetFrom;
import net.fortuna.ical4j.model.property.TzOffsetTo;
import net.fortuna.ical4j.model.property.XProperty;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import com.google.common.collect.Maps;
import com.zimbra.common.calendar.ZoneInfoParser.Day;
import com.zimbra.common.calendar.ZoneInfoParser.Day.DayType;
import com.zimbra.common.calendar.ZoneInfoParser.Rule;
import com.zimbra.common.calendar.ZoneInfoParser.RuleLine;
import com.zimbra.common.calendar.ZoneInfoParser.Time;
import com.zimbra.common.calendar.ZoneInfoParser.Until;
import com.zimbra.common.calendar.ZoneInfoParser.Weekday;
import com.zimbra.common.calendar.ZoneInfoParser.Zone;
import com.zimbra.common.calendar.ZoneInfoParser.ZoneLine;

public class ZoneInfo2iCalendar {

    private static final String CRLF = "\r\n";

    // these are the characters that MUST be escaped: , ; " \n and \ -- note that \
    // becomes \\\\ here because it is double-unescaped during the compile process!
    private static final Pattern MUST_ESCAPE = Pattern.compile("[,;\"\n\\\\]");
    private static final Pattern SIMPLE_ESCAPE = Pattern.compile("([,;\"\\\\])");
    private static final Pattern NEWLINE_CRLF_ESCAPE = Pattern.compile("\r\n");
    private static final Pattern NEWLINE_BARE_CR_OR_LF_ESCAPE = Pattern.compile("[\r\n]");

    public static String iCalEscape(String str) {
        if (str!= null && MUST_ESCAPE.matcher(str).find()) {
            // escape ([,;"])'s
            String toRet = SIMPLE_ESCAPE.matcher(str).replaceAll("\\\\$1");
            // escape CR and LF combos
            toRet = NEWLINE_CRLF_ESCAPE.matcher(toRet).replaceAll("\\\\n");
            toRet = NEWLINE_BARE_CR_OR_LF_ESCAPE.matcher(toRet).replaceAll("\\\\n");
            return toRet;
        }
        return str;
    }

    private static String getUtcOffset(Time time) {
        int sec = time.getSecond();
        String sign = time.isNegative() ? "-" : "+";
        if (sec == 0)
            return String.format("%s%02d%02d", sign, time.getHour(), time.getMinute());
        else
            return String.format("%s%02d%02d%02d", sign, time.getHour(), time.getMinute(), sec);
    }

    private static List<RuleLine> getRuleLinesForYear(List<RuleLine> ruleLines, int year) {
        List<RuleLine> result = new ArrayList<RuleLine>();
        for (RuleLine rline : ruleLines) {
            if (rline.getFromYear() <= year && rline.getToYear() >= year)
                result.add(rline);
        }
        return result;
    }

    private static String weekdayToICalWkday(Weekday wkday) {
        String val = null;
        switch (wkday) {
        case SUNDAY:
            val = "SU";
            break;
        case MONDAY:
            val = "MO";
            break;
        case TUESDAY:
            val = "TU";
            break;
        case WEDNESDAY:
            val = "WE";
            break;
        case THURSDAY:
            val = "TH";
            break;
        case FRIDAY:
            val = "FR";
            break;
        case SATURDAY:
            val = "SA";
            break;
        }
        return val;
    }

    private static String dayToICalRRulePart(int hintYear, int hintMonth, Day day) {
        DayType type = day.getType();

        int weeknum = day.getWeeknum();
        Weekday wkday = day.getWeekday();
        int date = day.getDate();

        // Turn ON rules into WEEKNUM rules using the wkday of the monthday in hintYear/hintMonth.
        if (DayType.ON.equals(type)) {
            Calendar hintDay = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            hintDay.set(hintYear, hintMonth - 1, date, 0, 0, 0);
            hintDay.set(Calendar.MILLISECOND, 0);
            int calWkday = hintDay.get(Calendar.DAY_OF_WEEK);
            wkday = Weekday.lookUp(calWkday);
            assert(wkday != null);
            weeknum = (date - 1) / 7 + 1;
            // Did they mean "last week" rather than "week 4"?
            if (hintDay.getActualMaximum(Calendar.DAY_OF_MONTH) - date < 7)
                weeknum = -1;
            type = DayType.WEEKNUM;
        }

        String icalWkday = weekdayToICalWkday(wkday);

        // Turn [ON_OR_]BEFORE/AFTER rules into WEEKNUM rules using the wkday of the monthday in hintYear/hintMonth.
        // Simplify: < to <=, > to >=.
        if (DayType.BEFORE.equals(type)) {
            type = DayType.ON_OR_BEFORE;
            --date;
        } else if (DayType.AFTER.equals(type)) {
            type = DayType.ON_OR_AFTER;
            ++date;
        }
        if (DayType.ON_OR_BEFORE.equals(type) || DayType.ON_OR_AFTER.equals(type)) {
            Calendar hintDay = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            hintDay.set(hintYear, hintMonth - 1, date, 0, 0, 0);
            hintDay.set(Calendar.MILLISECOND, 0);
            int numDaysInMonth = hintDay.getActualMaximum(Calendar.DAY_OF_MONTH);
            int calWkday = hintDay.get(Calendar.DAY_OF_WEEK);
            int wkdayInt = Weekday.toInt(wkday);
            int newDate;
            if (DayType.ON_OR_BEFORE.equals(type)) {
                // search backward
                if (calWkday > wkdayInt)  // e.g calWkday=Wed(4), wkdayInt=Sun(1) => back 3
                    newDate = date - (calWkday - wkdayInt);
                else  // eg. calWkday=Wed(4), wkdayInt=Fri(6) => back 5 = back 7-2
                    newDate = date - (7 + (calWkday - wkdayInt));
            } else {
                // search forward
                if (calWkday > wkdayInt)  // e.g calWkday=Wed(4), wkdayInt=Sun(1) => forward 4 = forward 7 - 3
                    newDate = date + (7 + (wkdayInt - calWkday));
                else  // eg. calWkday=Wed(4), wkdayInt=Fri(6) => forward 2
                    newDate = date + (wkdayInt - calWkday);
            }
            if (newDate >= 1 && newDate <= numDaysInMonth) {
                weeknum = (newDate - 1) / 7 + 1;
                // Did they mean "last week" rather than "week 4"?
                if (numDaysInMonth - newDate < 7)
                    weeknum = -1;
                type = DayType.WEEKNUM;
            }
        }
        // If we couldn't convert BYMONTHDAY rule to BYDAY rule for any reason, we have no choice but
        // to use the ugly BYMONTHDAY rule that lists 7 dates.
        // week 1: 1-7
        // week 2: 8-14
        // week 3: 15 - 21
        // week 4: 22 - 28
        // week 5: 29 - 31
        if (DayType.ON_OR_BEFORE.equals(type)) {
            if (date % 7 == 0) {  // days 7, 14, 21 and 28 only
                weeknum = (date - 1) / 7 + 1;
                type = DayType.WEEKNUM;
            } else {
                // Can't be done in WEEKNUM style.  We need to use a more verbose iCalendar recurrence
                // syntax.   Let's use the example of "Sun<=25".  This means the last Sunday of the month
                // on or before the 25th.  We can express this in iCalendar with 2 conditions ANDed together:
                // 1) BYDAY=SU (all Sundays in the month)
                // 2) BYMONTHDAY=19,20,21,22,23,24,25 (7 days ending on the given date)
                StringBuilder sb = new StringBuilder("BYDAY=");
                sb.append(icalWkday).append(";BYMONTHDAY=");
                int minDate = Math.max(date - 6, 1);
                sb.append(minDate);
                for (int i = minDate + 1; i <= date; ++i) {
                    sb.append(",").append(i);
                }
                return sb.toString();
            }
        } else if (DayType.ON_OR_AFTER.equals(type)) {
            if (date % 7 == 1) {  // days 1, 8, 15, 22, and 29 only
                weeknum = date / 7 + 1;
                type = DayType.WEEKNUM;
            } else {
                // Similar to ON_OR_BEFORE case above.  Combine BYDAY and BYMONTHDAY rules.
                // Example: "Sat>=13" means the first Sunday on or after the 9th.
                // 1) BYDAY=SA (all Saturdays in the month)
                // 2) BYMONTHDAY=13,14,15,16,17,18,19 (7 days starting on the given date)
                StringBuilder sb = new StringBuilder("BYDAY=");
                sb.append(icalWkday).append(";BYMONTHDAY=");
                sb.append(date);
                int maxDate = Math.min(date + 6, 31);
                for (int i = date + 1; i <= maxDate; ++i) {
                    sb.append(",").append(i);
                }
                return sb.toString();
            }
        }

        assert(DayType.WEEKNUM.equals(type));
        if (weeknum > 4)
            weeknum = -1;
        return String.format("BYDAY=%d%s", weeknum, icalWkday);
    }

    // Add to Time objects, limiting hour to 0-23 range.
    private static Time addTimes(Time t1, Time t2) {
        int sum = t1.getDuration() + t2.getDuration();
        boolean neg = sum < 0;
        sum = Math.abs(sum);
        int hour = (sum / 3600) % 24;
        int min = (sum / 60) % 60;
        int sec = sum % 60;
        return new Time(neg, hour, min, sec, Time.TimeType.WALL_TIME);
    }

    // Subtract t2 from t1.  Hour is limited to 0-23.
    private static Time subtractTimes(Time t1, Time t2) {
        Time t2neg = new Time(!t2.isNegative(), t2.getHour(), t2.getMinute(), t2.getSecond(), t2.getType());
        return addTimes(t1, t2neg);
    }

    private static DtStart msOutlookStyleDtStart = null;
    private static DtStart getDtStart(String dateString) {
        DtStart dtStart = null;
        try {
            dtStart = new DtStart(dateString);
        } catch (ParseException e) {
        }
        return dtStart;
    }

    private static DtStart getMsOutlookStyleDtstart() {
        if (msOutlookStyleDtStart != null) {
            return msOutlookStyleDtStart;
        }
        // YYYYMMDDThhmmss fixed to 16010101T000000 (MS Outlook style)
        msOutlookStyleDtStart = getDtStart("16010101T000000");
        return msOutlookStyleDtStart;
    }

    private static Observance toObservanceComp(int hintYear, RuleLine rline, boolean isStandard,
                                          Time standardOffset, Time daylightOffset,
                                          String tznameFormat) {
        PropertyList props = new PropertyList();
        if (tznameFormat != null && tznameFormat.length() > 0) {
            String tzname = null;
            if (tznameFormat.contains("%s")) {
                String letter = rline.getLetter();
                if (letter == null || letter.equals("-"))
                    letter = "";
                tzname = String.format(tznameFormat, letter);
            } else {
                tzname = tznameFormat;
            }
            if (tzname != null) {
                props.add(new TzName(tzname));
            }
        }
        Time at = rline.getAt();
        Time onset;
        switch (at.getType()) {
        case STANDARD_TIME:
            if (isStandard) {
                // We're moving from daylight time to standard time.  In iCalendar we want hh:mm:ss in
                // wall clock in the pre-transition time, so it's daylight time.
                // daylight = utc + daylight offset = (standard - standard offset) + daylight offset
                onset = addTimes(subtractTimes(at, standardOffset), daylightOffset);
            } else {
                // We're moving from standard time to daylight time.  In iCalendar we want hh:mm:ss in
                // wall clock in the pre-transition time, so it's standard time.  at is already in
                // standard time.
                onset = at;
            }
            break;
        case UTC_TIME:
            if (isStandard) {
                // We're moving from daylight time to standard time.  In iCalendar we want hh:mm:ss in
                // wall clock in the pre-transition time, so it's daylight time.
                // daylight = utc + daylightOffset.
                onset = addTimes(at, daylightOffset);
            } else {
                // We're moving from standard time to daylight time.  In iCalendar we want hh:mm:ss in
                // wall clock in the pre-transition time, so it's standard time.
                // standard = utc + standard offset.
                onset = addTimes(at, standardOffset);
            }
            break;
        default:  // WALL_TIME
            // at is already in the iCalendar style.
            onset = at;
            break;
        }
        int hh = onset.getHour();
        int mm = onset.getMinute();
        int ss = onset.getSecond();
        if (hh >= 24) {
            // Hour should be between 0 and 23, but sometimes we can get 24:00:00 from the zoneinfo source.
            // Since hour part in iCalendar only allows 0-23, let's approximate any time with hour >= 24 to
            // 23:59:59.
            hh = 23; mm = 59; ss = 59;
        }
        // YYYYMMDD fixed to 16010101 (MS Outlook style)
        props.add(getDtStart(String.format("16010101T%02d%02d%02d", hh, mm, ss)));
        Time toOffset, fromOffset;
        if (isStandard) {
            toOffset = standardOffset;
            fromOffset = daylightOffset;
        } else {
            toOffset = daylightOffset;
            fromOffset = standardOffset;
        }
        props.add(new TzOffsetTo(new UtcOffset(getUtcOffset(toOffset))));
        props.add(new TzOffsetFrom(new UtcOffset(getUtcOffset(fromOffset))));
        int month = rline.getIn();
        StringBuilder rruleVal = new StringBuilder();
        rruleVal.append("FREQ=YEARLY;WKST=MO;INTERVAL=1;BYMONTH=").append(month).append(";");
        rruleVal.append(dayToICalRRulePart(hintYear, month, rline.getOn()));
        try {
            RRule rrule = new RRule(new ParameterList(), rruleVal.toString());
            props.add(rrule);
        } catch (ParseException e) {
        }
        if (isStandard) {
            return new Standard(props);
        } else {
            return new Daylight(props);
        }
    }

    private static Standard toStandardComp(Time gmtOffset, String tznameFormat) {
        PropertyList props = new PropertyList();
        if (tznameFormat != null && tznameFormat.length() > 0 && !tznameFormat.contains("%")) {
            props.add(new TzName(iCalEscape(tznameFormat)));
        }
        props.add(getMsOutlookStyleDtstart());
        String offset = getUtcOffset(gmtOffset);
        UtcOffset utcOffset = new UtcOffset(offset);
        props.add(new TzOffsetTo(utcOffset));
        props.add(new TzOffsetFrom(utcOffset));
        return new Standard(props);
    }

    private static LastModified getLastModified(String lastModified) {
        LastModified lMod = null;
        DateTime dt;
        try {
            dt = new DateTime(lastModified);
            lMod = new LastModified(dt);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            System.err.println("String turning into LAST-MODIFIED: " + lastModified);
            e.printStackTrace();
            System.exit(1);
        }
        return lMod;
    }

    private static VTimeZone toVTimeZoneComp(int hintYear, ZoneLine zline, LastModified lastModified,
                Set<String> tzAliases, boolean isPrimary, Integer matchScore) {
        PropertyList vtzProps = new PropertyList();
        vtzProps.add(new TzId(zline.getName()));
        vtzProps.add(lastModified);
        if (isPrimary) {
            vtzProps.add(new XProperty(TZIDMapper.X_ZIMBRA_TZ_PRIMARY,"TRUE"));
        }
        if (matchScore != null) {
            vtzProps.add(new XProperty(TZIDMapper.X_ZIMBRA_TZ_MATCH_SCORE, matchScore.toString()));
        }
        if (tzAliases != null) {
            for (String alias : tzAliases) {
                vtzProps.add(new XProperty(TZIDMapper.X_ZIMBRA_TZ_ALIAS, alias));
            }
        }
        VTimeZone vtz = new VTimeZone(vtzProps);
        String tznameFormat = zline.getAbbrevFormat();
        if (zline.hasRule()) {
            Rule rule = zline.getRule();
            List<RuleLine> rlines = getRuleLinesForYear(rule.getRuleLines(), hintYear);
            RuleLine standard = null;
            RuleLine daylight = null;
            for (RuleLine rline : rlines) {
                if (rline.getSave().getDuration() == 0)
                    standard = rline;
                else
                    daylight = rline;
            }
            if (standard != null && daylight != null) {
                Time standarfOffset = zline.getGmtOff();
                Time daylightOffset = addTimes(standarfOffset, daylight.getSave());
                vtz.getObservances().add(toObservanceComp(
                        hintYear, standard, true, standarfOffset, daylightOffset, tznameFormat));
                vtz.getObservances().add(
                        toObservanceComp(hintYear, daylight, false, standarfOffset, daylightOffset, tznameFormat));
            } else {
                vtz.getObservances().add(toStandardComp(zline.getGmtOff(), tznameFormat));
            }
        } else {
            vtz.getObservances().add(toStandardComp(addTimes(zline.getGmtOff(), zline.getSave()), tznameFormat));
        }
        return vtz;
    }

    private static ZoneLine getCurrentZoneLine(Zone zone) {
        Set<ZoneLine> zlines = zone.getZoneLines();
        for (ZoneLine zline : zlines) {
            if (zline.hasUntil())
                continue;
            return zline;
        }
        return null;
    }

    private static ZoneLine getZoneLineForYear(Zone zone, Calendar referenceDate) {
        Until referenceUntil = new Until(referenceDate);
        Set<ZoneLine> zlines = zone.getZoneLines();
        for (ZoneLine zline : zlines) {
            Until until = zline.getUntil();
            if (until != null) {
                if (until.compareTo(referenceUntil) < 0) {
                    continue;
                }
            }
            return zline;
        }
        return null;
    }

    private static class ZoneComparatorByGmtOffset implements Comparator<Zone> {

        @Override
        public int compare(Zone z1, Zone z2) {
            if (z1 == null && z2 == null)
                return 0;
            else if (z1 == null)
                return -1;
            else if (z2 == null)
                return 1;

            ZoneLine zl1 = getCurrentZoneLine(z1);
            ZoneLine zl2 = getCurrentZoneLine(z2);
            if (zl1 == null && zl2 == null)
                return 0;
            else if (zl1 == null)
                return -1;
            else if (zl2 == null)
                return 1;
            int off1 = zl1.getGmtOff().getDuration();
            int off2 = zl2.getGmtOff().getDuration();
            int offDiff = off1 - off2;
            if (offDiff != 0)
                return offDiff;

            String name1 = z1.getName();
            String name2 = z2.getName();
            if (name1 == null && name2 == null)
                return 0;
            else if (name1 == null)
                return -1;
            else if (name2 == null)
                return 1;
            else
                return name1.compareTo(name2);
        }
    }

    private static Set<String /* TZID */> sPrimaryTZIDs = new HashSet<String>();
    private static Map<String /* TZID */, Integer /* matchScore */> sMatchScores = new HashMap<String, Integer>();

    private static enum LineType {
        PRIMARYZONE, ZONEMATCHSCORE, UNKNOWN;

        public static LineType lookUp(String str) {
            LineType lt = UNKNOWN;
            if (str != null) {
                try {
                    lt = valueOf(str.toUpperCase());
                } catch (IllegalArgumentException e) {}
            }
            return lt;
        }
    }

    // Read the file containing PrimaryZone and ZoneMatchScore lines.  PrimaryZone has one argument, a TZID.
    // A primary time zone is listed in web client's TZ selection list.  ZoneMatchScore has two arguments, TZID
    // and an integer match score.  Score is used to prioritize time zones with identical GMT offsets and
    // DST rules (or lack thereof) when looking up a system time zone that best matches a given time zone.
    private static void readExtraData(Reader reader) throws IOException, ParseException {
        char dquote = '"';
        StreamTokenizer tokenizer = new StreamTokenizer(reader);
        tokenizer.resetSyntax();
        tokenizer.wordChars(32, 126);
        tokenizer.whitespaceChars(' ', ' ');
        tokenizer.whitespaceChars('\t', '\t');
        tokenizer.whitespaceChars(0, 20);
        tokenizer.commentChar('#');
        tokenizer.quoteChar(dquote);
        tokenizer.eolIsSignificant(true);

        List<String> tokenList = new ArrayList<String>();
        LineType lineType = LineType.UNKNOWN;
        boolean atLineStart = true;

        int ttype;
        int prevTtype = StreamTokenizer.TT_EOL;  // used for empty line detection
        while ((ttype = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
            int lineNum = tokenizer.lineno();
            if (ttype == StreamTokenizer.TT_WORD || ttype == dquote) {
                String token = tokenizer.sval;
                if (atLineStart) {
                    lineType = LineType.lookUp(token);
                    if (LineType.UNKNOWN.equals(lineType))
                        throw new ParseException("Invalid line type", lineNum);
                } else {
                    tokenList.add(token);
                }
                atLineStart = false;
            } else if (ttype == StreamTokenizer.TT_EOL) {
                if (prevTtype == StreamTokenizer.TT_EOL) {
                    prevTtype = ttype;
                    continue;
                }
                atLineStart = true;
                switch (lineType) {
                case PRIMARYZONE:
                    if (tokenList.size() < 1)
                        throw new ParseException("Not enough fields in a PrimaryZone line", lineNum);
                    String primaryTZID = tokenList.get(0);
                    sPrimaryTZIDs.add(primaryTZID);
                    break;
                case ZONEMATCHSCORE:
                    if (tokenList.size() < 2)
                        throw new ParseException("Not enough fields in a ZoneMatchScore line", lineNum);
                    String zoneName = tokenList.get(0);
                    String zoneMatchScoreStr = tokenList.get(1);
                    int zoneMatchScore = 0;
                    try {
                        zoneMatchScore = Integer.parseInt(zoneMatchScoreStr);
                    } catch (NumberFormatException e) {
                        throw new ParseException("Zone match score must be an integer: " + zoneMatchScoreStr, lineNum);
                    }
                    sMatchScores.put(zoneName, zoneMatchScore);
                    break;
                }
                if (atLineStart) {
                    tokenList.clear();
                    lineType = LineType.UNKNOWN;
                }
            } else if (ttype == StreamTokenizer.TT_NUMBER) {
                // shouldn't happen
                throw new ParseException("Invalid parser state: TT_NUMBER found", lineNum);
            }
            prevTtype = ttype;
        }
    }



    // command line handling

    private static final String OPT_HELP = "h";
    private static final String OPT_TZDATA_DIR = "t";
    private static final String OPT_EXTRA_DATA_FILE = "e";
    private static final String OPT_OUTPUT_FILE = "o";
    private static final String OPT_YEAR = "y";
    private static final String OPT_DATE = "d";
    private static final String OPT_LAST_MODIFIED = "last-modified";
    private static final String OPT_OLD_TIMEZONES_FILE = "old-timezones-file";

    private static Options sOptions = new Options();

    static {
        sOptions.addOption(OPT_HELP, "help", false, "Show help (this output)");
        sOptions.addOption(OPT_TZDATA_DIR, "tzdata-dir", true, "directory containing tzdata source files");
        sOptions.addOption(OPT_EXTRA_DATA_FILE, "extra-data-file", true,
                "file containing list of primary time zones and match scores");
        sOptions.addOption(OPT_OUTPUT_FILE, "output-file", true, "output file; data is written to stdout by default");
        sOptions.addOption(OPT_DATE, "date", true,
                "reference date for determining simplified DST rules.\nUse format yyyy-MM-dd (default is today's date)");
        sOptions.addOption(null, OPT_LAST_MODIFIED, true, "LAST-MODIFIED value; current time by default");
        sOptions.addOption(null, OPT_OLD_TIMEZONES_FILE, true, "Old timezones.ics file - used for minimizing changes");
        sOptions.addOption(OPT_YEAR, "year", true,
                "4 digit year (DEPRECATED - use date)");
    }

    private static void usage(String errmsg) {
        if (errmsg != null) {
            System.err.println(errmsg);
        }

        String usage = "zmtzdata2ical <options> [tzdata source files ...]";
        Options opts = sOptions;
        PrintWriter pw = new PrintWriter(System.err, true);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(pw, formatter.getWidth(), usage,
                null, opts, formatter.getLeftPadding(), formatter.getDescPadding(),
                null);
        pw.flush();
    }

    private static CommandLine parseArgs(String args[]) throws org.apache.commons.cli.ParseException {
        CommandLineParser parser = new GnuParser();
        CommandLine cl = null;
        try {
            cl = parser.parse(sOptions, args);
        } catch (org.apache.commons.cli.ParseException pe) {
            usage(pe.getMessage());
            System.exit(1);
        }
        return cl;
    }

    private static class Params {
        public File[] tzdataFiles;                // tzdata source files
        public File extraDataFile;                // file containing PrimaryZone and ZoneMatchScore lines
        public File outputFile;                   // path to the output iCalendar file
        public Calendar referenceDate;            // reference date; today if not specified
        public String lastModified;               // value for LAST-MODIFIED property; current time if not specified
        public String oldTimezonesFileName;       // Name of old timezones.ics file if specified
    }

    private static Params initParams(CommandLine cl)
    throws IOException, org.apache.commons.cli.ParseException, ParseException {
        Params params = new Params();
        if (cl.hasOption(OPT_HELP))
            return params;

        if (cl.hasOption(OPT_OUTPUT_FILE)) {
            String fname = cl.getOptionValue(OPT_OUTPUT_FILE);
            File file = new File(fname);
            File parent = file.getParentFile();
            if (parent == null)
                parent = new File(".");
            if (!parent.exists())
                throw new FileNotFoundException("Output directory " + parent.getAbsolutePath() + " doesn't exist");
            if (!parent.canWrite())
                throw new IOException("Permission denied on directory " + parent.getAbsolutePath());
            params.outputFile = file;
        }

        if (cl.hasOption(OPT_DATE)) {
            String dateStr = cl.getOptionValue(OPT_DATE);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            Date date = formatter.parse(dateStr);
            params.referenceDate = new GregorianCalendar();
            params.referenceDate.setTime(date);
        } else if (cl.hasOption(OPT_YEAR)) {
            String yearStr = cl.getOptionValue(OPT_YEAR);
            try {
                params.referenceDate = new GregorianCalendar(Integer.parseInt(yearStr), 1, 1);
            } catch (NumberFormatException e) {
                throw new org.apache.commons.cli.ParseException("Invalid year " + yearStr);
            }
        } else {
            params.referenceDate = new GregorianCalendar();
        }

        if (cl.hasOption(OPT_LAST_MODIFIED)) {
            String lastMod = cl.getOptionValue(OPT_LAST_MODIFIED);
            if (!lastMod.matches("\\d{8}T\\d{6}Z"))
                throw new org.apache.commons.cli.ParseException(
                        "--" + OPT_LAST_MODIFIED + " option must match the pattern YYYYMMDDThhmmssZ");
            params.lastModified = lastMod;
        } else {
            Calendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
            params.lastModified = String.format(
                    "%04d%02d%02dT%02d%02d%02dZ",
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH),
                    now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND));
        }

        if (cl.hasOption(OPT_OLD_TIMEZONES_FILE)) {
            params.oldTimezonesFileName = cl.getOptionValue(OPT_OLD_TIMEZONES_FILE);
        } else {
            params.oldTimezonesFileName = null;
        }

        if (cl.hasOption(OPT_EXTRA_DATA_FILE)) {
            File file = new File(cl.getOptionValue(OPT_EXTRA_DATA_FILE));
            if (!file.exists())
                throw new FileNotFoundException("Primary TZ list file " + file.getAbsolutePath() + " doesn't exist");
            if (!file.canRead())
                throw new IOException("Permission denied on file " + file.getAbsolutePath());
            params.extraDataFile = file;
        }

        List<File> sourceFiles = new ArrayList<File>();

        if (cl.hasOption(OPT_TZDATA_DIR)) {
            File dir = new File(cl.getOptionValue(OPT_TZDATA_DIR));
            if (!dir.exists())
                throw new FileNotFoundException("Source directory " + dir.getAbsolutePath() + " doesn't exist");
            if (!dir.canRead())
                throw new IOException("Permission denied on directory " + dir.getAbsolutePath());

            File files[] = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.isFile())
                        continue;
                    // Skip some known non data files: *.tab, *.sh and "factory".
                    String name = file.getName();
                    if (name.endsWith(".tab") || name.endsWith(".sh") || name.endsWith(".awk") ||  name.endsWith(".pl") ||
                            (name.startsWith(".") || name.endsWith(".swp")) ||  // ignore editor temporary files
                            name.equalsIgnoreCase("CONTRIBUTING") ||
                            name.equalsIgnoreCase("Makefile") ||
                            name.equalsIgnoreCase("NEWS") ||
                            name.equalsIgnoreCase("README") ||
                            name.equalsIgnoreCase("Theory") ||
                            name.equalsIgnoreCase("factory") ||
                            name.equalsIgnoreCase("leap-seconds.list")) {
                        continue;
                    }
                    if (!file.canRead())
                        throw new IOException("Permission denied on file " + file.getAbsolutePath());
                    sourceFiles.add(file);
                }
            }
        }

        // Any leftover arguments are tzdata source filenames.
        String dataFiles[] = cl.getArgs();
        if (dataFiles != null) {
            for (String fname : dataFiles) {
                File file = new File(fname);
                if (!file.exists())
                    throw new FileNotFoundException("Source file " + file.getAbsolutePath() + " doesn't exist");
                if (!file.canRead())
                    throw new IOException("Permission denied on file " + file.getAbsolutePath());
                sourceFiles.add(file);
            }
        }

        if (sourceFiles.isEmpty())
            throw new org.apache.commons.cli.ParseException("No tzdata source files/directory specified");

        params.tzdataFiles = sourceFiles.toArray(new File[0]);

        return params;
    }

    private static Map<String,VTimeZone> makeOldTimeZonesMap(Params params) {
        Map<String,VTimeZone> oldTimeZones = Maps.newHashMap();
        if (null != params.oldTimezonesFileName) {
            try (FileInputStream fin = new FileInputStream(params.oldTimezonesFileName)) {
                CalendarBuilder builder = new CalendarBuilder();
                net.fortuna.ical4j.model.Calendar calendar = builder.build(fin, "UTF-8");
                for (Iterator i = calendar.getComponents().iterator(); i.hasNext();) {
                    Component component = (Component) i.next();
                    if (Component.VTIMEZONE.equals(component.getName())) {
                        VTimeZone vtz = (VTimeZone)component;
                        Property tzprop = vtz.getProperties().getProperty(Property.TZID);
                        if (null != tzprop) {
                            oldTimeZones.put(tzprop.getValue(), vtz);
                        }
                    }
                }
            } catch (IOException | ParserException e) {
                System.err.println("Problem loading old timezones.ics - ignoring it.  " + e.getMessage());
            }
        }
        return oldTimeZones;
    }

    private static String getTimeZoneForZoneLine(Zone zone, Params params, Set<String> zoneIDs,
            Map<String,VTimeZone> oldTimeZones) {
        String tzid = zone.getName();
        boolean isPrimary = sPrimaryTZIDs.contains(tzid);
        Integer matchScore = sMatchScores.get(tzid);
        if (matchScore == null) {
            if (isPrimary) {
                matchScore = Integer.valueOf(TZIDMapper.DEFAULT_MATCH_SCORE_PRIMARY);
            } else {
                matchScore = Integer.valueOf(TZIDMapper.DEFAULT_MATCH_SCORE_NON_PRIMARY);
            }
        }
        Set<String> aliases = zone.getAliases();
        Iterator<String> aliasesIter = aliases.iterator();
        while (aliasesIter.hasNext()) {
            String curr = aliasesIter.next();
            if (zoneIDs.contains(curr)) {
                aliasesIter.remove();
            }
        }
        ZoneLine zline = getZoneLineForYear(zone, params.referenceDate);
        if (zline == null) {
            return "";
        }
        VTimeZone oldVtz = oldTimeZones.get(zline.getName());
        Property oldLastModProp = null;
        if (null != oldVtz) {
            oldLastModProp = oldVtz.getProperties().getProperty(Property.LAST_MODIFIED);
        }
        LastModified newLastModified = getLastModified(params.lastModified);
        LastModified trialLastModified;
        if (null != oldLastModProp && oldLastModProp instanceof LastModified) {
            trialLastModified = (LastModified) oldLastModProp;
        } else {
            trialLastModified = newLastModified;
        }
        VTimeZone vtz = toVTimeZoneComp(params.referenceDate.get(Calendar.YEAR), zline,
                trialLastModified, aliases, isPrimary, matchScore);
        String asText = vtz.toString();
        if ((null != oldVtz) && (trialLastModified != newLastModified)) {
            String oldText = oldVtz.toString();
            if (!asText.equals(oldText)) {
                /* Work around non-round tripped entries where the original source has:
                 *     X-ZIMBRA-TZ-ALIAS:(GMT+12.00) Anadyr\, Petropavlovsk-Kamchatsky (RTZ 11)
                 * but in this we have:
                 *     X-ZIMBRA-TZ-ALIAS:(GMT+12.00) Anadyr\\\, Petropavlovsk-Kamchatsky (RTZ 11)
                 * suspect that is a bug in libical which may be fixed in a later revision
                 */
                String oldText2 = oldText.replace("\\\\\\,", "\\,");
                if (!asText.equals(oldText2)) {
                    LastModified lastModProp =
                            (LastModified) vtz.getProperties().getProperty(Property.LAST_MODIFIED);
                    try {
                        lastModProp.setValue(newLastModified.getValue());
                        asText = vtz.toString();
                    } catch (ParseException e) {
                        System.err.println("Problem assigning LAST-MODIFIED - " + e.getMessage());
                    }
                }
            }
        }
        return asText;
    }

    // main

    public static void main(String[] args) throws Exception {

        // command line handling
        CommandLine cl = null;
        Params params = null;
        try {
            cl = parseArgs(args);
            if (cl.hasOption(OPT_HELP)) {
                usage(null);
                System.exit(0);
            }
            params = initParams(cl);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        // parse tzdata source
        ZoneInfoParser parser = new ZoneInfoParser();
        for (File tzdataFile : params.tzdataFiles) {
            Reader r = null;
            try {
                r = new InputStreamReader(new FileInputStream(tzdataFile), "UTF-8");
                parser.readTzdata(r);
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                System.err.println("Line: " + e.getErrorOffset());
                System.err.println("File: " + tzdataFile.getAbsolutePath());
                e.printStackTrace();
                System.exit(1);
            } finally {
                if (r != null)
                    r.close();
            }
        }
        parser.analyze();

        // read extra data file containing primary TZ list and zone match scores
        if (params.extraDataFile != null) {
            Reader r = null;
            try {
                r = new InputStreamReader(new FileInputStream(params.extraDataFile), "UTF-8");
                readExtraData(r);
            } catch (ParseException e) {
                System.err.println(e.getMessage());
                System.err.println("Line: " + e.getErrorOffset());
                System.err.println("File: " + params.extraDataFile.getAbsolutePath());
                e.printStackTrace();
                System.exit(1);
            } finally {
                if (r != null)
                    r.close();
            }
        }

        Writer out;
        if (params.outputFile != null) {
            out = new PrintWriter(params.outputFile, "UTF-8");
        } else {
            out = new PrintWriter(new OutputStreamWriter(System.out, "UTF-8"));
        }

        try {
            StringBuilder hdr = new StringBuilder("BEGIN:VCALENDAR");
            hdr.append(CRLF);
            hdr.append("PRODID:Zimbra-Calendar-Provider").append(CRLF);
            hdr.append("VERSION:2.0").append(CRLF);
            hdr.append("METHOD:PUBLISH").append(CRLF);
            out.write(hdr.toString());

            Map<String,VTimeZone> oldTimeZones = makeOldTimeZonesMap(params);
            Set<Zone> zones = new TreeSet<Zone>(new ZoneComparatorByGmtOffset());
            zones.addAll(parser.getZones());
            Set<String> zoneIDs = new TreeSet<String>();
            for (Zone zone : zones) {
                zoneIDs.add(zone.getName());
            }
            for (Zone zone : zones) {
                out.write(getTimeZoneForZoneLine(zone, params, zoneIDs, oldTimeZones));
            }

            StringBuilder footer = new StringBuilder("END:VCALENDAR");
            footer.append(CRLF);
            out.write(footer.toString());
        } finally {
            out.close();
        }
    }
}
