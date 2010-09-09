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
import com.zimbra.cs.index.query.parser.QueryParser;
import com.zimbra.cs.index.query.parser.QueryParserException;

/**
 * Query by absolute date or relative date.
 *
 * @author tim
 * @author ysasaki
 */
public final class DateQuery extends Query {
    private Date mDate = null;
    private Date mEndDate = null;
    private long mLowestTime;
    private boolean mLowerEq;
    private long mHighestTime;
    private boolean mHigherEq;

    public DateQuery(int qType) {
        super(0, qType);
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
    public QueryOperation getQueryOperation(boolean truth) {
        DBQueryOperation op = new DBQueryOperation();

        truth = calcTruth(truth);

        if (this.getQueryType() == QueryParser.APPT_START) {
            op.addCalStartDateClause(mLowestTime, mLowerEq, mHighestTime, mHigherEq, truth);
        } else if (this.getQueryType() == QueryParser.APPT_END) {
            op.addCalEndDateClause(mLowestTime, mLowerEq, mHighestTime, mHigherEq, truth);
        } else{
            op.addDateClause(mLowestTime, mLowerEq, mHighestTime, mHigherEq, truth);
        }

        return op;
    }

    protected static final Pattern NUMERICDATE_PATTERN =
        Pattern.compile("^([0-9]+)$");
    protected static final Pattern RELDATE_PATTERN =
        Pattern.compile("([+-])([0-9]+)([mhdwy][a-z]*)?");

    public void parseDate(String s, TimeZone tz, Locale locale) throws QueryParserException {
        // DATE: absolute-date = mm/dd/yyyy (locale sensitive) OR
        //       relative-date = [+/-]nnnn{minute,hour,day,week,month,year}
        // * need to figure out how to represent "this week", "last week",
        // "this month", etc.

        mDate = null; // the beginning of the user-specified range (inclusive)
        mEndDate = null; // the end of the user-specified range (NOT-included in the range)
        mLowestTime = -1;
        mHighestTime = -1;
        boolean hasExplicitComparasins = false;
        boolean explicitLT = false;
        boolean explicitGT = false;
        boolean explicitEq = false;

        if (s.length() <= 0) {
            throw new QueryParserException("INVALID_DATE");
        }

        // remove trailing comma, for date:(12312, 123123, 123132) format
        if (s.charAt(s.length() - 1) == ',') {
            s = s.substring(0, s.length() - 1);
        }

        if (s.length() <= 0) {
            throw new QueryParserException("INVALID_DATE");
        }

        char ch = s.charAt(0);
        if (ch == '<' || ch == '>') {
            if (getQueryType() == QueryParser.BEFORE ||
                    getQueryType() == QueryParser.AFTER) {
                throw new QueryParserException("INVALID_DATE");
            }

            hasExplicitComparasins = true;

            if (s.length() <= 1) {
                throw new QueryParserException("INVALID_DATE");
            }

            char ch2 = s.charAt(1);
            if (ch2 == '=' && s.length() <= 2) {
                throw new QueryParserException("INVALID_DATE");
            }

            if (ch == '<') {
                explicitLT = true;
            } else if (ch == '>') {
                explicitGT = true;
            }
            if (ch2 == '=') {
                s = s.substring(2); // chop off the <= or >=
                explicitEq = true;
            } else {
                s = s.substring(1); // chop off the < or >
            }
        }

        if (s.length() <= 0) {
            throw new QueryParserException("INVALID_DATE");
        }


        int origType = getQueryType();

        if (s.equalsIgnoreCase("today")) {
            s = "-0d";
        }
        if (s.equalsIgnoreCase("yesterday")) {
            s = "-1d";
        }

        int field = 0;
        switch (origType) {
            case QueryParser.APPT_START:
            case QueryParser.APPT_END:
            case QueryParser.BEFORE:
            case QueryParser.AFTER:
            case QueryParser.DATE:
            case QueryParser.DAY:
                field = Calendar.DATE;
                break;
            case QueryParser.WEEK:
                field = Calendar.WEEK_OF_YEAR;
                break;
            case QueryParser.MONTH:
                field = Calendar.MONTH;
                break;
            case QueryParser.YEAR:
                field = Calendar.YEAR;
                break;
        }

        //
        // Now, do the actual parsing.  There are two cases: a relative date
        // or an absolute date.
        //
        {

            String mod = null;
            Matcher matcher = NUMERICDATE_PATTERN.matcher(s);
            if (matcher.lookingAt()) {
                long dateLong = Long.parseLong(s);
                mDate = new Date(dateLong);
                mEndDate = new Date(dateLong + 1000);
                // +1000 since SQL time is sec, java in msec
            } else {
                matcher = RELDATE_PATTERN.matcher(s);
                if (matcher.lookingAt()) {
                    // RELATIVE DATE!
                    String reltime;
                    String what;

                    mod = s.substring(matcher.start(1), matcher.end(1));
                    reltime = s.substring(matcher.start(2), matcher.end(2));

                    if (matcher.start(3) == -1) {
                        // no period specified -- use the defualt for the current operator
                    } else {
                        what = s.substring(matcher.start(3), matcher.end(3));

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

                    //
                    // special case 'day' clear all the fields that are lower than the one we're currently operating on...
                    //
                    //  E.G. "date:-1d"  people really expect that to mean 'midnight to midnight yesterday'
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

                    char first = s.charAt(0);
                    if (first == '-' || first == '+') {
                        s = s.substring(1);
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
                        mDate = df.parse(s);
                    } catch (java.text.ParseException e) {
                        // fall back to mm/dd/yyyy
                        df = DateFormat.getDateInstance(DateFormat.SHORT);
                        try {
                            mDate = df.parse(s);
                        } catch (java.text.ParseException again) {
                            throw new QueryParserException("INVALID_DATE");
                        }
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
        }

        // convert BEFORE, AFTER and DATE to the right explicit params...
        if (!hasExplicitComparasins) {
            switch(getQueryType()) {
                case QueryParser.BEFORE:
                    explicitLT = true;
                    explicitEq = false;
                    break;
                case QueryParser.AFTER:
                    explicitGT= true;
                    explicitEq = false;
                    break;
                case QueryParser.YEAR:
                case QueryParser.MONTH:
                case QueryParser.DATE:
                case QueryParser.APPT_START:
                case QueryParser.APPT_END:
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
    public StringBuilder dump(StringBuilder out) {
        super.dump(out);
        out.append(',');

        switch (getQueryType()) {
            case QueryParser.BEFORE:
                out.append("BEFORE");
                break;
            case QueryParser.AFTER:
                out.append("AFTER");
                break;
            case QueryParser.DATE:
                out.append("DATE");
                break;
            case QueryParser.APPT_START:
                out.append("APPT-START");
                break;
            case QueryParser.APPT_END:
                out.append("APPT-END");
                break;
            default:
                assert(false);
        }

        out.append(',');
        out.append(DateTools.dateToString(mDate, DateTools.Resolution.SECOND));
        return out.append(')');
    }
}
