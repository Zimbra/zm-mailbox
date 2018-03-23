/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.ldap;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.PasswordUtil.SSHA512;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth;
import com.zimbra.cs.ldap.LdapDateUtil;

public class LdapLockoutPolicy {

    private Provisioning mProv;
    private Account mAccount;
    private boolean mEnabled;
    private boolean mCaptchaEnabled;
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
        TwoFactorAuth twoFactorAuth = TwoFactorAuth.getFactory().getTwoFactorAuth(account);
        if (twoFactorAuth.twoFactorAuthEnabled()) {
            twoFactorFailedLogins = getTwoFactorAuthFailedLoginState();
        }
        mCaptchaEnabled = mAccount.getBooleanAttr(Provisioning.A_zimbraCAPTCHAEnabled, false);
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
        if (mCaptchaEnabled) {
            updateCaptchaCount(false);
        }
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

    private static class PasswordLockoutCache {
        private static long maxCacheSize = DebugConfig.invalidPasswordMaxCacheSize;
        private static int cacheExpiryInMinute = DebugConfig.invalidPasswordCacheExpirationInMinutes;
        private static Cache<String, List<String>> cache =
            CacheBuilder.newBuilder().maximumSize(maxCacheSize).expireAfterWrite(cacheExpiryInMinute, TimeUnit.MINUTES).build();

        private static boolean suppressPasswordLockOut(Account acct, String protocol, String password) throws ServiceException {
            if (!(StringUtil.isNullOrEmpty(password) || StringUtil.isNullOrEmpty(protocol))) {
                if (acct.isPasswordLockoutSuppressionEnabled()) {
                    for (String suppressionProtocol : acct.getPasswordLockoutSuppressionProtocolsAsString()) {
                        if (protocol.equalsIgnoreCase(suppressionProtocol)) {
                            List<String> pwds = null;
                            try {
                                pwds = cache.get(acct.getId(), new Callable<List<String>>() {
                                    @Override
                                    public List<String> call() throws Exception {
                                        return new ArrayList<String>();
                                    }
                                });
                            } catch (ExecutionException e) {
                                ZimbraLog.account.warn("Error while retrieving invalid password cache entry", e);
                                return false;
                            }
                            synchronized (pwds) {
                                int cacheSize = pwds.size();
                                if (cacheSize == 0) {
                                    pwds.add(SSHA512.generateSSHA512(password, null));
                                    ZimbraLog.account.debug("Created entry in password lockout cache");
                                } else {
                                    for (String pwd : pwds) {
                                        if (SSHA512.verifySSHA512(pwd, password)) {
                                            return true;
                                        }
                                    }
                                    int maxCacheSize = acct.getPasswordLockoutSuppressionCacheSize();
                                    if (acct.isTwoFactorAuthEnabled()) {
                                        if (acct.isFeatureAppSpecificPasswordsEnabled() &&
                                            acct.getAppSpecificPassword() != null) {
                                            maxCacheSize = maxCacheSize + acct.getAppSpecificPassword().length;
                                        }
                                    }
                                    ZimbraLog.account.debug("Password lockout suppression cache size = %s (max = %s)", cacheSize, maxCacheSize);
                                    if (cacheSize < maxCacheSize) {
                                        pwds.add(SSHA512.generateSSHA512(password, null));
                                        ZimbraLog.account.debug("Added entry in password lockout cache");
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return false;
        }
    }

    public void failedLogin() throws ServiceException {
        failedLogin(failedLogins, null, null);
    }

    public void failedSecondFactorLogin() throws ServiceException {
        failedLogin(twoFactorFailedLogins, null, null);
    }

    public void failedLogin(String protocol, String password) throws ServiceException {
        failedLogin(failedLogins, protocol, password);
    }

    private void updateCaptchaCount(boolean failedCaptcha) {
        Map<String, Object> attrs = new HashMap<String,Object>();
        if (failedCaptcha) {
            int loginFailCount = mAccount.getIntAttr(Provisioning.A_zimbraCAPTCHALoginFailedCount, 0);
            attrs.put(Provisioning.A_zimbraCAPTCHALoginFailedCount, ++loginFailCount);
        } else {
            attrs.put(Provisioning.A_zimbraCAPTCHALoginFailedCount, 0);
        }

        try {
            mProv.modifyAttrs(mAccount, attrs);
        } catch (Exception e) {
            ZimbraLog.account.warn("Unable to update account CAPTCHA loginFailCount attrs: %s", mAccount.getName(), e);
        }
    }

    private void failedLogin(FailedLoginState login, String protocol, String password) throws ServiceException {

        if (mCaptchaEnabled) {
            updateCaptchaCount(true);
        }

        if (!mEnabled || !login.mEnabled) return;

        if (PasswordLockoutCache.suppressPasswordLockOut(mAccount, protocol, password)) {
            ZimbraLog.security.info("Suppressed password lockout.");
            return;
        }
        // Account is lockout or lockout expired but still there is failed login.
        if (mIsLockedOut || mAccountStatus.equalsIgnoreCase(Provisioning.ACCOUNT_STATUS_LOCKOUT)) {
            ZimbraLog.security.info("Account is lockout, not updating failure time.");
            return;
        }
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
