/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.index;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;

/**
 */
abstract class ResultValidator {
    /** 
     */
    static abstract class ExpectedHit {
        public abstract boolean check(ZimbraHit hit) throws ServiceException;
    }

    /**
     */
    static class QueryResult extends ExpectedHit {
        public String mSubject;
        public QueryResult(String subject) {
            mSubject = subject;
        }
        public String toString() {
            return mSubject;
        }
        
        public boolean check(ZimbraHit hit) throws ServiceException {
            String subject = hit.getSubject();
            return ExpectedHitValidator.checkSubject(subject, mSubject);
        }
    }

    /**
     */
    static class ExpectedCalendarItemHit extends ExpectedHit {
        String mUid;
        public ExpectedCalendarItemHit(String uid) {
            mUid = uid;
        }
        public boolean check(ZimbraHit hit) throws ServiceException {
            if (!(hit instanceof CalendarItemHit))
                return false;
            return true;
        }
        
        public String toString() {
            return "CalendarItem uid="+mUid;
        }
    }

    /**
     */
    static class ExpectedMessageHit extends ExpectedHit {
        String mSubject;
        public ExpectedMessageHit(String subject) {
            mSubject = subject;
        }
        public boolean check(ZimbraHit hit) throws ServiceException {
            if (!(hit instanceof MessageHit))
                return false;
            
            return ExpectedHitValidator.checkSubject(hit.getSubject(), mSubject);
        }
        
        public String toString() {
            return "Message subject="+mSubject;
        }
    }

    /**
     */
    public static class ExpectedHitValidator extends ResultValidator {
        List<ExpectedHit> mExpected = new ArrayList<ExpectedHit>();
        int curHit = 0;
        
        ExpectedHitValidator(ExpectedHit... hits) {
            super(hits.length);
            
            for (ExpectedHit e : hits)
                mExpected.add(e);
        }
        
        static boolean checkSubject(String received, String expected) {
            String upperSub = received.toUpperCase();
            if (upperSub.startsWith("RE:  ")) {
                upperSub = upperSub.substring(5);
            } else if (upperSub.startsWith("RE: ")) {
                    upperSub = upperSub.substring(4);
            } else if (upperSub.startsWith("RE:")) {
                upperSub = upperSub.substring(3);
            } else if (upperSub.startsWith("FW:  ")) {
                upperSub = upperSub.substring(5);
            } else if (upperSub.startsWith("FW: ")) {
                upperSub = upperSub.substring(4);
            } else if (upperSub.startsWith("FW:")) {
                upperSub = upperSub.substring(3);
            }
    
            return upperSub.equals(expected.toUpperCase());
        }
        
        public void validate(ZimbraHit hit) throws ServiceException {
            if (curHit >= mExpected.size())
                throw ServiceException.FAILURE("Too many hits on hit "+curHit+", got: "+hit.toString()+getExpectedList(), null);
                
            ExpectedHit expected = mExpected.get(curHit);
            
            if (!expected.check(hit)) {
                throw ServiceException.FAILURE("Invalid hit:\n  Current Hit:\n\t"+hit.toString()+"\n"+getExpectedList(), null);
            }
            
            curHit++;
        }
        
        public String getExpectedList() {
            StringBuilder sb = new StringBuilder("  Expected: \n");
            int i = 0;
            for (ExpectedHit e : mExpected) {
                sb.append("\t").append(i).append(") ").append(e.toString()).append("\n");
                i++;
            }
            return sb.toString();
        }
        
        public Object getExpected(int num) { return mExpected.get(num); }
        
    }

    protected int mNumExpected = -1;
    ResultValidator() {}
    ResultValidator(int numExpected) { mNumExpected = numExpected; }
    int numExpected() { return mNumExpected; }
    boolean numReceived(int num) { return (mNumExpected == num || mNumExpected == -1); }
    public Object getExpected(int num) { return ""; }
    
    public abstract void validate(ZimbraHit hit) throws ServiceException;
}