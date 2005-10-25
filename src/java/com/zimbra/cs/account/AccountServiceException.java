/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jun 1, 2004
 *
 */
package com.zimbra.cs.account;

import com.zimbra.cs.service.ServiceException;


/**
 * @author schemers
 * 
 */
public class AccountServiceException extends ServiceException {

    public static final String AUTH_FAILED        = "account.AUTH_FAILED";
    public static final String CHANGE_PASSWORD    = "account.CHANGE_PASSWORD";    
    public static final String PASSWORD_LOCKED    = "account.PASSWORD_LOCKED";
    public static final String PASSWORD_CHANGE_TOO_SOON  = "account.PASSWORD_CHANGE_TOO_SOON";    
    public static final String PASSWORD_RECENTLY_USED = "account.PASSWORD_RECENTLY_USED";
    public static final String INVALID_PASSWORD   = "account.INVALID_PASSWORD";
    public static final String INVALID_ATTR_NAME  = "account.INVALID_ATTR_NAME";
    public static final String INVALID_ATTR_VALUE = "account.INVALID_ATTR_VALUE";
    public static final String NO_SUCH_ACCOUNT    = "account.NO_SUCH_ACCOUNT";
    public static final String NO_SUCH_ALIAS      = "account.NO_SUCH_ALIAS";
    public static final String NO_SUCH_GROUP      = "account.NO_SUCH_GROUP";
    public static final String NO_SUCH_DOMAIN     = "account.NO_SUCH_DOMAIN";    
    public static final String NO_SUCH_COS        = "account.NO_SUCH_COS";        
    public static final String NO_SUCH_SERVER     = "account.NO_SUCH_SERVER";        
    public static final String NO_SUCH_DISTRIBUTION_LIST = "account.NO_SUCH_DISTRIBUTION_LIST";
    public static final String ACCOUNT_EXISTS     = "account.ACCOUNT_EXISTS";        
    public static final String DOMAIN_EXISTS      = "account.DOMAIN_EXISTS";
    public static final String DOMAIN_NOT_EMPTY   = "account.DOMAIN_NOT_EMPTY";
    public static final String COS_EXISTS         = "account.COS_EXISTS";
    public static final String SERVER_EXISTS      = "account.SERVER_EXISTS";
    public static final String DISTRIBUTION_LIST_EXISTS = "account.DISTRIBUTION_LIST_EXISTS";
    public static final String MAINTENANCE_MODE   = "account.MAINTENANCE_MODE";
    
    private AccountServiceException(String message, String code, boolean isReceiversFault) {
        super(message, code, isReceiversFault);
    }
    
    private AccountServiceException(String message, String code, boolean isReceiversFault, Throwable cause) {
        super(message, code, isReceiversFault, cause);
    }
    
    public static AccountServiceException AUTH_FAILED(String accountName) {
        return new AccountServiceException("authentication failed for " + accountName, AUTH_FAILED, SENDERS_FAULT, null);
    }

    public static AccountServiceException AUTH_FAILED(String accountName, Throwable t) {
        return new AccountServiceException("authentication failed for " + accountName, AUTH_FAILED, SENDERS_FAULT, t);
    }    
    
    public static AccountServiceException CHANGE_PASSWORD() {
        return new AccountServiceException("must change password", CHANGE_PASSWORD, SENDERS_FAULT, null);
    }
    
    public static AccountServiceException PASSWORD_LOCKED() {
        return new AccountServiceException("password is locked and can't be changed", PASSWORD_LOCKED, SENDERS_FAULT, null);
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

    public static AccountServiceException NO_SUCH_GROUP(String name) {
        return new AccountServiceException("no such group: "+name, NO_SUCH_GROUP, SENDERS_FAULT, null);
    }
    
    public static AccountServiceException NO_SUCH_DOMAIN(String name) {
        return new AccountServiceException("no such domain: "+name, NO_SUCH_DOMAIN, SENDERS_FAULT, null);
    }    
    
    public static AccountServiceException DOMAIN_NOT_EMPTY(String name) {
        return new AccountServiceException("domain not empty: "+name, DOMAIN_NOT_EMPTY, SENDERS_FAULT, null);
    }        
    
    public static AccountServiceException NO_SUCH_COS(String name) {
        return new AccountServiceException("no such cos: "+name, NO_SUCH_COS, SENDERS_FAULT, null);
    }    
    
    public static AccountServiceException NO_SUCH_SERVER(String name) {
        return new AccountServiceException("no such server: "+name, NO_SUCH_SERVER, SENDERS_FAULT, null);
    }    

    public static AccountServiceException NO_SUCH_DISTRIBUTION_LIST(String name) {
    	return new AccountServiceException("no such distribution list: " + name, NO_SUCH_DISTRIBUTION_LIST, 
                SENDERS_FAULT, null);   
    }
    
    public static AccountServiceException ACCOUNT_EXISTS(String name) {
        return new AccountServiceException("account already exists: "+name, ACCOUNT_EXISTS, SENDERS_FAULT, null);
    }
    
    public static AccountServiceException DOMAIN_EXISTS(String name) {
        return new AccountServiceException("domain already exists: "+name, DOMAIN_EXISTS, SENDERS_FAULT, null);
    }

    public static AccountServiceException COS_EXISTS(String name) {
        return new AccountServiceException("cos already exists: "+name, COS_EXISTS, SENDERS_FAULT, null);
    }
    
    public static AccountServiceException SERVER_EXISTS(String name) {
        return new AccountServiceException("server already exists: "+name, SERVER_EXISTS, SENDERS_FAULT, null);
    }    
    
    public static AccountServiceException DISTRIBUTION_LIST_EXISTS(String name) {
        return new AccountServiceException("distribution list already exists: "+name, DISTRIBUTION_LIST_EXISTS, SENDERS_FAULT, null);
    }    
    
    public static AccountServiceException MAINTENANCE_MODE() {
        return new AccountServiceException("account is in maintenance mode", MAINTENANCE_MODE, RECEIVERS_FAULT, null);
    }

    public static AccountServiceException PASSWORD_RECENTLY_USED() {
        return new AccountServiceException("password was recently used", PASSWORD_RECENTLY_USED, SENDERS_FAULT, null);        
    }

}
