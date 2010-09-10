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
package com.zimbra.cs.index.query;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.DateTools;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.index.DBQueryOperation;
import com.zimbra.cs.index.QueryOperation;

/**
 * Query by absolute date or relative date.
 * <p>
 * The absolute-date (e.g. mm/dd/yyyy) pattern is locale sensitive. This
 * implementation delegates it to JDK's {@link DateFormat} class whose behavior
 * is as follows:
 * <table>
 *  <tr><td>ar</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>be</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>bg</td><td>yyyy-mm-dd</td></tr>
 *  <tr><td>ca</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>cs</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>da</td><td>dd-mm-yyyy</td></tr>
 *  <tr><td>de</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>el</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>en</td><td>mm/dd/yyyy (default)</td></tr>
 *  <tr><td>en_AU</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>en_CA</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>en_GB</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>en_IE</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>en_IN</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>en_NZ</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>en_ZA</td><td>yyyy/mm/dd</td></tr>
 *  <tr><td>es</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>es_DO</td><td>mm/dd/yyyy</td></tr>
 *  <tr><td>es_HN</td><td>mm-dd-yyyy</td></tr>
 *  <tr><td>es_PR</td><td>mm-dd-yyyy</td></tr>
 *  <tr><td>es_SV</td><td>mm-dd-yyyy</td></tr>
 *  <tr><td>et</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>fi</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>fr</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>fr_CA</td><td>yyyy-mm-dd</td></tr>
 *  <tr><td>fr_CH</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>hr</td><td>yyyy.MM.dd</td></tr>
 *  <tr><td>hr_HR</td><td>dd.MM.yyyy.</td></tr>
 *  <tr><td>hu</td><td>yyyy.MM.dd.</td></tr>
 *  <tr><td>is</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>it</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>it_CH</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>iw</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>ja</td><td>yyyy/mm/dd</td></tr>
 *  <tr><td>ko</td><td>yyyy. mm. dd</td></tr>
 *  <tr><td>lt</td><td>yyyy.mm.dd</td></tr>
 *  <tr><td>lv</td><td>yyyy.dd.mm</td></tr>
 *  <tr><td>mk</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>nl</td><td>dd-mm-yyyy</td></tr>
 *  <tr><td>nl_BE</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>no</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>pl</td><td>yyyy-mm-dd</td></tr>
 *  <tr><td>pl_PL</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>pt</td><td>dd-mm-yyyy</td></tr>
 *  <tr><td>pt_BR</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>ro</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>ru</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>sk</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>sl</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>sq</td><td>yyyy-mm-dd</td></tr>
 *  <tr><td>sv</td><td>yyyy-mm-dd</td></tr>
 *  <tr><td>th</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>tr</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>uk</td><td>dd.mm.yyyy</td></tr>
 *  <tr><td>vi</td><td>dd/mm/yyyy</td></tr>
 *  <tr><td>zh</td><td>yyyy-mm-dd</td></tr>
 *  <tr><td>zh_TW</td><td>yyyy/mm/dd</td></tr>
 * </table>
 * In case of format error, it falls back to <tt>mm/dd/yyyy</tt>.
 *
 * @author tim
 * @author ysasaki
 */
public final class DateQuery extends Query {

    public enum Type {
        APPT_START, APPT_END, CONV_START, CONV_END,
        BEFORE, AFTER, DATE, DAY, WEEK, MONTH, YEAR
    }

    private static final Pattern NUMERICDATE_PATTERN =
        Pattern.compile("^([0-9]+)$");
    private static final Pattern RELDATE_PATTERN =
        Pattern.compile("([+-])([0-9]+)([mhdwy][a-z]*)?");

    private Date mDate = null;
    private Date mEndDate = null;
    private long mLowestTime;
    private boolean mLowerEq;
    private long mHighestTime;
    private boolean mHigherEq;
    private final Type type;

    public DateQuery(Type type) {
        this.type = type;
    }

    public long getLowestTime() {
        return mLowestTime;
    }

    public boolean isLowestInclusive() {
        return mLowerEq;
    }

    public long getHighestTime() {
        return mHighestTime;
    }

    public boolean isHighestInclusive() {
        return mHigherEq;
    }

    @Override
    public QueryOperation getQueryOperation(boolean bool) {
        DBQueryOperation op = new DBQueryOperation();
        switch (type) {
            case APPT_START:
                op.addCalStartDateClause(mLowestTime, mLowerEq,
                        mHighestTime, mHigherEq, evalBool(bool));
                break;
            case APPT_END:
                op.addCalEndDateClause(mLowestTime, mLowerEq,
                        mHighestTime, mHigherEq, evalBool(bool));
                break;
            default:
                op.addDateClause(mLowestTime, mLowerEq,
                        mHighestTime, mHigherEq, evalBool(bool));
                break;
        }

        return op;
    }

    /**
     * DATE: {@code absolute-date = mm/dd/yyyy} (locale sensitive)
     *   OR  {@code relative-date = [+/-]nnnn{minute,hour,day,week,month,year}}
     * <p>
     * TODO need to figure out how to represent "this week", "last week",
     * "this month", etc.
     */
    public void parseDate(String src, TimeZone tz, Locale locale) throws ParseException {
        mDate = null; // the beginning of the user-specified range (inclusive)
        mEndDate = null; // the end of the user-specified range (NOT-included in the range)
        mLowestTime = -1;
        mHighestTime = -1;
        boolean hasExplicitComparasins = false;
        boolean explicitLT = false;
        boolean explicitGT = false;
        boolean explicitEq = false;

        if (src.length() <= 0) {
            throw new ParseException(src, 0);
        }

        // remove trailing comma, for date:(12312, 123123, 123132) format
        if (src.charAt(src.length() - 1) == ',') {
            src = src.substring(0, src.length() - 1);
        }

        if (src.length() <= 0) {
            throw new ParseException(src, 0);
        }

        char ch = src.charAt(0);
        if (ch == '<' || ch == '>') {
            switch (type) {
                case BEFORE:
                case AFTER:
                    throw new ParseException(src, 0);
            }

            hasExplicitComparasins = true;

            if (src.length() <= 1) {
                throw new ParseException(src, 0);
            }

            char ch2 = src.charAt(1);
            if (ch2 == '=' && src.length() <= 2) {
                throw new ParseException(src, 0);
            }

            if (ch == '<') {
                explicitLT = true;
            } else if (ch == '>') {
                explicitGT = true;
            }
            if (ch2 == '=') {
                src = src.substring(2); // chop off the <= or >=
                explicitEq = true;
            } else {
                src = src.substring(1); // chop off the < or >
            }
        }

        if (src.length() <= 0) {
            throw new ParseException(src, 0);
        }

        if (src.equalsIgnoreCase("today")) {
            src = "-0d";
        }
        if (src.equalsIgnoreCase("yesterday")) {
            src = "-1d";
        }

        int field = 0;
        switch (type) {
            case APPT_START:
            case APPT_END:
            case BEFORE:
            case AFTER:
            case DATE:
            case DAY:
                field = Calendar.DATE;
                break;
            case WEEK:
                field = Calendar.WEEK_OF_YEAR;
                break;
            case MONTH:
                field = Calendar.MONTH;
                break;
            case YEAR:
                field = Calendar.YEAR;
                break;
        }

        // Now, do the actual parsing.  There are two cases: a relative date
        // or an absolute date.

        String mod = null;
        Matcher matcher = NUMERICDATE_PATTERN.matcher(src);
        if (matcher.lookingAt()) {
            long dateLong = Long.parseLong(src);
            mDate = new Date(dateLong);
            mEndDate = new Date(dateLong + 1000);
            // +1000 since SQL time is sec, java in msec
        } else {
            matcher = RELDATE_PATTERN.matcher(src);
            if (matcher.lookingAt()) {
                // RELATIVE DATE!
                String reltime;
                String what;

                mod = src.substring(matcher.start(1), matcher.end(1));
                reltime = src.substring(matcher.start(2), matcher.end(2));

                if (matcher.start(3) == -1) {
                    // no period specified -- use the defualt for the current operator
                } else {
                    what = src.substring(matcher.start(3), matcher.end(3));

                    switch (what.charAt(0)) {
                        case 'm':
                            field = Calendar.MONTH;
                            if (what.length() > 1 && what.charAt(1) == 'i') {
                                field = Calendar.MINUTE;
                            }
                            break;
                        case 'h':
                            field = Calendar.HOUR_OF_DAY;
                            break;
                        case 'd':
                            field = Calendar.DATE;
                            break;
                        case 'w':
                            field = Calendar.WEEK_OF_YEAR;
                            break;
                        case 'y':
                            field = Calendar.YEAR;
                            break;
                    }
                } // (else m.start(3) == -1


                GregorianCalendar cal = new GregorianCalendar();
                if (tz != null) {
                    cal.setTimeZone(tz);
                }

                cal.setTime(new Date());

                // special case 'day' clear all the fields that are lower than the one we're currently operating on...
                // E.G. "date:-1d"  people really expect that to mean 'midnight to midnight yesterday'
                switch (field) {
                    case Calendar.YEAR:
                        cal.set(Calendar.MONTH, 0);
                        // fall-through
                    case Calendar.MONTH:
                        cal.set(Calendar.DAY_OF_MONTH, 1);
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        break;
                    case Calendar.WEEK_OF_YEAR:
                        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
                        // fall-through
                    case Calendar.DATE:
                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        // fall-through
                    case Calendar.HOUR:
                    case Calendar.HOUR_OF_DAY:
                        cal.set(Calendar.MINUTE, 0);
                        // fall-through
                    case Calendar.MINUTE:
                        cal.set(Calendar.SECOND, 0);
                }

                int num = Integer.parseInt(reltime);
                if (mod.equals("-")) {
                    num = num * -1;
                }

                cal.add(field,num);
                mDate = cal.getTime();

                cal.add(field,1);
                mEndDate = cal.getTime();
            } else {
                // ABSOLUTE dates:
                // use Locale information to parse date correctly

                char first = src.charAt(0);
                if (first == '-' || first == '+') {
                    src = src.substring(1);
                }

                DateFormat df;
                if (locale != null) {
                    df = DateFormat.getDateInstance(DateFormat.SHORT, locale);
                } else {
                    df = DateFormat.getDateInstance(DateFormat.SHORT);
                }

                df.setLenient(false);
                if (tz != null) {
                    df.setTimeZone(tz);
                }

                try {
                    mDate = df.parse(src);
                } catch (java.text.ParseException e) {
                    // fall back to mm/dd/yyyy
                    df = DateFormat.getDateInstance(DateFormat.SHORT);
                    mDate = df.parse(src);
                }

                Calendar cal = Calendar.getInstance();
                if (tz != null) {
                    cal.setTimeZone(tz);
                }

                cal.setTime(mDate);

                cal.add(field,1);
                mEndDate = cal.getTime();
            } // else (relative/absolute check)
        } // else (numeric check)

        if (ZimbraLog.index_search.isDebugEnabled()) {
            ZimbraLog.index_search.debug("Parsed date range to: (" +
                    mDate.toString() + "-" + mEndDate.toString() + ")");
        }

        // convert BEFORE, AFTER and DATE to the right explicit params...
        if (!hasExplicitComparasins) {
            switch (type) {
                case BEFORE:
                    explicitLT = true;
                    explicitEq = false;
                    break;
                case AFTER:
                    explicitGT= true;
                    explicitEq = false;
                    break;
                case YEAR:
                case MONTH:
                case DATE:
                case APPT_START:
                case APPT_END:
                    explicitEq = true;
                    break;
            }
        }

        //
        // At this point, we've parsed out "mDate" and calculated "mEndDate" to be the "next" date
        // in whatever unit of date measurement they're using.
        //
        // Now, we translate mDate and mEndDate into ranges, depending on the comparasin operators.
        //
        // Here's the logic table:
        //
        // User-Specified Search  |        SQL Search       |    in our local Variables
        //-----------------------------------------------------------------------
        //       <=                        |   date<mEnd             |    highest=mEndDate,highestEq=false
        //       <  (BEFORE)           |    date < mDate         |     highest=mDate, highestEq=false
        //       >=                        |    date >= mDate       |     lowest=mDate, lowestEq=true
        //        >  (AFTER)            |    date > mEnd          |     lowest=mEndDate, lowestEq=true
        //       =  (DATE)              |  (date>=mDate && date<mEnd) |  lowest=mDate,lowestEq=true,highest=mEndDate,highestEq=false
        //
        //

        if (explicitLT) {
            if (explicitEq) {
                // <=     highest=mEndDate,highestEq=false
                mLowestTime = -1;
                mLowerEq = false;
                mHighestTime = mEndDate.getTime();
                mHigherEq = false;
            } else {
                // <  highest=mDate, highestEq=false
                mLowestTime = -1;
                mLowerEq = false;
                mHighestTime = mDate.getTime();
                mHigherEq = false;
            }
        } else if (explicitGT) {
            if (explicitEq) {
                // >=  lowest=mDate, lowestEq=true
                mLowestTime = mDate.getTime();
                mLowerEq = true;
                mHighestTime = -1;
                mHigherEq = false;
            } else {
                // > lowest=mEndDate, lowestEq=true
                mLowestTime = mEndDate.getTime();
                mLowerEq = true;
                mHighestTime = -1;
                mHigherEq = false;
            }
        } else {
            // assert(explicitEq == true);
            // =  lowest=mDate,lowestEq=true,highest=mEndDate,highestEq=false
            mLowestTime = mDate.getTime();
            mLowerEq = true;
            mHighestTime = mEndDate.getTime();
            mHigherEq = false;
        }

    }

    @Override
    public void dump(StringBuilder out) {
        out.append("DATE,");
        out.append(type);
        out.append(',');
        out.append(DateTools.dateToString(mDate, DateTools.Resolution.SECOND));
    }
}
