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
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.index.query.parser.QueryParserException;
import com.zimbra.cs.index.query.parser.QueryParser;
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
        private int mModifierType;
        private int mQueryType;

        protected BaseQuery(int mod, int type) {
            mModifierType = mod;
            mQueryType = type;
        }

        protected final void setQueryType(int queryType) {
            mQueryType = queryType;
        }

        protected final int getQueryType() {
            return mQueryType;
        }

        /**
         * Reconstructs the query string.
         *
         * @param src parsed token value
         * @return query string which can be used for proxying
         */
        String toQueryString(String src) {
            String img = ZimbraQuery.unquotedTokenImage[mQueryType];
            if (img.equals("#")) {
                int delim = src.indexOf(':');
                if (delim <= 0 || delim >= src.length() - 2) {
                    return img + src;
                }
                StringBuilder buf = new StringBuilder(img);
                buf.append(src.subSequence(0, delim + 1));
                buf.append('"');
                for (int i = delim + 1; i < src.length(); i++) {
                    char ch = src.charAt(i);
                    if (ch == '"') {
                        buf.append("\\\"");
                    } else {
                        buf.append(ch);
                    }
                }
                buf.append('"');
                return buf.toString();
            } else {
                return img + src;
            }
        }

        /**
         * Used by the QueryParser when building up the list of Query terms.
         *
         * @param mod
         */
        public final void setModifier(int mod) {
            mModifierType = mod;
        }

        @Override
        public String toString() {
            return dump(new StringBuilder()).toString();
        }

        public StringBuilder dump(StringBuilder out) {
            out.append(modToString());
            out.append("Q(");
            out.append(QueryTypeString(getQueryType()));
            return out;
        }

        /**
         * Called by the optimizer, this returns an initialized QueryOperation of the requested type.
         *
         * @param type
         * @param truth
         * @return
         */
        protected abstract QueryOperation getQueryOperation(boolean truth);

        boolean isNegated() {
            return mModifierType == QueryParser.MINUS;
        }

        protected final String modToString() {
            switch(mModifierType) {
                case QueryParser.PLUS:
                    return "+";
                case QueryParser.MINUS:
                    return "-";
                default:
                    return "";
            }
        }

        protected final boolean calcTruth(boolean truth) {
            if (isNegated()) {
                return !truth;
            } else {
                return truth;
            }
        }
    }

    public static class AttachmentQuery extends LuceneTableQuery {
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

        public AttachmentQuery(Mailbox mbox, int mod, String what) {
            super(mbox, mod, QueryParser.TYPE, LuceneFields.L_ATTACHMENTS, lookup(mMap, what));
        }

        protected AttachmentQuery(Mailbox mbox, int mod, String luceneField, String what) {
            super(mbox, mod, QueryParser.TYPE, luceneField, lookup(mMap, what));
        }
    }

    public static class ConjQuery extends BaseQuery {
        private static final int AND = QueryParser.AND;
        private static final int OR = QueryParser.OR;

        int mOp;

        public ConjQuery(int qType) {
            super(0, qType);
        }

        final boolean isOr() {
            return getQueryType() == OR;
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            switch (getQueryType()) {
                case AND:
                    return out.append(" && ");
                case OR:
                    return out.append(" || ");
                default:
                    assert(false);
                    return out;
            }
        }

        @Override
        protected QueryOperation getQueryOperation(boolean truth) {
            assert(false);
            return null;
        }

    }

    public static class ConvQuery extends BaseQuery {
        private ItemId mConvId;
        private Mailbox mMailbox;

        private ConvQuery(Mailbox mbox, int mod, ItemId convId) throws ServiceException {
            super(mod, QueryParser.CONV);
            mMailbox = mbox;
            mConvId = convId;

            if (mConvId.getId() < 0) {
                // should never happen (make an ItemQuery instead
                throw ServiceException.FAILURE("Illegal Negative ConvID: " +
                        convId.toString() + ", use ItemQuery for virtual convs",
                        null);
            }
        }

        public static BaseQuery create(Mailbox mbox, int mod, String target)
            throws ServiceException {

            ItemId convId = new ItemId(target, mbox.getAccountId());
            if (convId.getId() < 0) {
                // ...convert negative convId to positive ItemId...
                convId = new ItemId(convId.getAccountId(), -1 * convId.getId());
                List<ItemId> iidList = new ArrayList<ItemId>(1);
                iidList.add(convId);
                return new ItemQuery(mbox, mod, false, false, iidList);
            } else {
                return new ConvQuery(mbox, mod, convId);
            }
        }

        @Override
        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation op = new DBQueryOperation();
            op.addConvId(mMailbox, mConvId, calcTruth(truth));
            return op;
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            out.append(',');
            out.append(mConvId);
            return out.append(')');
        }
    }

    public static class DateQuery extends BaseQuery {
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
        protected QueryOperation getQueryOperation(boolean truth) {
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

    public static class DomainQuery extends BaseQuery {
        private String mTarget;
        private Mailbox mMailbox;

        public DomainQuery(Mailbox mbox, int mod, int qType, String target) {
            super(mod, qType);
            mTarget = target;
            mMailbox = mbox;
        }

        @Override
        protected QueryOperation getQueryOperation(boolean truth) {
            TextQueryOperation op = mMailbox.getMailboxIndex().createTextQueryOperation();
            Query q = new TermQuery(new Term(QueryTypeString(getQueryType()), mTarget));
            op.addClause(toQueryString(mTarget), q, calcTruth(truth));
            return op;
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            out.append("-DOMAIN,");
            out.append(mTarget);
            return out.append(')');
        }
    }

    public static class DraftQuery extends TagQuery {
        public DraftQuery(Mailbox mailbox, int mod, boolean truth)
            throws ServiceException {

            super(mailbox, mod, "\\Draft", truth);
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            return out.append(mTruth ? ",DRAFT)" : ",UNDRAFT)");
        }
    }

    public static class FlaggedQuery extends TagQuery {
        public FlaggedQuery(Mailbox mailbox, int mod, boolean truth)
            throws ServiceException {

            super(mailbox, mod, "\\Flagged", truth);
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            return out.append(mTruth ? ",FLAGGED)" : ",UNFLAGGED)");
        }
    }

    public static class ForwardedQuery extends TagQuery {
        public ForwardedQuery(Mailbox mailbox, int mod, boolean truth)
            throws ServiceException {

            super(mailbox, mod, "\\Forwarded", truth);
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            return out.append(mTruth ? ",FORWARDED)" : ",UNFORWARDED)");
        }
    }

    public static class HasQuery extends LuceneTableQuery {
        protected static final Map<String, String> mMap = new HashMap<String, String>();

        static {
            addMapping(mMap, new String[] { "attachment", "att" }, "any");
            addMapping(mMap, new String[] { "phone" }, "phone");
            addMapping(mMap, new String[] { "u.po" }, "u.po");
            addMapping(mMap, new String[] { "ssn" }, "ssn");
            addMapping(mMap, new String[] { "url" }, "url");
        }

        public HasQuery(Mailbox mbox, int mod, String what) {
            super(mbox, mod, QueryParser.HAS, LuceneFields.L_OBJECTS,
                    lookup(mMap, what));
        }
    }

    public static class InQuery extends BaseQuery {
        public static final Integer IN_ANY_FOLDER = new Integer(-2);
        public static final Integer IN_LOCAL_FOLDER = new Integer(-3);
        public static final Integer IN_REMOTE_FOLDER = new Integer(-4);
        public static final Integer IN_NO_FOLDER = new Integer(-5);

        public static BaseQuery Create(Mailbox mailbox, int mod, Integer folderId,
                boolean includeSubfolders) throws ServiceException {
            if (folderId < 0) {
                return new InQuery(null, null, null, folderId,
                        includeSubfolders, mod);
            } else if (includeSubfolders &&
                    folderId == Mailbox.ID_FOLDER_USER_ROOT) {
                return new InQuery(null, null, null, IN_ANY_FOLDER,
                        includeSubfolders, mod);
            } else {
                Folder folder = mailbox.getFolderById(null, folderId.intValue());
                return new InQuery(folder, null, null, null,
                        includeSubfolders, mod);
            }
        }

        public static BaseQuery Create(Mailbox mailbox, int mod, String folderName,
                boolean includeSubfolders) throws ServiceException {
            Pair<Folder, String> result = mailbox.getFolderByPathLongestMatch(
                    null, Mailbox.ID_FOLDER_USER_ROOT, folderName);
            return recursiveResolve(mailbox, mod, result.getFirst(),
                    result.getSecond(), includeSubfolders);
        }

        public static BaseQuery Create(Mailbox mailbox, int mod, ItemId iid,
                String subfolderPath, boolean includeSubfolders) throws ServiceException {

            if (iid.belongsTo(mailbox)) { // local
                if (includeSubfolders &&
                        iid.getId() == Mailbox.ID_FOLDER_USER_ROOT) {
                    return new InQuery(null, iid, subfolderPath, IN_ANY_FOLDER,
                            includeSubfolders, mod);
                }
                // find the base folder
                Pair<Folder, String> result;
                if (subfolderPath != null && subfolderPath.length() > 0) {
                    result = mailbox.getFolderByPathLongestMatch(null,
                            iid.getId(), subfolderPath);
                } else {
                    Folder f = mailbox.getFolderById(null, iid.getId());
                    result = new Pair<Folder, String>(f, null);
                }
                return recursiveResolve(mailbox, mod, result.getFirst(),
                        result.getSecond(), includeSubfolders);
            } else { // remote
                return new InQuery(null, iid, subfolderPath, null,
                        includeSubfolders, mod);
            }
        }

        /**
         * Resolve through local mountpoints until we get to the actual folder,
         * or until we get to a remote folder.
         */
        private static BaseQuery recursiveResolve(Mailbox mailbox, int mod,
                Folder baseFolder, String subfolderPath,
                boolean includeSubfolders) throws ServiceException {

            if (!(baseFolder instanceof Mountpoint)) {
                if (subfolderPath != null) {
                    throw MailServiceException.NO_SUCH_FOLDER(
                            baseFolder.getPath() + "/" + subfolderPath);
                }
                return new InQuery(baseFolder, null, null, null,
                        includeSubfolders, mod);
            } else {
                Mountpoint mpt = (Mountpoint) baseFolder;
                if (mpt.isLocal()) { // local
                    if (subfolderPath == null || subfolderPath.length() == 0) {
                        return new InQuery(baseFolder, null, null, null,
                                includeSubfolders, mod);
                    } else {
                        Folder newBase = mailbox.getFolderById(null,
                                mpt.getRemoteId());
                        return recursiveResolve(mailbox, mod,
                                newBase, subfolderPath, includeSubfolders);
                    }
                } else { // remote
                    return new InQuery(null, mpt.getTarget(), subfolderPath,
                            null, includeSubfolders, mod);
                }
            }
        }

        private InQuery(Folder folder, ItemId remoteId,
                String subfolderPath, Integer specialTarget,
                boolean includeSubfolders, int mod) {
            super(mod, QueryParser.IN);
            mFolder = folder;
            mRemoteId = remoteId;
            mSubfolderPath = subfolderPath;
            mSpecialTarget = specialTarget;
            mIncludeSubfolders = includeSubfolders;
        }

        @Override
        protected QueryOperation getQueryOperation(boolean truth) {
            if (mSpecialTarget != null) {
                if (mSpecialTarget == IN_NO_FOLDER) {
                    return new NoResultsQueryOperation();
                } else if (mSpecialTarget == IN_ANY_FOLDER) {
                    DBQueryOperation dbOp = new DBQueryOperation();
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

            DBQueryOperation dbOp = new DBQueryOperation();
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
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            if (mSpecialTarget != null) {
                if (!mIncludeSubfolders) {
                    out.append(",IN:");
                } else {
                    out.append(",UNDER:");
                }

                if (mSpecialTarget == IN_ANY_FOLDER) {
                    out.append("ANY_FOLDER");
                } else if (mSpecialTarget == IN_LOCAL_FOLDER) {
                    out.append("LOCAL");
                } else if (mSpecialTarget == IN_REMOTE_FOLDER) {
                    out.append("REMOTE");
                }
            } else {
                out.append(',');
                out.append(mIncludeSubfolders ? "UNDER" : "IN");
                out.append(':');
                out.append(mRemoteId != null ? mRemoteId.toString() :
                        (mFolder != null ? mFolder.getName() : "ANY_FOLDER"));
                if (mSubfolderPath != null) {
                    out.append('/');
                    out.append(mSubfolderPath);
                }
            }
            return out.append(')');
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

        public LuceneTableQuery(Mailbox mbox, int mod, int target, String luceneField, String value) {
            super(mod, target);
            mMailbox = mbox;
            mLuceneField = luceneField;
            mValue = value;
        }

        @Override
        protected QueryOperation getQueryOperation(boolean truth) {
            TextQueryOperation op = mMailbox.getMailboxIndex().createTextQueryOperation();

            Query q = null;
            if (mValue != null) {
                q = new TermQuery(new Term(mLuceneField, mValue));
            }
            op.addClause(toQueryString(mValue), q,calcTruth(truth));

            return op;
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            out.append(',');
            out.append(mLuceneField);
            out.append(':');
            out.append(mValue);
            return out.append(')');
        }
    }

    public static class ReadQuery extends TagQuery {
        public ReadQuery(Mailbox mailbox, int mod, boolean truth)
            throws ServiceException {

            super(mailbox, mod, "\\Unread", !truth);
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            return out.append(mTruth ? ",UNREAD)" :",READ)");
        }
    }

    public static class RepliedQuery extends TagQuery {
        public RepliedQuery(Mailbox mailbox, int mod, boolean truth)
            throws ServiceException {

            super(mailbox, mod, "\\Answered", truth);
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            return out.append(mTruth ? ",REPLIED)" : ",UNREPLIED)");
        }
    }

    public static class IsInviteQuery extends TagQuery {
        public IsInviteQuery(Mailbox mailbox, int mod, boolean truth)
            throws ServiceException {

            super(mailbox, mod, "\\Invite", truth);
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            return out.append(mTruth ? ",INVITE)" : ",NOT_INVITE)");
        }
    }

    public static class SentQuery extends TagQuery {
        public SentQuery(Mailbox mailbox, int mod, boolean truth)
            throws ServiceException {

            super(mailbox, mod, "\\Sent", truth);
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            return out.append(mTruth ? ",SENT)" : ",RECEIVED)");
        }
    }

    public static class SizeQuery extends BaseQuery {
        private String mSizeStr;
        private long mSize;

        public SizeQuery(int mod, int target, String size) throws QueryParserException {
            super(mod, target);

            boolean hasEQ = false;

            mSizeStr = size;

            char ch = mSizeStr.charAt(0);
            if (ch == '>') {
                setQueryType(QueryParser.BIGGER);
                mSizeStr = mSizeStr.substring(1);
            } else if (ch == '<') {
                setQueryType(QueryParser.SMALLER);
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
                mSizeStr = mSizeStr.substring(0, mSizeStr.length() - 1);
                typeChar = Character.toLowerCase(mSizeStr.charAt(mSizeStr.length() - 1));
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

            try {
                mSize = Integer.parseInt(mSizeStr.trim()) * multiplier;
            } catch (NumberFormatException e) {
                throw new QueryParserException("PARSER_ERROR");
            }

            if (hasEQ) {
                if (getQueryType() == QueryParser.BIGGER) {
                    mSize--; // correct since range constraint is strict >
                } else if (getQueryType() == QueryParser.SMALLER) {
                    mSize++; // correct since range constraint is strict <
                }
            }

            mSizeStr = ZimbraAnalyzer.SizeTokenFilter.encodeSize(mSize);
            if (mSizeStr == null) {
                mSizeStr = "";
            }
        }

        @Override
        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation op = new DBQueryOperation();

            truth = calcTruth(truth);

            long highest = -1, lowest = -1;

            switch (getQueryType()) {
                case QueryParser.BIGGER:
                    highest = -1;
                    lowest = mSize;
                    break;
                case QueryParser.SMALLER:
                    highest = mSize;
                    lowest = -1;
                    break;
                case QueryParser.SIZE:
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
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            out.append(',');
            out.append(mSize);
            return out.append(')');
        }
    }

    public static class ModseqQuery extends BaseQuery {
        static enum Operator {
            EQ, GT, GTEQ, LT, LTEQ;
        }

        private int mValue;
        private Operator mOp;

        public ModseqQuery(int mod, int target, String changeId) {
            super(mod, target);

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

        @Override
        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation op = new DBQueryOperation();
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

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            out.append(',');
            out.append(mOp);
            out.append(' ');
            out.append(mValue);
            return out.append(')');
        }
    }


    public static class SubQuery extends BaseQuery {
        private List<BaseQuery> mSubClauses;

        public SubQuery(int mod, List<BaseQuery> exp) {
            super(mod, SUBQUERY_TOKEN);
            mSubClauses = exp;
        }

        List<BaseQuery> getSubClauses() {
            return mSubClauses;
        }

        @Override
        protected QueryOperation getQueryOperation(boolean truth) {
            assert(false);
            return null;
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            out.append(modToString());
            out.append('(');
            for (BaseQuery sub : mSubClauses) {
                sub.dump(out);
            }
            return out.append(')');
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
        protected AddrQuery(int mod, List<BaseQuery> exp) {
            super(mod, exp);
        }
        public static ZimbraQuery.BaseQuery createFromTarget(Mailbox mbox,
                Analyzer analyzer, int mod, int target, String text)
                throws ServiceException {
            int bitmask = 0;
            switch (target) {
                case QueryParser.TOFROM:
                    bitmask = ADDR_BITMASK_TO | ADDR_BITMASK_FROM;
                    break;
                case QueryParser.TOCC:
                    bitmask = ADDR_BITMASK_TO | ADDR_BITMASK_CC;
                    break;
                case QueryParser.FROMCC:
                    bitmask = ADDR_BITMASK_FROM | ADDR_BITMASK_CC;
                    break;
                case QueryParser.TOFROMCC:
                    bitmask = ADDR_BITMASK_TO | ADDR_BITMASK_FROM | ADDR_BITMASK_CC;
                    break;
            }
            return createFromBitmask(mbox, analyzer, mod, text, bitmask);
        }

        public static ZimbraQuery.BaseQuery createFromBitmask(Mailbox mbox,
                Analyzer analyzer, int mod, String text,
                int operatorBitmask) throws ServiceException {
            ArrayList<ZimbraQuery.BaseQuery> clauses = new ArrayList<ZimbraQuery.BaseQuery>();
            boolean atFirst = true;

            if ((operatorBitmask & ADDR_BITMASK_FROM) !=0) {
                clauses.add(new TextQuery(mbox, analyzer, mod, QueryParser.FROM, text));
                atFirst = false;
            }
            if ((operatorBitmask & ADDR_BITMASK_TO) != 0) {
                if (atFirst)
                    atFirst = false;
                else
                    clauses.add(new ConjQuery(ConjQuery.OR));
                clauses.add(new TextQuery(mbox, analyzer, mod, QueryParser.TO, text));
            }
            if ((operatorBitmask & ADDR_BITMASK_CC) != 0) {
                if (atFirst)
                    atFirst = false;
                else
                    clauses.add(new ConjQuery(ConjQuery.OR));
                clauses.add(new TextQuery(mbox, analyzer, mod, QueryParser.CC, text));
            }
            return new AddrQuery(mod, clauses);
        }
    }

    /** Messages "to me" "from me" or "cc me" or any combination thereof */
    public static class MeQuery extends SubQuery {
        protected MeQuery(int mod, List<BaseQuery> exp) {
            super(mod, exp);
        }

        public static ZimbraQuery.BaseQuery create(Mailbox mbox,
                Analyzer analyzer, int mod, int operatorBitmask)
                throws ServiceException {
            ArrayList<BaseQuery> clauses = new ArrayList<BaseQuery>();
            Account acct = mbox.getAccount();
            boolean atFirst = true;
            if ((operatorBitmask & ADDR_BITMASK_FROM) != 0) {
                clauses.add(new SentQuery(mbox, mod, true));
                atFirst = false;
            }
            if ((operatorBitmask & ADDR_BITMASK_TO) != 0) {
                if (atFirst) {
                    atFirst = false;
                } else {
                    clauses.add(new ConjQuery(ConjQuery.OR));
                }
                clauses.add(new TextQuery(mbox, analyzer, mod,
                        QueryParser.TO, acct.getName()));
            }
            if ((operatorBitmask & ADDR_BITMASK_CC) != 0) {
                if (atFirst) {
                    atFirst = false;
                } else {
                    clauses.add(new ConjQuery(ConjQuery.OR));
                }
                clauses.add(new TextQuery(mbox, analyzer, mod,
                        QueryParser.CC, acct.getName()));
            }

            String[] aliases = acct.getMailAlias();
            for (String alias : aliases) {
                if ((operatorBitmask & ADDR_BITMASK_TO) != 0) {
                    if (atFirst) {
                        atFirst = false;
                    } else {
                        clauses.add(new ConjQuery(ConjQuery.OR));
                    }
                    clauses.add(new TextQuery(mbox, analyzer, mod,
                            QueryParser.TO, alias));
                }
                if ((operatorBitmask & ADDR_BITMASK_CC) != 0) {
                    if (atFirst) {
                        atFirst = false;
                    } else {
                        clauses.add(new ConjQuery(ConjQuery.OR));
                    }
                    clauses.add(new TextQuery(mbox, analyzer, mod,
                            QueryParser.CC, alias));
                }
            }
            return new MeQuery(mod, clauses);
        }
    }

    public static class TagQuery extends BaseQuery {
        private final Tag mTag;

        public TagQuery(Mailbox mailbox, int mod, String name, boolean truth)
            throws ServiceException {
            super(mod, QueryParser.TAG);
            mTag = mailbox.getTagByName(name);
            mTruth = truth;
        }

        @Override
        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation dbOp = new DBQueryOperation();
            dbOp.addTagClause(mTag, calcTruth(truth));
            return dbOp;
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            out.append(',');
            return out.append(mTag.getName());
        }
    }

    public static class ItemQuery extends BaseQuery {
        public static BaseQuery Create(Mailbox mbox, int mod, String str)
            throws ServiceException {

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

            return new ItemQuery(mbox, mod, allQuery, noneQuery, itemIds);
        }

        private boolean mIsAllQuery;
        private boolean mIsNoneQuery;
        private List<ItemId> mItemIds;
        private Mailbox mMailbox;

        ItemQuery(Mailbox mbox, int mod, boolean all, boolean none, List<ItemId> ids) {
            super(mod, QueryParser.ITEM);
            mIsAllQuery = all;
            mIsNoneQuery = none;
            mItemIds = ids;
            mMailbox = mbox;
        }

        @Override
        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation dbOp = new DBQueryOperation();

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
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            if (mIsAllQuery) {
                out.append(",all");
            } else if (mIsNoneQuery) {
                out.append(",none");
            } else {
                for (ItemId id : mItemIds) {
                    out.append(',');
                    out.append(id.toString());
                }
            }
            return out.append(')');
        }
    }

    public static class FieldQuery {

        /**
         * @param targetImg {@code field:}, {@code #something:} or
         *  {@code field[something]:}
         */
        public static TextQuery Create(Mailbox mbox, Analyzer analyzer,
                int mod, int qType, String targetImg, String text)
                throws ServiceException {

            int open = targetImg.indexOf('[');
            if (open >= 0) {
                int close = targetImg.indexOf(']');
                if (close >= 0 && close > open) {
                    text = targetImg.substring(open + 1, close) + ":" + text;
                }
            } else if (targetImg.charAt(0) == '#') {
                text = targetImg.substring(1) + text;
            }

            return new TextQuery(mbox, analyzer, mod, qType, text);
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
        public TextQuery(Mailbox mbox, Analyzer analyzer, int mod,
                int qType, String text) throws ServiceException {
            super(mod, qType);

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

            TokenStream stream = analyzer.tokenStream(
                    QueryTypeString(qType), new StringReader(text));
            try {
                TermAttribute termAttr = stream.addAttribute(
                        TermAttribute.class);
                while (stream.incrementToken()) {
                    mTokens.add(termAttr.term());
                }
                stream.end();
                stream.close();
            } catch (IOException ignore) {
            }

            // okay, quite a bit of hackery here....basically, if they're doing a contact:
            // search AND they haven't manually specified a phrase query (expands to more than one term)
            // then lets hack their search and make it a * search.
            // for bug:17232 -- if the search string is ".", don't auto-wildcard it, because . is
            // supposed to match everything by default.
            if (qType == QueryParser.CONTACT && mTokens.size() <= 1 && text.length() > 0
                        && text.charAt(text.length() - 1) != '*' && !text.equals(".")) {
                text = text + '*';
            }

            mOrigText = text;

            // must look at original text here b/c analyzer strips *'s
            if (text.length() > 0 && text.charAt(text.length() - 1) == '*') {
                // wildcard query!
                String wcToken;

                // only the last token is allowed to have a wildcard in it
                if (mTokens.size() > 0) {
                    wcToken = mTokens.remove(mTokens.size() - 1);
                } else {
                    wcToken = text;
                }

                if (wcToken.charAt(wcToken.length() - 1) == '*') {
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
        }

        @Override
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

                TextQueryOperation textOp = mMailbox.getMailboxIndex().createTextQueryOperation();

                for (QueryInfo inf : mQueryInfo) {
                    textOp.addQueryInfo(inf);
                }

                String fieldName = QueryTypeString(getQueryType());

                if (mTokens.size() == 0) {
                    textOp.setQueryString(toQueryString(mOrigText));
                } else if (mTokens.size() == 1) {
                    TermQuery term = new TermQuery(new Term(fieldName, mTokens.get(0)));
                    textOp.addClause(toQueryString(mOrigText), term,
                            calcTruth(truth));
                } else if (mTokens.size() > 1) {
                    PhraseQuery phrase = new PhraseQuery();
                    phrase.setSlop(mSlop); // TODO configurable?
                    for (String token : mTokens) {
                        phrase.add(new Term(fieldName, token));
                    }
                    textOp.addClause(toQueryString(mOrigText), phrase,
                            calcTruth(truth));
                }

                if (mOredTokens.size() > 0) {
                    // probably don't need to do this here...can probably just call addClause
                    BooleanQuery orQuery = new BooleanQuery();
                    for (String token : mOredTokens) {
                        orQuery.add(new TermQuery(new Term(fieldName, token)), Occur.SHOULD);
                    }

                    textOp.addClause("", orQuery, calcTruth(truth));
                }

                return textOp;
            }
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            for (String token : mTokens) {
                out.append(',');
                out.append(token);
            }
            if (mWildcardTerm != null) {
                out.append(" WILDCARD=");
                out.append(mWildcardTerm);
                out.append(" [");
                out.append(mOredTokens.size());
                out.append(" terms]");
            }
            return out.append(')');
        }
    }

    public static class TypeQuery extends AttachmentQuery {
        public TypeQuery(Mailbox mbox,int mod, String what) {
            super(mbox, mod, LuceneFields.L_MIMETYPE, what);
        }
    }

    public static class SenderQuery extends BaseQuery {
        private String mStr;
        private boolean mLt;
        private boolean mEq;

        @Override
        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation op = new DBQueryOperation();
            truth = calcTruth(truth);

            if (mLt) {
                    op.addRelativeSender(null, false, mStr, mEq, truth);
            } else {
                op.addRelativeSender(mStr, mEq, null, false, truth);
            }

            return op;
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            return out.append("Sender(");
        }

        /**
         * Don't call directly -- use SubjectQuery.create()
         *
         * This is only invoked for subject queries that start with < or > -- otherwise we just
         * use the normal TextQuery class
         */
        private SenderQuery(int mod, int qType, String text) {
            super(mod, qType);

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
                int mod, int qType, String text) throws ServiceException {
            if (text.length() > 1 && (text.startsWith("<") || text.startsWith(">"))) {
                return new SenderQuery(mod, qType, text);
            } else {
                return new TextQuery(mbox, analyzer, mod, qType, text);
            }
        }
    }

    public static class ConvCountQuery extends BaseQuery {
        private int mLowestCount;  private boolean mLowerEq;
        private int mHighestCount; private boolean mHigherEq;

        private ConvCountQuery(int mod, int qType, int lowestCount,
                boolean lowerEq, int highestCount, boolean higherEq) {
            super(mod, qType);

            mLowestCount = lowestCount;
            mLowerEq = lowerEq;
            mHighestCount = highestCount;
            mHigherEq = higherEq;
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            out.append("ConvCount(");
            out.append(mLowerEq ? ">=" : ">");
            out.append(mLowestCount);
            out.append(' ');
            out.append(mHigherEq? "<=" : "<");
            out.append(mHighestCount);
            return out.append(')');
        }

        @Override
        protected QueryOperation getQueryOperation(boolean truthiness) {
            DBQueryOperation op = new DBQueryOperation();
            truthiness = calcTruth(truthiness);
            op.addConvCountClause(mLowestCount, mLowerEq, mHighestCount, mHigherEq, truthiness);
            return op;
        }

        public static BaseQuery create(int mod, int qType, String str) {
            if (str.charAt(0) == '<') {
                boolean eq = false;
                if (str.charAt(1) == '=') {
                    eq = true;
                    str = str.substring(2);
                } else {
                    str = str.substring(1);
                }
                int num = Integer.parseInt(str);
                return new ConvCountQuery(mod, qType, -1, false, num, eq);
            } else if (str.charAt(0) == '>') {
                boolean eq = false;
                if (str.charAt(1) == '=') {
                    eq = true;
                    str = str.substring(2);
                } else {
                    str = str.substring(1);
                }
                int num = Integer.parseInt(str);
                return new ConvCountQuery(mod, qType, num, eq, -1, false);
            } else {
                int num = Integer.parseInt(str);
                return new ConvCountQuery(mod, qType, num, true, num, true);
            }
        }
    }

    public static class SubjectQuery extends BaseQuery {
        private String mStr;
        private boolean mLt;
        private boolean mEq;

        @Override
        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation op = new DBQueryOperation();
            truth = calcTruth(truth);

            if (mLt) {
                op.addRelativeSubject(null, false, mStr, mEq, truth);
            } else {
                op.addRelativeSubject(mStr, mEq, null, false, truth);
            }

            return op;
        }

        @Override
        public StringBuilder dump(StringBuilder out) {
            super.dump(out);
            return out.append("Subject(");
        }

        /**
         * Don't call directly -- use SubjectQuery.create()
         *
         * This is only invoked for subject queries that start with {@code <} or
         * {@code >}, otherwise we just use the normal TextQuery class.
         */
        private SubjectQuery(int mod, int qType, String text) {
            super(mod, qType);

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
                int mod, int qType, String text) throws ServiceException {
            if (text.length() > 1 && (text.startsWith("<") || text.startsWith(">"))) {
                // real subject query!
                return new SubjectQuery(mod, qType, text);
            } else {
                return new TextQuery(mbox, analyzer, mod, qType, text);
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

        unquotedTokenImage = new String[QueryParser.tokenImage.length];
        for (int i = 0; i < QueryParser.tokenImage.length; i++) {
            String str = QueryParser.tokenImage[i].substring(1, QueryParser.tokenImage[i].length() - 1);
            if ("FIELD".equals(str)) {
                unquotedTokenImage[i] = "#"; // bug 22969 -- problem with proxying field queries
            } else {
                unquotedTokenImage[i] = str;
            }
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
            case QueryParser.CONTACT: return LuceneFields.L_CONTACT_DATA;
            case QueryParser.CONTENT: return LuceneFields.L_CONTENT;
            case QueryParser.MSGID: return LuceneFields.L_H_MESSAGE_ID;
            case QueryParser.ENVFROM: return LuceneFields.L_H_X_ENV_FROM;
            case QueryParser.ENVTO: return LuceneFields.L_H_X_ENV_TO;
            case QueryParser.FROM: return LuceneFields.L_H_FROM;
            case QueryParser.TO: return LuceneFields.L_H_TO;
            case QueryParser.CC: return LuceneFields.L_H_CC;
            case QueryParser.SUBJECT: return LuceneFields.L_H_SUBJECT;
            case QueryParser.IN: return "IN";
            case QueryParser.HAS: return "HAS";
            case QueryParser.FILENAME: return LuceneFields.L_FILENAME;
            case QueryParser.TYPE: return LuceneFields.L_MIMETYPE;
            case QueryParser.ATTACHMENT: return LuceneFields.L_ATTACHMENTS;
            case QueryParser.IS: return "IS";
            case QueryParser.DATE: return "DATE";
            case QueryParser.AFTER: return "AFTER";
            case QueryParser.BEFORE: return "BEFORE";
            case QueryParser.APPT_START: return "APPT-START";
            case QueryParser.APPT_END: return "APPT-END";
            case QueryParser.SIZE: return "SIZE";
            case QueryParser.BIGGER: return "BIGGER";
            case QueryParser.SMALLER: return "SMALLER";
            case QueryParser.TAG: return "TAG";
            case QueryParser.MY: return "MY";
            case QueryParser.MESSAGE: return "MESSAGE";
            case QueryParser.CONV: return "CONV";
            case QueryParser.CONV_COUNT: return "CONV-COUNT";
            case QueryParser.CONV_MINM: return "CONV_MINM";
            case QueryParser.CONV_MAXM: return "CONV_MAXM";
            case QueryParser.CONV_START: return "CONV-START";
            case QueryParser.CONV_END: return "CONV-END";
            case QueryParser.AUTHOR: return "AUTHOR";
            case QueryParser.TITLE: return "TITLE";
            case QueryParser.KEYWORDS: return "KEYWORDS";
            case QueryParser.COMPANY: return "COMPANY";
            case QueryParser.METADATA: return "METADATA";
            case QueryParser.ITEM: return "ITEMID";
            case QueryParser.FIELD: return LuceneFields.L_FIELD;
        }
        return "UNKNOWN:(" + qType + ")";
    }

    /**
     * ParseTree's job is to take the LIST of query terms (BaseQuery's) and build them
     * into a Tree structure of Things (return results) and Operators (AND and OR)
     *
     * Once a simple tree is built, then ParseTree "distributes the NOTs" down to the leaf
     * nodes: this is so we never have to do result-set inversions, which are prohibitively
     * expensive for nontrivial cases.
     */
    private static class ParseTree {
        private static final int STATE_AND = 1;
        private static final int STATE_OR = 2;

        private static final boolean SPEW = false;

        static abstract class Node {
            boolean mTruthFlag = true;

            Node() {
            }

            void setTruth(boolean truth) {
                mTruthFlag = truth;
            };

            void invertTruth() {
                mTruthFlag = !mTruthFlag;
            }

            abstract void pushNotsDown();
            abstract Node simplify();
            abstract QueryOperation getQueryOperation();
        }

        static class OperatorNode extends Node {
            private int mKind;
            private boolean mTruthFlag = true;
            private List<Node> mNodes = new ArrayList<Node>();

            OperatorNode(int kind) {
                mKind = kind;
            }

            @Override
            void setTruth(boolean truth) {
                mTruthFlag = truth;
            };

            @Override
            void invertTruth() {
                mTruthFlag = !mTruthFlag;
            }

            @Override
            void pushNotsDown() {
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

            @Override
            Node simplify() {
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

            void add(Node subNode) {
                mNodes.add(subNode);
            }

            @Override
            public String toString() {
                StringBuilder buff = mTruthFlag ?
                        new StringBuilder() : new StringBuilder(" NOT ");

                buff.append(mKind == STATE_AND ? " AND[" : " OR(");

                for (Node node : mNodes) {
                    buff.append(node.toString());
                    buff.append(", ");
                }
                buff.append(mKind == STATE_AND ? "] " : ") ");
                return buff.toString();
            }

            @Override
            QueryOperation getQueryOperation() {
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
            private BaseQuery mThing;

            ThingNode(BaseQuery thing) {
                mThing = thing;
                mTruthFlag = thing.mTruth;
            }

            @Override
            void invertTruth() {
                mTruthFlag = !mTruthFlag;
            }

            @Override
            void pushNotsDown() {
            }

            @Override
            Node simplify() {
                return this;
            }

            @Override
            public String toString() {
                StringBuilder buff = mTruthFlag ?
                        new StringBuilder() : new StringBuilder(" NOT ");
                buff.append(mThing.toString());
                return buff.toString();
            }

            @Override
            QueryOperation getQueryOperation() {
                return mThing.getQueryOperation(mTruthFlag);
            }
        }

        static Node build(List<BaseQuery> clauses) {
            OperatorNode top = new OperatorNode(STATE_OR);
            OperatorNode cur = new OperatorNode(STATE_AND);
            top.add(cur);

            for (BaseQuery q : clauses) {
                if (q instanceof ZimbraQuery.ConjQuery) {
                    if (((ConjQuery) q).isOr()) {
                        cur = new OperatorNode(STATE_AND);
                        top.add(cur);
                    }
                } else {
                    if (q instanceof SubQuery) {
                        SubQuery sq = (SubQuery) q;
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

        @Override
        public void recurseCallback(QueryOperation op) {
            if (op instanceof TextQueryOperation) {
                num++;
            }
        }
    }
    private static final class CountCombiningOperations implements QueryOperation.RecurseCallback {
        int num = 0;

        @Override
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

    /**
     * Take the specified query string and build an optimized query. Do not
     * execute the query, however.
     *
     * @param mbox
     * @param params
     * @throws ServiceException
     */
    public ZimbraQuery(OperationContext octxt, SoapProtocol proto,
            Mailbox mbox, SearchParams params) throws ServiceException {

        mParams = params;
        mMbox = mbox;
        long chunkSize = (long) mParams.getOffset() + (long) mParams.getLimit();
        if (chunkSize > 1000) {
            mChunkSize = 1000;
        } else {
            mChunkSize = (int)chunkSize;
        }

        Analyzer analyzer = null;
        MailboxIndex index = mbox.getMailboxIndex();

        // Step 1: parse the text using the JavaCC parser
        try {
            if (index != null) {
                index.initAnalyzer(mbox);
                analyzer = index.getAnalyzer();
            } else {
                analyzer = ZimbraAnalyzer.getDefaultAnalyzer();
            }
            QueryParser parser = new QueryParser(mbox, analyzer);
            parser.setDefaultField(params.getDefaultField());
            parser.setTimeZone(params.getTimeZone());
            parser.setLocale(params.getLocale());
            mClauses = parser.parse(params.getQueryStr());

            String sortBy = parser.getSortBy();
            if (sortBy != null) {
                handleSortByOverride(sortBy);
            }
        } catch (Error e) {
            throw ServiceException.FAILURE(
                    "ZimbraQueryParser threw Error: " + e, e);
        }

        if (ZimbraLog.index_search.isDebugEnabled()) {
            StringBuilder buf = new StringBuilder(toString());
            buf.append(" search([");
            buf.append(mParams.getTypesStr());
            buf.append("],");
            buf.append(mParams.getSortBy());
            buf.append(')');
            ZimbraLog.index_search.debug(buf.toString());
        }

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

        // Store some variables that we'll need later
        mParseTree = pt;
        mOp = null;

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

        // Step 3: Convert list of BaseQueries into list of QueryOperations, then Optimize the Ops
        if (mClauses.size() > 0) {
            // this generates all of the query operations
            mOp = mParseTree.getQueryOperation();

            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug("OP=%s", mOp);
            }

            // expand the is:local and is:remote parts into in:(LIST)'s
            mOp = mOp.expandLocalRemotePart(mbox);
            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug("AFTEREXP=%s", mOp);
            }

            // optimize the query down
            mOp = mOp.optimize(mMbox);
            if (mOp == null)
                mOp = new NoResultsQueryOperation();
            if (ZimbraLog.index_search.isDebugEnabled()) {
                ZimbraLog.index_search.debug("OPTIMIZED=%s", mOp);
            }
        }

        // STEP 4: use the OperationContext to update the set of visible referenced folders, local AND remote
        if (mOp != null) {
            QueryTargetSet queryTargets = mOp.getQueryTargets();
            assert(mOp instanceof UnionQueryOperation ||
                    queryTargets.countExplicitTargets() <= 1);

            // easiest to treat the query two unions: one the LOCAL and one REMOTE parts
            UnionQueryOperation remoteOps = new UnionQueryOperation();
            UnionQueryOperation localOps = new UnionQueryOperation();

            if (mOp instanceof UnionQueryOperation) {
                UnionQueryOperation union = (UnionQueryOperation) mOp;
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

            // Handle the REMOTE side:
            if (!remoteOps.mQueryOperations.isEmpty()) {
                // Since optimize() has already been run, we know that each of our ops
                // only has one target (or none).  Find those operations which have
                // an external target and wrap them in RemoteQueryOperations

                // iterate backwards so we can remove/add w/o screwing iteration
                for (int i = remoteOps.mQueryOperations.size()-1; i >= 0; i--) {
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
                        for (QueryOperation remoteOp : remoteOps.mQueryOperations) {
                            if (remoteOp instanceof RemoteQueryOperation) {
                                if (((RemoteQueryOperation) remoteOp).tryAddOredOperation(op)) {
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
                for (QueryOperation remoteOp : remoteOps.mQueryOperations) {
                    assert(remoteOp instanceof RemoteQueryOperation);
                    try {
                        ((RemoteQueryOperation) remoteOp).setup(proto, octxt.getAuthToken(), params);
                    } catch (Exception e) {
                        ZimbraLog.index_search.info("Ignoring " + e +
                                " during RemoteQuery generation for " + remoteOps);
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

                localOps = handleLocalPermissionChecks(localOps, visibleFolders,
                        allowPrivateAccess);

                if (ZimbraLog.index_search.isDebugEnabled()) {
                    ZimbraLog.index_search.debug("LOCAL_AFTER_PERM_CHECKS=%s", localOps);
                }

                if (!hasFolderRightPrivateSet.isEmpty()) {
                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        ZimbraLog.index_search.debug("CLONED_LOCAL_BEFORE_PERM=%s", clonedLocal);
                    }

                    //
                    // now we're going to setup the clonedLocal tree
                    // to run with private access ALLOWED, over the set of folders
                    // that have RIGHT_PRIVATE (note that we build this list from the visible
                    // folder list, so we are
                    //
                    clonedLocal = handleLocalPermissionChecks(
                            clonedLocal, hasFolderRightPrivateSet, true);

                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        ZimbraLog.index_search.debug("CLONED_LOCAL_AFTER_PERM=%s", clonedLocal);
                    }

                    // clonedLocal should only have the single INTERSECT in it
                    assert(clonedLocal.mQueryOperations.size() == 1);

                    QueryOperation optimizedClonedLocal = clonedLocal.optimize(mbox);
                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        ZimbraLog.index_search.debug("CLONED_LOCAL_AFTER_OPTIMIZE=%s", optimizedClonedLocal);
                    }

                    UnionQueryOperation withPrivateExcluded = localOps;
                    localOps = new UnionQueryOperation();
                    localOps.add(withPrivateExcluded);
                    localOps.add(optimizedClonedLocal);

                    if (ZimbraLog.index_search.isDebugEnabled()) {
                        ZimbraLog.index_search.debug("LOCAL_WITH_CLONED=%s", localOps);
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
                ZimbraLog.index_search.debug("BEFORE_FINAL_OPT=%s", union);
            }
            mOp = union.optimize(mbox);
            assert(union.mQueryOperations.size() > 0);
        }
        if (ZimbraLog.index_search.isDebugEnabled()) {
            ZimbraLog.index_search.debug("END_ZIMBRAQUERY_CONSTRUCTOR=%s", mOp);
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
     */
    final public ZimbraQueryResults execute() throws ServiceException {

        if (mOp != null) {
            QueryTargetSet targets = mOp.getQueryTargets();
            assert(mOp instanceof UnionQueryOperation || targets.countExplicitTargets() <=1);
            assert(targets.size() >1 || !targets.hasExternalTargets() || mOp instanceof RemoteQueryOperation);

            if (ZimbraLog.index_search.isDebugEnabled())
                ZimbraLog.index_search.debug("OPERATION:"+mOp.toString());

            assert(mResults == null);

            mResults = mOp.run(mMbox, mParams, mChunkSize);

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

        @Override
        public void recurseCallback(QueryOperation op) {
            if (op instanceof TextQueryOperation) {
                ((TextQueryOperation)op).addAndedClause(new TermQuery(new Term(
                        LuceneFields.L_FIELD, CalendarItem.INDEX_FIELD_ITEM_CLASS_PRIVATE)), false);
            }
        }
    }

    /**
     * For the local targets:
     *   - exclude all the not-visible folders from the query
     *   - look at all the text-operations and figure out if private appointments need to be excluded
     */
    private static UnionQueryOperation handleLocalPermissionChecks(
            UnionQueryOperation union, Set<Folder> visibleFolders,
            boolean allowPrivateAccess) {

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
                            DBQueryOperation newOp = new DBQueryOperation();
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

    public static String toString(List<BaseQuery> clauses) {
        StringBuilder out = new StringBuilder();
        for (BaseQuery clause : clauses) {
            clause.dump(out);
        }
        return out.toString();
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder("ZQ: ");
        for (BaseQuery clause : mClauses) {
            clause.dump(out);
        }
        return out.toString();
    }

    public String toQueryString() {
        if (mOp == null) {
            return "";
        } else {
            return mOp.toQueryString();
        }
    }

}
