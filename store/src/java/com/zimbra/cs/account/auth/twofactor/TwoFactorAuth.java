/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2015, 2016 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.auth.twofactor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.auth.twofactor.AuthenticatorConfig;
import com.zimbra.common.auth.twofactor.TwoFactorOptions;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.extension.ExtensionUtil;

public abstract class TwoFactorAuth implements SecondFactor {
    protected Account account;
    protected String acctNamePassedIn;
    private static Factory factory;

    public TwoFactorAuth(Account account, String namePassedIn) {
        this.account = account;
        this.acctNamePassedIn = namePassedIn;
    }

    public interface Factory {
        TwoFactorAuth getTwoFactorAuth(Account account) throws ServiceException;
        TwoFactorAuth getTwoFactorAuth(Account account, String acctNamePassedIn) throws ServiceException;
        TrustedDevices getTrustedDevices(Account account) throws ServiceException;
        TrustedDevices getTrustedDevices(Account account, String acctNamePassedIn) throws ServiceException;
        AppSpecificPasswords getAppSpecificPasswords(Account account) throws ServiceException;
        AppSpecificPasswords getAppSpecificPasswords(Account account, String acctNamePassedIn) throws ServiceException;
        ScratchCodes getScratchCodes(Account account) throws ServiceException;
        ScratchCodes getScratchCodes(Account account, String acctNamePassedIn) throws ServiceException;
    }

    public static Factory getFactory() throws ServiceException {
        if (factory == null) {
            setFactory(LC.zimbra_class_two_factor_auth_factory.value());
        }
        return factory;
    }

    public static class DefaultFactory implements Factory {

        @Override
        public TwoFactorAuth getTwoFactorAuth(Account account, String acctNamePassedIn) throws ServiceException {
            return new TwoFactorAuthUnavailable(account);
        }

        @Override
        public TwoFactorAuth getTwoFactorAuth(Account account) throws ServiceException {
            return new TwoFactorAuthUnavailable(account);
        }

        @Override
        public TrustedDevices getTrustedDevices(Account account) throws ServiceException {
            return null;
        }

        @Override
        public TrustedDevices getTrustedDevices(Account account, String acctNamePassedIn) throws ServiceException {
            return null;
        }

        @Override
        public AppSpecificPasswords getAppSpecificPasswords(Account account) throws ServiceException {
            return null;
        }

        @Override
        public AppSpecificPasswords getAppSpecificPasswords(Account account, String acctNamePassedIn) throws ServiceException {
            return null;
        }

        @Override
        public ScratchCodes getScratchCodes(Account account) throws ServiceException {
            return null;
        }

        @Override
        public ScratchCodes getScratchCodes(Account account, String acctNamePassedIn) throws ServiceException {
            return null;
        }

    }
    public static final void setFactory(String factoryClassName) {
        if (factoryClassName == null) {
            factory = null;
            return;
        }
        Class<? extends Factory> factoryClass = null;
        try {
            try {
                factoryClass = Class.forName(factoryClassName).asSubclass(Factory.class);
            } catch (ClassNotFoundException e) {
                try {
                    factoryClass = ExtensionUtil.findClass(factoryClassName)
                            .asSubclass(Factory.class);
                } catch (ClassNotFoundException cnfe) {
                    ZimbraLog.extensions.error("cannot instantiate specified two-factor auth factory, using default");
                    factoryClass = DefaultFactory.class;
                }
            }
        } catch (ClassCastException cce) {
            ZimbraLog.extensions.error("cannot instantiate specified two-factor auth factory, using default");
            factoryClass = DefaultFactory.class;
        }
        try {
            factory = factoryClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            ZimbraLog.extensions.error("cannot instantiate specified two-factor auth factory, using default");
            factory = new DefaultFactory();
        }
        ZimbraLog.extensions.info("Using two-factor auth factory %s", factory.getClass().getDeclaringClass().getSimpleName());
    }

    public abstract boolean twoFactorAuthRequired() throws ServiceException;
    public abstract boolean twoFactorAuthEnabled() throws ServiceException;
    public abstract void enableTwoFactorAuth() throws ServiceException;
    public abstract void disableTwoFactorAuth(boolean deleteCredentials) throws ServiceException;
    public abstract CredentialConfig getCredentialConfig() throws ServiceException;
    public abstract AuthenticatorConfig getAuthenticatorConfig() throws ServiceException;
    public abstract Credentials generateCredentials() throws ServiceException;
    public abstract void authenticateTOTP(String code) throws ServiceException;
    public abstract void clearData() throws ServiceException;

    public void enable() throws ServiceException {
        enableTwoFactorAuth();
        TwoFactorChangeListener.invokeEnabled(account);
    }

    public void disable(boolean deleteCredentials) throws ServiceException {
        disableTwoFactorAuth(deleteCredentials);
        TwoFactorChangeListener.invokeDisabled(account);
    }

    //  Helper classes and interfaces ----------------------------

    public static interface Credentials {

        public String getSecret();

        public List<String> getScratchCodes();

        public String getTimestamp();
    }

    public static class CredentialConfig {
        private int secretLength;
        private TwoFactorOptions.Encoding secretEncoding;
        private int scratchCodeLength;
        private TwoFactorOptions.Encoding scratchCodeEncoding;
        private int numScratchCodes;

        public CredentialConfig setEncoding(TwoFactorOptions.Encoding encoding) {
            this.secretEncoding = encoding;
            return this;
        }

        public CredentialConfig setScratchCodeEncoding(TwoFactorOptions.Encoding encoding) {
            this.scratchCodeEncoding = encoding;
            return this;
        }

        public CredentialConfig setNumScratchCodes(int n) {
            numScratchCodes = n;
            return this;
        }

        public CredentialConfig setScratchCodeLength(int n) {
            scratchCodeLength = n;
            return this;
        }

        public CredentialConfig setSecretLength(int n) {
            secretLength = n;
            return this;
        }

        public int getSecretLength() {
            return secretLength;
        }

        public int getNumScratchCodes() {
            return numScratchCodes;
        }

        public int getScratchCodeLength() {
            return scratchCodeLength;
        }

        public TwoFactorOptions.Encoding getEncoding() {
            return secretEncoding;
        }

        public TwoFactorOptions.Encoding getScratchCodeEncoding() {
            return scratchCodeEncoding;
        }

        public int getBytesPerSecret() {
            return getBytesPerCodeLength(secretEncoding, secretLength);
        }

        private static int getBytesPerCodeLength(TwoFactorOptions.Encoding encoding, int n) {
            switch(encoding) {
                case BASE32:
                    return (n/ 8) * 5;
                case BASE64:
                    return (n / 4) * 3;
                default:
                    return 0;
            }
        }

        public int getBytesPerScratchCode() {
            return getBytesPerCodeLength(scratchCodeEncoding, scratchCodeLength);
        }
    }

    public static abstract class TwoFactorChangeListener {
        private static Map<String, TwoFactorChangeListener> listeners = new HashMap<String, TwoFactorChangeListener>();

        public abstract void twoFactorAuthEnabled(Account acct);
        public abstract void twoFactorAuthDisabled(Account acct);
        public abstract void appSpecificPasswordRevoked(Account acct, String appName);

        public static void register(String name, TwoFactorChangeListener listener) {
            if (listeners.containsKey(name)) {
                ZimbraLog.extensions.warn("TwoFactorChangeListener " + name + " is already registered");
            } else {
                listeners.put(name,  listener);
            }
        }

        public static void invokeEnabled(Account acct) {
            for (Map.Entry<String, TwoFactorChangeListener> entry: listeners.entrySet()) {
                TwoFactorChangeListener listener = entry.getValue();
                listener.twoFactorAuthEnabled(acct);
            }
        }

        public static void invokeDisabled(Account acct) {
            for (Map.Entry<String, TwoFactorChangeListener> entry: listeners.entrySet()) {
                TwoFactorChangeListener listener = entry.getValue();
                listener.twoFactorAuthDisabled(acct);
            }
        }

        public static void revokeAppPassword(Account acct, String appName) {
            for (Map.Entry<String, TwoFactorChangeListener> entry: listeners.entrySet()) {
                TwoFactorChangeListener listener = entry.getValue();
                listener.appSpecificPasswordRevoked(acct, appName);
            }
        }
    }
}
