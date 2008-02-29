/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

package com.zimbra.cs.account.krb5;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.DomainBy;

public class Krb5Principal {
    
     public static Account getAccountFromKrb5Principal(String principal, boolean loadFromMaster) throws ServiceException {
         Provisioning prov = Provisioning.getInstance();
         Account acct = null;
         
         /*
          * first do a get account by foreign principal on "kerberos5:"+principal and use that if it exists
          */
         try {
             acct = prov.get(AccountBy.foreignPrincipal, Provisioning.FP_PREFIX_KERBEROS5+principal, loadFromMaster);
         } catch (ServiceException e) {
             throw e;
         }
         
         /*
          * if that fails, we need to grab the Kerberos realm from the principal and then look for a domain with
          * zimbraAuthKerberos5Realm set to that realm. If we find that domain, then we take the Kerberos principal 
          * name before the domain, then look up an account in that domain with the same name.
          * 
          * If that all fails, we just return null.
          */
         if (acct == null) {
             int idx = principal.indexOf('@');
             if (idx != -1) {
                 String realm = principal.substring(idx+1);
                 
                 Domain domain = prov.get(DomainBy.krb5Realm, realm);
                 if (domain != null) {
                     String localPart = principal.substring(0, idx);
                     String acctName = localPart +  "@" + domain.getName();
                     acct = prov.get(AccountBy.name, acctName, loadFromMaster);
                 }
                 
             }
         }
         
         return acct;
     }
     
     public static String getKrb5Principal(Account acct) throws ServiceException {
         Domain domain = Provisioning.getInstance().getDomain(acct);
         return getKrb5Principal(domain, acct);
     }
     
     public static String getKrb5Principal(Domain domain, Account acct) {
         String principal = null;
         String fps[] = acct.getMultiAttr(Provisioning.A_zimbraForeignPrincipal);
         if (fps != null && fps.length > 0) {
             for (String fp : fps) {
                 if (fp.startsWith(Provisioning.FP_PREFIX_KERBEROS5)) {
                     int idx = fp.indexOf(':');
                     if (idx != -1) {
                         principal = fp.substring(idx+1);
                         break;
                     }
                 }
             }
         }
         if (principal == null) {
             String realm = domain.getAttr(Provisioning.A_zimbraAuthKerberos5Realm);
             if (realm != null) {
                 String[] parts = EmailUtil.getLocalPartAndDomain(acct.getName());
                 if (parts != null)
                     principal = parts[0] + "@" + realm;
                 else
                     principal = acct.getName() + "@" + realm; // just use whatever in the name
             }
         }
         
         return principal;
     }
}
