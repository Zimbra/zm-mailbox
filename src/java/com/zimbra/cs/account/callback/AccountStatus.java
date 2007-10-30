/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
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

package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning;

public class AccountStatus extends AttributeCallback {
	
    private static final String KEY = AccountStatus.class.getName();

    /**
     * disable mail delivery if account status is changed to closed
     * reset lockout attributes if account status is changed to active
     */
    @SuppressWarnings("unchecked")
    public void preModify(Map context, String attrName, Object value,
            Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {
        
        if (!(value instanceof String))
            throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraAccountStatus+" is a single-valued attribute", null);
        
        String status = (String) value;

        if (status.equals(Provisioning.ACCOUNT_STATUS_CLOSED)) {
            attrsToModify.put(Provisioning.A_zimbraMailStatus, Provisioning.MAIL_STATUS_DISABLED);
        } else if (attrsToModify.get(Provisioning.A_zimbraMailStatus) == null) {
            // the request is not also changing zimbraMailStatus, set = zimbraMailStatus to enabled
            attrsToModify.put(Provisioning.A_zimbraMailStatus, Provisioning.MAIL_STATUS_ENABLED);
        }
       
        
        if ((entry instanceof Account) && (status.equals(Provisioning.ACCOUNT_STATUS_ACTIVE))) {
            if (entry.getAttr(Provisioning.A_zimbraPasswordLockoutFailureTime, null) != null) 
                attrsToModify.put(Provisioning.A_zimbraPasswordLockoutFailureTime, "");
            if (entry.getAttr(Provisioning.A_zimbraPasswordLockoutLockedTime, null) != null)             
                attrsToModify.put(Provisioning.A_zimbraPasswordLockoutLockedTime, "");
        }
    }

    /**
     * need to keep track in context on whether or not we have been called yet, only 
     * reset info once
     */

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
        if (!isCreate) {
            Object done = context.get(KEY);
            if (done == null) {
                context.put(KEY, KEY);
                if (entry instanceof Account) {
                    try {
                        handleAccountStatusClosed((Account)entry);
                    } catch (ServiceException se) {
                        // all exceptions are already swallowed by LdapProvisioning, just to be safe here.
                        ZimbraLog.account.warn("unable to remove account address and aliases from all DLs for closed account", se);
                        return;
                    }
                }    
            }
        }
    }
    
    private void handleAccountStatusClosed(Account account)  throws ServiceException {
        String status = account.getAccountStatus();
        
        if (status.equals(Provisioning.ACCOUNT_STATUS_CLOSED)) {
            ZimbraLog.misc.info("removing closed account and all its aliases from all distribution lists");
            LdapProvisioning prov = (LdapProvisioning) Provisioning.getInstance();
            
            String aliases[] = account.getAliases();
            String addrs[] = new String[aliases.length+1];
            addrs[0] = account.getName();
            if (aliases.length > 0)
                System.arraycopy(aliases, 0, addrs, 1, aliases.length);
            
            prov.removeAddressesFromAllDistributionLists(addrs);
        }
    }
    

}
