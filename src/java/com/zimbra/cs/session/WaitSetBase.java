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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.session;

import java.util.HashMap;
import java.util.HashSet;

/**
 * The base class defines shared functions, as well as any APIs which should be 
 * package-private
 */
public abstract class WaitSetBase implements IWaitSet {
    abstract HashMap<String, WaitSetAccount> destroy();
    abstract int countSessions();
    
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
            mCb.dataReady(this, "", true, null);
            mCb = null;
            mLastAccessedTime = System.currentTimeMillis();
        }
    }
    
    protected WaitSetBase(String ownerAccountId, String waitSetId, int defaultInterest) {
        mOwnerAccountId = ownerAccountId;
        mWaitSetId = waitSetId;
        mDefaultInterest = defaultInterest;
    }
    
    protected final String mWaitSetId;
    protected final String mOwnerAccountId;
    protected final int mDefaultInterest;
    
    protected long mLastAccessedTime = -1;
    protected WaitSetCallback mCb = null;
    
    /** this is the signalled set data that is new (has never been sent) */
    protected HashSet<String /*accountId*/> mCurrentSignalledSessions = new HashSet<String>();
    
    /** this is the signalled set data that we've already sent, it just hasn't been acked yet */
    protected HashSet<String /*accountId*/> mSentSignalledSessions = new HashSet<String>();
}
