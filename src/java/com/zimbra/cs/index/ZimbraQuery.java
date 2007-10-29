/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jul 6, 2004
 */
package com.zimbra.cs.index;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.*;
import java.util.*;

import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
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
import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.index.queryparser.Token;
import com.zimbra.cs.index.queryparser.ZimbraQueryParser;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.index.queryparser.ZimbraQueryParserConstants;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.SoapProtocol;

/**
 * @author tim
 *
 * Represents a search query.  Flow is simple:
 * 
 *    -- Constructor() 
 *       1) Parse the query string, turn it into a list of BaseQuery's
 *       2) Push "not's" down to the leaves, so that we never have to invert
 *           result sets
 *       3) Generate a list of QueryOperations from the BaseQuery list, then 
 *           optimize the QueryOperations in preparation to run the query.
 *               
 *    -- execute() -- Begin the search, get the ZimbraQueryResults iterator
 */
public final class ZimbraQuery {
    /**
     * BaseQuery
     *
     * Very simple wrapper classes that each represent a node in the parse tree for the
     * query string.
     */
    public static abstract class BaseQuery
    {
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

        public String toString() {
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
        protected static HashMap<String, String> mMap;

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

        public AttachmentQuery(Analyzer analyzer, int modifier, String what) {
            super(modifier, ZimbraQueryParser.TYPE, LuceneFields.L_ATTACHMENTS, lookup(mMap, what));
        }

        protected AttachmentQuery(int modifier, String luceneField, String what) {
            super(modifier, ZimbraQueryParser.TYPE, luceneField, lookup(mMap, what));
        }
    }

    public static class ConjQuery extends BaseQuery
    {
        private static final int AND = ZimbraQueryParser.AND_TOKEN;
        private static final int OR = ZimbraQueryParser.OR_TOKEN;

        int mOp;

        public ConjQuery(Analyzer analyzer, int qType) {
            super(0, qType);
        }

        final boolean isOr() { return getQueryType() == OR; }        
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

    public static class ConvQuery extends BaseQuery
    {
        private ItemId mConvId;
        private Mailbox mMailbox; 
        
        private ConvQuery(Mailbox mbox, Analyzer analyzer, int modifier, ItemId convId) throws ServiceException {
            super(modifier, ZimbraQueryParser.CONV);
            mMailbox = mbox;
            mConvId = convId; 
            
            if (mConvId.getId() < 0) {
                // should never happen (make an ItemQuery instead
                throw ServiceException.FAILURE("Illegal Negative ConvID: "+convId.toString()+", use ItemQuery for virtual convs", null);
            }
        }
        
        public static BaseQuery create(Mailbox mbox, Analyzer analyzer, int modifier, String target) throws ServiceException {
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

        public String toString(int expLevel) {
            return super.toString(expLevel)+","+mConvId+")";
        }
    }

    public static class DateQuery extends BaseQuery
    {
        private Date mDate = null;
        private Date mEndDate = null;
        private long mLowestTime;  private boolean mLowerEq;
        private long mHighestTime; private boolean mHigherEq;

        public DateQuery(Analyzer analyzer, int qType)
        {
            super(0,qType);
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation op = DBQueryOperation.Create();

            truth = calcTruth(truth);
            
            if (this.getQueryType() == ZimbraQueryParser.APPT_START)
                op.addCalStartDateClause(mLowestTime, mLowerEq, mHighestTime, mHigherEq, truth);
            else if (this.getQueryType() == ZimbraQueryParser.APPT_END)
                op.addCalEndDateClause(mLowestTime, mLowerEq, mHighestTime, mHigherEq, truth);
            else
                op.addDateClause(mLowestTime, mLowerEq, mHighestTime, mHigherEq, truth);
            
            return op;
        }

        protected static final String NUMERICDATE_PATTERN = "^([0-9]+)$";
        protected static final Pattern sNumericDatePattern = Pattern.compile(NUMERICDATE_PATTERN);

        protected static final String RELDATE_PATTERN = "([+-])([0-9]+)([mhdwy][a-z]*)?";
        protected static final Pattern sRelDatePattern = Pattern.compile(RELDATE_PATTERN);
        
        public void parseDate(int modifier, String s, TimeZone tz, Locale locale) throws com.zimbra.cs.index.queryparser.ParseException
        {
            //          * DATE:  absolute-date = mm/dd/yyyy | yyyy/dd/mm  OR
            //          *        relative-date = [+/-]nnnn{minute,hour,day,week,month,year}
            //          *        (need to figure out how to represent "this week", "last
            //          *        week", "this month", etc)

            mDate = null; // the beginning of the user-specified range (inclusive)
            mEndDate = null; // the end of the user-specified range (NOT-included in the range)
            mLowestTime = -1;
            mHighestTime = -1;
            boolean hasExplicitComparasins = false;
            boolean explicitLT = false;
            boolean explicitGT = false;
            boolean explicitEq = false;

            if (s.length() <= 0)
                throw new ParseException("Invalid string in date query: \"\"");
            char ch = s.charAt(0);
            if (ch == '<' || ch == '>') {
                if (getQueryType() == ZimbraQueryParser.BEFORE || getQueryType() == ZimbraQueryParser.AFTER) 
                    throw new ParseException(">, <, >= and <= may not be specified with BEFORE or AFTER searches");

                hasExplicitComparasins = true;

                if (s.length() <= 1)
                    throw new ParseException("Invalid string in date query: \""+s+"\"");

                char ch2 = s.charAt(1);
                if (ch2 == '=' && s.length() <= 2)
                    throw new ParseException("Invalid string in date query: \""+s+"\"");

                if (ch == '<')
                    explicitLT = true;
                else if (ch == '>')
                    explicitGT = true;

                if (ch2 == '=') {
                    s = s.substring(2); // chop off the <= or >=
                    explicitEq = true;
                } else {
                    s = s.substring(1); // chop off the < or >
                }
            }

            if (s.length() <= 0)
                throw new ParseException("Invalid string in date query: \""+s+"\"");


            int origType = getQueryType();

            if (s.equalsIgnoreCase("today"))
                s = "-0d";
            if (s.equalsIgnoreCase("yesterday"))
                s = "-1d";

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
                Matcher m;
                String mod = null;
                m = sNumericDatePattern.matcher(s);
                if (m.lookingAt()) {
                    long dateLong = Long.parseLong(s);
                    mDate = new Date(dateLong);
                    mEndDate = new Date(dateLong+1000); // +1000 since SQL time is sec, java in msec
                    
                } else {
                    m = sRelDatePattern.matcher(s);
                    if (m.lookingAt()) 
                    {
                        //
                        // RELATIVE DATE!
                        //
                        String reltime;
                        String what;

                        mod = s.substring(m.start(1), m.end(1));
                        reltime = s.substring(m.start(2), m.end(2));


                        if (m.start(3) == -1) {
                            // no period specified -- use the defualt for the current operator
                        } else {
                            what = s.substring(m.start(3), m.end(3));


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


                        //                System.out.println("RELDATE: MOD=\""+mod+"\" AMT=\""+reltime+"\" TYPE="+type);

                        GregorianCalendar cal = new GregorianCalendar();
                        if (tz != null)
                            cal.setTimeZone(tz);

                        cal.setTime(new Date());

                        //
                        // special case 'day' clear all the fields that are lower than the one we're currently operating on...
                        //
                        //  E.G. "date:-1d"  people really expect that to mean 'midnight to midnight yesterday'
                        switch (field) {
                            case Calendar.YEAR:
                                cal.set(Calendar.MONTH, 0);
                            case Calendar.MONTH:
                                cal.set(Calendar.WEEK_OF_MONTH, 0);
                            case Calendar.WEEK_OF_YEAR:
                                cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                            case Calendar.DATE:
                                cal.set(Calendar.HOUR_OF_DAY, 0);
                            case Calendar.HOUR:
                            case Calendar.HOUR_OF_DAY:
                                cal.set(Calendar.MINUTE, 0);
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
                        //
                        // ABSOLUTE dates:
                        //       use Locale information to parse date correctly
                        //

                        char first = s.charAt(0);
                        if (first == '-' || first == '+') {
                            s = s.substring(1);
                        }

                        DateFormat df;
                        if (locale != null)
                            df = DateFormat.getDateInstance(DateFormat.SHORT, locale); 
                        else
                            df = DateFormat.getDateInstance(DateFormat.SHORT);

                        df.setLenient(false);
                        if (tz != null) {
                            df.setTimeZone(tz);
                        }

                        try {
                            mDate = df.parse(s);
                        } catch (java.text.ParseException ex) {
                            Token fake = new Token();
                            fake.image = s;
                            ParseException pe = new ParseException(ex.getLocalizedMessage());
                            pe.currentToken = fake;
                            throw pe;
                        }

                        Calendar cal = Calendar.getInstance();
                        if (tz != null)
                            cal.setTimeZone(tz);

                        cal.setTime(mDate);

                        cal.add(field,1);
                        mEndDate = cal.getTime();
                    } // else (relative/absolute check)
                } // else (numeric check)

                if (mLog.isDebugEnabled()) {
                    mLog.debug("Parsed date range to: ("+mDate.toString()+"-"+mEndDate.toString()+")");
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
  //                assert(explicitEq == true);
                // =  lowest=mDate,lowestEq=true,highest=mEndDate,highestEq=false 
                mLowestTime = mDate.getTime();
                mLowerEq = true;
                mHighestTime = mEndDate.getTime();
                mHigherEq = false;
            }

        }

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

            return super.toString(expLevel)+","+str+","+mDate.toString()+")";
        }
    }

    public static class DomainQuery extends BaseQuery
    {
        private String mTarget;
        public DomainQuery(Analyzer analyzer, int modifier, int qType, String target) {
            super(modifier, qType);
            mTarget = target;
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            LuceneQueryOperation op = LuceneQueryOperation.Create();
            Query q = new TermQuery(new Term(QueryTypeString(getQueryType()), mTarget));
            op.addClause(getQueryOperatorString()+mTarget, q,calcTruth(truth));
            return op;
        }

        public String toString(int expLevel) {
            return super.toString(expLevel)+"-DOMAIN,"+mTarget+")";
        }
    }

    public static class DraftQuery extends TagQuery
    {
        public DraftQuery(Mailbox mailbox, Analyzer analyzer, int modifier, boolean truth) throws ServiceException
        {
            super(analyzer, modifier, mailbox.mDraftFlag, truth);
        }

        public String toString(int expLevel) {
            if (mTruth) {
                return super.toString(expLevel) + ",DRAFT)";
            } else {
                return super.toString(expLevel) + ",UNDRAFT)";
            }
        }
    }

    public static class FlaggedQuery extends TagQuery
    {
        public FlaggedQuery(Mailbox mailbox, Analyzer analyzer, int modifier, boolean truth) throws ServiceException
        {
            super(analyzer, modifier, mailbox.mFlaggedFlag, truth);
        }

        public String toString(int expLevel) {
            if (mTruth) {
                return super.toString(expLevel) + ",FLAGGED)";
            } else {
                return super.toString(expLevel) + ",UNFLAGGED)";
            }
        }
    }

    public static class ForwardedQuery extends TagQuery
    {
        public ForwardedQuery(Mailbox mailbox, Analyzer analyzer, int modifier, boolean truth) throws ServiceException
        {
            super(analyzer, modifier, mailbox.mForwardFlag, truth);
        }

        public String toString(int expLevel) {
            if (mTruth) {
                return super.toString(expLevel) + ",FORWARDED)";
            } else {
                return super.toString(expLevel) + ",UNFORWARDED)";
            }
        }
    }    

    public static class HasQuery extends LuceneTableQuery
    {
        protected static HashMap<String, String> mMap;

        static {
            mMap = new HashMap<String, String>();

            addMapping(mMap, new String[] { "attachment", "att" }  , "any");
            addMapping(mMap, new String[] { "phone" }              , "phone");
            addMapping(mMap, new String[] { "u.po" }               , "u.po");
            addMapping(mMap, new String[] { "ssn" }                , "ssn");
            addMapping(mMap, new String[] { "url" }                , "url");
        }

        public HasQuery(Analyzer analyzer, int modifier, String what) {
            super(modifier, ZimbraQueryParser.HAS, LuceneFields.L_OBJECTS, lookup(mMap, what));
        }
    }

    public static class InQuery extends BaseQuery
    {
        public static final Integer IN_ANY_FOLDER = new Integer(-2);
        public static final Integer IN_LOCAL_FOLDER = new Integer(-3);
        public static final Integer IN_REMOTE_FOLDER = new Integer(-4);

        public static BaseQuery Create(Mailbox mailbox, Analyzer analyzer, int modifier, Integer folderId, boolean includeSubfolders) throws ServiceException {
            if (folderId < 0) {
                InQuery toRet = new InQuery(mailbox, 
                    null, null, null, folderId,
                    includeSubfolders, analyzer, modifier);
//                toRet.mSpecialTarget = folderId;
                return toRet;
            } else {
                Folder folder = mailbox.getFolderById(null, folderId.intValue());
                InQuery toRet = new InQuery(mailbox, 
                    folder, null, null, null,
                    includeSubfolders, analyzer, modifier);
//                toRet.mFolder = folder;
                return toRet;
            }
        }

        public static BaseQuery Create(Mailbox mailbox, Analyzer analyzer, int modifier, String folderName, boolean includeSubfolders) throws ServiceException {
            Pair<Folder, String> result = mailbox.getFolderByPathLongestMatch(null, Mailbox.ID_FOLDER_USER_ROOT, folderName);
            return recursiveResolve(mailbox, analyzer, modifier, result.getFirst(), result.getSecond(), includeSubfolders);
        }
        
        public static BaseQuery Create(Mailbox mailbox, Analyzer analyzer, int modifier, ItemId iid, String subfolderPath, boolean includeSubfolders) throws ServiceException {
            if (!iid.belongsTo(mailbox)) {
                InQuery toRet = new InQuery(mailbox, 
                    null, iid, subfolderPath, null,
                    includeSubfolders, analyzer, modifier);
//                toRet.mFolder = null;
//                toRet.mRemoteId = iid;
//                toRet.mSubfolderPath = subfolderPath;
                return toRet;
            } else {
                // find the base folder
                Pair<Folder, String> result;
                if (subfolderPath != null && subfolderPath.length() > 0) {
                    result = mailbox.getFolderByPathLongestMatch(null, iid.getId(), subfolderPath);
                } else {
                    Folder f = mailbox.getFolderById(null, iid.getId());
                    result = new Pair<Folder, String>(f, null);
                }
                return recursiveResolve(mailbox, analyzer, modifier, result.getFirst(), result.getSecond(), includeSubfolders);                
            }
        }
            
        /** Resolve through local mountpoints until we get to the actual folder, or until we get to a remote folder */
        private static BaseQuery recursiveResolve(Mailbox mailbox, Analyzer analyzer, int modifier, 
            Folder baseFolder, String subfolderPath, boolean includeSubfolders) throws ServiceException {
            
            if (!(baseFolder instanceof Mountpoint)) {
                if (subfolderPath != null) {
                    throw MailServiceException.NO_SUCH_FOLDER(baseFolder.getPath() + "/" + subfolderPath);
                }
                InQuery toRet = new InQuery(mailbox,
                    baseFolder, null, null, null,
                    includeSubfolders, analyzer, modifier);
//                toRet.mFolder = baseFolder;
                return toRet;
            } else {
                Mountpoint mpt = (Mountpoint)baseFolder;
                
                if  (mpt.isLocal()) {
                    // local!
                    if (subfolderPath == null || subfolderPath.length() == 0) {
                        InQuery toRet = new InQuery(mailbox, 
                            baseFolder, null, null, null,    
                            includeSubfolders, analyzer, modifier);
//                        toRet.mFolder = baseFolder;
                        return toRet;
                    } else {
                        Folder newBase = mailbox.getFolderById(null, mpt.getRemoteId());
                        return recursiveResolve(mailbox, analyzer, modifier, newBase, subfolderPath, includeSubfolders);
                    }
                } else {
                    // remote!
                    InQuery toRet = new InQuery(mailbox,
                        null, new ItemId(mpt.getOwnerId(), mpt.getRemoteId()), subfolderPath, null,   
                        includeSubfolders, 
                        analyzer, modifier);
//                    toRet.mRemoteId = new ItemId(mpt.getOwnerId(), mpt.getRemoteId());
//                    toRet.mSubfolderPath = subfolderPath;
                    return toRet;
                }
            }
        }
        
        private InQuery(Mailbox mailbox,
            Folder folder, ItemId remoteId, String subfolderPath, Integer specialTarget,  
            boolean includeSubfolders, 
            Analyzer analyzer, int modifier) {
            super(modifier, ZimbraQueryParser.IN);
            mMailbox = mailbox;
            mFolder = folder;
            mRemoteId = remoteId;
            mSubfolderPath = subfolderPath;
            mSpecialTarget = specialTarget;
            mIncludeSubfolders = includeSubfolders;
        }
        
        /**
         * Used for "is:local" queries
         * 
         * @param mbox
         * @return 
         */
        private QueryOperation getLocalFolderOperation(Mailbox mbox) {
            try {
                Folder root = mbox.getFolderById(null, Mailbox.ID_FOLDER_ROOT);
                List<Folder> allFolders = root.getSubfolderHierarchy();

                Folder trash = mbox.getFolderById(null, Mailbox.ID_FOLDER_TRASH);
                List<Folder> trashFolders = trash.getSubfolderHierarchy();

                allFolders.remove(trash);
                for (Folder f : trashFolders) {
                    allFolders.remove(f);
                }

                Folder spam = mbox.getFolderById(null, Mailbox.ID_FOLDER_SPAM);
                allFolders.remove(spam);

                if (allFolders.size() == 0) {
                    return new NullQueryOperation();
                } else {
                    UnionQueryOperation outer = new UnionQueryOperation();

                    for (Folder f : allFolders) {
                        if (!(f instanceof Mountpoint) && !(f instanceof SearchFolder)) {
                            DBQueryOperation dbop = new DBQueryOperation();
                            outer.add(dbop);
                            dbop.addInClause(f, true);
                        }
                    }
                    return outer;
                }
            } catch (ServiceException e) {
                return new NullQueryOperation();
            }
        }

        /**
         * Used for "is:remote" queries
         *  
         * @param truth
         * @param mbox
         * @return
         */
        private QueryOperation getRemoteFolderOperation(boolean truth, Mailbox mbox) {
            try {
                Folder root = mbox.getFolderById(null, Mailbox.ID_FOLDER_ROOT);
                List<Folder> allFolders = root.getSubfolderHierarchy();

                Folder trash = mbox.getFolderById(null, Mailbox.ID_FOLDER_TRASH);
                List<Folder> trashFolders = trash.getSubfolderHierarchy();

                allFolders.remove(trash);
                for (Folder f : trashFolders) {
                    allFolders.remove(f);
                }

                Folder spam = mbox.getFolderById(null, Mailbox.ID_FOLDER_SPAM);
                allFolders.remove(spam);

                // from our list of all folders, remove anything that isn't a mountpoint
                // and also remove anything that is a local-pointing mountpoint
                for (Iterator<Folder> iter = allFolders.iterator(); iter.hasNext();) {
                    Folder f = iter.next();
                    if (!(f instanceof Mountpoint))
                        iter.remove();
                    else {
                        Mountpoint mpt = (Mountpoint)f;
                        if (mpt.isLocal()) {
                            iter.remove();
                        }
                    }
                }

                if (allFolders.size() == 0) {
                    if (truth) {
                        return new NullQueryOperation();
                    }
                } else {
                    if (truth) {
                        UnionQueryOperation outer = new UnionQueryOperation();

                        for (Folder f : allFolders) {
                            assert((f instanceof Mountpoint) && !((Mountpoint)f).isLocal());
                            Mountpoint mpt = (Mountpoint)f;
                            DBQueryOperation dbop = new DBQueryOperation();
                            outer.add(dbop);
                            dbop.addInRemoteFolderClause(new ItemId(mpt.getOwnerId(), mpt.getRemoteId()), "", mIncludeSubfolders, truth);                            
                        }
                        return outer;
                    } else {
                        IntersectionQueryOperation outer = new IntersectionQueryOperation();

                        for (Folder f : allFolders) {
                            assert((f instanceof Mountpoint) && !((Mountpoint)f).isLocal());
                            Mountpoint mpt = (Mountpoint)f;
                            DBQueryOperation dbop = new DBQueryOperation();
                            outer.addQueryOp(dbop);
                            dbop.addInRemoteFolderClause(new ItemId(mpt.getOwnerId(), mpt.getRemoteId()), "", mIncludeSubfolders, truth);                            
                        }

                        return outer;
                    }
                }
            } catch (ServiceException e) {}
            return new NullQueryOperation();
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            if (mSpecialTarget != null) {
                if (mSpecialTarget == IN_ANY_FOLDER) {
                    DBQueryOperation dbOp = DBQueryOperation.Create();
                    dbOp.addAnyFolderClause(calcTruth(truth));
                    return dbOp;
                } else {
                    if (calcTruth(truth)) {
                        if (mSpecialTarget == IN_REMOTE_FOLDER) {
                            return getRemoteFolderOperation(true, mMailbox);
                        } else {
                            assert(mSpecialTarget == IN_LOCAL_FOLDER);
                            return getLocalFolderOperation(mMailbox);
                        }
                    } else {
                        if (mSpecialTarget == IN_REMOTE_FOLDER) {
                            return getLocalFolderOperation(mMailbox);
                        } else {
                            assert(mSpecialTarget == IN_LOCAL_FOLDER);
                            return getRemoteFolderOperation(true, mMailbox);
                        }
                    }
                }
            }
            
            DBQueryOperation dbOp = DBQueryOperation.Create();
            if (mFolder != null) {
                if (mIncludeSubfolders) {
                    List<Folder> subFolders = mFolder.getSubfolderHierarchy();
                    
                    if (truth) {
                        // (A or B or C)
                        UnionQueryOperation union = new UnionQueryOperation();
                        
                        for (Folder f : subFolders) {
                            DBQueryOperation dbop = new DBQueryOperation();
                            union.add(dbop);
                            if (f instanceof Mountpoint) {
                                Mountpoint mpt = (Mountpoint)f;
                                if (!mpt.isLocal()) {
                                    dbop.addInRemoteFolderClause(new ItemId(mpt.getOwnerId(), mpt.getRemoteId()), "", mIncludeSubfolders, truth);
                                } else {
                                    // TODO FIXME handle local mountpoints.  Don't forget to check for infinite recursion!
                                }
                                
                            } else {
                                dbop.addInClause(f, truth);
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
                                    dbop.addInRemoteFolderClause(new ItemId(mpt.getOwnerId(), mpt.getRemoteId()), "", mIncludeSubfolders, truth);
                                } else {
                                    // TODO FIXME handle local mountpoints.  Don't forget to check for infinite recursion!
                                }
                                
                            } else {
                                dbop.addInClause(f, truth);
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

        public String toString(int expLevel) {
            if (mSpecialTarget != null) {
                String toRet;
                if (!mIncludeSubfolders) 
                    toRet = super.toString(expLevel)+",IN:";
                else 
                    toRet = super.toString(expLevel)+",UNDER:";
                    
                if (mSpecialTarget == IN_ANY_FOLDER) {
                    toRet = toRet + "ANY_FOLDER";
                } else if (mSpecialTarget == IN_LOCAL_FOLDER) {
                    toRet = toRet + "LOCAL";
                } else if (mSpecialTarget == IN_REMOTE_FOLDER) {
                    toRet = toRet + "REMOTE";
                }
                return toRet;
            } else {
                return super.toString(expLevel)+
                ",IN:"+(mRemoteId!=null ? mRemoteId.toString() :
                    (mFolder!=null?mFolder.getName():"ANY_FOLDER"))
                    + (mSubfolderPath != null ? "/"+mSubfolderPath : "")
                    +")";
            }
        }
        
        private Folder mFolder;
        private ItemId mRemoteId = null;
        private String mSubfolderPath = null;
        private Integer mSpecialTarget = null;
        private boolean mIncludeSubfolders = false;
        private Mailbox mMailbox;
    }

    public abstract static class LuceneTableQuery extends BaseQuery
    {
        protected static void addMapping(HashMap<String, String> map, String[] array, String value) {
            for (int i = array.length-1; i>=0; i--) {
                map.put(array[i], value);
            }
        }

        protected static String lookup(HashMap map, String key) {
            String toRet = (String)map.get(key);
            if (toRet == null) {
                return key;
            } else {
                return toRet;
            }
        }


        private String mLuceneField;
        private String mValue;

        public LuceneTableQuery(int modifier, int target, String luceneField, String value) {
            super(modifier, target);
            mLuceneField = luceneField;
            mValue = value;
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            LuceneQueryOperation op = LuceneQueryOperation.Create();

            Query q = null;
            if (mValue != null) {
                q = new TermQuery(new Term(mLuceneField, mValue));
            }
            op.addClause(getQueryOperatorString()+mValue, q,calcTruth(truth));

            return op;
        }

        public String toString(int expLevel) {
            return super.toString(expLevel)+","+mLuceneField+":"+mValue+")";
        }
    }

    public static class ReadQuery extends TagQuery
    {
        public ReadQuery(Mailbox mailbox, Analyzer analyzer, int modifier, boolean truth) throws ServiceException
        {
            super(analyzer, modifier, mailbox.mUnreadFlag, !truth);
        }

        public String toString(int expLevel) {
            if (!mTruth) {
                return super.toString(expLevel) + ",READ)";
            } else {
                return super.toString(expLevel) + ",UNREAD)";
            }
        }
    }

    public static class RepliedQuery extends TagQuery
    {
        public RepliedQuery(Mailbox mailbox, Analyzer analyzer, int modifier, boolean truth) throws ServiceException
        {
            super(analyzer, modifier, mailbox.mReplyFlag, truth);
        }

        public String toString(int expLevel) {
            if (mTruth) {
                return super.toString(expLevel) + ",REPLIED)";
            } else {
                return super.toString(expLevel) + ",UNREPLIED)";
            }
        }
    }
    

    public static class IsInviteQuery extends TagQuery
    {
        public IsInviteQuery(Mailbox mailbox, Analyzer analyzer, int modifier, boolean truth) throws ServiceException
        {
            super(analyzer, modifier, mailbox.mInviteFlag, truth);
        }

        public String toString(int expLevel) {
            if (mTruth) {
                return super.toString(expLevel) + ",INVITE)";
            } else {
                return super.toString(expLevel) + ",NOT_INVITE)";
            }
        }
    }    

    public static class SentQuery extends TagQuery
    {
        public SentQuery(Mailbox mailbox, Analyzer analyzer, int modifier, boolean truth) throws ServiceException
        {
            super(analyzer, modifier, mailbox.mSentFlag, truth);
        }

        public String toString(int expLevel) {
            if (mTruth) {
                return super.toString(expLevel) + ",SENT)";
            } else {
                return super.toString(expLevel) + ",RECEIVED)";
            }
        }
    }
    
    public static class SizeQuery extends BaseQuery
    {
        private String mSizeStr;
        private int mSize;

        public SizeQuery(Analyzer analyzer, int modifier, int target, String size) throws ParseException {
            super(modifier, target);

            mSizeStr = size;

            char ch = mSizeStr.charAt(0);
            if (ch == '>') {
                setQueryType(ZimbraQueryParser.BIGGER);
                mSizeStr = mSizeStr.substring(1);
            } else if (ch == '<') {
                setQueryType(ZimbraQueryParser.SMALLER);
                mSizeStr = mSizeStr.substring(1);
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
                    multiplier = 1024*1024;
                    break;
            }

            if (multiplier > 1) {
                mSizeStr = mSizeStr.substring(0,mSizeStr.length()-1);
            }


            mSize = Integer.parseInt(mSizeStr) * multiplier;

//          System.out.println("Size of \""+size+"\" parsed to "+mSize);

            mSizeStr = ZimbraAnalyzer.SizeTokenFilter.EncodeSize(mSize);
            if (mSizeStr == null) {
                mSizeStr = "";
            }
        }

        protected QueryOperation getQueryOperation(boolean truth) 
        {
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
                    highest = mSize;
                    lowest = mSize;
                    break;
                default:
                    assert(false);
            }
            op.addSizeClause(lowest, highest, truth);
            return op;
        }

        public String toString(int expLevel) {
            return super.toString(expLevel)+","+mSize +")";
        }
    }
    
    public static class ModseqQuery extends BaseQuery
    {
        static enum Operator {
            EQ, GT, GTEQ, LT, LTEQ;
        }
        
        private int mValue;
        private Operator mOp;

        public ModseqQuery(Mailbox mbox, Analyzer analyzer, int modifier, int target, String changeId) throws ParseException {
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

        protected QueryOperation getQueryOperation(boolean truth) 
        {
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

        public String toString(int expLevel) {
            return super.toString(expLevel)+","+mOp+" "+mValue+")";
        }
    }
    

    public static class SubQuery extends BaseQuery
    {
        private AbstractList mSubClauses;
        public SubQuery(Analyzer analyzer, int modifier, AbstractList exp)
        {
            super(modifier, SUBQUERY_TOKEN );
            mSubClauses = exp;
        }
        
        protected BaseQuery getSubClauseHead() {
            return (BaseQuery)mSubClauses.get(0);
        }

        AbstractList getSubClauses() { return mSubClauses; }

        protected QueryOperation getQueryOperation(boolean truth) {
            assert(false);
            return null;
        }

        public String toString(int expLevel) {
            String ret = indent(expLevel)+modToString()+"( ";
            BaseQuery sub = (BaseQuery)mSubClauses.get(0);
            while(sub != null) {
                ret += sub.toString(expLevel+1)+" ";
                sub = sub.getNext();
            }
            ret+=indent(expLevel)+" )";
            return ret;        
        }
    }

    // bitmask for choosing "FROM/TO/CC" of messages...used for AddrQuery and MeQuery 
    public static final int ADDR_BITMASK_FROM = 0x1;
    public static final int ADDR_BITMASK_TO =   0x2;
    public static final int ADDR_BITMASK_CC =   0x4;
    
    /** A simpler way of expressing (to:FOO or from:FOO or cc:FOO) */
    public static class AddrQuery extends SubQuery {
        protected AddrQuery(Analyzer analyzer, int modifier, AbstractList exp) {
            super(analyzer, modifier, exp);
        }
        public static ZimbraQuery.BaseQuery createFromTarget(Mailbox mbox, Analyzer analyzer, int modifier, int target, String text) throws ServiceException {
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
        
        public static ZimbraQuery.BaseQuery createFromBitmask(Mailbox mbox, Analyzer analyzer, int modifier, String text, int operatorBitmask) throws ServiceException {
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
        protected MeQuery(Analyzer analyzer, int modifier, AbstractList exp) {
            super(analyzer, modifier, exp);
        }
        
        public static ZimbraQuery.BaseQuery create(Mailbox mbox, Analyzer analyzer, int modifier, int operatorBitmask) throws ServiceException {
            ArrayList<ZimbraQuery.BaseQuery> clauses = new ArrayList<ZimbraQuery.BaseQuery>();
            Account acct = mbox.getAccount();
            boolean atFirst = true;
            if ((operatorBitmask & ADDR_BITMASK_FROM) !=0) {
                clauses.add(new SentQuery(mbox, analyzer, modifier, true));
                atFirst = false;
            } 
            if ((operatorBitmask & ADDR_BITMASK_TO) != 0) {
                if (atFirst) 
                    atFirst = false;
                else
                    clauses.add(new ConjQuery(analyzer, ConjQuery.OR));
                clauses.add(new TextQuery(mbox, analyzer, modifier, ZimbraQueryParser.TO, acct.getName())); 
            }
            if ((operatorBitmask & ADDR_BITMASK_CC) != 0) {
                if (atFirst) 
                    atFirst = false;
                else
                    clauses.add(new ConjQuery(analyzer, ConjQuery.OR));
                clauses.add(new TextQuery(mbox, analyzer, modifier, ZimbraQueryParser.CC, acct.getName())); 
            }
            
            String[] aliases = acct.getAliases();
            for (String alias : aliases) {
//                if ((operatorBitmask & ADDR_BITMASK_FROM) !=0) {
//                    if (atFirst) {
//                        clauses.add(new ConjQuery(analyzer, ConjQuery.OR));
//                        atFirst = false;
//                    }
//                    clauses.add(new SentQuery(mbox, analyzer, modifier, true)); 
//                } 
                if ((operatorBitmask & ADDR_BITMASK_TO) != 0) {
                    if (atFirst) 
                        atFirst = false;
                    else
                        clauses.add(new ConjQuery(analyzer, ConjQuery.OR));
                    clauses.add(new TextQuery(mbox, analyzer, modifier, ZimbraQueryParser.TO, alias)); 
                }
                if ((operatorBitmask & ADDR_BITMASK_CC) != 0) {
                    if (atFirst) 
                        atFirst = false;
                    else
                        clauses.add(new ConjQuery(analyzer, ConjQuery.OR));
                    clauses.add(new TextQuery(mbox, analyzer, modifier, ZimbraQueryParser.CC, alias)); 
                }
            }
            return new MeQuery(analyzer, modifier, clauses); 
        }
    }

    public static class TagQuery extends BaseQuery
    {
        private Tag mTag = null;

        public TagQuery(Analyzer analyzer, int modifier, Tag tag, boolean truth) 
        {
            super(modifier, ZimbraQueryParser.TAG);
            mTag = tag;
            mTruth = truth;
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation dbOp = DBQueryOperation.Create();

            dbOp.addTagClause(mTag, calcTruth(truth));

            return dbOp;
        }

        public String toString(int expLevel) {
            return super.toString(expLevel)+","+mTag+")";
        }
    }

    public static class ItemQuery extends BaseQuery
    {
        public static BaseQuery Create(Mailbox mbox, Analyzer analyzer, int modifier, String str) 
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

            return new ItemQuery(mbox, analyzer, modifier, allQuery, noneQuery, itemIds);
        }

        private boolean mIsAllQuery;
        private boolean mIsNoneQuery;
        private List<ItemId> mItemIds;
        private Mailbox mMailbox;

        ItemQuery(Mailbox mbox, Analyzer analyzer, int modifier, boolean all, boolean none, List<ItemId> ids) 
        {
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
                return new NullQueryOperation();
            } else {
                for (ItemId iid : mItemIds) {
                    dbOp.addItemIdClause(mMailbox, iid, truth);
                }
            }
            return dbOp;
        }

        public String toString(int expLevel) {
            StringBuffer toRet = new StringBuffer(super.toString(expLevel));
            if (mIsAllQuery) {
                toRet.append(",all");
            } else if (mIsNoneQuery) {
                toRet.append(",none");
            } else {
                for (Iterator<ItemId> iter = mItemIds.iterator(); iter.hasNext();) {
                    ItemId cur = iter.next();
                    toRet.append(","+cur.toString());
                }
            }
            return toRet.toString();
        }
    }

    
    public static class FieldQuery {
        public static TextQuery Create(Mailbox mbox, Analyzer analyzer, int modifier, int qType, String targetImg, String text) throws ServiceException {
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
                    System.out.println("Field is: \""+fieldName+"\"");
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
    public static class TextQuery extends BaseQuery
    {
        private ArrayList<String> mTokens;

        private LinkedList<String> mOredTokens;
        private String mWildcardTerm;
        private String mOrigText;
        private List<QueryInfo> mQueryInfo = new ArrayList<QueryInfo>(); 

        private static final int MAX_WILDCARD_TERMS = 2000;

        /**
         * @param mbox
         * @param analyzer
         * @param modifier
         * @param qType
         * @param text A single search term.  If text has multiple words, it is treated as a phrase (full exact match required)
         *       text may end in a *, which wildcards the last term
         * @throws ServiceException
         */
        public TextQuery(Mailbox mbox, Analyzer analyzer, int modifier, int qType, String text) throws ServiceException {
            super(modifier, qType);

            mOredTokens = new LinkedList<String>();

            // The set of tokens from the user's query.  The way the parser works, the token set should generally only be one element  
            mTokens = new ArrayList<String>(1);
            mWildcardTerm = null;

            TokenStream source = analyzer.tokenStream(QueryTypeString(qType), new StringReader(text));
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
            } catch (IOException e) { /* ignore */ }
            
            
            // okay, quite a bit of hackery here....basically, if they're doing a contact:
            // search AND they haven't manually specified a phrase query (expands to more than one term)
            // then lets hack their search and make it a * search.
            // for bug:17232 -- if the search string is ".", don't auto-wildcard it, because . is
            // supposed to match everything by default.
            if (qType == ZimbraQueryParser.CONTACT && mTokens.size() <= 1 && text.length() > 0 
                        && text.charAt(text.length()-1)!='*' && !text.equals(".")) {
                text = text+'*';
            }
            
            mOrigText = text;

            // must look at original text here b/c analyzer strips *'s
            if (text.length() > 0 && text.charAt(text.length()-1) == '*')
            {
                // wildcard query!
                String wcToken;

                // only the last token is allowed to have a wildcard in it 
                if (mTokens.size() > 0)
                    wcToken = mTokens.remove(mTokens.size()-1);
                else
                    wcToken = text;

                if (wcToken.charAt(wcToken.length()-1) == '*')
                    wcToken = wcToken.substring(0, wcToken.length()-1);

                if (wcToken.length() > 0) {
                    mWildcardTerm = wcToken;
                    MailboxIndex mbidx = mbox.getMailboxIndex();
                    List<String> expandedTokens = new ArrayList<String>(100);
                    boolean expandedAllTokens = false;
                    if (mbidx != null)
                        expandedAllTokens = mbidx.expandWildcardToken(expandedTokens, QueryTypeString(qType), wcToken, MAX_WILDCARD_TERMS);

//                  if (!expandedAllTokens) {
//                  throw MailServiceException.TOO_MANY_QUERY_TERMS_EXPANDED("Wildcard text: \""+wcToken
//                  +"\" expands to too many terms (maximum allowed is "+MAX_WILDCARD_TERMS+")", wcToken+"*", MAX_WILDCARD_TERMS);
//                  }

                    mQueryInfo.add(new WildcardExpansionQueryInfo(wcToken+"*", expandedTokens.size(), expandedAllTokens));
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

            MailboxIndex mbidx = mbox.getMailboxIndex();
            if (mbidx != null) {
                // don't check spelling for structured-data searches
                if (qType != ZimbraQueryParser.FIELD) 
                    for (String token : mTokens) {
                        List<SpellSuggestQueryInfo.Suggestion> suggestions = mbidx.suggestSpelling(QueryTypeString(qType), token);
                        if (suggestions != null) 
                            mQueryInfo.add(new SpellSuggestQueryInfo(token, suggestions));
                    }
            }
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            if (mTokens.size() <= 0 && mOredTokens.size()<=0) {
                // if we have no tokens, that is usually because the analyzer removed them
                // -- the user probably queried for a stop word like "a" or "an" or "the"
                //
                // By design: interpret *zero* tokens to mean "ignore this search term"
                // 
                // we can't simply ignore this query, however -- we have to put a null
                // query into the query list, otherwise conjunctions will get confused...so
                // we pass NULL to addClause which will add a blank clause for us...
                return new NoTermQueryOperation();
            } else {
                LuceneQueryOperation lop = LuceneQueryOperation.Create();

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
                    p.setSlop(0); // TODO configurable?
                    for (int i=0; i<mTokens.size(); i++) 
                        p.add(new Term(fieldName, mTokens.get(i)));
                    lop.addClause(this.getQueryOperatorString()+mOrigText, p,calcTruth(truth));
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

        public String toString(int expLevel) {
            String ret = super.toString(expLevel)+",";
            for (int i = 0; i < mTokens.size(); i++) {
                ret+=","+(mTokens.get(i)).toString();
            }
            if (mWildcardTerm != null) {
                ret+=" WILDCARD="+mWildcardTerm+" ["+mOredTokens.size()+" terms]";
            }
            return ret+")";
        }
    }

    public static class TypeQuery extends AttachmentQuery
    {
        public TypeQuery(Analyzer analyzer, int modifier, String what) {
            super(modifier, LuceneFields.L_MIMETYPE, what);
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

            if (mLt)
                op.addRelativeSender(null, false, mStr, mEq, truth);
            else 
                op.addRelativeSender(mStr, mEq, null, false, truth);

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
        private SenderQuery(Mailbox mbox, Analyzer analyzer, int modifier, int qType, String text) throws ServiceException {
            super(modifier, qType);

            mLt = (text.charAt(0) == '<');
            mEq = false;
            mStr = text.substring(1);

            if (mStr.charAt(0) == '=') {
                mEq = true;
                mStr= mStr.substring(1);
            }

            if (mStr.length() == 0)
                throw MailServiceException.PARSE_ERROR("Invalid sender string: "+text, null);
        }

        public static BaseQuery create(Mailbox mbox, Analyzer analyzer, int modifier, int qType, String text) throws ServiceException {
            if (text.length() > 1 && (text.startsWith("<") || text.startsWith(">")))
                return new SenderQuery(mbox, analyzer, modifier, qType, text);
            else
                return new TextQuery(mbox, analyzer, modifier, qType, text);
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

            if (mLt)
                op.addRelativeSubject(null, false, mStr, mEq, truth);
            else 
                op.addRelativeSubject(mStr, mEq, null, false, truth);

            return op;
        }

        @Override
        public String toString(int expLevel) {
            return super.toString(expLevel) + "Subject(";
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
        private SubjectQuery(Mailbox mbox, Analyzer analyzer, int modifier, int qType, String text) throws ServiceException {
            super(modifier, qType);

            mLt = (text.charAt(0) == '<');
            mEq = false;
            mStr = text.substring(1);

            if (mStr.charAt(0) == '=') {
                mEq = true;
                mStr= mStr.substring(1);
            }

            if (mStr.length() == 0)
                throw MailServiceException.PARSE_ERROR("Invalid subject string: "+text, null);
        }

        public static BaseQuery create(Mailbox mbox, Analyzer analyzer, int modifier, int qType, String text) throws ServiceException {
            if (text.length() > 1 && (text.startsWith("<") || text.startsWith(">"))) {
                // real subject query!
                return new SubjectQuery(mbox, analyzer, modifier, qType, text);
            } else {
                return new TextQuery(mbox, analyzer, modifier, qType, text);
            }
        }
    }

    private static Log mLog = LogFactory.getLog(ZimbraQuery.class);

    private static final int SUBQUERY_TOKEN = 9999;

    private AbstractList mClauses;
    private ParseTree.Node mParseTree = null;
    private QueryOperation mOp;
    private Mailbox mMbox;
    private ZimbraQueryResults mResults;
    private SearchParams mParams;
    private int mChunkSize;

    private static String[] unquotedTokenImage;
    private static HashMap<String, Integer> sTokenImageMap;

    static {
        sTokenImageMap = new HashMap<String,Integer>();
        
        unquotedTokenImage = new String[ZimbraQueryParserConstants.tokenImage.length];
        for (int i = 0; i < ZimbraQueryParserConstants.tokenImage.length; i++) {
            String str = ZimbraQueryParserConstants.tokenImage[i].substring(1, ZimbraQueryParserConstants.tokenImage[i].length()-1);
            unquotedTokenImage[i] = str;
            sTokenImageMap.put(str, i);
        }
    }
    
    private static int lookupQueryTypeFromString(String str) throws ServiceException {
        Integer toRet = sTokenImageMap.get(str);
        if (toRet == null)
            throw MailServiceException.QUERY_PARSE_ERROR(str, null, str, -1, -1);
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
     * @author tim
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

        public static abstract class Node {
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

        public static class OperatorNode extends Node {
            int mKind;
            boolean mTruthFlag = true;
            public ArrayList<Node> mNodes = new ArrayList<Node>();

            public OperatorNode(int kind) {
                mKind = kind;
            }
            protected OperatorNode() {}

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
                    for (Iterator cur = mNodes.iterator(); cur.hasNext();) {
                        Node n = (Node)cur.next();
                        n.invertTruth();
                    }
                }
                assert(mTruthFlag);
                for (Iterator cur = mNodes.iterator(); cur.hasNext();) {
                    Node n = (Node)cur.next();
                    n.pushNotsDown();
                }
            }

            public Node simplify() 
            {
                boolean simplifyAgain;
                do {
                    simplifyAgain = false;
                    // first, simplify our sub-ops...
                    ArrayList<Node> newNodes = new ArrayList<Node>();
                    for (Iterator cur = mNodes.iterator(); cur.hasNext();) {
                        Node n = (Node)cur.next();

                        newNodes.add(n.simplify());
                    }
                    mNodes = newNodes;

                    // now, see if any of our subops can be trivially combined with us
                    newNodes = new ArrayList<Node>();
                    for (Iterator cur = mNodes.iterator(); cur.hasNext();) {
                        Node n = (Node)cur.next();

                        boolean addIt = true;

                        if (n instanceof OperatorNode) {
                            OperatorNode opn = (OperatorNode)n;
                            if (opn.mKind == mKind && opn.mTruthFlag == true) {
                                addIt = false;
                                simplifyAgain = true;
                                for (Iterator opIter = opn.mNodes.iterator(); opIter.hasNext();) {
                                    newNodes.add((Node)(opIter.next()));
                                }
                            }
                        }
                        if (addIt) {
                            newNodes.add(n);
                        }
                    }
                    mNodes = newNodes;
                } while(simplifyAgain);

                if (mNodes.size() == 0) {
                    return null;
                }
                if (mNodes.size() == 1) {
                    Node n = (Node)mNodes.get(0);
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

            public String toString() {
                StringBuffer toRet = new StringBuffer(mTruthFlag ? "" : " NOT ");

                toRet.append(mKind == STATE_AND ? " AND[" : " OR(");

                for (Iterator cur = mNodes.iterator(); cur.hasNext();) {
                    Node n = (Node)cur.next();
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

                    for (Iterator cur = mNodes.iterator(); cur.hasNext();) {
                        Node n = (Node)cur.next();

                        QueryOperation op = n.getQueryOperation();
                        assert(op!=null);
                        intersect.addQueryOp(op);
                    }

                    if (ParseTree.SPEW) System.out.print(") ");
                    return intersect;
                } else {
                    if (ParseTree.SPEW) System.out.print(" OR(");

                    UnionQueryOperation union = new UnionQueryOperation();

                    for (Iterator cur = mNodes.iterator(); cur.hasNext();) {
                        Node n = (Node)cur.next();

                        QueryOperation op = n.getQueryOperation();
                        assert(op!=null);
                        union.add(op);
                    }
                    if (ParseTree.SPEW) System.out.print(") ");
                    return union;                    
                }
            }

        }

        public static class ThingNode extends Node {
            BaseQuery mThing;

            // used so that we can uniquely identify each permutation with an integer
            int mPermuteBase;
            int mNumCanExecute;  

            public ThingNode(BaseQuery thing) {
                mThing = thing;
                mTruthFlag = thing.mTruth;
            }

            public void invertTruth() 
            {
                mTruthFlag = !mTruthFlag;
            }
            public void pushNotsDown() {
            }

            public Node simplify() { return this; }


            public String toString() {
                StringBuffer toRet = new StringBuffer(mTruthFlag ? "" : " NOT ");
                toRet.append(mThing.toString());

                return toRet.toString();
            }
            public QueryOperation getQueryOperation() {
                return mThing.getQueryOperation(mTruthFlag);
            }
        }

        public static Node build(AbstractList clauses)
        {
            OperatorNode top = new OperatorNode(STATE_OR);

            OperatorNode cur = new OperatorNode(STATE_AND);
            top.add(cur);

            for (Iterator iter = clauses.iterator(); iter.hasNext();) {
                ZimbraQuery.BaseQuery q = (ZimbraQuery.BaseQuery)iter.next();

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

    private void handleSortByOverride(String str) throws ServiceException
    {
        SortBy sortBy = SortBy.lookup(str);
        if (sortBy == null) 
            throw ServiceException.FAILURE("Unkown sortBy: specified in search string: "+str, null);

        mSortByOverride = sortBy;
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
            //                       query                     lower                 higher
            // string dates
            testDate(mbox, "date:"+JAN1Str,        JAN1, true,       JAN2, false);
            testDate(mbox, "date:<"+JAN1Str,       -1L,  false,      JAN1, false);
            testDate(mbox, "before:"+JAN1Str,      -1L,  false,      JAN1, false);
            testDate(mbox, "date:<="+JAN1Str,     -1L,  false,      JAN2, false);
            testDate(mbox, "date:>"+JAN1Str,      JAN2,  true,      -1L, false); 
            testDate(mbox, "after:"+JAN1Str,      JAN2,  true,      -1L, false);
            testDate(mbox, "date:>="+JAN1Str,     JAN1,  true,      -1L, false); 
            
            // numeric dates
            testDate(mbox, "date:"+JAN1,             JAN1, true,     JAN1+GRAN, false);
            testDate(mbox, "date:<"+JAN1,           -1L, false,      JAN1, false);
            testDate(mbox, "before:"+JAN1,          -1L, false,      JAN1, false);
            testDate(mbox, "date:<="+JAN1,          -1L, false,     JAN1+GRAN, false);
            testDate(mbox, "date:>"+JAN1,           JAN1+GRAN, true,  -1L, false);
            testDate(mbox, "after:"+JAN1,           JAN1+GRAN, true,  -1L, false);
            testDate(mbox, "date:>="+JAN1,          JAN1, true,     -1L, false);
            
            return true;
        } catch (ServiceException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    private static void testDate(Mailbox mbox, String qs, long lowest, boolean lowestEq, 
                long highest, boolean highestEq)  throws ServiceException, ParseException {
        
        AbstractList<ZimbraQuery.BaseQuery>  c; // clauses
        c = unitTestParse(mbox,  qs);
        for (BaseQuery t : c) {
            if (t instanceof DateQuery) {
                DateQuery dq = (DateQuery)t;
                if (dq.mLowestTime != lowest)
                    throw ServiceException.FAILURE("Invalid lowest time (found "+ dq.mLowestTime + " expected "+lowest+"), query is \""+qs+"\"", null);
                if (dq.mLowerEq != lowestEq)
                    throw ServiceException.FAILURE("Invalid lower EQ (expected "+lowestEq+"), query is \""+qs+"\"", null);
                if (dq.mHighestTime != highest)
                    throw ServiceException.FAILURE("Invalid highest time (found "+ dq.mHighestTime + " expected "+highest+"), query is \""+qs+"\"", null);
                if (dq.mHigherEq != highestEq)
                    throw ServiceException.FAILURE("Invalid higher EQ (expected "+highestEq+"), query is \""+qs+"\"", null);
            }
        }
    }
    
    private static AbstractList<BaseQuery>  unitTestParse(Mailbox mbox, String qs) throws ServiceException, ParseException {
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
    
    public ZimbraQuery(Mailbox mbox, SearchParams params, boolean includeTrash, boolean includeSpam) throws ParseException, ServiceException {
        mParams = params;
        mMbox = mbox;
        long chunkSize = (long)mParams.getOffset() + (long)mParams.getLimit();
        if (chunkSize > 1000)
            mChunkSize = 1000;
        else 
            mChunkSize = (int)chunkSize;
        
        //
        // Step 1: parse the text using the JavaCC parser
        ZimbraQueryParser parser = new ZimbraQueryParser(new StringReader(mParams.getQueryStr()));
        Analyzer analyzer = null;
        MailboxIndex mi = mbox.getMailboxIndex();
        if (mi != null) {
            mi.initAnalyzer(mbox);
            analyzer = mi.getAnalyzer();
        } else {
            analyzer = ZimbraAnalyzer.getDefaultAnalyzer();
        }
        parser.init(analyzer, mMbox, params.getTimeZone(), params.getLocale(), lookupQueryTypeFromString(params.getDefaultField()));
        mClauses = parser.Parse();

        String sortByStr = parser.getSortByStr();
        if (sortByStr != null)
            handleSortByOverride(sortByStr);

        //
        // Step 2: build a parse tree and push all the "NOT's" down to the
        // bottom level -- this is because we cannot invert result sets 
        if (ParseTree.SPEW) System.out.println("QueryString: "+mParams.getQueryStr());
        ParseTree.Node pt = ParseTree.build(mClauses);
        if (ParseTree.SPEW) System.out.println("PT: "+pt.toString());
        if (ParseTree.SPEW)System.out.println("Simplified:");
        pt = pt.simplify();
        if (ParseTree.SPEW)System.out.println("PT: "+pt.toString());
        if (ParseTree.SPEW)System.out.println("Pushing nots down:");
        pt.pushNotsDown();
        if (ParseTree.SPEW)System.out.println("PT: "+pt.toString());

        // 
        // Store some variables that we'll need later
        mParseTree = pt;
        mOp = null;

        //
        // handle the special "sort:" tag in the search string
        if (mSortByOverride != null) {
            if (mLog.isDebugEnabled())
                mLog.debug("Overriding SortBy parameter to execute ("+params.getSortBy().toString()+") w/ specification from QueryString: "+mSortByOverride.toString());

            params.setSortBy(mSortByOverride);
        }

        //
        // Step 3: Convert list of BaseQueries into list of QueryOperations, then Optimize the Ops

        if (mClauses.size() > 0) {
            // this generates all of the query operations
            mOp = mParseTree.getQueryOperation();

            if (mLog.isDebugEnabled()) {
                mLog.debug("OP="+mOp.toString());
            }

            // optimize the query down
            mOp = mOp.optimize(mMbox);
            if (mOp == null)
                mOp = new NullQueryOperation();
            
            assert(mOp != null);
            if (mLog.isDebugEnabled()) {
                mLog.debug("OPTIMIZED="+mOp.toString());
            }

            // do spam/trash hackery!
            if (!includeTrash || !includeSpam) {
                if (!mOp.hasSpamTrashSetting()) {
                    mOp = mOp.ensureSpamTrashSetting(mMbox, includeTrash, includeSpam);
                    if (mLog.isDebugEnabled()) {
                        mLog.debug("AFTERTS="+mOp.toString());
                    }
                    mOp = mOp.optimize(mMbox); // optimize again now that we have the trash/spam setting
                }
            }
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
    final public ZimbraQueryResults execute(OperationContext octxt, SoapProtocol proto) throws ServiceException, IOException
    {
        // 
        // STEP 1: use the OperationContext to update the set of visible referenced folders, local AND remote
        //
        if (mOp!= null) {
            QueryTargetSet targets = mOp.getQueryTargets();
            assert(mOp instanceof UnionQueryOperation || targets.countExplicitTargets() <=1);
            MailboxIndex mbidx = mMbox.getMailboxIndex();

            UnionQueryOperation union = null;
            if (mOp instanceof UnionQueryOperation) { 
                union = (UnionQueryOperation)mOp;
            } else {
                union = new UnionQueryOperation();
                union.add(mOp);
            }
            
            mOp = handlePermissionChecks(union, proto, mMbox, octxt, mbidx, mParams);
        }
        
        //
        // STEP 2: run the query
        // 
        MailboxIndex mbidx = mMbox.getMailboxIndex();

        if (ZimbraLog.index.isDebugEnabled()) {
            String str = this.toString() +" search([";
            for (int i = 0; i < mParams.getTypes().length; i++) {
                if (i > 0) {
                    str += ",";
                }
                str+=mParams.getTypes()[i];
            }
            str += "]," + mParams.getSortBy()+ ")";
            ZimbraLog.index.debug(str);
        }

        if (mOp!= null) {
            QueryTargetSet targets = mOp.getQueryTargets();
            assert(mOp instanceof UnionQueryOperation || targets.countExplicitTargets() <=1);
            assert(targets.size() >1 || !targets.hasExternalTargets() || mOp instanceof RemoteQueryOperation);

            if (mLog.isDebugEnabled())
                mLog.debug("OPERATION:"+mOp.toString());

            assert(mResults == null);

            // if we've only got one target, and it is external, then mOp must be a RemoteQueryOp
            mResults = mOp.run(mMbox, mbidx, mParams, mChunkSize);
            mResults = new HitIdGrouper(mResults, mParams.getSortBy());
            
            if (!mParams.getIncludeTagDeleted()) {
                FilteredQueryResults filtered = new FilteredQueryResults(mResults);
                filtered.setFilterTagDeleted(true);
                mResults = filtered;
            }
            
            return mResults;
        } else {
            mLog.debug("Operation optimized to nothing.  Returning no results");
            return new EmptyQueryResults(mParams.getTypes(), mParams.getSortBy(), mParams.getMode());
        }
    }
    
    /**
     * Callback -- adds a "-l.field:_calendaritemclass:private" term to all Lucene search parts: to exclude
     *             text data from searches in private appointments 
     */
    private static final class excludePrivateCalendarItems implements QueryOperation.RecurseCallback {
        public void recurseCallback(QueryOperation op) {
            if (op instanceof LuceneQueryOperation) {
                ((LuceneQueryOperation)op).addAndedClause(new TermQuery(new Term(LuceneFields.L_FIELD, CalendarItem.INDEX_FIELD_ITEM_CLASS_PRIVATE)), false);
            }
        }
    }
    
    /**
     * Go through the top-level query Union, look at the specific QueryTarget 
     * (local or remote server) for each Op in the union, wrap the remote 
     * targets in RemoteQueryOperations, and set them up.
     *
     * For the local targets, look at all the text-operations and figure 
     * out if private appointments need to be excluded 
     */
    private static QueryOperation handlePermissionChecks(UnionQueryOperation union, SoapProtocol proto, Mailbox mbox, OperationContext octxt, MailboxIndex mbidx, SearchParams params) 
    throws ServiceException, IOException {

        boolean hasRemoteOps = false;
        
        Set<Folder> visibleFolders = mbox.getVisibleFolders(octxt);
        
        //
        // Check to see if we need to filter out private appointment data
        boolean allowPrivateAccess = true;
        if (octxt != null) 
            mbox.getAccount().allowPrivateAccess(octxt.getAuthenticatedUser());

        // Since optimize() has already been run, we know that each of our ops
        // only has one target (or none).  Find those operations which have
        // an external target and wrap them in RemoteQueryOperations
        for (int i = union.mQueryOperations.size()-1; i >= 0; i--) { // iterate backwards so we can remove/add w/o screwing iteration
            QueryOperation op = union.mQueryOperations.get(i);

            QueryTargetSet targets = op.getQueryTargets();
            
            // this assertion OK because we have already distributed multi-target query ops
            // during the optimize() step
            assert(targets.countExplicitTargets() <= 1);
            
            // the assertion above is critical: the code below all assumes
            // that we only have ONE target (ie we've already distributed if necessary)

            if (targets.hasExternalTargets()) {
                union.mQueryOperations.remove(i);   

                hasRemoteOps = true;
                boolean foundOne = false;

                // find a remoteOp to add this one to
                for (QueryOperation tryIt : union.mQueryOperations) {
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
                    union.mQueryOperations.add(i, remoteOp);
                }
            } else {
                // local target
                if (!allowPrivateAccess) 
                    op.depthFirstRecurse(new excludePrivateCalendarItems());
                
                if (visibleFolders != null) {
                    if (visibleFolders.size() == 0) {
                        union.mQueryOperations.remove(i);
                        ZimbraLog.index.debug("Query changed to NULL_QUERY_OPERATION, no visible folders");
                        union.mQueryOperations.add(i, new NullQueryOperation());
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
        
        if (hasRemoteOps) {
            // if we actually have remote operations, then we need to call setup() on each
            for (QueryOperation toSetup : union.mQueryOperations) {
                if (toSetup instanceof RemoteQueryOperation) {
                    try {
                        RemoteQueryOperation remote = (RemoteQueryOperation) toSetup;
                        remote.setup(proto, octxt.getAuthenticatedUser(), octxt.isUsingAdminPrivileges(), params);
                    } catch(Exception e) {
                        ZimbraLog.index.info("Ignoring "+e+" during RemoteQuery generation for "+union.toString());
                    }
                }
            }
        }
        
        assert(union.mQueryOperations.size() > 0);
        
//        if (union.mQueryOperations.size() == 1) {
//            // this can happen if we replaced ALL of our operations with a single remote op...
//            return union.mQueryOperations.get(0).optimize(mbox);
//        }
//        
        
        return union.optimize(mbox);
    }
    
    
    public String toString() {
        String ret = "ZQ:\n";

        if (mClauses.size() > 0) {
            BaseQuery head = (BaseQuery)mClauses.get(0); 
            for (BaseQuery q = head; q != null; q = q.getNext()) {
                ret+=q.toString(1)+"\n";
            }
        }
        return ret;
    }
    
    public String toQueryString() {
        if (mOp == null)
            return "";
        else
            return mOp.toQueryString();
    }

}
