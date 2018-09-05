/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2015, 2016 Synacor, Inc.
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

package com.zimbra.cs.account.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javax.security.auth.login.LoginException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.QuotedStringParser;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException.AuthFailedServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.auth.AuthContext.Protocol;
import com.zimbra.cs.account.auth.twofactor.AppSpecificPasswords;
import com.zimbra.cs.account.auth.twofactor.TwoFactorAuth;
import com.zimbra.cs.account.krb5.Krb5Login;
import com.zimbra.cs.account.krb5.Krb5Principal;
import com.zimbra.cs.account.ldap.LdapProv;
import com.zimbra.cs.account.ldap.entry.LdapEntry;

public abstract class AuthMechanism {

    public static enum AuthMech {
        /**
         * zimbraAuthMech type of "zimbra" means our own (use userPassword)
         */
        zimbra,

        /**
         * zimbraAuthMech type of "ldap" means use configured LDAP attrs
         * (zimbraAuthLdapURL, zimbraAuthLdapBindDn)
         */
        ldap,

        /**
         * zimbraAuthMech type of "ad" means use configured LDAP attrs
         * (zimbraAuthLdapURL, zimbraAuthLdapBindDn) for use with ActiveDirectory
         */
        ad,

        /**
         * zimbraAuthMech type of "kerberos5" means use kerberos5 authentication.
         * The principal can be obtained by, either:
         * (1) {email-local-part}@{domain-attr-zimbraAuthKerberos5Realm}
         * or
         * (2) {principal-name} if account zimbraForeignPrincipal is in the format of
         *     kerberos5:{principal-name}
         */
        kerberos5,

        /**
         * zimbraAuthMech type of "custom:{handler}" means use registered extension
         * of ZimbraCustomAuth.authenticate() method
         * see customauth.txt
         */
        custom;

        public static AuthMech fromString(String authMechStr) throws ServiceException {
            if (authMechStr == null) {
                return null;
            }

            try {
                return AuthMech.valueOf(authMechStr);
            } catch (IllegalArgumentException e) {
                throw ServiceException.INVALID_REQUEST("unknown auth mech: " + authMechStr, e);
            }
        }
    }

    protected AuthMech authMech;

    protected AuthMechanism(AuthMech authMech) {
        this.authMech = authMech;
    }

    public static AuthMechanism newInstance(Account acct, Map<String, Object> context)
    throws ServiceException {
        String authMechStr = AuthMech.zimbra.name();

        // bypass domain AuthMech and always use Zimbra auth for external virtual accounts

        if (!acct.isIsExternalVirtualAccount()) {
            Provisioning prov = Provisioning.getInstance();
            Domain domain = prov.getDomain(acct);

            // see if it specifies an alternate auth
            if (domain != null) {
                String am;
                Boolean asAdmin = context == null ? null : (Boolean) context.get(AuthContext.AC_AS_ADMIN);
                if (asAdmin != null && asAdmin) {
                    am = domain.getAuthMechAdmin();
                    if (am == null) {
                        // fallback to zimbraAuthMech if zimbraAuthMechAdmin is not specified
                        am = domain.getAuthMech();
                    }
                } else {
                    am = domain.getAuthMech();
                }

                if (am != null) {
                    authMechStr = am;
                }
            }
        }


        if (authMechStr.startsWith(AuthMech.custom.name() + ":")) {
            return new CustomAuth(AuthMech.custom, authMechStr);
        } else {
            try {
                AuthMech authMech = AuthMech.fromString(authMechStr);

                switch (authMech) {
                    case zimbra:
                        return new ZimbraAuth(authMech);
                    case ldap:
                    case ad:
                        return new LdapAuth(authMech);
                    case kerberos5:
                        return new Kerberos5Auth(authMech);
                }
            } catch (ServiceException e) {
                ZimbraLog.account.warn("invalid auth mech", e);
            }

            ZimbraLog.account.warn("unknown value for " + Provisioning.A_zimbraAuthMech+": "
                    + authMechStr+", falling back to default mech");
            return new ZimbraAuth(AuthMech.zimbra);
        }

    }

    public static void doZimbraAuth(LdapProv prov, Domain domain, Account acct, String password,
            Map<String, Object> authCtxt)
    throws ServiceException {
        ZimbraAuth zimbraAuth = new ZimbraAuth(AuthMech.zimbra);
        zimbraAuth.doAuth(prov, domain, acct, password, authCtxt);
    }

    public boolean isZimbraAuth() {
        return false;
    }

    public abstract boolean checkPasswordAging() throws ServiceException;

    public abstract void doAuth(LdapProv prov, Domain domain, Account acct, String password,
            Map<String, Object> authCtxt)
    throws ServiceException;

    public AuthMech getMechanism() {
        return authMech;
    }

    public static String namePassedIn(Map<String, Object> authCtxt) {
        String npi;
        if (authCtxt != null) {
            npi = (String)authCtxt.get(AuthContext.AC_ACCOUNT_NAME_PASSEDIN);
            if (npi==null)
                npi = "";
        } else
            npi = "";
        return npi;
    }

    /**
     * @param acct
     * @param password
     * @param authCtxt
     * @throws ServiceException
     * @throws AuthFailedServiceException
     */
    public static boolean doTwoFactorAuth(Account acct, String password, Map<String, Object> authCtxt)
        throws ServiceException, AuthFailedServiceException {
        TwoFactorAuth twoFactorManager = TwoFactorAuth.getFactory().getTwoFactorAuth(acct);
        AppSpecificPasswords appPasswords = TwoFactorAuth.getFactory().getAppSpecificPasswords(acct);
        boolean authDone = false;
        if (twoFactorManager.twoFactorAuthRequired() && authCtxt != null) {
            //if two-factor auth is enabled, check non-http protocols against app-specific passwords
            Protocol proto = (Protocol) authCtxt.get("proto");
            switch(proto) {
            case soap:
            case http_basic:
                break;
            default:
                if (appPasswords.isEnabled()) {
                    appPasswords.authenticate(password);
                    authDone = true;
                } else {
                    throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),
                            namePassedIn(authCtxt), "invalid password");
                }
            }
        }

        return authDone;
    }

    /*
     * ZimbraAuth
     */
    public static class ZimbraAuth extends AuthMechanism {
        ZimbraAuth(AuthMech authMech) {
            super(authMech);
        }

        protected boolean isEncodedPassword(String encodedPassword) {
            return PasswordUtil.SSHA512.isSSHA512(encodedPassword) || PasswordUtil.SSHA.isSSHA(encodedPassword);
        }

        protected boolean isValidEncodedPassword(String encodedPassword, String password) {
            return PasswordUtil.SSHA512.verifySSHA512(encodedPassword, password) || PasswordUtil.SSHA.verifySSHA(encodedPassword, password);
        }

        @Override
        public boolean isZimbraAuth() {
            return true;
        }

        @Override
        public void doAuth(LdapProv prov, Domain domain, Account acct, String password,
                Map<String, Object> authCtxt) throws ServiceException {

            if (AuthMechanism.doTwoFactorAuth(acct, password, authCtxt)) {
                return;
            }

            String encodedPassword = acct.getAttr(Provisioning.A_userPassword);
            if (encodedPassword == null) {
                throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),
                        namePassedIn(authCtxt), "missing "+Provisioning.A_userPassword);
            }

            if (isEncodedPassword(encodedPassword)) {
                if (isValidEncodedPassword(encodedPassword, password)) {
                    return;
                } else {
                    acct.refreshUserCredentials();
                    String refreshedPassword = acct.getAttr(Provisioning.A_userPassword);
                    if (!isEncodedPassword(refreshedPassword)) {
                        doAuth(prov, domain, acct, password, authCtxt);
                        return;
                    }
                    if (!isValidEncodedPassword(refreshedPassword, password)) {
                        throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),
                                namePassedIn(authCtxt), "invalid password");
                    }
                    return;
                }

            } else if (acct instanceof LdapEntry) {
                // not SSHA/SSHA512, authenticate to Zimbra LDAP
                prov.zimbraLdapAuthenticate(acct, password, authCtxt);
                return;  // good password, RETURN
            }
            throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), namePassedIn(authCtxt));
        }

        @Override
        public boolean checkPasswordAging() throws ServiceException {
            return true;
        }
    }

    /*
     * LdapAuth
     */
    static class LdapAuth extends AuthMechanism {
        LdapAuth(AuthMech authMech) {
            super(authMech);
        }

        @Override
        public void doAuth(LdapProv prov, Domain domain, Account acct, String password,
                Map<String, Object> authCtxt) throws ServiceException {

            if (AuthMechanism.doTwoFactorAuth(acct, password, authCtxt)) {
                return;
            }
            prov.externalLdapAuth(domain, authMech, acct, password, authCtxt);
        }

        @Override
        public boolean checkPasswordAging() throws ServiceException {
            return false;
        }
    }

    /*
     * Kerberos5Auth
     */
    static class Kerberos5Auth extends AuthMechanism {
        Kerberos5Auth(AuthMech authMech) {
            super(authMech);
        }

        @Override
        public void doAuth(LdapProv prov, Domain domain, Account acct, String password,
                Map<String, Object> authCtxt) throws ServiceException {
            String principal = Krb5Principal.getKrb5Principal(domain, acct);

            if (principal == null)
                throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),
                        namePassedIn(authCtxt), "cannot obtain principal for " + authMech.name() + " auth");

            if (principal != null) {
                try {
                    Krb5Login.verifyPassword(principal, password);
                } catch (LoginException e) {
                    throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),
                            namePassedIn(authCtxt) + "(kerberos5 principal: " + principal + ")", e.getMessage(), e);
                }
            }
        }

        @Override
        public boolean checkPasswordAging() throws ServiceException {
            return false;
        }
    }

    /*
     * CustomAuth
     */
    static class CustomAuth extends AuthMechanism {
        private String authMechStr;  // value of the zimbraAuthMech attribute

        private String mHandlerName = "";
        private ZimbraCustomAuth mHandler;
        List<String> mArgs;

        CustomAuth(AuthMech authMech, String authMechStr) {
            super(authMech);
            this.authMechStr = authMechStr;

            /*
             * value is in the format of custom:{handler} [arg1 arg2 ...]
             * args can be quoted.  whitespace and empty string in quoted args are preserved
             *
             * e.g. http://blah.com:123    green " ocean blue   "  "" yelllow ""
             * will be parsed and passed to the custom handler as:
             * [http://blah.com:123]
             * [green]
             * [ ocean blue   ]
             * []
             * [yelllow]
             * []
             *
             */
            int handlerNameStart = authMechStr.indexOf(':');
            if (handlerNameStart != -1) {
                int handlerNameEnd = authMechStr.indexOf(' ');
                if (handlerNameEnd != -1) {
                    mHandlerName = authMechStr.substring(handlerNameStart+1, handlerNameEnd);
                    QuotedStringParser parser = new QuotedStringParser(authMechStr.substring(handlerNameEnd+1));
                    mArgs = parser.parse();
                    if (mArgs.size() == 0)
                        mArgs = null;
                } else {
                    mHandlerName = authMechStr.substring(handlerNameStart+1);
                }

                if (!StringUtil.isNullOrEmpty(mHandlerName))
                    mHandler = ZimbraCustomAuth.getHandler(mHandlerName);
            }

            if (ZimbraLog.account.isDebugEnabled()) {
                StringBuffer sb = null;
                if (mArgs != null) {
                    sb = new StringBuffer();
                    for (String s : mArgs)
                        sb.append("[" + s + "] ");
                }
                ZimbraLog.account.debug("CustomAuth: handlerName=" + mHandlerName + ", args=" + sb);
            }
        }

        @Override
        public void doAuth(LdapProv prov, Domain domain, Account acct, String password,
                Map<String, Object> authCtxt) throws ServiceException {

            if (mHandler == null)
                throw AuthFailedServiceException.AUTH_FAILED(acct.getName(),
                        namePassedIn(authCtxt), "handler " + mHandlerName +
                        " for custom auth for domain " + domain.getName() + " not found");

            try {
                mHandler.authenticate(acct, password, authCtxt, mArgs);
                return;
            } catch (Exception e) {
                if (e instanceof ServiceException) {
                    throw (ServiceException)e;
                } else {
                    String msg = e.getMessage();
                    if (StringUtil.isNullOrEmpty(msg))
                        msg = "";
                    else
                        msg = " (" + msg + ")";
                    /*
                     * include msg in the response, in addition to logs.  This is because custom
                     * auth handlers might want to pass the reason back to the client.
                     */
                    throw AuthFailedServiceException.AUTH_FAILED(acct.getName(), namePassedIn(authCtxt)+msg, msg, e);
                }
            }

        }

        @Override
        public boolean checkPasswordAging() throws ServiceException {
            if (mHandler == null)
                throw ServiceException.FAILURE("custom auth handler " + mHandlerName + " not found", null);
            return mHandler.checkPasswordAging();
        }
    }

    public static void main(String[] args) {
        QuotedStringParser parser = new QuotedStringParser( "http://blah.com:123    green \" ocean blue   \"  \"\" yelllow \"\"");
        List<String> tokens = parser.parse();
        int i = 0;
        for (String s : tokens)
            System.out.format("%d [%s]\n", ++i, s);

        CustomAuth ca = new CustomAuth(AuthMech.custom,
                "custom:sample http://blah.com:123    green \" ocean blue   \"  \"\" yelllow \"\"");

    }
}
