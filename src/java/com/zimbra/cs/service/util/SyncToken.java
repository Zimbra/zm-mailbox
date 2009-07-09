/*
 * ***** BEGIN LICENSE BLOCK *****
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
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.util;

import java.util.Enumeration;

import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestResult;
import junit.framework.TestSuite;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CliUtil;
import com.zimbra.cs.mailbox.MailServiceException;

/**
 * A short identifier which identifies the synchronization state between a client and the server.
 * 
 * The sync token can have two forms:
 * 
 * 1)   "INTEGER"  -- this is the highest change ID the client knows about (see Mailbox.getLastChangeID)
 * 
 *    OR
 *    
 * 2)   "INTEGER-INTEGER" 
 *         -- the first integer is the highest change ID that the client has *all* the data for
 *         -- the second integer is the highest item id in the NEXT CHANGE ID that the client has data for
 *         
 *         e.g. "4-32" means "I have all of change 4, AND I have up to item 32 in change 5" 
 *    
 */
public class SyncToken implements Cloneable, Comparable {
    private int mChangeId;
    private int mOffsetInNext = -1;
    
    public SyncToken(int changeid) {
        assert(changeid >= 0);
        mChangeId = changeid;
    }
    
    public SyncToken(int changeid, int offsetInNextChange) {
        assert(changeid >= 0 && offsetInNextChange >= 0);
        mChangeId = changeid;
        mOffsetInNext = offsetInNextChange;
    }
    
    public SyncToken(String s) throws ServiceException {
        int idx = s.indexOf('-'); 
        if (idx < 0) {
            mChangeId = Integer.parseInt(s);
        } else {
            if (idx == s.length()-1) 
                throw MailServiceException.INVALID_SYNC_TOKEN(s);
            String lhs = s.substring(0, idx);
            mChangeId = Integer.parseInt(lhs);
            String rhs = s.substring(idx+1);
            mOffsetInNext = Integer.parseInt(rhs);
            if (mOffsetInNext < 0)
                throw MailServiceException.INVALID_SYNC_TOKEN(s);
        }
    }
    
    public int getChangeId() { return mChangeId; }
    public boolean hasOffsetInNext() { return mOffsetInNext > 0; }
    public int getOffsetInNext() { return mOffsetInNext; }
    public String toString() {
        if (mOffsetInNext < 0) {
            return Integer.toString(mChangeId);
        } else {
            return mChangeId+"-"+mOffsetInNext;
        }
    }
    
    /**
     * TRUE if this syncToken is AFTER or UP-TO-DATE with the passed-in token
     * 
     * @param changeId
     * @return
     */
    public boolean after(int changeId) {
        return mChangeId >= changeId; 
    }
    
    public boolean after(int changeId, int offset) {
        if (mChangeId < changeId)
            return false;
        if (mChangeId > changeId)
            return true;
        return (mOffsetInNext >= offset);
    }
    
    public boolean after(SyncToken other) {
        if (other.mOffsetInNext >= 0)
            return after(other.mChangeId, other.mOffsetInNext);
        else
            return after(other.mChangeId);
    }
    
    
    @Override public SyncToken clone() { 
        if (mOffsetInNext >= 0)
            return new SyncToken(mChangeId, mOffsetInNext);
        else
            return new SyncToken(mChangeId);
    }

    public int compareTo(Object arg0) {
        SyncToken other = (SyncToken)arg0;
        int diff = this.mChangeId - other.mChangeId;
        if (diff == 0) {
            if (this.mOffsetInNext == -1 && other.mOffsetInNext == -1)
                return 0;
            else if (this.mOffsetInNext >=0 && other.mOffsetInNext == -1)
                return 1;
            else if (this.mOffsetInNext == -1 && other.mOffsetInNext >=0)
                return -1;
            else return (this.mOffsetInNext - other.mOffsetInNext);
        } else 
            return diff;
    }
    
    public static class Tester extends TestCase {
        public Tester() {}
        
        public void testSyncToken() throws ServiceException {
            SyncToken one = new SyncToken(1);
            SyncToken two = new SyncToken(2);
            SyncToken three = new SyncToken(3);
            SyncToken two_one = new SyncToken(2,1);
            SyncToken two_two = new SyncToken(2,2);
            SyncToken three_one = new SyncToken(3,1);
            
            assertTrue(two.after(one));
            assertTrue(three.after(two));
            assertTrue(three.after(one));
            
            assertFalse(one.after(three));
            
            assertTrue(two_one.after(two));
            assertFalse(two.after(two_one));
            
            assertTrue(two_two.after(two_one));
            assertFalse(two_one.after(two_two));
            
            assertTrue(three_one.after(three));
            assertTrue(three_one.after(two));

            assertFalse(three.after(three_one));
            assertFalse(one.after(three_one));
        }
    }

    public static void main(String[] args) {
        CliUtil.toolSetup("DEBUG");
        TestSuite suite = new TestSuite(Tester.class);
        TestResult results = new TestResult();
        suite.run(results);
        
        if (!results.wasSuccessful()) {
            System.out.println("\n**************************");
            System.out.println("TEST FAILURES:");
            System.out.println("**************************");
        }

        if (results.failureCount() > 0) {
            Enumeration failures = results.failures();
            while(failures.hasMoreElements()) {
                TestFailure error = (TestFailure)failures.nextElement();
                System.out.println("--> Test Failure: " + error.trace() + error.thrownException());
                System.out.print("\n");
            }
        }

        if (results.errorCount() > 0) {
            Enumeration errors = results.errors();
            while(errors.hasMoreElements()) {
                TestFailure failure = (TestFailure)errors.nextElement();
                System.out.println("--> Test Error: " + failure.trace() + failure.thrownException() + " at ");
                failure.thrownException().printStackTrace();
                System.out.print("\n");
            }
        }

        if (results.wasSuccessful()) {
            System.out.println("\n**************************");
            System.out.println("Tests SUCCESSFUL!");
            System.out.println("**************************");
        }
        
    }
    
}
