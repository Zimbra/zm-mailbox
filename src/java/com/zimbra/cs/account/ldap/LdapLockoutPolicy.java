/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014 Zimbra, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.ldap;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.twofactor.TwoFactorManager;
import com.zimbra.cs.ldap.LdapDateUtil;

public class LdapLockoutPolicy {

    private Provisioning mProv;
    private Account mAccount;
    private boolean mEnabled;
    private boolean mLockoutExpired;
    private boolean mIsLockedOut;
    private String mAccountStatus;
    private FailedLoginState failedLogins = null;
    private FailedLoginState twoFactorFailedLogins = null;

    public LdapLockoutPolicy(Provisioning prov, Account account) throws ServiceException {
        mAccount = account;
        mProv = prov;
        mAccountStatus = account.getAccountStatus(prov);
        long maxFailures = mAccount.getLongAttr(Provisioning.A_zimbraPasswordLockoutMaxFailures, 0);
        long max2FAFailures = mAccount.getLongAttr(Provisioning.A_zimbraTwoFactorAuthLockoutMaxFailures, 0);
        mEnabled = (maxFailures > 0 || max2FAFailures > 0) && mAccount.getBooleanAttr(Provisioning.A_zimbraPasswordLockoutEnabled, false);
        mIsLockedOut = computeIsLockedOut();
        failedLogins = getFailedLoginState();
        TwoFactorManager manager = new TwoFactorManager(account);
        if (manager.twoFactorAuthEnabled()) {
            twoFactorFailedLogins = getTwoFactorAuthFailedLoginState();
        }
    }

    private FailedLoginState getFailedLoginState() {
        return new FailedLoginState(mAccount,
                Provisioning.A_zimbraPasswordLockoutFailureTime,
                Provisioning.A_zimbraPasswordLockoutMaxFailures);
    }

    private FailedLoginState getTwoFactorAuthFailedLoginState() {
        return new FailedLoginState(mAccount,
                Provisioning.A_zimbraTwoFactorAuthLockoutFailureTime,
                Provisioning.A_zimbraTwoFactorAuthLockoutMaxFailures);
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

    public void successfulLogin() {
        if (!mEnabled) return;
        Map<String, Object> attrs = new HashMap<String,Object>();
        if (failedLogins.mEnabled && failedLogins.mFailures.length > 0) {
            attrs.put(failedLogins.failuresAttrName, "");
        }
        if (twoFactorFailedLogins != null && twoFactorFailedLogins.mEnabled && twoFactorFailedLogins.mFailures.length > 0) {
            attrs.put(twoFactorFailedLogins.failuresAttrName, "");
        }
        if (mLockoutExpired) {
            if (mAccountStatus.equalsIgnoreCase(Provisioning.ACCOUNT_STATUS_LOCKOUT)) {
                ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                        new String[] {"cmd", "Auth","account", mAccount.getName(),
                        "info", "account re-activated from lockout status upon successful login"}));
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
        failedLogin(failedLogins);
    }

    public void failedSecondFactorLogin() {
        failedLogin(twoFactorFailedLogins);
    }

    private void failedLogin(FailedLoginState login) {
        if (!mEnabled || !login.mEnabled) return;
        Map<String, Object> attrs = new HashMap<String,Object>();

        int totalFailures = login.updateFailureTimes(attrs);

        if (totalFailures >= login.mMaxFailures && !mIsLockedOut) {
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "Auth","account", mAccount.getName(), "error", "account lockout due to too many failed logins"}));
            attrs.put(Provisioning.A_zimbraPasswordLockoutLockedTime, LdapDateUtil.toGeneralizedTime(new Date()));
            attrs.put(Provisioning.A_zimbraAccountStatus, Provisioning.ACCOUNT_STATUS_LOCKOUT);
        }

        try {
            mProv.modifyAttrs(mAccount, attrs);
        } catch (Exception e) {
            ZimbraLog.account.warn("Unable to update account password lockout attrs: "+mAccount.getName(), e);
        }
    }

    private static class FailedLoginState {

        private Account mAccount;
        private String[] mFailures;
        private List<String> mFailuresToRemove;
        private long mMaxFailures;
        private String failuresAttrName;
        private boolean mEnabled;

        FailedLoginState(Account account, String failuresAttr, String maxFailuresAttr) {
            mAccount = account;
            mMaxFailures = mAccount.getLongAttr(maxFailuresAttr, 0);
            mEnabled = mMaxFailures > 0 && mAccount.getBooleanAttr(Provisioning.A_zimbraPasswordLockoutEnabled, false);
            mFailures = mAccount.getMultiAttr(failuresAttr);
            this.failuresAttrName = failuresAttr;
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
                String expiredTime = LdapDateUtil.toGeneralizedTime(new Date(System.currentTimeMillis() - duration));
                for (String failure : mFailures) {
                    if (failure.compareTo(expiredTime) < 0) {
                        if (mFailuresToRemove == null) mFailuresToRemove = new ArrayList<String>();
                        mFailuresToRemove.add(failure);
                    }
                }
            }

            String currentFailure = LdapDateUtil.toGeneralizedTime(new Date());
            // need to toss out the oldest if we are at our limit.
            boolean removeOldest = mFailures.length >= mMaxFailures && mFailuresToRemove == null;
            if (removeOldest) {
                int i, j = 0;
                for (i=0; i < mFailures.length; i++) {
                    // remove oldest iif the one we are adding isn't already in, otherwise we
                    // are effectively removing one without adding another
                    if (mFailures[i].equalsIgnoreCase(currentFailure)) {
                        removeOldest = false;
                        break;
                    }
                    if (i > 0 && mFailures[i].compareTo(mFailures[j]) < 0) {
                        j = i;
                    }
                }
                if (removeOldest) attrs.put("-" + failuresAttrName, mFailures[j]);
            } else if (mFailuresToRemove != null) {
                // remove any expired
                attrs.put("-" + failuresAttrName, mFailuresToRemove);
            }

            // add latest failure
            attrs.put("+" + failuresAttrName, currentFailure);

            // return total of all outstanding failures, including latest
            return 1 + mFailures.length - (removeOldest ? 1 : 0 ) - (mFailuresToRemove == null ? 0 : mFailuresToRemove.size());
        }
    }
}
