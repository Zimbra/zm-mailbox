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
package com.zimbra.cs.session;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.service.mail.WaitSetRequest;

/**
 * The base class defines shared functions, as well as any APIs which should be 
 * package-private
 */
public abstract class WaitSetBase implements IWaitSet {
    abstract HashMap<String, WaitSetAccount> destroy();
    abstract int countSessions();
    abstract protected boolean cbSeqIsCurrent();
    abstract protected String toNextSeqNo();
    
    
    public long getLastAccessedTime() {
        return mLastAccessedTime;
    }

    public void setLastAccessedTime(long lastAccessedTime) {
        mLastAccessedTime = lastAccessedTime;
    }
    
    /* @see com.zimbra.cs.session.IWaitSet#getDefaultInterest() */
    public int getDefaultInterest() {
        return mDefaultInterest;
    }

    /* @see com.zimbra.cs.session.IWaitSet#getOwnerAccountId() */
    public String getOwnerAccountId() {
        return mOwnerAccountId;
    }

    /* @see com.zimbra.cs.session.IWaitSet#getWaitSetId() */
    public String getWaitSetId() {
        return mWaitSetId;
    }
    
    synchronized WaitSetCallback getCb() { return mCb; }

    /**
     * Cancel any existing callback
     */
    protected synchronized void cancelExistingCB() {
        if (mCb != null) {
            // cancel the existing waiter
            mCb.dataReady(this, "", true, null, null);
            mCb = null;
            mLastAccessedTime = System.currentTimeMillis();
        }
    }
    
    public synchronized void doneWaiting() {
        mCb = null;
        mLastAccessedTime = System.currentTimeMillis();
    }
    
    
    protected WaitSetBase(String ownerAccountId, String waitSetId, int defaultInterest) {
        mOwnerAccountId = ownerAccountId;
        mWaitSetId = waitSetId;
        mDefaultInterest = defaultInterest;
    }
    
    protected synchronized void trySendData() {
        if (mCb == null) {
            return;
        }
        
        boolean cbIsCurrent = cbSeqIsCurrent(); 
        
        if (cbIsCurrent) {
            mSentSignalledSessions.clear();
            mSentErrors.clear();
        }
        
        /////////////////////
        // Cases:
        //
        // CB up to date 
        //   AND Current empty --> WAIT
        //   AND Current NOT empty --> SEND
        //
        // CB not up to date
        //   AND Current empty AND Sent empty --> WAIT
        //   AND (Current NOT empty OR Sent NOT empty) --> SEND BOTH
        //
        // ...simplifies to:
        //        send if Current NOT empty OR
        //                (CB not up to date AND Sent not empty)
        //
        if ((mCurrentSignalledSessions.size() > 0 || mCurrentErrors.size() > 0) || 
                        (!cbIsCurrent && (mSentSignalledSessions.size() > 0 || mSentErrors.size() > 0))) {
            // if sent empty, then just swap sent,current instead of copying
            if (mSentSignalledSessions.size() == 0) {
                // SWAP mSent,mCurrent!
                HashSet<String> temp = mCurrentSignalledSessions;
                mCurrentSignalledSessions = mSentSignalledSessions;
                mSentSignalledSessions = temp;
            } else {
                assert(!cbIsCurrent);
                mSentSignalledSessions.addAll(mCurrentSignalledSessions);
                mCurrentSignalledSessions.clear();
            }

            // error list 
            mSentErrors.addAll(mCurrentErrors);
            mCurrentErrors.clear();
            
            // at this point, mSentSignalled is everything we're supposed to send...lets
            // make an array of the account IDs and signal them up!
            assert(mSentSignalledSessions.size() > 0  || mSentErrors.size() > 0);
            String[] toRet = new String[mSentSignalledSessions.size()];
            int i = 0;
            for (String accountId : mSentSignalledSessions) {
                toRet[i++] = accountId;
            }
            
            mCb.dataReady(this, toNextSeqNo(), false, mSentErrors, toRet);
            mCb = null;
            mLastAccessedTime = System.currentTimeMillis();
        }
    }
    
    public synchronized void handleQuery(Element response) {
        response.addAttribute(AdminConstants.A_ID, mWaitSetId);
        response.addAttribute(AdminConstants.A_OWNER, mOwnerAccountId);
        response.addAttribute(AdminConstants.A_DEFTYPES, WaitSetRequest.expandInterestStr(mDefaultInterest));
        response.addAttribute(AdminConstants.A_LAST_ACCESSED_DATE, mLastAccessedTime);

        if (mCurrentErrors.size() > 0) {
            Element errors = response.addElement(AdminConstants.E_ERRORS);
            for (WaitSetError error : mCurrentErrors) {
                Element errorElt = errors.addElement("error");
                errorElt.addAttribute(AdminConstants.A_ID, error.accountId); 
                errorElt.addAttribute(AdminConstants.A_TYPE, error.error.name()); 
            }
        }
        
        // signaled accounts
        if (mCurrentSignalledSessions.size() > 0) {
            Element signaled = response.addElement(AdminConstants.A_READY);
            StringBuilder signaledStr = new StringBuilder();
            for (String accountId : mCurrentSignalledSessions) {
                if (signaledStr.length() > 0)
                    signaledStr.append(",");
                signaledStr.append(accountId);
            }
            signaled.addAttribute(AdminConstants.A_ACCOUNTS, signaledStr.toString());
        }
    }
    
    protected synchronized void signalError(WaitSetError err) {
        mCurrentErrors.add(err);
        trySendData();
    }
    
    
    protected final String mWaitSetId;
    protected final String mOwnerAccountId;
    protected final int mDefaultInterest;
    
    protected long mLastAccessedTime = -1;
    protected WaitSetCallback mCb = null;
    
    /**
     * List of errors (right now, only mailbox deletion notifications) to be sent
     */
    protected List<WaitSetError> mCurrentErrors = new ArrayList<WaitSetError>();
    protected List<WaitSetError> mSentErrors = new ArrayList<WaitSetError>();
    
    
    /** this is the signalled set data that is new (has never been sent) */
    protected HashSet<String /*accountId*/> mCurrentSignalledSessions = new HashSet<String>();
    
    /** this is the signalled set data that we've already sent, it just hasn't been acked yet */
    protected HashSet<String /*accountId*/> mSentSignalledSessions = new HashSet<String>();
}
