/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
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

package com.zimbra.cs.account.ldap;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.DateUtil;
import com.zimbra.common.util.ZimbraLog;

class LdapLockoutPolicy {

    private String[] mFailures;
    private List<String> mFailuresToRemove;
    private Provisioning mProv;
    private Account mAccount;
    private boolean mEnabled;
    private long mMaxFailures;
    private boolean mLockoutExpired;
    private boolean mIsLockedOut;
    private String mAccountStatus;
    
    LdapLockoutPolicy(Provisioning prov, Account account) throws ServiceException {
        mAccount = account;
        mProv = prov;
        mAccountStatus = account.getAccountStatus(prov);
        mMaxFailures = mAccount.getLongAttr(Provisioning.A_zimbraPasswordLockoutMaxFailures, 0);
        mEnabled = mMaxFailures > 0 && mAccount.getBooleanAttr(Provisioning.A_zimbraPasswordLockoutEnabled, false);
        mFailures = mAccount.getMultiAttr(Provisioning.A_zimbraPasswordLockoutFailureTime);
        mIsLockedOut = computeIsLockedOut();
    }

    private boolean computeIsLockedOut() throws ServiceException {
        // locking not enabled
        if (!mEnabled) return false;

        Date locked = mAccount.getGeneralizedTimeAttr(Provisioning.A_zimbraPasswordLockoutLockedTime, null);
        
        // not locked
        if (locked == null) return false;
        
        // see if still locked
        long duration = mAccount.getTimeInterval(Provisioning.A_zimbraPasswordLockoutDuration, 0);
        
       //An account is considered locked if the current time is less than the
       //  value zimbraPasswordLockoutLockedTime + zimbraPasswordLockoutDuration.

        // was locked, no longer locked
        if (duration != 0 && System.currentTimeMillis() > (locked.getTime() + duration)) {
            mLockoutExpired = true;
            return false;
        }

        // still locked out if status is set to lockout
        return mAccountStatus.equalsIgnoreCase(Provisioning.ACCOUNT_STATUS_LOCKOUT);
    }

    public boolean isLockedOut() {
        return mIsLockedOut;
    }

    /**
     * update the failure time attr list. remove oldest if it at limit, add new entry,
     * and return number of entries in the list.
     *  
     * @param acct
     * @param attrs
     * @param max
     * @return total number of failure time attrs 
     */
    private int updateFailureTimes(Map<String, Object> attrs) {
        // need to toss out any "expired" failures 
        long duration = mAccount.getTimeInterval(Provisioning.A_zimbraPasswordLockoutFailureLifetime, 0);
        if (duration != 0) {
            String expiredTime = DateUtil.toGeneralizedTime(new Date(System.currentTimeMillis() - duration));
            for (String failure : mFailures) {
                if (failure.compareTo(expiredTime) < 0) {
                    if (mFailuresToRemove == null) mFailuresToRemove = new ArrayList<String>();
                    mFailuresToRemove.add(failure);
                }
            }
        }

        String currentFailure = DateUtil.toGeneralizedTime(new Date());
        // need to toss out the oldest if we are at our limit        
        boolean removeOldest = mFailures.length == mMaxFailures && mFailuresToRemove == null;
        if (removeOldest) {
            int i, j = 0;
            for (i=1; i < mFailures.length; i++) {
                if (mFailures[i].compareTo(mFailures[j]) < 0) {
                    j = i;
                }
            }
            // remove oldest iif the one we are adding isn't already in, otherwise we
            // are effectively removing one without adding another
            for (String f: mFailures) {
                if (f.equalsIgnoreCase(currentFailure)) {
                    removeOldest = false;
                    break;
                }
            }
            if (removeOldest) attrs.put("-" + Provisioning.A_zimbraPasswordLockoutFailureTime, mFailures[j]);
        } else if (mFailuresToRemove != null) {
            // remove any expired
            attrs.put("-" + Provisioning.A_zimbraPasswordLockoutFailureTime, mFailuresToRemove);
        }

        // add latest failure
        attrs.put("+" + Provisioning.A_zimbraPasswordLockoutFailureTime, currentFailure);
        
        // return total of all outstanding failures, including latest
        return 1 + mFailures.length - (removeOldest ? 1 : 0 ) - (mFailuresToRemove == null ? 0 : mFailuresToRemove.size());
    }

    public void successfulLogin() {
        if (!mEnabled) return;
        Map<String, Object> attrs = new HashMap<String,Object>();
        if (mFailures.length > 0)
            attrs.put(Provisioning.A_zimbraPasswordLockoutFailureTime, "");
        if (mLockoutExpired) {
            if (mAccountStatus.equalsIgnoreCase(Provisioning.ACCOUNT_STATUS_LOCKOUT)) {
                attrs.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_ACTIVE);
            }
            attrs.put(Provisioning.A_zimbraPasswordLockoutLockedTime, "");
        }

        try {
            if (attrs.size() > 0)
                mProv.modifyAttrs(mAccount, attrs);
        } catch (Exception e) {
            ZimbraLog.account.warn("Unable to update account password lockout attrs: "+mAccount.getName(), e);            
        }
    }

    public void failedLogin() {
        if (!mEnabled) return;
        Map<String, Object> attrs = new HashMap<String,Object>();

        int totalFailures = updateFailureTimes(attrs);

        if (totalFailures >= mMaxFailures && !mIsLockedOut) {
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "Auth","account", mAccount.getName(), "error", "account lockout due to too many failed logins"}));
            attrs.put(Provisioning.A_zimbraPasswordLockoutLockedTime, DateUtil.toGeneralizedTime(new Date()));
            attrs.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_LOCKOUT);
        }
        
        try {
            mProv.modifyAttrs(mAccount, attrs);
        } catch (Exception e) {
            ZimbraLog.account.warn("Unable to update account password lockout attrs: "+mAccount.getName(), e);
        }
    }
}
