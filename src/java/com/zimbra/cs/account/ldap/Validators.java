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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning.ProvisioningValidator;

public class Validators {
    
    public static void init() {
        LdapProvisioning.register(new DomainAccountValidator());
    }

    // cache the result for 1 min unless the count is within 5 of the limit.
    private static class DomainAccountValidator implements ProvisioningValidator {
        private static final long LDAP_CHECK_INTERVAL  = 60 * 1000;  // 1 min
        private static final long NUM_ACCT_THRESHOLD = 5;
        
        private long mNextCheck;
        private long mLastUserCount;
        
            public void validate(LdapProvisioning prov, String action, Object arg) throws ServiceException {
            if (!action.equals("createAccount") || !(arg instanceof String))
                return;
            
            String emailAddr = (String)arg;
            String domain = null;
            int index = emailAddr.indexOf('@');
            if (index != -1)
                domain = emailAddr.substring(index+1);
            
            if (domain == null)
                return;

            Domain d = prov.get(Provisioning.DomainBy.name, domain);
            if (d == null)
                return;
            
            String limit = d.getAttr(Provisioning.A_zimbraDomainMaxAccounts);
            if (limit == null)
                return;

            long maxAccount = Long.parseLong(limit);
            long now = System.currentTimeMillis();
            if (now > mNextCheck) {
                mLastUserCount = prov.countAccounts(domain);
                mNextCheck = (maxAccount - mLastUserCount) > NUM_ACCT_THRESHOLD ? 
                        LDAP_CHECK_INTERVAL : 0;
            }
            
            if (maxAccount <= mLastUserCount)
                throw AccountServiceException.TOO_MANY_ACCOUNTS("domain="+domain+" ("+maxAccount+")");
        }
        
    }
}
