/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

/*
 * Created on Jun 1, 2004
 *
 */
package com.zimbra.cs.account;

import com.google.common.base.Strings;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;

/**
 * @author schemers
 *
 */
@SuppressWarnings("serial")
public class AccountServiceException extends ServiceException {

    public static final String AUTH_FAILED        = "account.AUTH_FAILED";
    public static final String CHANGE_PASSWORD    = "account.CHANGE_PASSWORD";
    public static final String RESET_PASSWORD    = "account.RESET_PASSWORD";
    public static final String PASSWORD_LOCKED    = "account.PASSWORD_LOCKED";
    public static final String PASSWORD_CHANGE_TOO_SOON  = "account.PASSWORD_CHANGE_TOO_SOON";
    public static final String PASSWORD_RECENTLY_USED = "account.PASSWORD_RECENTLY_USED";
    public static final String INVALID_PASSWORD   = "account.INVALID_PASSWORD";
    public static final String INVALID_ATTR_NAME  = "account.INVALID_ATTR_NAME";
    public static final String INVALID_ATTR_VALUE = "account.INVALID_ATTR_VALUE";
    public static final String MULTIPLE_ACCOUNTS_MATCHED  = "account.MULTIPLE_ACCOUNTS_MATCHED";
    public static final String MULTIPLE_DOMAINS_MATCHED  = "account.MULTIPLE_DOMAINS_MATCHED";
    public static final String MULTIPLE_ENTRIES_MATCHED  = "account.MULTIPLE_ENTRIES_MATCHED";
    public static final String NO_SMIME_CONFIG    = "account.NO_SMIME_CONFIG";
    public static final String NO_SUCH_ACCOUNT    = "account.NO_SUCH_ACCOUNT";
    public static final String NO_SUCH_ALIAS      = "account.NO_SUCH_ALIAS";
    public static final String NO_SUCH_COS        = "account.NO_SUCH_COS";
    public static final String NO_SUCH_GRANT      = "account.NO_SUCH_GRANT";
    public static final String NO_SUCH_IDENTITY   = "account.NO_SUCH_IDENTITY";
    public static final String NO_SUCH_SIGNATURE   = "account.NO_SUCH_SIGNATURE";
    public static final String NO_SUCH_DATA_SOURCE = "account.NO_SUCH_DATA_SOURCE";
    public static final String NO_SUCH_RIGHT       = "account.NO_SUCH_RIGHT";
    public static final String NO_SUCH_SERVER      = "account.NO_SUCH_SERVER";
    public static final String NO_SUCH_ALWAYSONCLUSTER      = "account.NO_SUCH_ALWAYSONCLUSTER";
    public static final String NO_SUCH_UC_SERVICE  = "account.NO_SUCH_UC_SERVICE";
    public static final String NO_SUCH_SHARE_LOCATOR = "account.NO_SUCH_SHARE_LOCATOR";
    public static final String NO_SUCH_ZIMLET     = "account.NO_SUCH_ZIMLET";
    public static final String NO_SUCH_XMPP_COMPONENT = "account.NO_SUCH_XMPP_COMPONENT";
    public static final String NO_SUCH_DISTRIBUTION_LIST = "account.NO_SUCH_DISTRIBUTION_LIST";
    public static final String NO_SUCH_ADDRESS_LIST = "account.NO_SUCH_ADDRESS_LIST";
    public static final String NO_SUCH_GROUP      = "account.NO_SUCH_GROUP";
    public static final String NO_SUCH_CALENDAR_RESOURCE = "account.NO_SUCH_CALENDAR_RESOURCE";
    public static final String NO_SUCH_EXTERNAL_ENTRY    = "account.NO_SUCH_EXTERNAL_ENTRY";
    public static final String MEMBER_EXISTS      = "account.MEMBER_EXISTS";
    public static final String NO_SUCH_MEMBER     = "account.NO_SUCH_MEMBER";
    public static final String ACCOUNT_EXISTS     = "account.ACCOUNT_EXISTS";
    public static final String ALIAS_EXISTS       = "account.ALIAS_EXISTS";
    public static final String DOMAIN_EXISTS      = "account.DOMAIN_EXISTS";
    public static final String DOMAIN_NOT_EMPTY   = "account.DOMAIN_NOT_EMPTY";
    public static final String COS_EXISTS         = "account.COS_EXISTS";
    public static final String RIGHT_EXISTS       = "account.RIGHT_EXISTS";
    public static final String SERVER_EXISTS      = "account.SERVER_EXISTS";
    public static final String ALWAYSONCLUSTER_EXISTS = "account.ALWAYSONCLUSTER_EXISTS";
    public static final String SHARE_LOCATOR_EXISTS = "account.SHARE_LOCATOR_EXISTS";
    public static final String NO_SHARE_EXISTS = "account.NO_SHARE_EXISTS";
    public static final String ZIMLET_EXISTS      = "account.ZIMLET_EXISTS";
    public static final String DISTRIBUTION_LIST_EXISTS = "account.DISTRIBUTION_LIST_EXISTS";
    public static final String UC_SERVICE_EXISTS  = "account.UC_SERVICE_EXISTS";
    public static final String MAINTENANCE_MODE   = "account.MAINTENANCE_MODE";
    public static final String ACCOUNT_INACTIVE   = "account.ACCOUNT_INACTIVE";
    public static final String IDENTITY_EXISTS     = "account.IDENTITY_EXISTS";
    public static final String TOO_MANY_IDENTITIES = "account.TOO_MANY_IDENTITIES";
    public static final String SIGNATURE_EXISTS = "account.SIGNATURE_EXISTS";
    public static final String TOO_MANY_SIGNATURES = "account.TOO_MANY_SIGNATURES";
    public static final String TOO_MANY_ZIMLETUSERPROPERTIES = "account.TOO_MANY_ZIMLETUSERPROPERTIES";
    public static final String DATA_SOURCE_EXISTS = "account.DATA_SOURCE_EXISTS";
    public static final String IM_COMPONENT_EXISTS = "account.IM_COMPONENT_EXISTS";
    public static final String TOO_MANY_DATA_SOURCES = "account.TOO_MANY_DATA_SOURCES";
    public static final String TOO_MANY_ACCOUNTS = "account.TOO_MANY_ACCOUNTS";
    public static final String TOO_MANY_SEARCH_RESULTS = "account.TOO_MANY_SEARCH_RESULTS";
    public static final String TOO_MANY_TRUSTED_SENDERS = "account.TOO_MANY_TRUSTED_SENDERS";
    public static final String TWO_FACTOR_SETUP_REQUIRED = "account.TWO_FACTOR_SETUP_REQUIRED";
    public static final String INVALID_TRUSTED_DEVICE_TOKEN = "account.INVALID_TRUSTED_DEVICE_TOKEN";
    public static final String TWO_FACTOR_AUTH_FAILED = "account.TWO_FACTOR_AUTH_FAILED";
    public static final String TWO_FACTOR_AUTH_REQUIRED = "account.TWO_FACTOR_AUTH_REQUIRED";
    public static final String NO_SUCH_ORG_UNIT = "account.NO_SUCH_ORG_UNIT";
    public static final String WEB_CLIENT_ACCESS_NOT_ALLOWED = "account.WEB_CLIENT_ACCESS_NOT_ALLOWED";

    private AccountServiceException(String message, String code, boolean isReceiversFault) {
        super(message, code, isReceiversFault);
    }

    protected AccountServiceException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
    }

    protected AccountServiceException(String message, String code, boolean isReceiversFault, Throwable cause, Argument... arguments) {
        super(message, code, isReceiversFault, cause, arguments);
    }

    /*
     * AuthFailedServiceException provides the functionality of keeping the reason(why auth failed) separately
     * from the message.
     *
     * getMessage() returns a generic message "authentication failed for {account-name}"
     *              This info is returned to the client.
     *
     * getReason() returns the reason why auth failed.
     *             This info is logged but not returned to the client, for security reasons.
     */
    public static class AuthFailedServiceException extends AccountServiceException {
        private final String mReason;
        private final String mAcctName;  // real account name

        private AuthFailedServiceException(String acctName, String namePassedIn, String reason, String code, boolean isReceiversFault, Throwable cause) {
            super("authentication failed for [" + Strings.nullToEmpty(namePassedIn) + "]", code, isReceiversFault, cause);
            mReason = reason;
            mAcctName = acctName;
        }

        public String getReason() {
            return mReason==null?"":mReason;
        }

        public String getReason(String format) {
            if (mReason==null)
                return "";
            else
                return String.format(format, mReason);
        }

        public static AuthFailedServiceException AUTH_FAILED(String acctName, String namePassedIn) {
            return AUTH_FAILED(acctName, namePassedIn, null, null);
        }

        public static AuthFailedServiceException AUTH_FAILED(String acctName, String namePassedIn, String reason) {
            return AUTH_FAILED(acctName, namePassedIn, reason, null);
        }

        public static AuthFailedServiceException AUTH_FAILED(String acctName, String namePassedIn, String reason, Throwable t) {
            return new AuthFailedServiceException(acctName, namePassedIn, reason, AUTH_FAILED, SENDERS_FAULT, t);
        }

        public static AuthFailedServiceException AUTH_FAILED(String namePassedIn, String reason, Throwable t) {
            return new AuthFailedServiceException("N/A", namePassedIn, reason, AUTH_FAILED, SENDERS_FAULT, t);
        }

        public static AuthFailedServiceException AUTH_FAILED(String reason, Throwable t) {
            return new AuthFailedServiceException("N/A", "", reason, AUTH_FAILED, SENDERS_FAULT, t);
        }

        public static AuthFailedServiceException AUTH_FAILED(String reason) {
            return new AuthFailedServiceException("N/A", "", reason, AUTH_FAILED, SENDERS_FAULT, null);
        }
    }

    public static AccountServiceException CHANGE_PASSWORD() {
        return new AccountServiceException("must change password", CHANGE_PASSWORD, SENDERS_FAULT, null);
    }

    public static AccountServiceException RESET_PASSWORD() {
        return new AccountServiceException("reset password", RESET_PASSWORD, SENDERS_FAULT, null);
    }

    public static AccountServiceException PASSWORD_LOCKED() {
        return new AccountServiceException("password is locked and can't be changed", PASSWORD_LOCKED, SENDERS_FAULT, null);
    }

    public static AccountServiceException MULTIPLE_ACCOUNTS_MATCHED(String desc) {
        return new AccountServiceException("lookup returned multiple accounts: "+desc, MULTIPLE_ACCOUNTS_MATCHED, SENDERS_FAULT, null);
    }


    public static AccountServiceException MULTIPLE_DOMAINS_MATCHED(String desc) {
        return new AccountServiceException("lookup returned multiple domains: "+desc, MULTIPLE_DOMAINS_MATCHED, SENDERS_FAULT, null);
    }

    public static AccountServiceException MULTIPLE_ENTRIES_MATCHED(String desc, Throwable t) {
        return new AccountServiceException("lookup returned multiple entries: "+desc, MULTIPLE_ENTRIES_MATCHED, SENDERS_FAULT, t);
    }

    public static AccountServiceException PASSWORD_CHANGE_TOO_SOON() {
        return new AccountServiceException("password can't be changed yet", PASSWORD_CHANGE_TOO_SOON, SENDERS_FAULT, null);
    }

    public static AccountServiceException INVALID_PASSWORD(String desc) {
        return new AccountServiceException("invalid password: "+desc, INVALID_PASSWORD, SENDERS_FAULT, null);
    }

    public static AccountServiceException INVALID_PASSWORD(String desc, Argument... arguments) {
        return new AccountServiceException("invalid password: "+desc, INVALID_PASSWORD, SENDERS_FAULT, null, arguments);
    }

    public static AccountServiceException INVALID_ATTR_NAME(String msg, Throwable t) {
        return new AccountServiceException(msg, INVALID_ATTR_NAME, SENDERS_FAULT, t);
    }

    public static AccountServiceException INVALID_ATTR_VALUE(String msg, Throwable t) {
        return new AccountServiceException(msg, INVALID_ATTR_VALUE, SENDERS_FAULT, t);
    }

    public static AccountServiceException NO_SMIME_CONFIG(String desc) {
        return new AccountServiceException("no SMIME config: "+ desc, NO_SMIME_CONFIG, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_ACCOUNT(String name) {
        return new AccountServiceException("no such account: "+name, NO_SUCH_ACCOUNT, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_ALIAS(String name) {
        return new AccountServiceException("no such alias: "+name, NO_SUCH_ALIAS, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_DOMAIN(String name) {
        return new AccountServiceException("no such domain: "+name, Constants.ERROR_CODE_NO_SUCH_DOMAIN, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_ORG_UNIT(String name) {
        return new AccountServiceException("no such organizational unit: " + name, NO_SUCH_ORG_UNIT, SENDERS_FAULT, null);
    }

    public static AccountServiceException DOMAIN_NOT_EMPTY(String name, Exception e) {
        return new AccountServiceException("domain not empty: "+name, DOMAIN_NOT_EMPTY, SENDERS_FAULT, e);
    }

    public static AccountServiceException NO_SUCH_COS(String name) {
        return new AccountServiceException("no such cos: "+name, NO_SUCH_COS, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_SHARE_LOCATOR(String id) {
        return new AccountServiceException("no such share locator: "+id, NO_SUCH_SHARE_LOCATOR, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_GRANT(String grant) {
        return new AccountServiceException("no such grant: "+grant, NO_SUCH_GRANT, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_RIGHT(String name) {
        return new AccountServiceException("no such right: "+name, NO_SUCH_RIGHT, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_SERVER(String name) {
        return new AccountServiceException("no such server: "+name, NO_SUCH_SERVER, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_ALWAYSONCLUSTER(String name) {
        return new AccountServiceException("no such alwaysoncluster: "+name, NO_SUCH_ALWAYSONCLUSTER, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_UC_SERVICE(String name) {
        return new AccountServiceException("no such uc service: "+name, NO_SUCH_UC_SERVICE, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_IDENTITY(String name) {
        return new AccountServiceException("no such identity: "+name, NO_SUCH_IDENTITY, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_SIGNATURE(String name) {
        return new AccountServiceException("no such signature: "+name, NO_SUCH_SIGNATURE, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_DATA_SOURCE(String id) {
        return new AccountServiceException("no such data source: "+id, NO_SUCH_DATA_SOURCE, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_ZIMLET(String name) {
        return new AccountServiceException("no such zimlet: "+name, NO_SUCH_ZIMLET, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_XMPP_COMPONENT(String name) {
        return new AccountServiceException("no such xmpp component: "+name, NO_SUCH_XMPP_COMPONENT, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_DISTRIBUTION_LIST(String name) {
        return new AccountServiceException("no such distribution list: " + name, NO_SUCH_DISTRIBUTION_LIST,
                SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_ADDRESS_LIST(String name) {
        return new AccountServiceException("no such address list: " + name, NO_SUCH_ADDRESS_LIST,
                SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_GROUP(String name) {
        return new AccountServiceException("no such group: " + name, NO_SUCH_GROUP,
                SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_CALENDAR_RESOURCE(String name) {
        return new AccountServiceException("no such calendar resource: " + name, NO_SUCH_CALENDAR_RESOURCE,
                SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_EXTERNAL_ENTRY(String name) {
        return new AccountServiceException("no such external entry: " + name, NO_SUCH_EXTERNAL_ENTRY,
                SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_MEMBER(String dlName, String members) {
        return new AccountServiceException("non-existent members: " + members + " in distribution list: " + dlName, NO_SUCH_MEMBER, SENDERS_FAULT, null);
    }

    public static AccountServiceException ACCOUNT_EXISTS(String name) {
        return new AccountServiceException("email address already exists: "+name, ACCOUNT_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException ACCOUNT_EXISTS(String name, String atDn, Throwable t) {
        return new AccountServiceException("email address already exists: "+name + ", at DN: " + atDn, ACCOUNT_EXISTS, SENDERS_FAULT, t);
    }

    public static AccountServiceException DOMAIN_EXISTS(String name) {
        return new AccountServiceException("domain already exists: " + name, DOMAIN_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException COS_EXISTS(String name) {
        return new AccountServiceException("cos already exists: " + name, COS_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException RIGHT_EXISTS(String name) {
        return new AccountServiceException("right already exists: " + name, RIGHT_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException SERVER_EXISTS(String name) {
        return new AccountServiceException("server already exists: " + name, SERVER_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException ALWAYSONCLUSTER_EXISTS(String name) {
        return new AccountServiceException("alwaysOnCluster already exists: " + name, ALWAYSONCLUSTER_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException SHARE_LOCATOR_EXISTS(String id) {
        return new AccountServiceException("share locator already exists: " + id, SHARE_LOCATOR_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SHARE_EXISTS() {
        return new AccountServiceException("no share exists: ", NO_SHARE_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException ZIMLET_EXISTS(String name) {
        return new AccountServiceException("zimlet already exists: " + name, ZIMLET_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException DISTRIBUTION_LIST_EXISTS(String name) {
        return new AccountServiceException("email address already exists: " + name, DISTRIBUTION_LIST_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException IDENTITY_EXISTS(String name) {
        return new AccountServiceException("identity already exists: " + name, IDENTITY_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException UC_SERVICE_EXISTS(String name) {
        return new AccountServiceException("uc service already exists: " + name, UC_SERVICE_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException TOO_MANY_IDENTITIES() {
        return new AccountServiceException("too many identities. can't create any more", TOO_MANY_IDENTITIES, SENDERS_FAULT, null);
    }

    public static AccountServiceException SIGNATURE_EXISTS(String name) {
        return new AccountServiceException("signature already exists: " + name, SIGNATURE_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException TOO_MANY_SIGNATURES() {
        return new AccountServiceException("too many signatures. can't create any more", TOO_MANY_SIGNATURES, SENDERS_FAULT, null);
    }
    public static AccountServiceException TOO_MANY_ZIMLETUSERPROPERTIES() {
        return new AccountServiceException("too many user properties for zimlets. can't create any more", TOO_MANY_ZIMLETUSERPROPERTIES, SENDERS_FAULT, null);
    }
    public static AccountServiceException DATA_SOURCE_EXISTS(String name) {
        return new AccountServiceException("data source already exists: " + name, DATA_SOURCE_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException TOO_MANY_DATA_SOURCES() {
        return new AccountServiceException("too many data sources. can't create any more", TOO_MANY_DATA_SOURCES, SENDERS_FAULT, null);
    }

    public static AccountServiceException IM_COMPONENT_EXISTS(String name) {
        return new AccountServiceException("IM Component already exists: " + name, IM_COMPONENT_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException MAINTENANCE_MODE() {
        return new AccountServiceException("account is in maintenance mode", MAINTENANCE_MODE, RECEIVERS_FAULT, null);
    }

    public static AccountServiceException ACCOUNT_INACTIVE(String name) {
        return new AccountServiceException("account is not active:" + name, ACCOUNT_INACTIVE, RECEIVERS_FAULT, null);
    }

    public static AccountServiceException PASSWORD_RECENTLY_USED() {
        return new AccountServiceException("password was recently used", PASSWORD_RECENTLY_USED, SENDERS_FAULT, null);
    }

    public static AccountServiceException TOO_MANY_ACCOUNTS(String str) {
        return new AccountServiceException("number of accounts reached the limit: "+str, TOO_MANY_ACCOUNTS, RECEIVERS_FAULT);
    }

    public static AccountServiceException TOO_MANY_SEARCH_RESULTS(String str, Exception e) {
        return new AccountServiceException("number of results exceeded the limit: "+str, TOO_MANY_SEARCH_RESULTS, RECEIVERS_FAULT, e);
    }

    public static AccountServiceException TOO_MANY_TRUSTED_SENDERS(String message) {
        return new AccountServiceException(message, TOO_MANY_TRUSTED_SENDERS, SENDERS_FAULT, null);
    }

    public static AccountServiceException TWO_FACTOR_SETUP_REQUIRED() {
        return new AccountServiceException("two-factor authentication setup required", TWO_FACTOR_SETUP_REQUIRED, SENDERS_FAULT, null);
    }

    public static AccountServiceException INVALID_TRUSTED_DEVICE_TOKEN() {
        return new AccountServiceException("invalid trusted device token", INVALID_TRUSTED_DEVICE_TOKEN, SENDERS_FAULT, null);
    }

    public static AuthFailedServiceException TWO_FACTOR_AUTH_FAILED(String acctName, String namePassedIn, String reason) {
        return new AuthFailedServiceException(acctName, namePassedIn, reason, TWO_FACTOR_AUTH_FAILED, SENDERS_FAULT, null);
    }

    public static AccountServiceException TWO_FACTOR_AUTH_REQUIRED() {
        return new AccountServiceException("two-factor auth required", TWO_FACTOR_AUTH_REQUIRED, SENDERS_FAULT, null);
    }

    public static AccountServiceException ALIAS_EXISTS(String name) {
        return new AccountServiceException("email address alias already exists: "+name, ALIAS_EXISTS, SENDERS_FAULT, null);
    }


    public static AccountServiceException WEB_CLIENT_ACCESS_NOT_ALLOWED(String name) {
        return new AccountServiceException("web client access not allowed: " + name, WEB_CLIENT_ACCESS_NOT_ALLOWED, SENDERS_FAULT, null);
    }
}
