/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.index;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zimbra.common.util.Pair;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.index.queryparser.Token;
import com.zimbra.cs.index.queryparser.ZimbraQueryParser;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.index.queryparser.ZimbraQueryParserConstants;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox.SearchResultMode;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.SoapProtocol;

/**
 * Represents a search query.
 * <p>
 * Flow is simple:
 * <ul>
 *  <li>Constructor() -
 *   1) Parse the query string, turn it into a list of BaseQuery's. This is done
 *      by the JavaCC-generated QueryParser in the index.queryparser package.
 *   2) Push "not's" down to the leaves, so that we never have to invert result
 *      sets. See the internal ParseTree class.
 *   3) Generate a QueryOperation (which is usually a tree of QueryOperation
 *      objects) from the ParseTree, then optimize them QueryOperations in
 *      preparation to run the query.
 *  <li>execute() - Begin the search, get the ZimbraQueryResults iterator.
 * <p>
 * long-standing TODO is to move BaseQuery classes and ParseTree classes out of
 * this class.
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
 */
public final class ZimbraQuery {
    /**
     * BaseQuery
     *
     * Very simple wrapper classes that each represent a node in the parse tree for the
     * query string.
     */
    public static abstract class BaseQuery {
        protected boolean mTruth = true;
        private BaseQuery mNext = null;
        private int mModifierType;
        private int mQueryType;

        protected BaseQuery(int modifierType, int queryType) {
            mModifierType = modifierType;
            mQueryType = queryType;
        }

        protected final void setQueryType(int queryType) {
            mQueryType = queryType;
        }

        protected final int getQueryType() {
            return mQueryType;
        }

        public final BaseQuery getNext() {
            return mNext;
        }

        String getQueryOperatorString() {
            return ZimbraQuery.unquotedTokenImage[mQueryType];
        }

        /**
         * Used by the QueryParser when building up the list of Query terms.
         *
         * @param mod
         */
        public final void setModifier(int mod) {
            mModifierType = mod;
        }

        /**
         * Used by the QueryParser when building up the list of Query terms.
         */
        public final void setNext(BaseQuery next) {
            mNext = next;
        }

        @Override public String toString() {
            return toString(0);
        }

        public String toString(int expLevel) {
            return indent(expLevel)+modToString()+"Q("+QueryTypeString(getQueryType());
        }

        /**
         * Called by the optimizer, this returns an initialized QueryOperation of the requested type.
         *
         * @param type
         * @param truth
         * @return
         */
        protected abstract QueryOperation getQueryOperation(boolean truth);

        // helper for writing toString
        protected final String indent(int level) {
            String ret = "";
            for (int i = 0; i < level; i++) {
                ret+="    ";
            }
            return ret;
        }

        boolean isNegated() {
            return mModifierType == ZimbraQueryParser.MINUS;
        }

        protected final String modToString() {
            String modString = "";
            switch(mModifierType) {
                case ZimbraQueryParser.PLUS:
                    modString = "+";
                    break;
                case ZimbraQueryParser.MINUS:
                    modString = "-";
                    break;
            }
            return modString;
        }

        protected final boolean calcTruth(boolean truth) {
            if (isNegated()) {
                return !truth;
            } else {
                return truth;
            }
        }
    }

    public static class AttachmentQuery extends LuceneTableQuery
    {
        protected static Map<String, String> mMap;

        static {
            mMap = new HashMap<String, String>();

            //                              Friendly Name                                  Mime Type
            addMapping(mMap, new String[] { "any" }                                      , "any");
            addMapping(mMap, new String[] { "application", "application/*"}              , "application");
            addMapping(mMap, new String[] { "bmp", "image/bmp" }                         , "image/bmp");
            addMapping(mMap, new String[] { "gif", "image/gif" }                         , "image/gif");
            addMapping(mMap, new String[] { "image", "image/*" }                         , "image");
            addMapping(mMap, new String[] { "jpeg", "image/jpeg", }                      , "image/jpeg");
            addMapping(mMap, new String[] { "excel", "application/vnd.ms-excel", "xls" } , "application/vnd.ms-excel");
            addMapping(mMap, new String[] { "ppt", "application/vnd.ms-powerpoint"}      , "application/vnd.ms-powerpoint");
            addMapping(mMap, new String[] { "ms-tnef", "application/ms-tnef"}            , "application/ms-tnef");
            addMapping(mMap, new String[] { "word", "application/msword", "msword" }     , "application/msword");
            addMapping(mMap, new String[] { "none" }                                     , "none");
            addMapping(mMap, new String[] { "pdf", "application/pdf" }                   , "application/pdf");
            addMapping(mMap, new String[] { "text", "text/*" }                           , "text");
        }

        public AttachmentQuery(Mailbox mbox, Analyzer analyzer, int modifier, String what) {
            super(mbox, modifier, ZimbraQueryParser.TYPE, LuceneFields.L_ATTACHMENTS, lookup(mMap, what));
        }

        protected AttachmentQuery(Mailbox mbox, int modifier, String luceneField, String what) {
            super(mbox, modifier, ZimbraQueryParser.TYPE, luceneField, lookup(mMap, what));
        }
    }

    public static class ConjQuery extends BaseQuery {
        private static final int AND = ZimbraQueryParser.AND_TOKEN;
        private static final int OR = ZimbraQueryParser.OR_TOKEN;

        int mOp;

        public ConjQuery(Analyzer analyzer, int qType) {
            super(0, qType);
        }

        final boolean isOr() {
            return getQueryType() == OR;
        }

        @Override
        public String toString(int expLevel) {
            switch (getQueryType()) {
                case AND:
                    return indent(expLevel)+"_AND_";
                case OR:
                    return indent(expLevel)+"_OR_";
            }
            assert(false);
            return "";
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            assert(false);
            return null;
        }

    }

    public static class ConvQuery extends BaseQuery {
        private ItemId mConvId;
        private Mailbox mMailbox;

        private ConvQuery(Mailbox mbox, Analyzer analyzer, int modifier,
                ItemId convId) throws ServiceException {
            super(modifier, ZimbraQueryParser.CONV);
            mMailbox = mbox;
            mConvId = convId;

            if (mConvId.getId() < 0) {
                // should never happen (make an ItemQuery instead
                throw ServiceException.FAILURE("Illegal Negative ConvID: " +
                        convId.toString() + ", use ItemQuery for virtual convs",
                        null);
            }
        }

        public static BaseQuery create(Mailbox mbox, Analyzer analyzer,
                int modifier, String target) throws ServiceException {
            ItemId convId = new ItemId(target, mbox.getAccountId());
            if (convId.getId() < 0) {
                // ...convert negative convId to positive ItemId...
                convId = new ItemId(convId.getAccountId(), -1 * convId.getId());
                List<ItemId> iidList = new ArrayList<ItemId>(1);
                iidList.add(convId);
                return new ItemQuery(mbox, analyzer, modifier, false, false, iidList);
            } else {
                return new ConvQuery(mbox, analyzer, modifier, convId);
            }
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation op = DBQueryOperation.Create();
            op.addConvId(mMailbox, mConvId, calcTruth(truth));
            return op;
        }

        @Override
        public String toString(int expLevel) {
            return super.toString(expLevel) + "," + mConvId + ")";
        }
    }

    public static class DateQuery extends BaseQuery {
        private Date mDate = null;
        private Date mEndDate = null;
        private long mLowestTime;  private boolean mLowerEq;
        private long mHighestTime; private boolean mHigherEq;

        public DateQuery(Analyzer analyzer, int qType) {
            super(0, qType);
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation op = DBQueryOperation.Create();

            truth = calcTruth(truth);

            if (this.getQueryType() == ZimbraQueryParser.APPT_START) {
                op.addCalStartDateClause(mLowestTime, mLowerEq, mHighestTime, mHigherEq, truth);
            } else if (this.getQueryType() == ZimbraQueryParser.APPT_END) {
                op.addCalEndDateClause(mLowestTime, mLowerEq, mHighestTime, mHigherEq, truth);
            } else{
                op.addDateClause(mLowestTime, mLowerEq, mHighestTime, mHigherEq, truth);
            }

            return op;
        }


        public static final ParseException parseException(String s, String code, Token t) throws ParseException {
            ParseException pe = new ParseException(s, code);
            pe.currentToken = t;
            return pe;
         }

        protected static final Pattern NUMERICDATE_PATTERN =
            Pattern.compile("^([0-9]+)$");
        protected static final Pattern RELDATE_PATTERN =
            Pattern.compile("([+-])([0-9]+)([mhdwy][a-z]*)?");

        public void parseDate(int modifier, String s, Token tok,
                TimeZone tz, Locale locale) throws ParseException {
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
                throw parseException("Invalid string in date query: \"\"",
                        "INVALID_DATE", tok);
            }

            // remove trailing comma, for date:(12312, 123123, 123132) format
            if (s.charAt(s.length() - 1) == ',') {
                s = s.substring(0, s.length() - 1);
            }

            if (s.length() <= 0) {
                throw parseException(
                        "Invalid string in date query after trimming trailing comma: \"\"",
                        "INVALID_DATE", tok);
            }

            char ch = s.charAt(0);
            if (ch == '<' || ch == '>') {
                if (getQueryType() == ZimbraQueryParser.BEFORE ||
                        getQueryType() == ZimbraQueryParser.AFTER) {
                    throw parseException(
                            ">, <, >= and <= may not be specified with BEFORE or AFTER searches",
                            "INVALID_DATE", tok);
                }

                hasExplicitComparasins = true;

                if (s.length() <= 1) {
                    throw parseException(
                            "Invalid string in date query: \""+s+"\"",
                            "INVALID_DATE", tok);
                }

                char ch2 = s.charAt(1);
                if (ch2 == '=' && s.length() <= 2) {
                    throw parseException(
                            "Invalid string in date query: \""+s+"\"",
                            "INVALID_DATE", tok);
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
                throw parseException(
                        "Invalid string in date query: \"" + s + "\"",
                        "INVALID_DATE", tok);
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
                case ZimbraQueryParser.APPT_START:
                case ZimbraQueryParser.APPT_END:
                case ZimbraQueryParser.BEFORE:
                case ZimbraQueryParser.AFTER:
                case ZimbraQueryParser.DATE:
                case ZimbraQueryParser.DAY:
                    field = Calendar.DATE;
                    break;
                case ZimbraQueryParser.WEEK:
                    field = Calendar.WEEK_OF_YEAR;
                    break;
                case ZimbraQueryParser.MONTH:
                    field = Calendar.MONTH;
                    break;
                case ZimbraQueryParser.YEAR:
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
                                throw parseException(
                                        again.getLocalizedMessage(),
                                        "INVALID_DATE", tok);
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
                    case ZimbraQueryParser.BEFORE:
                        explicitLT = true;
                        explicitEq = false;
                        break;
                    case ZimbraQueryParser.AFTER:
                        explicitGT= true;
                        explicitEq = false;
                        break;
                    case ZimbraQueryParser.YEAR:
                    case ZimbraQueryParser.MONTH:
                    case ZimbraQueryParser.DATE:
                    case ZimbraQueryParser.APPT_START:
                    case ZimbraQueryParser.APPT_END:
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
        public String toString(int expLevel) {
            String str;
            switch (getQueryType()) {
                case ZimbraQueryParser.BEFORE:
                    str = "BEFORE";
                    break;
                case ZimbraQueryParser.AFTER:
                    str = "AFTER";
                    break;
                case ZimbraQueryParser.DATE:
                    str = "DATE";
                    break;
                case ZimbraQueryParser.APPT_START:
                    str = "APPT-START";
                    break;
                case ZimbraQueryParser.APPT_END:
                    str = "APPT-END";
                    break;
                default:
                    str = "ERROR";
            }

            return super.toString(expLevel) + "," + str + "," +
                mDate.toString() + ")";
        }
    }

    public static class DomainQuery extends BaseQuery {
        private String mTarget;
        private Mailbox mMailbox;

        public DomainQuery(Mailbox mbox, Analyzer analyzer, int modifier,
                int qType, String target) {
            super(modifier, qType);
            mTarget = target;
            mMailbox = mbox;
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            TextQueryOperation op = mMailbox.getMailboxIndex().createTextQueryOperation();
            Query q = new TermQuery(new Term(QueryTypeString(getQueryType()), mTarget));
            op.addClause(getQueryOperatorString() + mTarget, q,calcTruth(truth));
            return op;
        }

        @Override
        public String toString(int expLevel) {
            return super.toString(expLevel) + "-DOMAIN," + mTarget + ")";
        }
    }

    public static class DraftQuery extends TagQuery {
        public DraftQuery(Mailbox mailbox, Analyzer analyzer, int modifier,
                boolean truth) throws ServiceException {
            super(mailbox, analyzer, modifier, "\\Draft", truth);
        }

        @Override
        public String toString(int expLevel) {
            if (mTruth) {
                return super.toString(expLevel) + ",DRAFT)";
            } else {
                return super.toString(expLevel) + ",UNDRAFT)";
            }
        }
    }

    public static class FlaggedQuery extends TagQuery {
        public FlaggedQuery(Mailbox mailbox, Analyzer analyzer, int modifier,
                boolean truth) throws ServiceException {
            super(mailbox, analyzer, modifier, "\\Flagged", truth);
        }

        @Override
        public String toString(int expLevel) {
            if (mTruth) {
                return super.toString(expLevel) + ",FLAGGED)";
            } else {
                return super.toString(expLevel) + ",UNFLAGGED)";
            }
        }
    }

    public static class ForwardedQuery extends TagQuery {
        public ForwardedQuery(Mailbox mailbox, Analyzer analyzer, int modifier,
                boolean truth) throws ServiceException {
            super(mailbox, analyzer, modifier, "\\Forwarded", truth);
        }

        @Override
        public String toString(int expLevel) {
            if (mTruth) {
                return super.toString(expLevel) + ",FORWARDED)";
            } else {
                return super.toString(expLevel) + ",UNFORWARDED)";
            }
        }
    }

    public static class HasQuery extends LuceneTableQuery {
        protected static Map<String, String> mMap;

        static {
            mMap = new HashMap<String, String>();

            addMapping(mMap, new String[] { "attachment", "att" }  , "any");
            addMapping(mMap, new String[] { "phone" }              , "phone");
            addMapping(mMap, new String[] { "u.po" }               , "u.po");
            addMapping(mMap, new String[] { "ssn" }                , "ssn");
            addMapping(mMap, new String[] { "url" }                , "url");
        }

        public HasQuery(Mailbox mbox, Analyzer analyzer, int modifier, String what) {
            super(mbox, modifier, ZimbraQueryParser.HAS,
                    LuceneFields.L_OBJECTS, lookup(mMap, what));
        }
    }

    public static class InQuery extends BaseQuery {
        public static final Integer IN_ANY_FOLDER = new Integer(-2);
        public static final Integer IN_LOCAL_FOLDER = new Integer(-3);
        public static final Integer IN_REMOTE_FOLDER = new Integer(-4);
        public static final Integer IN_NO_FOLDER = new Integer(-5);

        public static BaseQuery Create(Mailbox mailbox, Analyzer analyzer,
                int modifier, Integer folderId, boolean includeSubfolders)
                throws ServiceException {
            if (folderId < 0) {
                InQuery toRet = new InQuery(mailbox, null, null, null, folderId,
                        includeSubfolders, analyzer, modifier);
                // toRet.mSpecialTarget = folderId;
                return toRet;
            } else {
                Folder folder = mailbox.getFolderById(null, folderId.intValue());
                InQuery toRet = new InQuery(mailbox, folder, null, null, null,
                        includeSubfolders, analyzer, modifier);
                // toRet.mFolder = folder;
                return toRet;
            }
        }

        public static BaseQuery Create(Mailbox mailbox, Analyzer analyzer,
                int modifier, String folderName, boolean includeSubfolders)
                throws ServiceException {
            Pair<Folder, String> result = mailbox.getFolderByPathLongestMatch(
                    null, Mailbox.ID_FOLDER_USER_ROOT, folderName);
            return recursiveResolve(mailbox, analyzer, modifier,
                    result.getFirst(), result.getSecond(), includeSubfolders);
        }

        public static BaseQuery Create(Mailbox mailbox, Analyzer analyzer,
                int modifier, ItemId iid, String subfolderPath,
                boolean includeSubfolders) throws ServiceException {
            if (!iid.belongsTo(mailbox)) {
                InQuery toRet = new InQuery(mailbox, null, iid, subfolderPath,
                        null, includeSubfolders, analyzer, modifier);
                // toRet.mFolder = null;
                // toRet.mRemoteId = iid;
                // toRet.mSubfolderPath = subfolderPath;
                return toRet;
            } else {
                // find the base folder
                Pair<Folder, String> result;
                if (subfolderPath != null && subfolderPath.length() > 0) {
                    result = mailbox.getFolderByPathLongestMatch(null,
                            iid.getId(), subfolderPath);
                } else {
                    Folder f = mailbox.getFolderById(null, iid.getId());
                    result = new Pair<Folder, String>(f, null);
                }
                return recursiveResolve(mailbox, analyzer, modifier,
                        result.getFirst(), result.getSecond(), includeSubfolders);
            }
        }

        /**
         * Resolve through local mountpoints until we get to the actual folder,
         * or until we get to a remote folder.
         */
        private static BaseQuery recursiveResolve(Mailbox mailbox,
                Analyzer analyzer, int modifier, Folder baseFolder,
                String subfolderPath, boolean includeSubfolders)
                throws ServiceException {

            if (!(baseFolder instanceof Mountpoint)) {
                if (subfolderPath != null) {
                    throw MailServiceException.NO_SUCH_FOLDER(
                            baseFolder.getPath() + "/" + subfolderPath);
                }
                InQuery toRet = new InQuery(mailbox, baseFolder, null, null,
                        null, includeSubfolders, analyzer, modifier);
                // toRet.mFolder = baseFolder;
                return toRet;
            } else {
                Mountpoint mpt = (Mountpoint) baseFolder;

                if  (mpt.isLocal()) {
                    // local!
                    if (subfolderPath == null || subfolderPath.length() == 0) {
                        InQuery toRet = new InQuery(mailbox, baseFolder, null,
                                null, null, includeSubfolders, analyzer, modifier);
                        // toRet.mFolder = baseFolder;
                        return toRet;
                    } else {
                        Folder newBase = mailbox.getFolderById(null,
                                mpt.getRemoteId());
                        return recursiveResolve(mailbox, analyzer, modifier,
                                newBase, subfolderPath, includeSubfolders);
                    }
                } else {
                    // remote!
                    InQuery toRet = new InQuery(mailbox, null, mpt.getTarget(),
                            subfolderPath, null, includeSubfolders, analyzer,
                            modifier);
                    // toRet.mRemoteId = new ItemId(mpt.getOwnerId(), mpt.getRemoteId());
                    // toRet.mSubfolderPath = subfolderPath;
                    return toRet;
                }
            }
        }

        private InQuery(Mailbox mailbox, Folder folder, ItemId remoteId,
                String subfolderPath, Integer specialTarget,
                boolean includeSubfolders, Analyzer analyzer, int modifier) {
            super(modifier, ZimbraQueryParser.IN);
            mFolder = folder;
            mRemoteId = remoteId;
            mSubfolderPath = subfolderPath;
            mSpecialTarget = specialTarget;
            mIncludeSubfolders = includeSubfolders;
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            if (mSpecialTarget != null) {
                if (mSpecialTarget == IN_NO_FOLDER) {
                    return new NoResultsQueryOperation();
                } else if (mSpecialTarget == IN_ANY_FOLDER) {
                    DBQueryOperation dbOp = DBQueryOperation.Create();
                    dbOp.addAnyFolderClause(calcTruth(truth));
                    return dbOp;
                } else {
                    if (calcTruth(truth)) {
                        if (mSpecialTarget == IN_REMOTE_FOLDER) {
                            DBQueryOperation dbop = new DBQueryOperation();
                            dbop.addIsRemoteClause();
                            return dbop;
                        } else {
                            assert(mSpecialTarget == IN_LOCAL_FOLDER);
                            DBQueryOperation dbop = new DBQueryOperation();
                            dbop.addIsLocalClause();
                            return dbop;
                        }
                    } else {
                        if (mSpecialTarget == IN_REMOTE_FOLDER) {
                            DBQueryOperation dbop = new DBQueryOperation();
                            dbop.addIsLocalClause();
                            return dbop;
                        } else {
                            assert(mSpecialTarget == IN_LOCAL_FOLDER);
                            DBQueryOperation dbop = new DBQueryOperation();
                            dbop.addIsRemoteClause();
                            return dbop;
                        }
                    }
                }
            }

            DBQueryOperation dbOp = DBQueryOperation.Create();
            if (mFolder != null) {
                if (mIncludeSubfolders) {
                    List<Folder> subFolders = mFolder.getSubfolderHierarchy();

                    if (calcTruth(truth)) {
                        // (A or B or C)
                        UnionQueryOperation union = new UnionQueryOperation();

                        for (Folder f : subFolders) {
                            DBQueryOperation dbop = new DBQueryOperation();
                            union.add(dbop);
                            if (f instanceof Mountpoint) {
                                Mountpoint mpt = (Mountpoint)f;
                                if (!mpt.isLocal()) {
                                    dbop.addInRemoteFolderClause(mpt.getTarget(), "", mIncludeSubfolders, calcTruth(truth));
                                } else {
                                    // TODO FIXME handle local mountpoints. Don't forget to check for infinite recursion!
                                }

                            } else {
                                dbop.addInClause(f, calcTruth(truth));
                            }
                        }
                        return union;
                    } else {
                        // -(A or B or C) ==> -A and -B and -C
                        IntersectionQueryOperation iop = new IntersectionQueryOperation();

                        for (Folder f : subFolders) {
                            DBQueryOperation dbop = new DBQueryOperation();
                            iop.addQueryOp(dbop);
                            if (f instanceof Mountpoint) {
                                Mountpoint mpt = (Mountpoint)f;
                                if (!mpt.isLocal()) {
                                    dbop.addInRemoteFolderClause(mpt.getTarget(), "", mIncludeSubfolders, calcTruth(truth));
                                } else {
                                    // TODO FIXME handle local mountpoints.  Don't forget to check for infinite recursion!
                                }

                            } else {
                                dbop.addInClause(f, calcTruth(truth));
                            }
                        }
                        return iop;
                    }
                } else {
                    dbOp.addInClause(mFolder, calcTruth(truth));
                }
            } else if (mRemoteId != null) {
                dbOp.addInRemoteFolderClause(mRemoteId, mSubfolderPath, mIncludeSubfolders, calcTruth(truth));
            } else {
                assert(false);
            }

            return dbOp;
        }

        @Override
        public String toString(int expLevel) {
            if (mSpecialTarget != null) {
                String toRet;
                if (!mIncludeSubfolders) {
                    toRet = super.toString(expLevel) + ",IN:";
                } else {
                    toRet = super.toString(expLevel) + ",UNDER:";
                }

                if (mSpecialTarget == IN_ANY_FOLDER) {
                    toRet = toRet + "ANY_FOLDER";
                } else if (mSpecialTarget == IN_LOCAL_FOLDER) {
                    toRet = toRet + "LOCAL";
                } else if (mSpecialTarget == IN_REMOTE_FOLDER) {
                    toRet = toRet + "REMOTE";
                }
                return toRet;
            } else {
                return super.toString(expLevel) + "," +
                    (mIncludeSubfolders ? "UNDER" : "IN") + ":" +
                    (mRemoteId!=null ? mRemoteId.toString() :
                        (mFolder != null ? mFolder.getName() : "ANY_FOLDER")) +
                        (mSubfolderPath != null ? "/"+mSubfolderPath : "") + ")";
            }
        }

        private Folder mFolder;
        private ItemId mRemoteId = null;
        private String mSubfolderPath = null;
        private Integer mSpecialTarget = null;
        private boolean mIncludeSubfolders = false;
    }

    public abstract static class LuceneTableQuery extends BaseQuery {
        private Mailbox mMailbox;

        protected static void addMapping(Map<String, String> map, String[] array, String value) {
            for (int i = array.length - 1; i >= 0; i--) {
                map.put(array[i], value);
            }
        }

        protected static String lookup(Map<String, String> map, String key) {
            String toRet = map.get(key);
            if (toRet == null) {
                return key;
            } else {
                return toRet;
            }
        }

        private String mLuceneField;
        private String mValue;

        public LuceneTableQuery(Mailbox mbox, int modifier, int target, String luceneField, String value) {
            super(modifier, target);
            mMailbox = mbox;
            mLuceneField = luceneField;
            mValue = value;
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            TextQueryOperation op = mMailbox.getMailboxIndex().createTextQueryOperation();

            Query q = null;
            if (mValue != null) {
                q = new TermQuery(new Term(mLuceneField, mValue));
            }
            op.addClause(getQueryOperatorString()+mValue, q,calcTruth(truth));

            return op;
        }

        @Override public String toString(int expLevel) {
            return super.toString(expLevel) + "," + mLuceneField + ":" + mValue + ")";
        }
    }

    public static class ReadQuery extends TagQuery {
        public ReadQuery(Mailbox mailbox, Analyzer analyzer, int modifier,
                boolean truth) throws ServiceException {
            super(mailbox, analyzer, modifier, "\\Unread", !truth);
        }

        @Override
        public String toString(int expLevel) {
            if (!mTruth) {
                return super.toString(expLevel) + ",READ)";
            } else {
                return super.toString(expLevel) + ",UNREAD)";
            }
        }
    }

    public static class RepliedQuery extends TagQuery {
        public RepliedQuery(Mailbox mailbox, Analyzer analyzer, int modifier,
                boolean truth) throws ServiceException {
            super(mailbox, analyzer, modifier, "\\Answered", truth);
        }

        @Override
        public String toString(int expLevel) {
            if (mTruth) {
                return super.toString(expLevel) + ",REPLIED)";
            } else {
                return super.toString(expLevel) + ",UNREPLIED)";
            }
        }
    }

    public static class IsInviteQuery extends TagQuery {
        public IsInviteQuery(Mailbox mailbox, Analyzer analyzer, int modifier,
                boolean truth) throws ServiceException {
            super(mailbox, analyzer, modifier, "\\Invite", truth);
        }

        @Override
        public String toString(int expLevel) {
            if (mTruth) {
                return super.toString(expLevel) + ",INVITE)";
            } else {
                return super.toString(expLevel) + ",NOT_INVITE)";
            }
        }
    }

    public static class SentQuery extends TagQuery {
        public SentQuery(Mailbox mailbox, Analyzer analyzer, int modifier,
                boolean truth) throws ServiceException {
            super(mailbox, analyzer, modifier, "\\Sent", truth);
        }

        @Override
        public String toString(int expLevel) {
            if (mTruth) {
                return super.toString(expLevel) + ",SENT)";
            } else {
                return super.toString(expLevel) + ",RECEIVED)";
            }
        }
    }

    public static class SizeQuery extends BaseQuery {
        private String mSizeStr;
        private long mSize;

        public SizeQuery(Analyzer analyzer, int modifier, int target,
                String size) throws ParseException {
            super(modifier, target);

            boolean hasEQ = false;

            mSizeStr = size;

            char ch = mSizeStr.charAt(0);
            if (ch == '>') {
                setQueryType(ZimbraQueryParser.BIGGER);
                mSizeStr = mSizeStr.substring(1);
            } else if (ch == '<') {
                setQueryType(ZimbraQueryParser.SMALLER);
                mSizeStr = mSizeStr.substring(1);
            }

            ch = mSizeStr.charAt(0);
            if (ch == '=') {
                mSizeStr = mSizeStr.substring(1);
                hasEQ = true;
            }

            char typeChar = '\0';

            typeChar = Character.toLowerCase(mSizeStr.charAt(mSizeStr.length() - 1));
            // strip "b" off end (optimize me)
            if (typeChar == 'b') {
                mSizeStr = mSizeStr.substring(0,mSizeStr.length()-1);
                typeChar = Character.toLowerCase(mSizeStr.charAt(mSizeStr.length() -1));
            }

            // size:100b size:1kb size:1mb bigger:10kb smaller:3gb
            //
            // n+{b,kb,mb}    // default is b
            int multiplier = 1;
            switch (typeChar) {
                case 'k':
                    multiplier = 1024;
                    break;
                case 'm':
                    multiplier = 1024 * 1024;
                    break;
            }

            if (multiplier > 1) {
                mSizeStr = mSizeStr.substring(0, mSizeStr.length() - 1);
            }

            mSize = Integer.parseInt(mSizeStr) * multiplier;

            if (hasEQ) {
                if (getQueryType() == ZimbraQueryParser.BIGGER) {
                    mSize--; // correct since range constraint is strict >
                } else if (getQueryType() == ZimbraQueryParser.SMALLER) {
                    mSize++; // correct since range constraint is strict <
                }
            }

            mSizeStr = ZimbraAnalyzer.SizeTokenFilter.encodeSize(mSize);
            if (mSizeStr == null) {
                mSizeStr = "";
            }
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation op = DBQueryOperation.Create();

            truth = calcTruth(truth);

            long highest = -1, lowest = -1;

            switch (getQueryType()) {
                case ZimbraQueryParser.BIGGER:
                    highest = -1;
                    lowest = mSize;
                    break;
                case ZimbraQueryParser.SMALLER:
                    highest = mSize;
                    lowest = -1;
                    break;
                case ZimbraQueryParser.SIZE:
                    highest = mSize+1;
                    lowest = mSize-1;
                    break;
                default:
                    assert(false);
            }
            op.addSizeClause(lowest, highest, truth);
            return op;
        }

        @Override
        public String toString(int expLevel) {
            return super.toString(expLevel) + "," + mSize + ")";
        }
    }

    public static class ModseqQuery extends BaseQuery {
        static enum Operator {
            EQ, GT, GTEQ, LT, LTEQ;
        }

        private int mValue;
        private Operator mOp;

        public ModseqQuery(Mailbox mbox, Analyzer analyzer, int modifier,
                int target, String changeId) throws ParseException {
            super(modifier, target);

            if (changeId.charAt(0) == '<') {
                if (changeId.charAt(1) == '=') {
                    mOp = Operator.LTEQ;
                    changeId = changeId.substring(2);
                } else {
                    mOp = Operator.LT;
                    changeId = changeId.substring(1);
                }
            } else if (changeId.charAt(0) == '>') {
                if (changeId.charAt(1) == '=') {
                    mOp = Operator.GTEQ;
                    changeId = changeId.substring(2);
                } else {
                    mOp = Operator.GT;
                    changeId = changeId.substring(1);
                }
            } else {
                mOp = Operator.EQ;
            }
            mValue = Integer.parseInt(changeId);
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation op = DBQueryOperation.Create();
            truth = calcTruth(truth);

            long highest = -1, lowest = -1;
            boolean lowestEq = false;
            boolean highestEq = false;

            switch (mOp) {
                case EQ:
                    highest = mValue;
                    lowest = mValue;
                    highestEq = true;
                    lowestEq = true;
                    break;
                case GT:
                    lowest = mValue;
                    break;
                case GTEQ:
                    lowest = mValue;
                    lowestEq = true;
                    break;
                case LT:
                    highest = mValue;
                    break;
                case LTEQ:
                    highest = mValue;
                    highestEq = true;
                    break;
            }

            op.addModSeqClause(lowest, lowestEq, highest, highestEq, truth);
            return op;
        }

        @Override public String toString(int expLevel) {
            return super.toString(expLevel) + "," + mOp + " " + mValue + ")";
        }
    }


    public static class SubQuery extends BaseQuery {
        private List<BaseQuery> mSubClauses;
        public SubQuery(Analyzer analyzer, int modifier, List<BaseQuery> exp) {
            super(modifier, SUBQUERY_TOKEN );
            mSubClauses = exp;
        }

        protected BaseQuery getSubClauseHead() {
            return mSubClauses.get(0);
        }

        List<BaseQuery> getSubClauses() {
            return mSubClauses;
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            assert(false);
            return null;
        }

        @Override
        public String toString(int expLevel) {
            String ret = indent(expLevel) + modToString() + "( ";
            BaseQuery sub = mSubClauses.get(0);
            while (sub != null) {
                ret += sub.toString(expLevel+1)+" ";
                sub = sub.getNext();
            }
            ret+=indent(expLevel) + " )";
            return ret;
        }
    }

    // bitmask for choosing "FROM/TO/CC" of messages...used for AddrQuery and MeQuery
    public static final int ADDR_BITMASK_FROM = 0x1;
    public static final int ADDR_BITMASK_TO =   0x2;
    public static final int ADDR_BITMASK_CC =   0x4;

    /**
     * A simpler way of expressing (to:FOO or from:FOO or cc:FOO)
     */
    public static class AddrQuery extends SubQuery {
        protected AddrQuery(Analyzer analyzer, int modifier,
                List<BaseQuery> exp) {
            super(analyzer, modifier, exp);
        }
        public static ZimbraQuery.BaseQuery createFromTarget(Mailbox mbox,
                Analyzer analyzer, int modifier, int target, String text)
                throws ServiceException {
            int bitmask = 0;
            switch (target) {
                case ZimbraQueryParser.TOFROM:
                    bitmask = ADDR_BITMASK_TO | ADDR_BITMASK_FROM;
                    break;
                case ZimbraQueryParser.TOCC:
                    bitmask = ADDR_BITMASK_TO | ADDR_BITMASK_CC;
                    break;
                case ZimbraQueryParser.FROMCC:
                    bitmask = ADDR_BITMASK_FROM | ADDR_BITMASK_CC;
                    break;
                case ZimbraQueryParser.TOFROMCC:
                    bitmask = ADDR_BITMASK_TO | ADDR_BITMASK_FROM | ADDR_BITMASK_CC;
                    break;
            }
            return createFromBitmask(mbox, analyzer, modifier, text, bitmask);
        }

        public static ZimbraQuery.BaseQuery createFromBitmask(Mailbox mbox,
                Analyzer analyzer, int modifier, String text,
                int operatorBitmask) throws ServiceException {
            ArrayList<ZimbraQuery.BaseQuery> clauses = new ArrayList<ZimbraQuery.BaseQuery>();
            boolean atFirst = true;

            if ((operatorBitmask & ADDR_BITMASK_FROM) !=0) {
                clauses.add(new TextQuery(mbox, analyzer, modifier, ZimbraQueryParser.FROM, text));
                atFirst = false;
            }
            if ((operatorBitmask & ADDR_BITMASK_TO) != 0) {
                if (atFirst)
                    atFirst = false;
                else
                    clauses.add(new ConjQuery(analyzer, ConjQuery.OR));
                clauses.add(new TextQuery(mbox, analyzer, modifier, ZimbraQueryParser.TO, text));
            }
            if ((operatorBitmask & ADDR_BITMASK_CC) != 0) {
                if (atFirst)
                    atFirst = false;
                else
                    clauses.add(new ConjQuery(analyzer, ConjQuery.OR));
                clauses.add(new TextQuery(mbox, analyzer, modifier, ZimbraQueryParser.CC, text));
            }
            return new AddrQuery(analyzer, modifier, clauses);
        }
    }

    /** Messages "to me" "from me" or "cc me" or any combination thereof */
    public static class MeQuery extends SubQuery {
        protected MeQuery(Analyzer analyzer, int modifier, List<BaseQuery> exp) {
            super(analyzer, modifier, exp);
        }

        public static ZimbraQuery.BaseQuery create(Mailbox mbox,
                Analyzer analyzer, int modifier, int operatorBitmask)
                throws ServiceException {
            ArrayList<BaseQuery> clauses = new ArrayList<BaseQuery>();
            Account acct = mbox.getAccount();
            boolean atFirst = true;
            if ((operatorBitmask & ADDR_BITMASK_FROM) != 0) {
                clauses.add(new SentQuery(mbox, analyzer, modifier, true));
                atFirst = false;
            }
            if ((operatorBitmask & ADDR_BITMASK_TO) != 0) {
                if (atFirst) {
                    atFirst = false;
                } else {
                    clauses.add(new ConjQuery(analyzer, ConjQuery.OR));
                }
                clauses.add(new TextQuery(mbox, analyzer, modifier,
                        ZimbraQueryParser.TO, acct.getName()));
            }
            if ((operatorBitmask & ADDR_BITMASK_CC) != 0) {
                if (atFirst) {
                    atFirst = false;
                } else {
                    clauses.add(new ConjQuery(analyzer, ConjQuery.OR));
                }
                clauses.add(new TextQuery(mbox, analyzer, modifier,
                        ZimbraQueryParser.CC, acct.getName()));
            }

            String[] aliases = acct.getMailAlias();
            for (String alias : aliases) {
                // if ((operatorBitmask & ADDR_BITMASK_FROM) !=0) {
                //     if (atFirst) {
                //         clauses.add(new ConjQuery(analyzer, ConjQuery.OR));
                //         atFirst = false;
                //     }
                //     clauses.add(new SentQuery(mbox, analyzer, modifier, true));
                // }
                if ((operatorBitmask & ADDR_BITMASK_TO) != 0) {
                    if (atFirst) {
                        atFirst = false;
                    } else {
                        clauses.add(new ConjQuery(analyzer, ConjQuery.OR));
                    }
                    clauses.add(new TextQuery(mbox, analyzer, modifier,
                            ZimbraQueryParser.TO, alias));
                }
                if ((operatorBitmask & ADDR_BITMASK_CC) != 0) {
                    if (atFirst) {
                        atFirst = false;
                    } else {
                        clauses.add(new ConjQuery(analyzer, ConjQuery.OR));
                    }
                    clauses.add(new TextQuery(mbox, analyzer, modifier,
                            ZimbraQueryParser.CC, alias));
                }
            }
            return new MeQuery(analyzer, modifier, clauses);
        }
    }

    public static class TagQuery extends BaseQuery {
        private Tag mTag = null;

        public TagQuery(Mailbox mailbox, Analyzer analyzer, int modifier,
                String name, boolean truth) throws ServiceException {
            super(modifier, ZimbraQueryParser.TAG);
            mTag = mailbox.getTagByName(name);
            mTruth = truth;
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation dbOp = DBQueryOperation.Create();

            dbOp.addTagClause(mTag, calcTruth(truth));

            return dbOp;
        }

        @Override
        public String toString(int expLevel) {
            return super.toString(expLevel) + "," + mTag + ")";
        }
    }

    public static class ItemQuery extends BaseQuery {
        public static BaseQuery Create(Mailbox mbox, Analyzer analyzer,
                int modifier, String str) throws ServiceException {
            boolean allQuery = false;
            boolean noneQuery = false;
            List<ItemId> itemIds = new ArrayList<ItemId>();

            if (str.equalsIgnoreCase("all")) {
                allQuery = true;
            } else if (str.equalsIgnoreCase("none")) {
                noneQuery = true;
            } else {
                String[] items = str.split(",");
                for (int i = 0; i < items.length; i++) {
                    if (items[i].length() > 0) {
                        ItemId iid = new ItemId(items[i], mbox.getAccountId());
                        itemIds.add(iid);
                    }
                }
                if (itemIds.size() == 0) {
                    noneQuery = true;
                }
            }

            return new ItemQuery(mbox, analyzer, modifier, allQuery, noneQuery, itemIds);
        }

        private boolean mIsAllQuery;
        private boolean mIsNoneQuery;
        private List<ItemId> mItemIds;
        private Mailbox mMailbox;

        ItemQuery(Mailbox mbox, Analyzer analyzer, int modifier, boolean all,
                boolean none, List<ItemId> ids) {
            super(modifier, ZimbraQueryParser.ITEM);
            mIsAllQuery = all;
            mIsNoneQuery = none;
            mItemIds = ids;
            mMailbox = mbox;
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation dbOp = DBQueryOperation.Create();

            truth = calcTruth(truth);

            if (truth&&mIsAllQuery || !truth&&mIsNoneQuery) {
                // adding no constraints should match everything...
            } else if (truth&&mIsNoneQuery || !truth&&mIsAllQuery) {
                return new NoResultsQueryOperation();
            } else {
                for (ItemId iid : mItemIds) {
                    dbOp.addItemIdClause(mMailbox, iid, truth);
                }
            }
            return dbOp;
        }

        @Override
        public String toString(int expLevel) {
            StringBuilder toRet = new StringBuilder(super.toString(expLevel));
            if (mIsAllQuery) {
                toRet.append(",all");
            } else if (mIsNoneQuery) {
                toRet.append(",none");
            } else {
                for (ItemId id : mItemIds) {
                    toRet.append("," + id.toString());
                }
            }
            return toRet.toString();
        }
    }


    public static class FieldQuery {
        public static TextQuery Create(Mailbox mbox, Analyzer analyzer,
                int modifier, int qType, String targetImg, String text)
                throws ServiceException {
            // targetImg can be:
            //      field:
            //      #something:
            //      field[something]:

            int open = targetImg.indexOf('[');
            if (open >= 0) {
                String fieldName = null;
                int close = targetImg.indexOf(']');
                if (close >= 0 && close > open) {
                    fieldName = targetImg.substring(open+1, close);
                    System.out.println("Field is: \"" + fieldName + "\"");
                }
                text = fieldName + ":" + text;
            } else {
                if (targetImg.charAt(0) == '#') {
                    String fieldName = targetImg.substring(1);
                    text = fieldName + text;
                }
            }

            return new TextQuery(mbox, analyzer, modifier, qType, text);
        }
    }
    public static class TextQuery extends BaseQuery {
        private ArrayList<String> mTokens;
        private int mSlop;  // sloppiness for PhraseQuery

        private List<String> mOredTokens;
        private String mWildcardTerm;
        private String mOrigText;
        private List<QueryInfo> mQueryInfo = new ArrayList<QueryInfo>();
        private Mailbox mMailbox;

        private static final int MAX_WILDCARD_TERMS =
            LC.zimbra_index_wildcard_max_terms_expanded.intValue();

        /**
         * @param mbox
         * @param analyzer
         * @param modifier
         * @param qType
         * @param text A single search term. If text has multiple words, it is
         * treated as a phrase (full exact match required) text may end in a *,
         * which wildcards the last term.
         * @throws ServiceException
         */
        public TextQuery(Mailbox mbox, Analyzer analyzer, int modifier,
                int qType, String text) throws ServiceException {
            super(modifier, qType);

            if (mbox == null) {
                throw new IllegalArgumentException(
                        "Must not pass a null mailbox into TextQuery constructor");
            }

            mMailbox = mbox;
            mOredTokens = new LinkedList<String>();

            // The set of tokens from the user's query. The way the parser
            // works, the token set should generally only be one element.
            mTokens = new ArrayList<String>(1);
            mWildcardTerm = null;

            // TODO: shameless hack for bug 28666 -- the custom TO/FROM/CC
            // analyzer causing problems with wildcards here, so for now use the
            // standard content analyzer when handling TO/FROM/CC searches. This
            // is probably fine for western languages, but it likely creates
            // problems with custom analyzers used for Asian languages.
            // Investigate this issue.
            if (!text.endsWith("*") || (qType != ZimbraQueryParser.TO &&
                    qType != ZimbraQueryParser.CC &&
                    qType != ZimbraQueryParser.FROM)) {
                TokenStream source;
                if (analyzer instanceof ZimbraAnalyzer) {
                    source = ((ZimbraAnalyzer) analyzer).tokenStreamSearching(
                            QueryTypeString(qType), new StringReader(text));
                    if (source instanceof ZimbraAnalyzer.AddressTokenFilter) {
                        // should we check for filed name also? probably not
                        mSlop = 1;
                    }
                } else {
                    source = analyzer.tokenStream(QueryTypeString(qType),
                            new StringReader(text));
                }
                org.apache.lucene.analysis.Token t;

                while(true) {
                    try {
                        t = source.next();
                    } catch (IOException e) {
                        t = null;
                    }
                    if (t == null)
                        break;
                    mTokens.add(t.termText());
                }
                try {
                    source.close();
                } catch (IOException ignore) {
                }
            } else {
                mTokens.add(text);
            }


            // okay, quite a bit of hackery here....basically, if they're doing a contact:
            // search AND they haven't manually specified a phrase query (expands to more than one term)
            // then lets hack their search and make it a * search.
            // for bug:17232 -- if the search string is ".", don't auto-wildcard it, because . is
            // supposed to match everything by default.
            if (qType == ZimbraQueryParser.CONTACT && mTokens.size() <= 1 && text.length() > 0
                        && text.charAt(text.length()-1)!='*' && !text.equals(".")) {
                text = text + '*';
            }

            mOrigText = text;

            // must look at original text here b/c analyzer strips *'s
            if (text.length() > 0 && text.charAt(text.length()-1) == '*') {
                // wildcard query!
                String wcToken;

                // only the last token is allowed to have a wildcard in it
                if (mTokens.size() > 0) {
                    wcToken = mTokens.remove(mTokens.size() - 1);
                } else {
                    wcToken = text;
                }

                if (wcToken.charAt(wcToken.length()-1) == '*') {
                    wcToken = wcToken.substring(0, wcToken.length()-1);
                }

                if (wcToken.length() > 0) {
                    mWildcardTerm = wcToken;
                    MailboxIndex mbidx = mbox.getMailboxIndex();
                    List<String> expandedTokens = new ArrayList<String>(100);
                    boolean expandedAllTokens = false;
                    if (mbidx != null) {
                        expandedAllTokens = mbidx.expandWildcardToken(
                                expandedTokens, QueryTypeString(qType), wcToken,
                                MAX_WILDCARD_TERMS);
                    }

//                  if (!expandedAllTokens) {
//                  throw MailServiceException.TOO_MANY_QUERY_TERMS_EXPANDED("Wildcard text: \""+wcToken
//                  +"\" expands to too many terms (maximum allowed is "+MAX_WILDCARD_TERMS+")", wcToken+"*", MAX_WILDCARD_TERMS);
//                  }

                    mQueryInfo.add(new WildcardExpansionQueryInfo(wcToken + "*",
                            expandedTokens.size(), expandedAllTokens));
                    //
                    // By design, we interpret *zero* tokens to mean "ignore this search term"
                    // therefore if the wildcard expands to no terms, we need to stick something
                    // in right here, just so we don't get confused when we go to execute the
                    // query later
                    //
                    if (expandedTokens.size() == 0 || !expandedAllTokens) {
                        mTokens.add(wcToken);
                    } else {
                        for (String token : expandedTokens) {
                            mOredTokens.add(token);
                        }
                    }
                }
            }

            // MailboxIndex mbidx = mbox.getMailboxIndex();
            // if (mbidx != null) {
            //     // don't check spelling for structured-data searches
            //     if (qType != ZimbraQueryParser.FIELD) {
            //         for (String token : mTokens) {
            //             List<SpellSuggestQueryInfo.Suggestion> suggestions = mbidx.suggestSpelling(QueryTypeString(qType), token);
            //             if (suggestions != null)
            //                 mQueryInfo.add(new SpellSuggestQueryInfo(token, suggestions));
            //             }
            //         }
            //     }
            // }
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            if (mTokens.size() <= 0 && mOredTokens.size() <= 0) {
                // if we have no tokens, that is usually because the analyzer removed them
                // -- the user probably queried for a stop word like "a" or "an" or "the"
                //
                // By design: interpret *zero* tokens to mean "ignore this search term"
                //
                // We can't simply skip this term in the generated parse tree -- we have to put a null
                // query into the query list, otherwise conjunctions will get confused...so
                // we pass NULL to addClause which will add a blank clause for us...
                return new NoTermQueryOperation();
            } else {
                // indexing is disabled
                if (mMailbox.getMailboxIndex() == null)
                    return new NoTermQueryOperation();

                TextQueryOperation lop = mMailbox.getMailboxIndex().createTextQueryOperation();

                for (QueryInfo inf : mQueryInfo) {
                    lop.addQueryInfo(inf);
                }

                String fieldName = QueryTypeString(getQueryType());

                if (mTokens.size() == 0) {
                    lop.setQueryString(this.getQueryOperatorString()+mOrigText);
                } else if (mTokens.size() == 1) {
                    TermQuery q = null;
                    String queryTerm = mTokens.get(0);
                    q = new TermQuery(new Term(fieldName, queryTerm));
                    lop.addClause(this.getQueryOperatorString()+mOrigText,  q,calcTruth(truth));
                } else if (mTokens.size() > 1) {
                    PhraseQuery p = new PhraseQuery();
                    p.setSlop(mSlop); // TODO configurable?
                    for (int i=0; i<mTokens.size(); i++)
                        p.add(new Term(fieldName, mTokens.get(i)));
                    String qos = this.getQueryOperatorString();
                    lop.addClause(qos+mOrigText, p,calcTruth(truth));
                }

                if (mOredTokens.size() > 0) {
                    // probably don't need to do this here...can probably just call addClause
                    BooleanQuery orQuery = new BooleanQuery();
                    for (String token : mOredTokens) {
                        orQuery.add(new TermQuery(new Term(fieldName, token)), Occur.SHOULD);
                    }

                    lop.addClause("", orQuery, calcTruth(truth));
                }

                return lop;
            }
        }

        @Override
        public String toString(int expLevel) {
            String ret = super.toString(expLevel) + ",";
            for (int i = 0; i < mTokens.size(); i++) {
                ret += "," + (mTokens.get(i)).toString();
            }
            if (mWildcardTerm != null) {
                ret += " WILDCARD=" + mWildcardTerm + " [" + mOredTokens.size() + " terms]";
            }
            return ret + ")";
        }
    }

    public static class TypeQuery extends AttachmentQuery {
        public TypeQuery(Mailbox mbox, Analyzer analyzer, int modifier, String what) {
            super(mbox, modifier, LuceneFields.L_MIMETYPE, what);
        }
    }

    public static class SenderQuery extends BaseQuery {
        private String mStr;
        private boolean mLt;
        private boolean mEq;

        @Override
        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation op = DBQueryOperation.Create();
            truth = calcTruth(truth);

            if (mLt) {
                    op.addRelativeSender(null, false, mStr, mEq, truth);
            } else {
                op.addRelativeSender(mStr, mEq, null, false, truth);
            }

            return op;
        }

        @Override
        public String toString(int expLevel) {
            return super.toString(expLevel) + "Sender(";
        }

        /**
         * Don't call directly -- use SubjectQuery.create()
         *
         * This is only invoked for subject queries that start with < or > -- otherwise we just
         * use the normal TextQuery class
         *
         * @param mbox
         * @param analyzer
         * @param modifier
         * @param qType
         * @param text
         * @throws ServiceException
         */
        private SenderQuery(Mailbox mbox, Analyzer analyzer, int modifier,
                int qType, String text) throws ServiceException {
            super(modifier, qType);

            mLt = (text.charAt(0) == '<');
            mEq = false;
            mStr = text.substring(1);

            if (mStr.charAt(0) == '=') {
                mEq = true;
                mStr= mStr.substring(1);
            }

            // bug: 27976 -- we have to allow >"" for cursors to work as expected
            //            if (mStr.length() == 0)
            //                throw MailServiceException.PARSE_ERROR("Invalid sender string: "+text, null);
        }

        public static BaseQuery create(Mailbox mbox, Analyzer analyzer,
                int modifier, int qType, String text) throws ServiceException {
            if (text.length() > 1 && (text.startsWith("<") || text.startsWith(">"))) {
                return new SenderQuery(mbox, analyzer, modifier, qType, text);
            } else {
                return new TextQuery(mbox, analyzer, modifier, qType, text);
            }
        }
    }

    public static class ConvCountQuery extends BaseQuery {
        private int mLowestCount;  private boolean mLowerEq;
        private int mHighestCount; private boolean mHigherEq;

        private ConvCountQuery(Mailbox mbox, Analyzer analyzer, int modifier, int qType,
           int lowestCount, boolean lowerEq, int highestCount, boolean higherEq) {
            super(modifier, qType);

            mLowestCount = lowestCount;
            mLowerEq = lowerEq;
            mHighestCount = highestCount;
            mHigherEq = higherEq;
        }

        @Override
        public String toString(int expLevel) {
            return super.toString(expLevel) + "ConvCount("
               + (mLowerEq ? ">=" : ">") + mLowestCount + " "
               + (mHigherEq? "<=" : "<") + mHighestCount + ")";
        }

        @Override
        protected QueryOperation getQueryOperation(boolean truthiness) {
            DBQueryOperation op = DBQueryOperation.Create();
            truthiness = calcTruth(truthiness);
            op.addConvCountClause(mLowestCount, mLowerEq, mHighestCount, mHigherEq, truthiness);
            return op;
        }

        public static BaseQuery create(Mailbox mbox, Analyzer analyzer, int modifier, int qType, String str)
        throws ServiceException {
            if (str.charAt(0) == '<') {
                boolean eq = false;
                if (str.charAt(1) == '=') {
                    eq = true;
                    str = str.substring(2);
                } else {
                    str = str.substring(1);
                }
                int num = Integer.parseInt(str);
                return new ConvCountQuery(mbox, analyzer, modifier, qType,
                    -1, false, num, eq);
            } else if (str.charAt(0) == '>') {
                boolean eq = false;
                if (str.charAt(1) == '=') {
                    eq = true;
                    str = str.substring(2);
                } else {
                    str = str.substring(1);
                }
                int num = Integer.parseInt(str);
                return new ConvCountQuery(mbox, analyzer, modifier, qType,
                    num, eq, -1, false);
            } else {
                int num = Integer.parseInt(str);
                return new ConvCountQuery(mbox, analyzer, modifier, qType,
                    num, true, num, true);
            }
        }
    }

    public static class SubjectQuery extends BaseQuery {
        private String mStr;
        private boolean mLt;
        private boolean mEq;

        @Override
        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation op = DBQueryOperation.Create();
            truth = calcTruth(truth);

            if (mLt) {
                op.addRelativeSubject(null, false, mStr, mEq, truth);
            } else {
                op.addRelativeSubject(mStr, mEq, null, false, truth);
            }

            return op;
        }

        @Override
        public String toString(int expLevel) {
            return super.toString(expLevel) + "Subject(";
        }

        /**
         * Don't call directly -- use SubjectQuery.create()
         *
         * This is only invoked for subject queries that start with &lt; or
         * &gt;, otherwise we just use the normal TextQuery class.
         *
         * @param mbox
         * @param analyzer
         * @param modifier
         * @param qType
         * @param text
         * @throws ServiceException
         */
        private SubjectQuery(Mailbox mbox, Analyzer analyzer, int modifier,
                int qType, String text) throws ServiceException {
            super(modifier, qType);

            mLt = (text.charAt(0) == '<');
            mEq = false;
            mStr = text.substring(1);

            if (mStr.charAt(0) == '=') {
                mEq = true;
                mStr= mStr.substring(1);
            }

            // bug: 27976 -- we have to allow >"" for cursors to work as expected
            //if (mStr.length() == 0)
            //    throw MailServiceException.PARSE_ERROR("Invalid subject string: "+text, null);
        }

        public static BaseQuery create(Mailbox mbox, Analyzer analyzer,
                int modifier, int qType, String text) throws ServiceException {
            if (text.length() > 1 && (text.startsWith("<") || text.startsWith(">"))) {
                // real subject query!
                return new SubjectQuery(mbox, analyzer, modifier, qType, text);
            } else {
                return new TextQuery(mbox, analyzer, modifier, qType, text);
            }
        }
    }

    private static final int SUBQUERY_TOKEN = 9999;

    private List<BaseQuery> mClauses;
    private ParseTree.Node mParseTree = null;
    private QueryOperation mOp;
    private Mailbox mMbox;
    private ZimbraQueryResults mResults;
    private SearchParams mParams;
    private int mChunkSize;

    private static String[] unquotedTokenImage;
    private static Map<String, Integer> sTokenImageMap;

    static {
        sTokenImageMap = new HashMap<String,Integer>();

        unquotedTokenImage = new String[ZimbraQueryParserConstants.tokenImage.length];
        for (int i = 0; i < ZimbraQueryParserConstants.tokenImage.length; i++) {
            String str = ZimbraQueryParserConstants.tokenImage[i].substring(1, ZimbraQueryParserConstants.tokenImage[i].length()-1);
            if ("FIELD".equals(str))
                unquotedTokenImage[i] = "#"; // bug 22969 -- problem with proxying field queries
            else
                unquotedTokenImage[i] = str;
            sTokenImageMap.put(str, i);
        }
    }

    public static int lookupQueryTypeFromString(String str) throws ServiceException {
        Integer toRet = sTokenImageMap.get(str);
        if (toRet == null)
            throw MailServiceException.QUERY_PARSE_ERROR(str, null, str, -1, "UNKNOWN_QUERY_TYPE");
        return toRet.intValue();
    }

    private static String QueryTypeString(int qType) {
        switch (qType) {
            case ZimbraQueryParser.CONTACT:   return LuceneFields.L_CONTACT_DATA;
            case ZimbraQueryParser.CONTENT:    return LuceneFields.L_CONTENT;
            case ZimbraQueryParser.MSGID:       return LuceneFields.L_H_MESSAGE_ID;
            case ZimbraQueryParser.ENVFROM:       return LuceneFields.L_H_X_ENV_FROM;
            case ZimbraQueryParser.ENVTO:         return LuceneFields.L_H_X_ENV_TO;
            case ZimbraQueryParser.FROM:       return LuceneFields.L_H_FROM;
            case ZimbraQueryParser.TO:         return LuceneFields.L_H_TO;
            case ZimbraQueryParser.CC:         return LuceneFields.L_H_CC;
            case ZimbraQueryParser.SUBJECT:    return LuceneFields.L_H_SUBJECT;
            case ZimbraQueryParser.IN:         return "IN";
            case ZimbraQueryParser.HAS:        return "HAS";
            case ZimbraQueryParser.FILENAME:   return LuceneFields.L_FILENAME;
            case ZimbraQueryParser.TYPE:       return LuceneFields.L_MIMETYPE;
            case ZimbraQueryParser.ATTACHMENT: return LuceneFields.L_ATTACHMENTS;
            case ZimbraQueryParser.IS:         return "IS";
            case ZimbraQueryParser.DATE:       return "DATE";
            case ZimbraQueryParser.AFTER:      return "AFTER";
            case ZimbraQueryParser.BEFORE:     return "BEFORE";
            case ZimbraQueryParser.APPT_START: return "APPT-START";
            case ZimbraQueryParser.APPT_END: return "APPT-END";
            case ZimbraQueryParser.SIZE:       return "SIZE";
            case ZimbraQueryParser.BIGGER:     return "BIGGER";
            case ZimbraQueryParser.SMALLER:    return "SMALLER";
            case ZimbraQueryParser.TAG:        return "TAG";
            case ZimbraQueryParser.MY:         return "MY";
            case ZimbraQueryParser.MESSAGE:    return "MESSAGE";
            case ZimbraQueryParser.CONV:       return "CONV";
            case ZimbraQueryParser.CONV_COUNT: return "CONV-COUNT";
            case ZimbraQueryParser.CONV_MINM:  return "CONV_MINM";
            case ZimbraQueryParser.CONV_MAXM:  return "CONV_MAXM";
            case ZimbraQueryParser.CONV_START: return "CONV-START";
            case ZimbraQueryParser.CONV_END:   return "CONV-END";
            case ZimbraQueryParser.AUTHOR:     return "AUTHOR";
            case ZimbraQueryParser.TITLE:      return "TITLE";
            case ZimbraQueryParser.KEYWORDS:   return "KEYWORDS";
            case ZimbraQueryParser.COMPANY:    return "COMPANY";
            case ZimbraQueryParser.METADATA:   return "METADATA";
            case ZimbraQueryParser.ITEM:       return "ITEMID";
            case ZimbraQueryParser.FIELD:      return LuceneFields.L_FIELD;
        }
        return "UNKNOWN:(" + qType + ")";
    }

    /**
     *
     * ParseTree's job is to take the LIST of query terms (BaseQuery's) and build them
     * into a Tree structure of Things (return results) and Operators (AND and OR)
     *
     * Once a simple tree is built, then ParseTree "distributes the NOTs" down to the leaf
     * nodes: this is so we never have to do result-set inversions, which are prohibitively
     * expensive for nontrivial cases.
     *
     */
    private static class ParseTree
    {
        private static final int STATE_AND    = 1;
        private static final int STATE_OR     = 2;

        private static final boolean SPEW = false;

        static abstract class Node {
            boolean mTruthFlag = true;

            protected Node() {}

            public void setTruth(boolean truth) { mTruthFlag = truth; };
            public void invertTruth() {
                mTruthFlag = !mTruthFlag;
            }

            public abstract void pushNotsDown();

            public abstract Node simplify();

            public abstract QueryOperation getQueryOperation();
        }

        static class OperatorNode extends Node {
            int mKind;
            boolean mTruthFlag = true;
            public List<Node> mNodes = new ArrayList<Node>();

            public OperatorNode(int kind) {
                mKind = kind;
            }

            public void setTruth(boolean truth) { mTruthFlag = truth; };
            public void invertTruth() {
                mTruthFlag = !mTruthFlag;
            }

            public void pushNotsDown() {
                if (!mTruthFlag) { // ONLY push down if this is a "not"
                    mTruthFlag = !mTruthFlag;

                    if (mKind == STATE_AND) {
                        mKind = STATE_OR;
                    } else {
                        mKind = STATE_AND;
                    }
                    for (Node n : mNodes) {
                        n.invertTruth();
                    }
                }
                assert(mTruthFlag);
                for (Node n : mNodes) {
                    n.pushNotsDown();
                }
            }

            public Node simplify() {
                boolean simplifyAgain;
                do {
                    simplifyAgain = false;
                    // first, simplify our sub-ops...
                    List<Node> newNodes = new ArrayList<Node>();
                    for (Node n : mNodes) {
                        newNodes.add(n.simplify());
                    }
                    mNodes = newNodes;

                    // now, see if any of our subops can be trivially combined with us
                    newNodes = new ArrayList<Node>();
                    for (Node n : mNodes) {
                        boolean addIt = true;

                        if (n instanceof OperatorNode) {
                            OperatorNode opn = (OperatorNode)n;
                            if (opn.mKind == mKind && opn.mTruthFlag == true) {
                                addIt = false;
                                simplifyAgain = true;
                                for (Node opNode: opn.mNodes) {
                                    newNodes.add(opNode);
                                }
                            }
                        }
                        if (addIt) {
                            newNodes.add(n);
                        }
                    }
                    mNodes = newNodes;
                } while (simplifyAgain);

                if (mNodes.size() == 0) {
                    return null;
                }
                if (mNodes.size() == 1) {
                    Node n = mNodes.get(0);
                    if (!mTruthFlag) {
                        n.invertTruth();
                    }
                    return n;
                }
                return this;
            }

            public void add(Node subNode) {
                mNodes.add(subNode);
            }

            @Override
            public String toString() {
                StringBuffer toRet = new StringBuffer(mTruthFlag ? "" : " NOT ");

                toRet.append(mKind == STATE_AND ? " AND[" : " OR(");

                for (Node n : mNodes) {
                    toRet.append(n.toString());
                    toRet.append(", ");
                }
                toRet.append(mKind == STATE_AND ? "] " : ") ");
                return toRet.toString();
            }

            public QueryOperation getQueryOperation() {
                assert(mTruthFlag == true); // we should have pushed the NOT's down the tree already
                if (mKind == STATE_AND) {
                    if (ParseTree.SPEW) System.out.print(" AND(");

                    IntersectionQueryOperation intersect = new IntersectionQueryOperation();

                    for (Node n : mNodes) {
                        QueryOperation op = n.getQueryOperation();
                        assert(op!=null);
                        intersect.addQueryOp(op);
                    }

                    if (ParseTree.SPEW) {
                        System.out.print(") ");
                    }
                    return intersect;
                } else {
                    if (ParseTree.SPEW) {
                        System.out.print(" OR(");
                    }

                    UnionQueryOperation union = new UnionQueryOperation();

                    for (Node n : mNodes) {
                        QueryOperation op = n.getQueryOperation();
                        assert(op != null);
                        union.add(op);
                    }
                    if (ParseTree.SPEW) {
                        System.out.print(") ");
                    }
                    return union;
                }
            }

        }

        static class ThingNode extends Node {
            BaseQuery mThing;

            public ThingNode(BaseQuery thing) {
                mThing = thing;
                mTruthFlag = thing.mTruth;
            }

            public void invertTruth() {
                mTruthFlag = !mTruthFlag;
            }
            public void pushNotsDown() {
            }

            public Node simplify() {
                return this;
            }

            @Override
            public String toString() {
                StringBuffer toRet = new StringBuffer(mTruthFlag ? "" : " NOT ");
                toRet.append(mThing.toString());

                return toRet.toString();
            }

            public QueryOperation getQueryOperation() {
                return mThing.getQueryOperation(mTruthFlag);
            }
        }

        static Node build(List<BaseQuery> clauses) {
            OperatorNode top = new OperatorNode(STATE_OR);

            OperatorNode cur = new OperatorNode(STATE_AND);
            top.add(cur);

            for (BaseQuery q : clauses) {
                if (q instanceof ZimbraQuery.ConjQuery) {
                    if (((ConjQuery)q).isOr()) {
                        cur = new OperatorNode(STATE_AND);
                        top.add(cur);
                    }
                } else {
                    if (q instanceof SubQuery) {
                        SubQuery sq = (SubQuery)q;
                        Node subTree = build(sq.getSubClauses());
                        subTree.setTruth(!sq.isNegated());

                        cur.add(subTree);
                    } else {
                        cur.add(new ThingNode(q));
                    }
                }
            }

            return top;
        }
    }

    /**
     * the query string can OPTIONALLY have a "sortby:" element which will override
     * the sortBy specified in the <SearchRequest> xml...this is basically to allow
     * people to do more with cut-and-pasted search strings
     */
    private SortBy mSortByOverride = null;

    private void handleSortByOverride(String str) throws ServiceException {
        SortBy sortBy = SortBy.lookup(str);
        if (sortBy == null) {
            throw ServiceException.FAILURE(
                    "Unkown sortBy: specified in search string: " + str, null);
        }

        mSortByOverride = sortBy;
    }

    private static final class CountTextOperations implements QueryOperation.RecurseCallback {
        int num = 0;
        public void recurseCallback(QueryOperation op) {
            if (op instanceof TextQueryOperation) {
                num++;
            }
        }
    }
    private static final class CountCombiningOperations implements QueryOperation.RecurseCallback {
        int num = 0;
        public void recurseCallback(QueryOperation op) {
            if (op instanceof CombiningQueryOperation) {
                if (((CombiningQueryOperation)op).getNumSubOps() > 1) {
                    num++;
                }
            }
        }
    }

    /**
     * @return number of Text parts of this query
     */
    int countSearchTextOperations() {
        if (mOp == null) {
            return 0;
        }
        CountTextOperations count = new CountTextOperations();
        mOp.depthFirstRecurse(count);
        return count.num;
    }

    /**
     * @return number of Text parts of this query
     */
    private static int countSearchTextOperations(QueryOperation op) {
        if (op == null) {
            return 0;
        }
        CountTextOperations count = new CountTextOperations();
        op.depthFirstRecurse(count);
        return count.num;
    }

    /**
     * @return number of non-trivial (num sub-ops > 1) Combining operations (joins/unions)
     */
    int countNontrivialCombiningOperations() {
        if (mOp == null) {
            return 0;
        }
        CountCombiningOperations count =  new CountCombiningOperations();
        mOp.depthFirstRecurse(count);
        return count.num;
    }


    public static boolean unitTests(Mailbox mbox) throws ServiceException {

        try {
            final long GRAN = 1000L; // time granularity in SQL (1000ms = 1sec)
            final String JAN1Str = "01/01/2007";
            final long JAN1 = 1167638400000L;
            final long JAN2 = 1167724800000L;
            ///////////////////////////////////////////////////////////////
            //
            // Validate that date queries parse to the proper ranges.  The only caveat
            // here is that a query like "date:>foo" turns into the range
            // (foo+1, true, -1, false) instead of the more obvious one
            // (foo, false, -1, false) -- this is a quirk of the query parsing code.  Both
            // are correct.
            //              query             lower       higher
            // string dates
            testDate(mbox, "date:" + JAN1Str, JAN1, true, JAN2, false);
            testDate(mbox, "date:<" + JAN1Str, -1L, false, JAN1, false);
            testDate(mbox, "before:" + JAN1Str, -1L, false, JAN1, false);
            testDate(mbox, "date:<=" + JAN1Str, -1L, false, JAN2, false);
            testDate(mbox, "date:>" + JAN1Str, JAN2, true, -1L, false);
            testDate(mbox, "after:" + JAN1Str, JAN2, true, -1L, false);
            testDate(mbox, "date:>=" + JAN1Str, JAN1, true, -1L, false);

            // numeric dates
            testDate(mbox, "date:" + JAN1, JAN1, true, JAN1 + GRAN, false);
            testDate(mbox, "date:<" + JAN1, -1L, false, JAN1, false);
            testDate(mbox, "before:" + JAN1, -1L, false, JAN1, false);
            testDate(mbox, "date:<=" + JAN1, -1L, false, JAN1 + GRAN, false);
            testDate(mbox, "date:>" + JAN1, JAN1 + GRAN, true, -1L, false);
            testDate(mbox, "after:" + JAN1, JAN1 + GRAN, true, -1L, false);
            testDate(mbox, "date:>=" + JAN1, JAN1, true, -1L, false);

            return true;
        } catch (ServiceException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static void testDate(Mailbox mbox, String qs, long lowest,
            boolean lowestEq, long highest, boolean highestEq)
        throws ServiceException, ParseException {

        List<BaseQuery>  c; // clauses
        c = unitTestParse(mbox,  qs);
        for (BaseQuery t : c) {
            if (t instanceof DateQuery) {
                DateQuery dq = (DateQuery)t;
                if (dq.mLowestTime != lowest) {
                    throw ServiceException.FAILURE(
                            "Invalid lowest time (found " + dq.mLowestTime +
                            " expected " + lowest + "), query is \"" + qs + "\"",
                            null);
                }
                if (dq.mLowerEq != lowestEq) {
                    throw ServiceException.FAILURE(
                            "Invalid lower EQ (expected " + lowestEq +
                            "), query is \"" + qs + "\"", null);
                }
                if (dq.mHighestTime != highest) {
                    throw ServiceException.FAILURE(
                            "Invalid highest time (found " + dq.mHighestTime +
                            " expected " + highest + "), query is \"" + qs + "\"",
                            null);
                }
                if (dq.mHigherEq != highestEq) {
                    throw ServiceException.FAILURE(
                            "Invalid higher EQ (expected " + highestEq +
                            "), query is \"" + qs + "\"", null);
                }
            }
        }
    }

    private static List<BaseQuery> unitTestParse(Mailbox mbox, String qs)
        throws ServiceException, ParseException {

        Analyzer analyzer = null;
        MailboxIndex mi = mbox.getMailboxIndex();
        if (mi != null) {
            mi.initAnalyzer(mbox);
            analyzer = mi.getAnalyzer();
        } else {
            analyzer = ZimbraAnalyzer.getDefaultAnalyzer();
        }
        ZimbraQueryParser parser = new ZimbraQueryParser(new StringReader(qs));
        parser.init(analyzer, mbox, null, null, lookupQueryTypeFromString("content:"));
        return parser.Parse();
    }

    /**
     * Take the specified query string and build an optimized query. Do not
     * execute the query, however.
     *
     * @param mbox
     * @param params
     * @throws ParseException
     * @throws ServiceException
     */
    public ZimbraQuery(OperationContext octxt, SoapProtocol proto,
            Mailbox mbox, SearchParams params)
        throws ParseException, ServiceException {

        mParams = params;
        mMbox = mbox;
        long chunkSize = (long)mParams.getOffset() + (long)mParams.getLimit();
        if (chunkSize > 1000) {
            mChunkSize = 1000;
        } else {
            mChunkSize = (int)chunkSize;
        }

        Analyzer analyzer = null;
        MailboxIndex mi = mbox.getMailboxIndex();

        //
        // Step 1: parse the text using the JavaCC parser
        try {
            ZimbraQueryParser parser = new ZimbraQueryParser(
                    new StringReader(mParams.getQueryStr()));
            if (mi != null) {
                mi.initAnalyzer(mbox);
                analyzer = mi.getAnalyzer();
            } else {
                analyzer = ZimbraAnalyzer.getDefaultAnalyzer();
            }
            parser.init(analyzer, mMbox,
                    params.getTimeZone(), params.getLocale(),
                    lookupQueryTypeFromString(params.getDefaultField()));
            mClauses = parser.Parse();

            String sortByStr = parser.getSortByStr();
            if (sortByStr != null)
                handleSortByOverride(sortByStr);
        } catch (OutOfMemoryError e) {
            throw e;
        } catch (Error e) {
            throw ServiceException.FAILURE(
                    "ZimbraQueryParser threw Error: " + e, e);
        }

        if (ZimbraLog.index_search.isDebugEnabled()) {
            String str = this.toString() + " search([";
            for (int i = 0; i < mParams.getTypes().length; i++) {
                if (i > 0) {
                    str += ",";
                }
                str+=mParams.getTypes()[i];
            }
            str += "]," + mParams.getSortBy() + ")";
            ZimbraLog.index_search.debug(str);
        }

        //
        // Step 2: build a parse tree and push all the "NOT's" down to the
        // bottom level -- this is because we cannot invert result sets
        if (ParseTree.SPEW) {
            System.out.println("QueryString: " + mParams.getQueryStr());
        }
        ParseTree.Node pt = ParseTree.build(mClauses);
        if (ParseTree.SPEW) {
            System.out.println("PT: " + pt.toString());
        }
        if (ParseTree.SPEW) {
            System.out.println("Simplified:");
        }
        pt = pt.simplify();
        if (ParseTree.SPEW) {
            System.out.println("PT: " + pt.toString());
        }
        if (ParseTree.SPEW) {
            System.out.println("Pushing nots down:");
        }
        pt.pushNotsDown();
        if (ParseTree.SPEW) {
            System.out.println("PT: " + pt.toString());
        }

        //
        // Store some variables that we'll need later
        mParseTree = pt;
        mOp = null;

        //
        // handle the special "sort:" tag in the search string
        if (mSortByOverride != null) {
            if (ZimbraLog.index_search.isDebugEnabled())
                ZimbraLog.index_search.debug(
                        "Overriding SortBy parameter to execute (" +
                        params.getSortBy().toString() +
                        ") w/ specification from QueryString: " +
                        mSortByOverride.toString());

            params.setSortBy(mSortByOverride);
        }

        //
        // Step 3: Convert list of BaseQueries into list of QueryOperations, then Optimize the Ops

        if (mClauses.size() > 0) {
            // this generates all of the query operations
            mOp = mParseTree.getQueryOperation();

            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug("OP=" + mOp.toString());
            }

            // expand the is:local and is:remote parts into in:(LIST)'s
            mOp = mOp.expandLocalRemotePart(mbox);
            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug("AFTEREXP=" + mOp.toString());
            }

            // optimize the query down
            mOp = mOp.optimize(mMbox);
            if (mOp == null)
                mOp = new NoResultsQueryOperation();
            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug("OPTIMIZED=" + mOp.toString());
            }
        }


        //
        // STEP 4: use the OperationContext to update the set of visible referenced folders, local AND remote
        //
        if (mOp!= null) {
            {
                QueryTargetSet targets = mOp.getQueryTargets();
                assert(mOp instanceof UnionQueryOperation ||
                        targets.countExplicitTargets() <= 1);
            }

            //
            // easiest to treat the query two unions: one the LOCAL and one REMOTE parts
            //
            UnionQueryOperation remoteOps = new UnionQueryOperation();
            UnionQueryOperation localOps = new UnionQueryOperation();

            if (mOp instanceof UnionQueryOperation) {
                UnionQueryOperation union = (UnionQueryOperation)mOp;
                // separate out the LOCAL vs REMOTE parts...
                for (QueryOperation op : union.mQueryOperations) {
                    QueryTargetSet targets = op.getQueryTargets();

                    // this assertion OK because we have already distributed multi-target query ops
                    // during the optimize() step
                    assert(targets.countExplicitTargets() <= 1);

                    // the assertion above is critical: the code below all assumes
                    // that we only have ONE target (ie we've already distributed if necessary)

                    if (targets.hasExternalTargets()) {
                        remoteOps.add(op);
                    } else {
                        localOps.add(op);
                    }
                }
            } else {
                // single target: might be local, might be remote

                QueryTargetSet targets = mOp.getQueryTargets();
                // this assertion OK because we have already distributed multi-target query ops
                // during the optimize() step
                assert(targets.countExplicitTargets() <= 1);

                if (targets.hasExternalTargets()) {
                    remoteOps.add(mOp);
                } else {
                    localOps.add(mOp);
                }
            }

            //
            // Handle the REMOTE side:
            //
            if (!remoteOps.mQueryOperations.isEmpty()) {
                // Since optimize() has already been run, we know that each of our ops
                // only has one target (or none).  Find those operations which have
                // an external target and wrap them in RemoteQueryOperations
                for (int i = remoteOps.mQueryOperations.size()-1; i >= 0; i--) { // iterate backwards so we can remove/add w/o screwing iteration
                    QueryOperation op = remoteOps.mQueryOperations.get(i);

                    QueryTargetSet targets = op.getQueryTargets();

                    // this assertion OK because we have already distributed multi-target query ops
                    // during the optimize() step
                    assert(targets.countExplicitTargets() <= 1);

                    // the assertion above is critical: the code below all assumes
                    // that we only have ONE target (ie we've already distributed if necessary)

                    if (targets.hasExternalTargets()) {
                        remoteOps.mQueryOperations.remove(i);
                        boolean foundOne = false;
                        // find a remoteOp to add this one to
                        for (QueryOperation tryIt : remoteOps.mQueryOperations) {
                            if (tryIt instanceof RemoteQueryOperation) {
                                if (((RemoteQueryOperation)tryIt).tryAddOredOperation(op)) {
                                    foundOne = true;
                                    break;
                                }
                            }
                        }
                        if (!foundOne) {
                            RemoteQueryOperation remoteOp = new RemoteQueryOperation();
                            remoteOp.tryAddOredOperation(op);
                            remoteOps.mQueryOperations.add(i, remoteOp);
                        }
                    }
                }

                // ...we need to call setup on every RemoteQueryOperation we end up with...
                for (QueryOperation toSetup : remoteOps.mQueryOperations) {
                    assert(toSetup instanceof RemoteQueryOperation);
                    try {
                        RemoteQueryOperation remote = (RemoteQueryOperation) toSetup;
                        remote.setup(proto, octxt.getAuthToken(), params);
                    } catch(Exception e) {
                        ZimbraLog.index_search.info("Ignoring "+e+" during RemoteQuery generation for "+remoteOps.toString());
                    }
                }
            }

            //
            // For the LOCAL parts of the query, do permission checks, do trash/spam exclusion
            //
            if (!localOps.mQueryOperations.isEmpty()) {
                if (ZimbraLog.index_search.isDebugEnabled()) {
                    ZimbraLog.index_search.debug("LOCAL_IN=" + localOps.toString());
                }

                Account authAcct = null;
                if (octxt != null) {
                    authAcct = octxt.getAuthenticatedUser();
                } else {
                    authAcct = mbox.getAccount();
                }

                //
                // Now, for all the LOCAL PARTS of the query, add the trash/spam exclusion part
                //
                boolean includeTrash = false;
                boolean includeSpam = false;
                if (authAcct != null) {
                    includeTrash = authAcct.getBooleanAttr(Provisioning.A_zimbraPrefIncludeTrashInSearch, false);
                    includeSpam = authAcct.getBooleanAttr(Provisioning.A_zimbraPrefIncludeSpamInSearch, false);
                }
                if (!includeTrash || !includeSpam) {
                    List<QueryOperation> toAdd = new ArrayList<QueryOperation>();
                    for (Iterator<QueryOperation> iter = localOps.mQueryOperations.iterator(); iter.hasNext();) {
                        QueryOperation cur = iter.next();
                        if (!cur.hasSpamTrashSetting()) {
                            QueryOperation newOp = cur.ensureSpamTrashSetting(mbox, includeTrash, includeSpam);
                            if (newOp != cur) {
                                iter.remove();
                                toAdd.add(newOp);
                            }
                        }
                    }
                    localOps.mQueryOperations.addAll(toAdd);
                }

                if (ZimbraLog.index_search.isDebugEnabled()) {
                    ZimbraLog.index_search.debug(
                            "LOCAL_AFTERTS=" + localOps.toString());
                }

                //
                // Check to see if we need to filter out private appointment data
                boolean allowPrivateAccess = true;
                if (octxt != null) {
                    allowPrivateAccess = AccessManager.getInstance().allowPrivateAccess(octxt.getAuthenticatedUser(),
                                                                    mbox.getAccount(), octxt.isUsingAdminPrivileges());
                }

                //
                // bug 28892 - ACL.RIGHT_PRIVATE support:
                //
                // Basically, if ACL.RIGHT_PRIVATE is set somewhere, and if we're excluding private items from
                // search, then we need to run the query twice -- once over the whole mailbox with
                // private items excluded and then UNION it with a second run, this time only in the
                // RIGHT_PRIVATE enabled folders, with private items enabled.
                //
                UnionQueryOperation clonedLocal = null;
                Set<Folder> hasFolderRightPrivateSet = new HashSet<Folder>();

                // ...don't do any of this if they aren't asking for a calendar type...
                boolean hasCalendarType = false;
                if (params.getTypes() != null) {
                    for (byte b : params.getTypes()) {
                        if (b == MailItem.TYPE_APPOINTMENT || b == MailItem.TYPE_TASK) {
                            hasCalendarType = true;
                            break;
                        }
                    }
                }
                if (hasCalendarType && !allowPrivateAccess && countSearchTextOperations(localOps)>0) {
                    // the searcher is NOT allowed to see private items globally....lets check
                    // to see if there are any individual folders that they DO have rights to...
                    // if there are any, then we'll need to run special searches in those
                    // folders
                    Set<Folder> allVisibleFolders = mbox.getVisibleFolders(octxt);
                    if (allVisibleFolders == null) {
                        allVisibleFolders = new HashSet<Folder>();
                        allVisibleFolders.addAll(mbox.getFolderList(octxt, SortBy.NONE));
                    }
                    for (Folder f : allVisibleFolders) {
                        if (f.getType() == MailItem.TYPE_FOLDER &&
                                CalendarItem.allowPrivateAccess(f, authAcct, false)) {
                            hasFolderRightPrivateSet.add(f);
                        }
                    }
                    if (!hasFolderRightPrivateSet.isEmpty()) {
                        clonedLocal = (UnionQueryOperation)localOps.clone();
                    }
                }

                Set<Folder> visibleFolders = mbox.getVisibleFolders(octxt);

                localOps = handleLocalPermissionChecks(localOps, mMbox, octxt,
                                                       mMbox.getMailboxIndex(), mParams,
                                                       visibleFolders,
                                                       allowPrivateAccess);

                if (ZimbraLog.index_search.isDebugEnabled()) {
                    ZimbraLog.index_search.debug("LOCAL_AFTER_PERM_CHECKS="+localOps.toString());
                }

                if (!hasFolderRightPrivateSet.isEmpty()) {
                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        ZimbraLog.index_search.debug("CLONED_LOCAL_BEFORE_PERM="+clonedLocal.toString());
                    }

                    //
                    // now we're going to setup the clonedLocal tree
                    // to run with private access ALLOWED, over the set of folders
                    // that have RIGHT_PRIVATE (note that we build this list from the visible
                    // folder list, so we are
                    //
                    clonedLocal = handleLocalPermissionChecks(clonedLocal, mMbox, octxt,
                                                              mMbox.getMailboxIndex(), mParams,
                                                              hasFolderRightPrivateSet,
                                                              true);

                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        ZimbraLog.index_search.debug("CLONED_LOCAL_AFTER_PERM="+clonedLocal.toString());
                    }

                    // clonedLocal should only have the single INTERSECT in it
                    assert(clonedLocal.mQueryOperations.size() == 1);

                    QueryOperation optimizedClonedLocal = clonedLocal.optimize(mbox);
                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        ZimbraLog.index_search.debug("CLONED_LOCAL_AFTER_OPTIMIZE="+optimizedClonedLocal.toString());
                    }

                    UnionQueryOperation withPrivateExcluded = localOps;
                    localOps = new UnionQueryOperation();
                    localOps.add(withPrivateExcluded);
                    localOps.add(optimizedClonedLocal);

                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        ZimbraLog.index_search.debug("LOCAL_WITH_CLONED="+localOps.toString());
                    }

                    //
                    // we should end up with:
                    //
                    // localOps =
                    //    UNION(withPrivateExcluded,
                    //          UNION(INTERSECT(clonedLocal,
                    //                          UNION(hasFolderRightPrivateList)
                    //                         )
                    //                )
                    //          )
                    //
                }

            }

            UnionQueryOperation union = new UnionQueryOperation();
            union.add(localOps);
            union.add(remoteOps);
            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug("BEFORE_FINAL_OPT="+union.toString());
            }
            mOp = union.optimize(mbox);
            assert(union.mQueryOperations.size() > 0);
        }
        if (ZimbraLog.index_search.isDebugEnabled()) {
            ZimbraLog.index_search.debug("END_ZIMBRAQUERY_CONSTRUCTOR="+mOp.toString());
        }
    }

    public void doneWithQuery() throws ServiceException {
        if (mResults != null)
            mResults.doneWithSearchResults();

        if (mOp != null)
            mOp.doneWithSearchResults();
    }

    /**
     * Runs the search and gets an open result set.
     *
     * WARNING: You **MUST** call ZimbraQueryResults.doneWithSearchResults() when you are done with them!
     *
     * @param octxt The operation context
     * @param proto The soap protocol the response should be returned with
     * @return Open ZimbraQueryResults -- YOU MUST CALL doneWithSearchResults() to release the results set!
     * @throws ServiceException
     * @throws IOException
     */
    final public ZimbraQueryResults execute() throws ServiceException, IOException {
        MailboxIndex mbidx = mMbox.getMailboxIndex();

        if (mOp!= null) {
            QueryTargetSet targets = mOp.getQueryTargets();
            assert(mOp instanceof UnionQueryOperation || targets.countExplicitTargets() <=1);
            assert(targets.size() >1 || !targets.hasExternalTargets() || mOp instanceof RemoteQueryOperation);

            if (ZimbraLog.index_search.isDebugEnabled())
                ZimbraLog.index_search.debug("OPERATION:"+mOp.toString());

            assert(mResults == null);

            mResults = mOp.run(mMbox, mbidx, mParams, mChunkSize);

            mResults = HitIdGrouper.Create(mResults, mParams.getSortBy());

            if ((!mParams.getIncludeTagDeleted() && mParams.getMode() != SearchResultMode.IDS)
                            || mParams.getAllowableTaskStatuses()!=null) {
                // we have to do some filtering of the result set
                FilteredQueryResults filtered = new FilteredQueryResults(mResults);

                if (!mParams.getIncludeTagDeleted())
                    filtered.setFilterTagDeleted(true);
                if (mParams.getAllowableTaskStatuses()!=null)
                    filtered.setAllowedTaskStatuses(mParams.getAllowableTaskStatuses());
                mResults = filtered;
            }

            return mResults;
        } else {
            ZimbraLog.index_search.debug("Operation optimized to nothing.  Returning no results");
            return new EmptyQueryResults(mParams.getTypes(), mParams.getSortBy(), mParams.getMode());
        }
    }

    /**
     * Callback -- adds a "-l.field:_calendaritemclass:private" term to all Lucene search parts: to exclude
     *             text data from searches in private appointments
     */
    private static final class excludePrivateCalendarItems implements QueryOperation.RecurseCallback {
        public void recurseCallback(QueryOperation op) {
            if (op instanceof TextQueryOperation) {
                ((TextQueryOperation)op).addAndedClause(new TermQuery(new Term(LuceneFields.L_FIELD, CalendarItem.INDEX_FIELD_ITEM_CLASS_PRIVATE)), false);
            }
        }
    }

    /**
     * For the local targets:
     *   - exclude all the not-visible folders from the query
     *   - look at all the text-operations and figure out if private appointments need to be excluded
     */
    private static UnionQueryOperation handleLocalPermissionChecks(UnionQueryOperation union,
                                                              Mailbox mbox,
                                                              OperationContext octxt,
                                                              MailboxIndex mbidx,
                                                              SearchParams params,
                                                              Set<Folder> visibleFolders,
                                                              boolean allowPrivateAccess)
    throws ServiceException {

        // Since optimize() has already been run, we know that each of our ops
        // only has one target (or none).  Find those operations which have
        // an external target and wrap them in RemoteQueryOperations
        for (int i = union.mQueryOperations.size()-1; i >= 0; i--) { // iterate backwards so we can remove/add w/o screwing iteration
            QueryOperation op = union.mQueryOperations.get(i);
            QueryTargetSet targets = op.getQueryTargets();

            // this assertion is OK because we have already distributed multi-target query ops
            // during the optimize() step
            assert(targets.countExplicitTargets() <= 1);
            // the assertion above is critical: the code below all assumes
            // that we only have ONE target (ie we've already distributed if necessary)

            assert(!targets.hasExternalTargets());

            if (!targets.hasExternalTargets()) {
                // local target
                if (!allowPrivateAccess)
                    op.depthFirstRecurse(new excludePrivateCalendarItems());

                if (visibleFolders != null) {
                    if (visibleFolders.size() == 0) {
                        union.mQueryOperations.remove(i);
                        ZimbraLog.index_search.debug("Query changed to NULL_QUERY_OPERATION, no visible folders");
                        union.mQueryOperations.add(i, new NoResultsQueryOperation());
                    } else {
                        union.mQueryOperations.remove(i);

                        // build a "and (in:visible1 or in:visible2 or in:visible3...)" query tree here!
                        IntersectionQueryOperation intersect = new IntersectionQueryOperation();
                        intersect.addQueryOp(op);

                        UnionQueryOperation newUnion = new UnionQueryOperation();
                        intersect.addQueryOp(newUnion);

                        for (Folder f : visibleFolders) {
                            DBQueryOperation newOp = DBQueryOperation.Create();
                            newUnion.add(newOp);
                            newOp.addInClause(f, true);
                        }

                        union.mQueryOperations.add(i, intersect);
                    }
                }
            }
        }

        return union;
    }


    @Override
    public String toString() {
        String ret = "ZQ:\n";

        if (mClauses.size() > 0) {
            BaseQuery head = mClauses.get(0);
            for (BaseQuery q = head; q != null; q = q.getNext()) {
                ret+=q.toString(1) + "\n";
            }
        }
        return ret;
    }

    public String toQueryString() {
        if (mOp == null) {
            return "";
        } else {
            return mOp.toQueryString();
        }
    }

}
