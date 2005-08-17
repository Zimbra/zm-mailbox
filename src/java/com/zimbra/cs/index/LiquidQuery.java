/*
 * Created on Jul 6, 2004
 */
package com.zimbra.cs.index;

import java.io.IOException;
import java.io.StringReader;
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
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.zimbra.cs.index.queryparser.LiquidQueryParser;
import com.zimbra.cs.index.queryparser.ParseException;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Tag;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.service.ServiceException;



/**
 * @author tim
 * 
 * Very simple wrapper classes that each represent a node in the parse tree for the
 * query string.
 */
public final class LiquidQuery {
     
    
    
    /************************************************************************
     * 
     * BaseQuery
     * 
     ***********************************************************************/
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
            return mModifierType == LiquidQueryParser.MINUS;
        }

        protected final String modToString() {
            String modString = "";
            switch(mModifierType) {
              case LiquidQueryParser.PLUS:
                modString = "+";
                break;
              case LiquidQueryParser.MINUS:
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
        protected static HashMap mMap;
        
        static {
            mMap = new HashMap();

            addMapping(mMap, new String[] { "any" }  , "any");
            addMapping(mMap, new String[] { "application", "application/*"}  , "application");
            addMapping(mMap, new String[] { "bmp", "image/bmp" }  , "image/bmp");
            addMapping(mMap, new String[] { "gif", "image/gif" }  , "image/gif");
            addMapping(mMap, new String[] { "image", "image/*" }  , "image");
            addMapping(mMap, new String[] { "jpeg", "image/jpeg", }  , "image/jpeg");
            addMapping(mMap, new String[] { "excel", "application/vnd.ms-excel", "xls" }  , "application/vnd.ms-excel");
            addMapping(mMap, new String[] { "ppt", "application/vnd.ms-powerpoint"}  , "application/vnd.ms-powerpoint");
            addMapping(mMap, new String[] { "ms-tnef", "application/ms-tnef"}  , "application/ms-tnef");
            addMapping(mMap, new String[] { "word", "application/msword", "msword" }  , "application/msword");
            addMapping(mMap, new String[] { "none" }  , "none");
            addMapping(mMap, new String[] { "pdf", "application/pdf" }  , "application/pdf");
            addMapping(mMap, new String[] { "text", "text/*" }  , "text");
        }
        
        public AttachmentQuery(Analyzer analyzer, int modifier, String what) {
            super(modifier, LiquidQueryParser.TYPE, LuceneFields.L_ATTACHMENTS, lookup(mMap, what));
        }
        
        protected AttachmentQuery(int modifier, String luceneField, String what) {
            super(modifier, LiquidQueryParser.TYPE, luceneField, lookup(mMap, what));
        }
    }
    
    public static class ConjQuery extends BaseQuery
    {
    	private static final int AND = LiquidQueryParser.AND_TOKEN;
    	private static final int OR = LiquidQueryParser.OR_TOKEN;
    	
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
            super(modifier, LiquidQueryParser.CONV);
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
            case LiquidQueryParser.BEFORE:
                lowDate = -1;
                highDate = myTime;
                break;
            case LiquidQueryParser.AFTER:
                lowDate = myTime;
                highDate = -1;
                break;
            case LiquidQueryParser.DATE:
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
        = "(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})";
        
        protected static final String RELDATE_PATTERN 
        = "([+-])([0-9]+)([mhdwy][a-z]*)?";
        
        
        protected static final Pattern sAbsYFirstPattern = Pattern.compile(ABSDATE_YFIRST_PATTERN);
        protected static final Pattern sAbsYLastPattern = Pattern.compile(ABSDATE_YLAST_PATTERN);
        protected static final Pattern sRelDatePattern = Pattern.compile(RELDATE_PATTERN);
        
        public void parseDate(int modifier, String s) throws com.zimbra.cs.index.queryparser.ParseException
        {
            //          * DATE:  absolute-date = mm/dd/yyyy | yyyy/dd/mm  OR
            //          *        relative-date = [+/-]nnnn{minute,hour,day,week,month,year}
            //          *        (need to figure out how to represent "this week", "last
            //          *        week", "this month", etc)
            
            int origType = getQueryType();
            switch (origType) {
            case LiquidQueryParser.DATE:
            case LiquidQueryParser.DAY:
                setQueryType(LiquidQueryParser.DATE);
            
                if (s.equalsIgnoreCase("today")) 
                {
                    GregorianCalendar cal = new GregorianCalendar();
                    cal.setTime(new Date());
                    
                    cal.set(Calendar.HOUR_OF_DAY, 0);
                    cal.set(Calendar.MINUTE, 0);
                    cal.set(Calendar.SECOND, 0);
                    
                    mDate = cal.getTime();
                    
                    cal.add(Calendar.DATE, 1);
                    mEndDate = cal.getTime(); 
                    
                } else if (s.equalsIgnoreCase("yesterday")) {
                    GregorianCalendar cal = new GregorianCalendar();
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
            case LiquidQueryParser.WEEK:
                setQueryType(LiquidQueryParser.DATE);
            break;
            case LiquidQueryParser.MONTH:
                setQueryType(LiquidQueryParser.DATE);
            break;
            case LiquidQueryParser.YEAR:
                setQueryType(LiquidQueryParser.DATE);
            break;
            }
            
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
                        case LiquidQueryParser.DATE:
                        case LiquidQueryParser.DAY:
                            field = Calendar.DATE;
                        break;
                        case LiquidQueryParser.WEEK:
                            field = Calendar.WEEK_OF_YEAR;
                        break;
                        case LiquidQueryParser.MONTH:
                            field = Calendar.MONTH;
                        break;
                        case LiquidQueryParser.YEAR:
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
                    //
                    String yearStr = null;
                    String monthStr = null;
                    String dayStr = null;
                    
                    char first = s.charAt(0);
                    if (first == '-' || first == '+') {
                        s = s.substring(1);
                    }
                    m = sAbsYFirstPattern.matcher(s);
                    if (m.lookingAt()) {
                        yearStr = s.substring(m.start(1), m.end(1));
                        monthStr = s.substring(m.start(2), m.end(2));
                        dayStr = s.substring(m.start(3), m.end(3));
                    } else {
                        m = sAbsYLastPattern.matcher(s);
                        if (m.lookingAt()) {
                            monthStr = s.substring(m.start(1), m.end(1));
                            dayStr = s.substring(m.start(2), m.end(2));
                            yearStr = s.substring(m.start(3), m.end(3));
                        }
                    }
                    
                    int month = 0;
                    int day = 0;
                    int year = 0;
                    
                    Calendar cal = Calendar.getInstance();
                    
                    if (yearStr == null || monthStr == null || dayStr == null) {

                        int num;
                        
                        try {
                            num = Integer.parseInt(s);
                        } catch (Exception e) {
                            throw new ParseException("Error parsing date: \""+s+"\"");
                        }

                        switch (origType) {
                        case LiquidQueryParser.DATE:
                        case LiquidQueryParser.DAY:
                            throw new ParseException("Error parsing date: \""+s+"\"");
                        case LiquidQueryParser.WEEK:
                            throw new ParseException("Error parsing date: \""+s+"\"");
                        case LiquidQueryParser.MONTH:
                            throw new ParseException("Error parsing date: \""+s+"\"");
                        case LiquidQueryParser.YEAR:
                            month = 1;
                            day = 1;
                            year = num;
                        break;
                        }
                    } else {
                        
                        year = Integer.parseInt(yearStr);
                        if (year < 100) {
                            year += 2000;
                        }
                        
                        month = Integer.parseInt(monthStr);
                        day = Integer.parseInt(dayStr);
                    }
                    
                    if (mLog.isDebugEnabled()) {
                        mLog.debug("Setting to year="+year+" month="+month+" day="+day);
                    }
                    
                    // January is 0-indexed, so subtract 1
                    cal.set(year,month+(Calendar.JANUARY-1),day,0,0,0);
                    
                    mDate = cal.getTime();
                    
                    switch (origType) {
                    case LiquidQueryParser.DATE:
                    case LiquidQueryParser.DAY:
                        cal.add(Calendar.DATE,1);
                        mEndDate = cal.getTime();
                    break;
                    case LiquidQueryParser.WEEK:
                        cal.add(Calendar.WEEK_OF_YEAR,1);
                        mEndDate = cal.getTime();
                    break;
                    case LiquidQueryParser.MONTH:
                        cal.add(Calendar.MONTH,1);
                    mEndDate = cal.getTime();
                    break;
                    case LiquidQueryParser.YEAR:
                        cal.add(Calendar.YEAR,1);
                        mEndDate = cal.getTime();
                    break;
                    }
                    
                }
                
                if (mLog.isDebugEnabled()) {
                    if (mEndDate == null) { mEndDate = mDate; };
                    mLog.debug("Parsed date range to: ("+mDate.toString()+"-"+mEndDate.toString()+")");
                }
            }
        }
        
        public String toString(int expLevel) {
            String str;
            switch (getQueryType()) {
            case LiquidQueryParser.BEFORE:
                str = "BEFORE";
            break;
            case LiquidQueryParser.AFTER:
                str = "AFTER";
            break;
            case LiquidQueryParser.DATE:
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
            op.addClause(q,calcTruth(truth));
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
        protected static HashMap mMap;
        
        static {
            mMap = new HashMap();
            
            addMapping(mMap, new String[] { "attachment", "att" }  , "any");
            addMapping(mMap, new String[] { "phone" }              , "phone");
            addMapping(mMap, new String[] { "u.po" }               , "u.po");
            addMapping(mMap, new String[] { "ssn" }                , "ssn");
            addMapping(mMap, new String[] { "url" }                , "url");
        }
        
        public HasQuery(Analyzer analyzer, int modifier, String what) {
        	super(modifier, LiquidQueryParser.HAS, LuceneFields.L_OBJECTS, lookup(mMap, what));
        }
	}
    
    public static class InQuery extends BaseQuery
	{
        public static final Integer IN_ANY_FOLDER = new Integer(-2);
        
        public static InQuery Create(Mailbox mailbox, Analyzer analyzer, int modifier, Integer folderId) throws ServiceException {
            try {
                if (folderId == IN_ANY_FOLDER) {
                    return new InQuery(mailbox, analyzer, modifier);
                } else { 
                    Folder folder = mailbox.getFolderById(folderId.intValue());
                    return new InQuery(mailbox, analyzer, modifier, folder);
                }
            } catch (NoSuchItemException nsie) {
                return null;
            }
        }
        
        public static InQuery Create(Mailbox mailbox, Analyzer analyzer, int modifier, String folderName) throws ServiceException {
            try {
                Folder folder = mailbox.getFolderByPath(folderName);
                return new InQuery(mailbox, analyzer, modifier, folder);
            } catch (NoSuchItemException nsie) {
                return null;
            }
        }
        private Folder mFolder;

        public InQuery(Mailbox mailbox, Analyzer analyzer, int modifier, Folder folder) {
            super(modifier, LiquidQueryParser.IN);
            assert(folder != null);
            mFolder = folder;
        }

        /**
         * Matches something in ANY folder
         * 
         * @param mailbox
         * @param analyzer
         * @param modifier
         */
        public InQuery(Mailbox mailbox, Analyzer analyzer, int modifier) {
            super(modifier, LiquidQueryParser.IN);
            mFolder = null;
        }
       
        protected QueryOperation getQueryOperation(boolean truth) {
            DBQueryOperation dbOp = DBQueryOperation.Create();
            
            if (mFolder != null) {
                dbOp.addInClause(mFolder, calcTruth(truth));
            } else {
                dbOp.addAnyFolderClause(calcTruth(truth));
            }

            return dbOp;
        }
       
        public String toString(int expLevel) {
            return super.toString(expLevel)+",IN:"+(mFolder!=null?mFolder.toString():"ANY_FOLDER")+")";
        }
	}

    public abstract static class LuceneTableQuery extends BaseQuery
    {
        protected static void addMapping(HashMap map, String[] array, String value) {
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
            op.addClause(q,calcTruth(truth));
            
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
                setQueryType(LiquidQueryParser.BIGGER);
                mSizeStr = mSizeStr.substring(1);
            } else if (ch == '<') {
                setQueryType(LiquidQueryParser.SMALLER);
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
            // n+{b,kb,mb,gb}    // default is kb?
            int multiplier = 1;
            switch (typeChar) {
            case 'k':
                multiplier = 1024;
                break;
            case 'm':
                multiplier = 1024*1024;
                break;
            case 'g':
                multiplier = 1024*1024*1024;
                break;
            }
            
            if (multiplier > 1) {
                mSizeStr = mSizeStr.substring(0,mSizeStr.length()-1);
            }


            mSize = Integer.parseInt(mSizeStr) * multiplier;
            
//            System.out.println("Size of \""+size+"\" parsed to "+mSize);
            
            mSizeStr = LiquidAnalyzer.SizeTokenFilter.EncodeSize(mSize);
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
            case LiquidQueryParser.BIGGER:
                highest = -1;
                lowest = mSize;
                break;
            case LiquidQueryParser.SMALLER:
                highest = mSize;
                lowest = -1;
                break;
            case LiquidQueryParser.SIZE:
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
            super(modifier, LiquidQueryParser.TAG);
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
            super(modifier, LiquidQueryParser.IS);
            mType = type;
        }
        
        public static DBTypeQuery IS_INVITE(Mailbox mailbox, Analyzer analyzer, int modifier) throws ServiceException {
//            return new DBTypeQuery(analyzer, modifier, MailItem.TYPE_INVITE);
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
        public static BaseQuery Create(Mailbox mailbox, Analyzer analyzer, int modifier, String str) 
        throws ServiceException {
            boolean allQuery = false;
            boolean noneQuery = false;
            List itemIds = new ArrayList();
            
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
            
            return new ItemQuery(analyzer, mailbox, modifier, allQuery, noneQuery, itemIds);
        }
        
        private boolean mIsAllQuery;
        private boolean mIsNoneQuery;
        private List mItemIds;
        
        public ItemQuery(Analyzer analyzer, Mailbox mbx, int modifier, boolean all, boolean none, List ids) 
        {
            super(modifier, LiquidQueryParser.ITEM);
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
        private ArrayList mTokens;
        public TextQuery(Analyzer analyzer, int modifier, int qType, String text) {
            super(modifier, qType);
            
            mTokens = new ArrayList(1);
            
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
        }
        
        protected QueryOperation getQueryOperation(boolean truth) {
            LuceneQueryOperation lop = LuceneQueryOperation.Create();

            Query q = null;
            if (mTokens.size() <= 0) {
                // if we have no tokens, that is usually because the analyzer removed them
                // -- the user probably queried for a stop word like "a" or "an" or "the"
                // 
                // we can't simply ignore this query, however -- we have to put a null
                // query into the query list, otherwise conjunctions will get confused...so
                // we pass NULL to addClause which will add a blank clause for us...
                return new NoTermQueryOperation();
            } else {
                if (mTokens.size() == 1) {
                    String fieldName = QueryTypeString(getQueryType());
                    String queryTerm = (String)mTokens.get(0);
                    
                    q = new TermQuery(new Term(fieldName, queryTerm));
                } else {
                    PhraseQuery p = new PhraseQuery();
                    p.setSlop(0); // TODO configurable?
                    String fieldName = QueryTypeString(getQueryType());
                    for (int i=0; i<mTokens.size(); i++) {
                        p.add(new Term(fieldName, (String) mTokens.get(i)));
                    }
                    q=p;
                }
            }
            lop.addClause(q,calcTruth(truth));
            
            return lop;
        }
        
        public String toString(int expLevel) {
            String ret = super.toString(expLevel)+",";
            for (int i = 0; i < mTokens.size(); i++) {
            	ret+=","+((String)mTokens.get(i)).toString();
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

//////////////////////////////////////////////////////////////////////    
//////////////////////////////////////////////////////////////////////    
//////////////////////////////////////////////////////////////////////
    
    private static Log mLog = LogFactory.getLog(LiquidQuery.class);

    private static final int SUBQUERY_TOKEN = 9999;

    private static String QueryTypeString(int qType) {
        switch (qType) {
          case LiquidQueryParser.CONTENT:    return LuceneFields.L_CONTENT;
          case LiquidQueryParser.FROM:       return LuceneFields.L_H_FROM;
          case LiquidQueryParser.TO:         return LuceneFields.L_H_TO;
          case LiquidQueryParser.CC:         return LuceneFields.L_H_CC;
          case LiquidQueryParser.SUBJECT:    return LuceneFields.L_H_SUBJECT;
          case LiquidQueryParser.IN:         return "in";
          case LiquidQueryParser.HAS:        return "has";
          case LiquidQueryParser.FILENAME:   return LuceneFields.L_FILENAME;
          case LiquidQueryParser.TYPE:       return LuceneFields.L_MIMETYPE;
          case LiquidQueryParser.ATTACHMENT: return LuceneFields.L_ATTACHMENTS;
          case LiquidQueryParser.IS:         return "IS";
          case LiquidQueryParser.DATE:       return LuceneFields.L_DATE;
          case LiquidQueryParser.AFTER:      return "after";
          case LiquidQueryParser.BEFORE:     return "before";
          case LiquidQueryParser.SIZE:       return "SIZE";
          case LiquidQueryParser.BIGGER:     return "BIGGER";
          case LiquidQueryParser.SMALLER:    return "SMALLER";
          case LiquidQueryParser.TAG:        return "TAG";
          case LiquidQueryParser.MY:         return "MY";
          case LiquidQueryParser.MESSAGE:    return "MESSAGE";
          case LiquidQueryParser.CONV:       return "CONV";
          case LiquidQueryParser.CONV_COUNT: return "CONV-COUNT";
          case LiquidQueryParser.CONV_MINM:  return "CONV_MINM";
          case LiquidQueryParser.CONV_MAXM:  return "CONV_MAXM";
          case LiquidQueryParser.CONV_START: return "conv-start";
          case LiquidQueryParser.CONV_END:   return "conv-end";
          case LiquidQueryParser.AUTHOR:     return "author";
          case LiquidQueryParser.TITLE:      return "title";
          case LiquidQueryParser.KEYWORDS:   return "keywords";
          case LiquidQueryParser.COMPANY:    return "company";
          case LiquidQueryParser.METADATA:   return "metadata";
          case LiquidQueryParser.ITEM:       return "itemId";
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
            public ArrayList mNodes = new ArrayList();
            
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
                    ArrayList newNodes = new ArrayList();
                    for (Iterator cur = mNodes.iterator(); cur.hasNext();) {
                        Node n = (Node)cur.next();
                        
                        newNodes.add(n.simplify());
                    }
                    mNodes = newNodes;
                    
                    // now, see if any of our subops can be trivially combined with us
                    newNodes = new ArrayList();
                    for (Iterator cur = mNodes.iterator(); cur.hasNext();) {
                        Node n = (Node)cur.next();
                        
                        boolean addIt = true;
                        
                        if (n instanceof OperatorNode) {
                            OperatorNode opn = (OperatorNode)n;
                            if (opn.mKind == mKind && opn.mTruthFlag == true) {
                                addIt = false;
                                simplifyAgain = true;
                                for (Iterator opIter = opn.mNodes.iterator(); opIter.hasNext();) {
                                    newNodes.add(opIter.next());
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
                LiquidQuery.BaseQuery q = (LiquidQuery.BaseQuery)iter.next();
                
                if (q instanceof LiquidQuery.ConjQuery) {
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
	
    private AbstractList mClauses;
    private ParseTree.Node mParseTree = null;
    
	/**
	 * @throws ParseException
	 * @throws ServiceException
	 * 
	 */
	public LiquidQuery(String queryString, Mailbox mbx) throws ParseException, ServiceException
	{
        LiquidQueryParser parser = new LiquidQueryParser(new StringReader(queryString));
        parser.init(new LiquidAnalyzer(), mbx);
         
        mClauses = parser.Parse();
        
        if (true) {
            if (ParseTree.SPEW) System.out.println("QueryString: "+queryString);
            ParseTree.Node pt = ParseTree.build(mClauses);
            if (ParseTree.SPEW) System.out.println("PT: "+pt.toString());
            if (ParseTree.SPEW)System.out.println("Simplified:");
            pt = pt.simplify();
            if (ParseTree.SPEW)System.out.println("PT: "+pt.toString());
            if (ParseTree.SPEW)System.out.println("Pushing nots down:");
            pt.pushNotsDown();
            if (ParseTree.SPEW)System.out.println("PT: "+pt.toString());
            
            mParseTree = pt;
            
        }
	}
    
    /**
     * 
     * Convert this query into QueryOperations, optimize the operations, run them, and return
     * an iterable Results set. 
     * 
	 * @param mailboxId
	 * @param mbidx
	 * @param types
	 * @param searchOrder
	 * @return
	 * @throws IOException
	 * @throws ServiceException
	 */
	public LiquidQueryResults execute(int mailboxId, MailboxIndex mbidx, byte[] types, int searchOrder,
	        boolean includeTrash, boolean includeSpam) throws IOException, ServiceException 
	        {
	    BaseQuery head = getHead();
	    if (null != head) {
            
	        Mailbox mbox = Mailbox.getMailboxById(mailboxId);
	        
	        QueryOperation op= mParseTree.getQueryOperation();
	        
	        if (mLog.isDebugEnabled()) {
	            mLog.debug("OP="+op.toString());
	        }
	        
	        op = op.optimize(mbox);
	        if (op == null) {
	            op = new NoTermQueryOperation();
	        }
	        
	        if (mLog.isDebugEnabled()) {
	            mLog.debug("OPTIMIZED="+op.toString());
	        }
	        
	        if (!includeTrash || !includeSpam) {
	            if (!op.hasSpamTrashSetting()) {
	                op = op.ensureSpamTrashSetting(mbox, includeTrash, includeSpam);
	                if (mLog.isDebugEnabled()) {
	                    mLog.debug("AFTERTS="+op.toString());
	                }
	                op = op.optimize(mbox); // optimize again now that we have the trash/spam setting
	            }
	        }
	        
	        if (mLog.isDebugEnabled()) {
	            mLog.debug("OPERATION:"+op.toString());
	        }
	        
	        LiquidQueryResults res = op.run(mbox, mbidx, types, searchOrder);
	        
	        return res;
	    }

	    // return an empty SimpleQueryResults set...
	    return new EmptyQueryResults(types, searchOrder);
    }
	
	public String toString() {
        String ret = "LQ:\n";
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
