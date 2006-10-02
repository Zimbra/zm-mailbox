/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.zimbra.cs.index.MailboxIndex.SortBy;
import com.zimbra.cs.index.queryparser.Token;
import com.zimbra.cs.index.queryparser.ZimbraQueryParser;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.index.queryparser.ZimbraQueryParserConstants;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.SearchFolder;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.SoapProtocol;

/**
 * @author tim
 *
 * Represents a search query.  Call flow is simple:
 *    -- Constructor() -- parse the query string
 *    -- execute() -- Begin the search, get the ZimbraQueryResults iterator
 */
public final class ZimbraQuery {
    /**
     * @author tim
     * 
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
         * Type must be one of the types returned by canExec() 
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


        protected int canExec() {
            assert(false);
            return 0;
        }

    } 

    public static class ConvQuery extends BaseQuery
    {
        private int mConvId;
        public ConvQuery(Analyzer analyzer, int modifier, String target) {
            super(modifier, ZimbraQueryParser.CONV);
            mConvId = 0;
            mConvId = Integer.parseInt(target);
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation op = DBQueryOperation.Create();

            op.addConvId(mConvId, calcTruth(truth));

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

        public DateQuery(Analyzer analyzer, int qType)
        {
            super(0,qType);
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation op = DBQueryOperation.Create();

            truth = calcTruth(truth);

            long lowDate =-1, highDate = -1;
            long myTime = mDate.getTime();

            switch(getQueryType()) {
                case ZimbraQueryParser.BEFORE:
                    lowDate = -1;
                    highDate = myTime;
                    break;
                case ZimbraQueryParser.AFTER:
                    lowDate = myTime;
                    highDate = -1;
                    break;
                case ZimbraQueryParser.DATE:
                    lowDate = myTime;
                    highDate = mEndDate.getTime();
                    break;
            }

            op.addDateClause(lowDate, highDate, truth);
            return op;
        }

        protected static final String ABSDATE_YFIRST_PATTERN 
        = "(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})";

        protected static final String ABSDATE_YLAST_PATTERN 
        = "(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})";

        protected static final String RELDATE_PATTERN 
        = "([+-])([0-9]+)([mhdwy][a-z]*)?";


        protected static final Pattern sAbsYFirstPattern = Pattern.compile(ABSDATE_YFIRST_PATTERN);
        protected static final Pattern sAbsYLastPattern = Pattern.compile(ABSDATE_YLAST_PATTERN);
        protected static final Pattern sRelDatePattern = Pattern.compile(RELDATE_PATTERN);

        public void parseDate(int modifier, String s, TimeZone tz, Locale locale) throws com.zimbra.cs.index.queryparser.ParseException
        {
            
            //          * DATE:  absolute-date = mm/dd/yyyy | yyyy/dd/mm  OR
            //          *        relative-date = [+/-]nnnn{minute,hour,day,week,month,year}
            //          *        (need to figure out how to represent "this week", "last
            //          *        week", "this month", etc)

            mDate = null;
            mEndDate = null;
            
            //
            // Step 1: special-case 'yesterday' and 'today' and also map
            //    DATE/DAY/WEEK/MONTH/YEAR to general "date" -- basically we're
            //    undoing some of the work the parser did, because it is easier to
            //    do all the date parsing here
            //
            int origType = getQueryType();
            switch (origType) {
                case ZimbraQueryParser.DATE:
                case ZimbraQueryParser.DAY:
                    setQueryType(ZimbraQueryParser.DATE);

                    if (s.equalsIgnoreCase("today")) 
                    {
                        GregorianCalendar cal = new GregorianCalendar();
                        if (tz != null)
                            cal.setTimeZone(tz);
                        cal.setTime(new Date());

                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);

                        mDate = cal.getTime();

                        cal.add(Calendar.DATE, 1);
                        mEndDate = cal.getTime(); 

                    } else if (s.equalsIgnoreCase("yesterday")) {
                        GregorianCalendar cal = new GregorianCalendar();
                        if (tz != null)
                            cal.setTimeZone(tz);
                        
                        cal.setTime(new Date());

                        cal.set(Calendar.HOUR_OF_DAY, 0);
                        cal.set(Calendar.MINUTE, 0);
                        cal.set(Calendar.SECOND, 0);
                        cal.add(Calendar.DATE, -1);

                        mDate = cal.getTime();
                        cal.add(Calendar.DATE, 1);
                        mEndDate = cal.getTime();
                    }
                    break;
                case ZimbraQueryParser.WEEK:
                    setQueryType(ZimbraQueryParser.DATE);
                    break;
                case ZimbraQueryParser.MONTH:
                    setQueryType(ZimbraQueryParser.DATE);
                    break;
                case ZimbraQueryParser.YEAR:
                    setQueryType(ZimbraQueryParser.DATE);
                    break;
            }

            if (mDate == null) {
                //
                // Now, do the actual parsing.  There are two cases: a relative date 
                // or an absolute date.  
                //
                {
                    Matcher m;
                    String mod = null;
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

                        int field = 0;

                        if (m.start(3) == -1) {
                            // no period specified -- use the defualt for the current operator
                            switch (origType) {
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
                                    field = Calendar.HOUR;
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
                        }
                        //                System.out.println("RELDATE: MOD=\""+mod+"\" AMT=\""+reltime+"\" TYPE="+type);

                        GregorianCalendar cal = new GregorianCalendar();
                        if (tz != null)
                            cal.setTimeZone(tz);

                        cal.setTime(new Date());

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
                        
                        switch (origType) {
                            case ZimbraQueryParser.DATE:
                            case ZimbraQueryParser.DAY:
                                cal.add(Calendar.DATE,1);
                                mEndDate = cal.getTime();
                                break;
                            case ZimbraQueryParser.WEEK:
                                cal.add(Calendar.WEEK_OF_YEAR,1);
                                mEndDate = cal.getTime();
                                break;
                            case ZimbraQueryParser.MONTH:
                                cal.add(Calendar.MONTH,1);
                                mEndDate = cal.getTime();
                                break;
                            case ZimbraQueryParser.YEAR:
                                cal.add(Calendar.YEAR,1);
                                mEndDate = cal.getTime();
                                break;
                        } 
                    } // else (relative/absolute check)
                } // (if mDate!=null)

                if (mLog.isDebugEnabled()) {
                    if (mEndDate == null) { mEndDate = mDate; };
                    mLog.debug("Parsed date range to: ("+mDate.toString()+"-"+mEndDate.toString()+")");
                }
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

        public static BaseQuery Create(Mailbox mailbox, Analyzer analyzer, int modifier, Integer folderId) throws ServiceException {
//          try {
            if (folderId < 0) {
                InQuery toRet = new InQuery(mailbox, analyzer, folderId);
                toRet.mSpecialTarget = folderId;
                return toRet;
            } else {
                Folder folder = mailbox.getFolderById(null, folderId.intValue());
                return new InQuery(mailbox, analyzer, modifier, folder);
            }
//          } catch (NoSuchItemException nsie) {
//          TIM: why is this here?  Seems strange to eat this error!
//          return null;
//          } 
        }

        public static BaseQuery Create(Mailbox mailbox, Analyzer analyzer, int modifier, String folderName) throws ServiceException {
//          try {
            Folder folder = mailbox.getFolderByPath(null, folderName);
            return new InQuery(mailbox, analyzer, modifier, folder);
//          } catch (NoSuchItemException nsie) {
//          return null;
//          }
        }
        private Folder mFolder;
        private ItemId mInId;
        private Integer mSpecialTarget = null;
        private Mailbox mMailbox;

        public InQuery(Mailbox mailbox, Analyzer analyzer, int modifier, ItemId itemId) {
            super(modifier, ZimbraQueryParser.IN);
            mFolder = null;
            mInId = itemId;
            mMailbox = mailbox;
        }

        public InQuery(Mailbox mailbox, Analyzer analyzer, int modifier, Folder folder) {
            super(modifier, ZimbraQueryParser.IN);
            assert(folder != null);
            mFolder = folder;
            mMailbox = mailbox;
        }
        
        private InQuery(Mailbox mailbox, Analyzer analyzer, int modifier) {
            super(modifier, ZimbraQueryParser.IN);
            mMailbox = mailbox;
        }
        
        protected QueryOperation getLocalFolderOperation(Mailbox mbox) {

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
                            System.out.println("Adding: "+f.toString());
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
        
        protected QueryOperation getRemoteFolderOperation(boolean truth, Mailbox mbox) {
            
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
                
                for (Iterator<Folder> iter = allFolders.iterator(); iter.hasNext();) {
                    Folder f = iter.next();
                    if (!(f instanceof Mountpoint))
                        iter.remove();
                    else {
                        Mountpoint mpt = (Mountpoint)f;
//                        if (mpt. .getAccount() == mbox.getAccount()) {
//                            iter.remove();
//                        }
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
                            DBQueryOperation dbop = new DBQueryOperation();
                            outer.add(dbop);
                            dbop.addInClause(f, truth);
                        }
                        return outer;
                    } else {
                        IntersectionQueryOperation outer = new IntersectionQueryOperation();
                        
                        for (Folder f : allFolders) {
                            DBQueryOperation dbop = new DBQueryOperation();
                            outer.addQueryOp(dbop);
                            dbop.addInClause(f, truth);
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
//                            DBQueryOperation dbOp = DBQueryOperation.Create();
//                            dbOp.addLocalFolderClause(true, mMailbox);
//                            return dbOp;
                            return getLocalFolderOperation(mMailbox);
                        }
                    } else {
                        if (mSpecialTarget == IN_REMOTE_FOLDER) {
                            return getLocalFolderOperation(mMailbox);
//                            
//                            DBQueryOperation dbOp = DBQueryOperation.Create();
//                            dbOp.addLocalFolderClause(true, mMailbox);
//                            return dbOp;
                        } else {
                            assert(mSpecialTarget == IN_LOCAL_FOLDER);
                            return getRemoteFolderOperation(true, mMailbox);
                        }
                    }
                }
            }

            DBQueryOperation dbOp = DBQueryOperation.Create();
            if (mFolder != null) {
                dbOp.addInClause(mFolder, calcTruth(truth));
            } else if (mInId != null) {
                dbOp.addInIdClause(mInId, calcTruth(truth));
            } else {
                assert(false);
            }

            return dbOp;
        }

        public String toString(int expLevel) {
            if (mSpecialTarget != null) {
                String toRet = super.toString(expLevel)+",IN:";
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
                ",IN:"+(mInId!=null ? mInId.toString() :
                    (mFolder!=null?mFolder.getName():"ANY_FOLDER"))
                    +")";
            }
        }
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

        protected int canExec() {
            assert(false);
            return 0;
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

    public static class DBTypeQuery extends BaseQuery
    {
        private byte mType;

        public DBTypeQuery(Analyzer analyzer, int modifier, byte type) 
        {
            super(modifier, ZimbraQueryParser.IS);
            mType = type;
        }

        public static DBTypeQuery IS_INVITE(Mailbox mailbox, Analyzer analyzer, int modifier) throws ServiceException {
//          return new DBTypeQuery(analyzer, modifier, MailItem.TYPE_INVITE);
            return new DBTypeQuery(analyzer, modifier, MailItem.TYPE_MESSAGE);
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation dbOp = DBQueryOperation.Create();

            dbOp.addTypeClause(mType, calcTruth(truth));

            return dbOp;
        }

        public String toString(int expLevel) {
            return super.toString(expLevel)+","+mType+")";
        }
    }

    public static class ItemQuery extends BaseQuery
    {
        public static BaseQuery Create(Analyzer analyzer, int modifier, String str) 
        throws ServiceException {
            boolean allQuery = false;
            boolean noneQuery = false;
            List<Integer> itemIds = new ArrayList<Integer>();

            if (str.equalsIgnoreCase("all")) {
                allQuery = true;
            } else if (str.equalsIgnoreCase("none")) {
                noneQuery = true;
            } else {
                String[] items = str.split(",");
                for (int i = 0; i < items.length; i++) {
                    if (items[i].length() > 0) {
                        Integer id = Integer.decode(items[i].trim());
                        itemIds.add(id);
                    }
                }
                if (itemIds.size() == 0) {
                    noneQuery = true;
                }
            }

            return new ItemQuery(analyzer, modifier, allQuery, noneQuery, itemIds);
        }

        private boolean mIsAllQuery;
        private boolean mIsNoneQuery;
        private List mItemIds;

        public ItemQuery(Analyzer analyzer, int modifier, boolean all, boolean none, List ids) 
        {
            super(modifier, ZimbraQueryParser.ITEM);
            mIsAllQuery = all;
            mIsNoneQuery = none;
            mItemIds = ids;
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation dbOp = DBQueryOperation.Create();

            truth = calcTruth(truth);

            if (truth&&mIsAllQuery || !truth&&mIsNoneQuery) {
                // adding no constraints should match everything...
            } else if (truth&&mIsNoneQuery || !truth&&mIsAllQuery) {
                return new NullQueryOperation();
            } else {
                for (Iterator iter = mItemIds.iterator(); iter.hasNext();) {
                    Integer cur = (Integer)iter.next();
                    dbOp.addItemIdClause(cur, truth);
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
                for (Iterator iter = mItemIds.iterator(); iter.hasNext();) {
                    Integer cur = (Integer)iter.next();
                    toRet.append(","+cur);
                }
            }
            return toRet.toString();
        }
    }

    public static class TextQuery extends BaseQuery
    {
        private ArrayList<String> mTokens;

        // simple prefix wildcards
        private String mWildcardPrefix;

        // for complicated wildcards
        private LinkedList<String> mOredTokens;
        private String mWildcardTerm;
        private String mOrigText;

        private static final int MAX_WILDCARD_TERMS = 500;

        public TextQuery(Mailbox mbox, Analyzer analyzer, int modifier, int qType, String text) throws ServiceException {
            super(modifier, qType);

            mOredTokens = new LinkedList<String>();
            mTokens = new ArrayList<String>(1);
            mWildcardTerm = null;
            mWildcardPrefix = null;
            mOrigText = text;

            TokenStream source = analyzer.tokenStream(QueryTypeString(qType), new StringReader(text));
            org.apache.lucene.analysis.Token t;

            while(true) {
                try {
                    t = source.next();
                }
                catch (IOException e) {
                    t = null;
                }
                if (t == null)
                    break;
                mTokens.add(t.termText());
            }
            try {
                source.close();
            }
            catch (IOException e) {
                // ignore
            }

            // must look at original text here b/c analyzer strips *'s
            if (text.length() > 0 && text.charAt(text.length()-1) == '*')
            {
                // wildcard query!
                String wcToken;

                if (mTokens.size() > 0)
                    wcToken = mTokens.remove(mTokens.size()-1);
                else
                    wcToken = text;

                // the field may have a tokenizer which removed the *
                if (wcToken.indexOf('*') < 0)
                    wcToken = wcToken+'*';

//              // strip the '*' from the end
//              int starIdx = wcToken.indexOf('*');
//              if (starIdx >= 0)
//              wcToken = wcToken.substring(0, starIdx);

//              if (wcToken.length() > 0) 
//              mWildcardPrefix = wcToken;


                if (wcToken.length() >= 1) {
                    mWildcardTerm = wcToken;
                    MailboxIndex mbidx = mbox.getMailboxIndex();
                    List<String> expandedTokens = mbidx.expandWildcardToken(QueryTypeString(qType), wcToken, MAX_WILDCARD_TERMS);

                    for (String token : expandedTokens) {
                        mOredTokens.add(token);
                    }
                }
            }

//          MailboxIndex mbidx = mbox.getMailboxIndex();
//          for (String token : mTokens) {
//          mbidx.suggestSpelling(QueryTypeString(qType), token);
//          }
        }

        protected QueryOperation getQueryOperation(boolean truth) {
            if (mTokens.size() <= 0 && mOredTokens.size()<=0 && mWildcardPrefix==null) {
                // if we have no tokens, that is usually because the analyzer removed them
                // -- the user probably queried for a stop word like "a" or "an" or "the"
                // 
                // we can't simply ignore this query, however -- we have to put a null
                // query into the query list, otherwise conjunctions will get confused...so
                // we pass NULL to addClause which will add a blank clause for us...
                return new NoTermQueryOperation();
            } else {
                LuceneQueryOperation lop = LuceneQueryOperation.Create();
                String fieldName = QueryTypeString(getQueryType());

                if (mTokens.size() == 1) {
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

                if (mWildcardPrefix != null) {
                    lop.addClause("", new PrefixQuery(new Term(fieldName, mWildcardPrefix)), calcTruth(truth));
                }

                if (mOredTokens.size() > 0) {
                    // probably don't need to do this here...can probably just call addClause
                    BooleanQuery orQuery = new BooleanQuery();
                    for (String token : mOredTokens)
                        orQuery.add(new TermQuery(new Term(fieldName, token)), false, false);

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





    private static Log mLog = LogFactory.getLog(ZimbraQuery.class);

    private static final int SUBQUERY_TOKEN = 9999;

    private AbstractList mClauses;
    private ParseTree.Node mParseTree = null;
    private QueryOperation mOp;
    private byte[] mTypes;
    private SortBy mSearchOrder;
    private int mChunkSize;
    private Mailbox mMbox;
    private ZimbraQueryResults mResults;
    private boolean mPrefetch;
    private Mailbox.SearchResultMode mMode;
    private java.util.TimeZone mTimeZone;
    private Locale mLocale;
    
    private static String[] unquotedTokenImage;

    static {
        unquotedTokenImage = new String[ZimbraQueryParserConstants.tokenImage.length];
        for (int i = 0; i < ZimbraQueryParserConstants.tokenImage.length; i++) {
            unquotedTokenImage[i] = ZimbraQueryParserConstants.tokenImage[i].substring(1, ZimbraQueryParserConstants.tokenImage[i].length()-1);
        }

    }

    private static String QueryTypeString(int qType) {
        switch (qType) {
            case ZimbraQueryParser.CONTENT:    return LuceneFields.L_CONTENT;
            case ZimbraQueryParser.FROM:       return LuceneFields.L_H_FROM;
            case ZimbraQueryParser.TO:         return LuceneFields.L_H_TO;
            case ZimbraQueryParser.CC:         return LuceneFields.L_H_CC;
            case ZimbraQueryParser.SUBJECT:    return LuceneFields.L_H_SUBJECT;
            case ZimbraQueryParser.IN:         return "in";
            case ZimbraQueryParser.HAS:        return "has";
            case ZimbraQueryParser.FILENAME:   return LuceneFields.L_FILENAME;
            case ZimbraQueryParser.TYPE:       return LuceneFields.L_MIMETYPE;
            case ZimbraQueryParser.ATTACHMENT: return LuceneFields.L_ATTACHMENTS;
            case ZimbraQueryParser.IS:         return "IS";
            case ZimbraQueryParser.DATE:       return LuceneFields.L_DATE;
            case ZimbraQueryParser.AFTER:      return "after";
            case ZimbraQueryParser.BEFORE:     return "before";
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
            case ZimbraQueryParser.CONV_START: return "conv-start";
            case ZimbraQueryParser.CONV_END:   return "conv-end";
            case ZimbraQueryParser.AUTHOR:     return "author";
            case ZimbraQueryParser.TITLE:      return "title";
            case ZimbraQueryParser.KEYWORDS:   return "keywords";
            case ZimbraQueryParser.COMPANY:    return "company";
            case ZimbraQueryParser.METADATA:   return "metadata";
            case ZimbraQueryParser.ITEM:       return "itemId";
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

    /**
     * Parse a query string and build a query plan from it.
     * 
     * @param queryString
     * @param mbox
     * @param types
     * @param searchOrder
     * @param includeTrash
     * @param includeSpam
     * @param chunkSize
     * @throws ParseException
     * @throws ServiceException
     */
    public ZimbraQuery(String queryString, java.util.TimeZone tz, Locale locale, Mailbox mbox, byte[] types, SortBy searchOrder, boolean includeTrash, boolean includeSpam, int chunkSize, boolean prefetch, Mailbox.SearchResultMode mode) 
    throws ParseException, ServiceException
    {
        mMbox = mbox;
        mPrefetch = prefetch;
        mMode = mode;
        mTimeZone = tz;
        mLocale = locale;

        //
        // Step 1: parse the text using the JavaCC parser
        ZimbraQueryParser parser = new ZimbraQueryParser(new StringReader(queryString));
        mbox.getMailboxIndex().initAnalyzer(mbox);
        parser.init(mbox.getMailboxIndex().getAnalyzer(), mMbox, mTimeZone, mLocale);
        mClauses = parser.Parse();

        String sortByStr = parser.getSortByStr();
        if (sortByStr != null)
            handleSortByOverride(sortByStr);

        //
        // Step 2: build a parse tree and push all the "NOT's" down to the
        // bottom level -- this is because we cannot invert result sets 
        if (ParseTree.SPEW) System.out.println("QueryString: "+queryString);
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
        mSearchOrder = searchOrder;
        mTypes = types;
        mChunkSize = chunkSize;
        mOp = null;

        //
        // handle the special "sort:" tag in the search string
        if (mSortByOverride != null) {
            if (mLog.isDebugEnabled())
                mLog.debug("Overriding SortBy parameter to execute ("+searchOrder.toString()+") w/ specification from QueryString: "+mSortByOverride.toString());

            mSearchOrder = mSortByOverride;
        }

        BaseQuery head = getHead();
        if (null != head) {

            // this generates all of the query operations
            mOp = mParseTree.getQueryOperation();

            if (mLog.isDebugEnabled()) {
                mLog.debug("OP="+mOp.toString());
            }

            // optimize the query down
            mOp = mOp.optimize(mMbox);
            if (mOp == null) {
                mOp = new NoTermQueryOperation();
            }
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
    
//    
//    void checkPermissions(Mailbox mbox, Mailbox.OperationContext octxt) throws ServiceException {
//        Set<Folder> folders = mbox.getVisibleFolders(octxt);
//        if (folders == null) {
//            return;
//        } else {
//            // Since optimize() has already been run, we know that each of our ops
//            // only has one target (or none).  
//            
//            if (folders.size() == 0)
//                return new NullQueryOperation();
//            
//            IntersectionQueryOperation toRet = new IntersectionQueryOperation();
//            toRet.addQueryOp(this);
//            
//            UnionQueryOperation union = new UnionQueryOperation();
//            toRet.addQueryOp(union);
//            
//            for (Folder f : folders) {
//                DBQueryOperation newOp = DBQueryOperation.Create();
//                union.add(newOp);
//                newOp.addInClause(f, false);
//            }
//            
//            return toRet.optimize(mbox);
//        }
//    }

    public void executeRemoteOps(SoapProtocol proto, OperationContext octxt) throws ServiceException, IOException
    {
        if (mOp!= null) {
            QueryTargetSet targets = mOp.getQueryTargets();
            assert(mOp instanceof UnionQueryOperation || targets.countExplicitTargets() <=1);
            MailboxIndex mbidx = mMbox.getMailboxIndex();

            if (targets.size() > 1) {
                UnionQueryOperation union = (UnionQueryOperation)mOp;
                mOp = union.runRemoteSearches(proto, mMbox, octxt, mbidx, mTypes, mSearchOrder, 0, mChunkSize, mMode);
            } else {
                if (targets.hasExternalTargets()) {
                    RemoteQueryOperation remote = new RemoteQueryOperation();
                    remote.tryAddOredOperation(mOp);
                    remote.setup(proto, octxt.getAuthenticatedUser(), mTypes, mSearchOrder, 0, mChunkSize, mMode);
                    mOp = remote;
                } else {
                    // 1 local target...HACK: for now we'll temporarily wrap it in a UnionQueryOperation,
                    // then call union.runRemoteSearches() --- that way we don't have to duplicate the
                    // important code there (permissions checks).  FIXME!
                    UnionQueryOperation union = new UnionQueryOperation();
                    union.add(mOp);
                    mOp = union.runRemoteSearches(proto, mMbox, octxt, mbidx, mTypes, mSearchOrder, 0, mChunkSize, mMode);
                    mOp = mOp.optimize(mMbox);
                }
            }
        }
    }

    /**
     * Runs the search and gets an open result set.
     * 
     * WARNING: You **MUST** call ZimbraQueryResults.doneWithSearchResults() when you are done with them!
     * 
     * @param mbox
     * @return Open ZimbraQueryResults -- YOU MUST CALL doneWithSearchResults() to release the results set! 
     * @throws ServiceException
     * @throws IOException
     */
    public ZimbraQueryResults execute() throws ServiceException, IOException
    {
        MailboxIndex mbidx = mMbox.getMailboxIndex();

        if (ZimbraLog.index.isDebugEnabled()) {
            String str = this.toString() +" search([";
            for (int i = 0; i < mTypes.length; i++) {
                if (i > 0) {
                    str += ",";
                }
                str+=mTypes[i];
            }
            str += "]," + mSearchOrder + ")";
            ZimbraLog.index.debug(str);
        }

        if (mOp!= null) {
            if (mLog.isDebugEnabled())
                mLog.debug("OPERATION:"+mOp.toString());

            QueryTargetSet targets = mOp.getQueryTargets();
            assert(mOp instanceof UnionQueryOperation || targets.countExplicitTargets() <=1);
            assert(mResults == null);

            // if we've only got one target, and it is external, then mOp must be a RemoteQueryOp
            assert(targets.size() >1 || !targets.hasExternalTargets() || mOp instanceof RemoteQueryOperation);

            mResults = mOp.run(mMbox, mbidx, mTypes, mSearchOrder, mChunkSize, mPrefetch, mMode);
            mResults = new HitIdGrouper(mResults, mSearchOrder);
            return mResults;
        } else {
            mLog.debug("Operation optimized to nothing.  Returning no results");
        }

        return new EmptyQueryResults(mTypes, mSearchOrder, mMode);
    }

    public String toString() {
        String ret = "ZQ:\n";
        for (BaseQuery q = getHead(); q != null; q = q.getNext()) {
            ret+=q.toString(1)+"\n";
        }
        return ret;
    }

    private BaseQuery getHead() {
        if (mClauses.size() > 0) {
            return (BaseQuery)mClauses.get(0);
        } else {
            return null;
        }
    }
}
