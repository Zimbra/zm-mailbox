/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on Jun 1, 2004
 *
 */
package com.zimbra.cs.account;

import com.zimbra.common.service.ServiceException;


/**
 * @author schemers
 * 
 */
@SuppressWarnings("serial")
public class AccountServiceException extends ServiceException {

    public static final String AUTH_FAILED        = "account.AUTH_FAILED";
    public static final String CHANGE_PASSWORD    = "account.CHANGE_PASSWORD";    
    public static final String PASSWORD_LOCKED    = "account.PASSWORD_LOCKED";
    public static final String PASSWORD_CHANGE_TOO_SOON  = "account.PASSWORD_CHANGE_TOO_SOON";    
    public static final String PASSWORD_RECENTLY_USED = "account.PASSWORD_RECENTLY_USED";
    public static final String INVALID_PASSWORD   = "account.INVALID_PASSWORD";
    public static final String INVALID_ATTR_NAME  = "account.INVALID_ATTR_NAME";
    public static final String INVALID_ATTR_VALUE = "account.INVALID_ATTR_VALUE";
    public static final String MULTIPLE_ACCOUNTS_MATCHED  = "account.MULTIPLE_ACCOUNTS_MATCHED"; 
    public static final String MULTIPLE_DOMAINS_MATCHED  = "account.MULTIPLE_DOMAINS_MATCHED";
    public static final String NO_SUCH_ACCOUNT    = "account.NO_SUCH_ACCOUNT";
    public static final String NO_SUCH_ALIAS      = "account.NO_SUCH_ALIAS";
    public static final String NO_SUCH_DOMAIN     = "account.NO_SUCH_DOMAIN";    
    public static final String NO_SUCH_COS        = "account.NO_SUCH_COS";        
    public static final String NO_SUCH_IDENTITY   = "account.NO_SUCH_IDENTITY";
    public static final String NO_SUCH_SIGNATURE   = "account.NO_SUCH_SIGNATURE";
    public static final String NO_SUCH_DATA_SOURCE = "account.NO_SUCH_DATA_SOURCE";
    public static final String NO_SUCH_SERVER     = "account.NO_SUCH_SERVER";        
    public static final String NO_SUCH_ZIMLET     = "account.NO_SUCH_ZIMLET";        
    public static final String NO_SUCH_DISTRIBUTION_LIST = "account.NO_SUCH_DISTRIBUTION_LIST";
    public static final String NO_SUCH_CALENDAR_RESOURCE = "account.NO_SUCH_CALENDAR_RESOURCE";
    public static final String MEMBER_EXISTS      = "account.MEMBER_EXISTS";
    public static final String NO_SUCH_MEMBER     = "account.NO_SUCH_MEMBER";
    public static final String ACCOUNT_EXISTS     = "account.ACCOUNT_EXISTS";        
    public static final String DOMAIN_EXISTS      = "account.DOMAIN_EXISTS";
    public static final String DOMAIN_NOT_EMPTY   = "account.DOMAIN_NOT_EMPTY";
    public static final String COS_EXISTS         = "account.COS_EXISTS";
    public static final String SERVER_EXISTS      = "account.SERVER_EXISTS";
    public static final String DISTRIBUTION_LIST_EXISTS = "account.DISTRIBUTION_LIST_EXISTS";
    public static final String MAINTENANCE_MODE   = "account.MAINTENANCE_MODE";
    public static final String ACCOUNT_INACTIVE   = "account.ACCOUNT_INACTIVE";
    public static final String IDENTITY_EXISTS     = "account.IDENTITY_EXISTS";    
    public static final String TOO_MANY_IDENTITIES = "account.TOO_MANY_IDENTITIES";
    public static final String SIGNATURE_EXISTS = "account.SIGNATURE_EXISTS";    
    public static final String TOO_MANY_SIGNATURES = "account.TOO_MANY_SIGNATURES";
    public static final String DATA_SOURCE_EXISTS = "account.DATA_SOURCE_EXISTS";        
    public static final String TOO_MANY_DATA_SOURCES = "account.TOO_MANY_DATA_SOURCES";
    public static final String TOO_MANY_ACCOUNTS = "account.TOO_MANY_ACCOUNTS";
    public static final String TOO_MANY_SEARCH_RESULTS = "account.TOO_MANY_SEARCH_RESULTS";

    private AccountServiceException(String message, String code, boolean isReceiversFault) {
        super(message, code, isReceiversFault);
    }
    
    private AccountServiceException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
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
        private String mReason;
        private String mAcctName;  // real account name
        
        private AuthFailedServiceException(String acctName, String namePassedIn, String reason, String code, boolean isReceiversFault, Throwable cause) {
            super("authentication failed for " + namePassedIn, code, isReceiversFault, cause);
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
        
        public static AuthFailedServiceException AUTH_FAILED( String reason, Throwable t) {
            return new AuthFailedServiceException("N/A", "N/A", reason, AUTH_FAILED, SENDERS_FAULT, t);
        }   
        
 
    }
    
    public static AccountServiceException CHANGE_PASSWORD() {
        return new AccountServiceException("must change password", CHANGE_PASSWORD, SENDERS_FAULT, null);
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

    public static AccountServiceException PASSWORD_CHANGE_TOO_SOON() {
        return new AccountServiceException("password can't be chnaged yet", PASSWORD_CHANGE_TOO_SOON, SENDERS_FAULT, null);
    }
    
    public static AccountServiceException INVALID_PASSWORD(String desc) {
        return new AccountServiceException("invalid password: "+desc, INVALID_PASSWORD, SENDERS_FAULT, null);
    }
    
    public static AccountServiceException INVALID_ATTR_NAME(String msg, Throwable t) {
        return new AccountServiceException(msg, INVALID_ATTR_NAME, SENDERS_FAULT, t);
    }
    
    public static AccountServiceException INVALID_ATTR_VALUE(String msg, Throwable t) {
        return new AccountServiceException(msg, INVALID_ATTR_VALUE, SENDERS_FAULT, t);
    }
    
    public static AccountServiceException NO_SUCH_ACCOUNT(String name) {
        return new AccountServiceException("no such account: "+name, NO_SUCH_ACCOUNT, SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_ALIAS(String name) {
        return new AccountServiceException("no such alias: "+name, NO_SUCH_ALIAS, SENDERS_FAULT, null);
    }
    
    public static AccountServiceException NO_SUCH_DOMAIN(String name) {
        return new AccountServiceException("no such domain: "+name, NO_SUCH_DOMAIN, SENDERS_FAULT, null);
    }    
    
    public static AccountServiceException DOMAIN_NOT_EMPTY(String name) {
        return new AccountServiceException("domain not empty: "+name+". You may beed to remove the value in zimbraNotebookAccount attribute before deleting accounts in this domain.", DOMAIN_NOT_EMPTY, SENDERS_FAULT, null);
    }        
    
    public static AccountServiceException NO_SUCH_COS(String name) {
        return new AccountServiceException("no such cos: "+name, NO_SUCH_COS, SENDERS_FAULT, null);
    }    
    
    public static AccountServiceException NO_SUCH_SERVER(String name) {
        return new AccountServiceException("no such server: "+name, NO_SUCH_SERVER, SENDERS_FAULT, null);
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

    public static AccountServiceException NO_SUCH_DISTRIBUTION_LIST(String name) {
        return new AccountServiceException("no such distribution list: " + name, NO_SUCH_DISTRIBUTION_LIST, 
                SENDERS_FAULT, null);   
    }

    public static AccountServiceException NO_SUCH_CALENDAR_RESOURCE(String name) {
        return new AccountServiceException("no such calendar resource: " + name, NO_SUCH_CALENDAR_RESOURCE,
                SENDERS_FAULT, null);
    }

    public static AccountServiceException NO_SUCH_MEMBER(String dlName, String members) {
        return new AccountServiceException("non-existent members: " + members + " in distribution list: " + dlName, NO_SUCH_MEMBER, SENDERS_FAULT, null);   
    }

    public static AccountServiceException ACCOUNT_EXISTS(String name) {
        return new AccountServiceException("email address already exists: "+name, ACCOUNT_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException DOMAIN_EXISTS(String name) {
        return new AccountServiceException("domain already exists: " + name, DOMAIN_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException COS_EXISTS(String name) {
        return new AccountServiceException("cos already exists: " + name, COS_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException SERVER_EXISTS(String name) {
        return new AccountServiceException("server already exists: " + name, SERVER_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException DISTRIBUTION_LIST_EXISTS(String name) {
        return new AccountServiceException("email address already exists: " + name, DISTRIBUTION_LIST_EXISTS, SENDERS_FAULT, null);
    }
    
    public static AccountServiceException IDENTITY_EXISTS(String name) {
        return new AccountServiceException("identity already exists: " + name, IDENTITY_EXISTS, SENDERS_FAULT, null);
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

    public static AccountServiceException DATA_SOURCE_EXISTS(String name) {
        return new AccountServiceException("data source already exists: " + name, DATA_SOURCE_EXISTS, SENDERS_FAULT, null);
    }
    
    public static AccountServiceException TOO_MANY_DATA_SOURCES() {
        return new AccountServiceException("too many data sources. can't create any more", TOO_MANY_DATA_SOURCES, SENDERS_FAULT, null);
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
}
