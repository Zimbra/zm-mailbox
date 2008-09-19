/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.account.ldap;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.ldap.LdapProvisioning.ProvisioningValidator;

public class Validators {
    
    public static void init() {
        LdapProvisioning.register(new DomainAccountValidator());
        LdapProvisioning.register(new DomainMaxAccountsValidator());
    }

    // cache the result for 1 min unless the count is within 5 of the limit.
    private static class DomainAccountValidator implements ProvisioningValidator {
        private static final long LDAP_CHECK_INTERVAL  = 60 * 1000;  // 1 min
        private static final long NUM_ACCT_THRESHOLD = 5;
        
        private long mNextCheck;
        private long mLastUserCount; // PFN: this isn't counted per-domain, is it?
        
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
    /**
     * Validate that we are not exceeding max feature and cos counts for the given domain.
     * <p>
     * arg is an Object[] consisting of:
     * 
     * @author pfnguyen
     */
    @SuppressWarnings("unchecked")
    private static class DomainMaxAccountsValidator implements ProvisioningValidator {
        
        private LdapDIT mDIT = null;
        public void validate(LdapProvisioning prov, String action, Object arg) throws ServiceException {
            if (mDIT == null)
                mDIT = new LdapDIT(prov);
            
            if (!(arg instanceof Object[]))
                return;
            if (!"createAccountCheckDomainCosAndFeature".equals(action) &&
                    !"modifyAccountCheckDomainCosAndFeature".equals(action))
                return;
            
            Object[] args = (Object[]) arg;
            if (args.length < 2)
                return;

            HashMap<String,Integer> cosCountMap = new HashMap<String,Integer>();
            HashMap<String,Integer> cosLimitMap = new HashMap<String,Integer>();
            HashMap<String,Integer> featureCountMap = new HashMap<String,Integer>();
            HashMap<String,Integer> featureLimitMap = new HashMap<String,Integer>();

            String emailAddress = (String) args[0];
            Map<String, Object> attrs = (Map) args[1];
            Account account = null;
            if (args.length == 3)
                account = (Account) args[2];
            String domainName = null;
            int index = emailAddress.indexOf('@');
            if (index != -1)
                domainName = emailAddress.substring(index+1);
            
            if (domainName == null)
                return;
            
            Domain domain = prov.get(Provisioning.DomainBy.name, domainName);
            if (domain == null)
                return;
            
            String defaultCosId = domain.getAttr(Provisioning.A_zimbraDomainDefaultCOSId);

            Set<String> cosLimit = domain.getMultiAttrSet(
                    Provisioning.A_zimbraDomainCOSMaxAccounts);
            Set<String> featureLimit = domain.getMultiAttrSet(
                    Provisioning.A_zimbraDomainFeatureMaxAccounts);

            if (cosLimit.size() == 0 && featureLimit.size() == 0)
                return;

            for (String limit : cosLimit)
                parseLimit(cosLimitMap, limit);
            for (String limit : featureLimit)
                parseLimit(featureLimitMap, limit);
            
            String desiredCosId = (String) attrs.get(Provisioning.A_zimbraCOSId);
            if (desiredCosId == null)
                desiredCosId = defaultCosId;
            Set<String> desiredFeatures = new HashSet<String>();
            for (Map.Entry<String,Object> entry : attrs.entrySet()) {
                String k = entry.getKey();
                if (k.startsWith("zimbraFeature") && k.endsWith("Enabled")) {
                    if ("true".equalsIgnoreCase(entry.getValue().toString()))
                        desiredFeatures.add(k);
                }
            }
            String originalCosId = null;
            if (account != null)
                originalCosId = account.getAttr(Provisioning.A_zimbraCOSId);
            if (desiredFeatures.size() > 0) {
                if (account != null) {
                    Map<String, Object> acctAttrs = account.getAttrs();
                    for (Iterator<String> i = desiredFeatures.iterator() ; i.hasNext(); ) {
                        String feature = i.next();
                        if (acctAttrs.containsKey(feature)) // should we check for "TRUE" also?
                            i.remove();
                        else if (!featureLimitMap.containsKey(feature))
                            i.remove();
                    }
                }
            }
            if ((desiredCosId != null && !desiredCosId.equals(originalCosId)
                    && cosLimitMap.containsKey(desiredCosId)) || desiredFeatures.size() > 0) {
                buildDomainCounts(domainName, defaultCosId, cosCountMap, featureCountMap);
                if (desiredCosId != null && !desiredCosId.equals(originalCosId)
                        && cosLimitMap.containsKey(desiredCosId)) {
                    if (cosCountMap.containsKey(desiredCosId)
                            && cosCountMap.get(desiredCosId) >= cosLimitMap.get(desiredCosId)) {
                        throw AccountServiceException.TOO_MANY_ACCOUNTS(
                                String.format("domain=%s[cos=%s,limit=%d]",
                                        domainName, desiredCosId, cosCountMap.get(desiredCosId)));
                    }
                }
                if (desiredFeatures.size() > 0) {
                    for (String feature : desiredFeatures) {
                        if (featureCountMap.containsKey(feature)
                                && featureCountMap.get(feature) > featureLimitMap.get(feature)) {
                            throw AccountServiceException.TOO_MANY_ACCOUNTS(
                                    String.format("domain=%s[%s,limit=%d]",
                                            domainName, feature, featureCountMap.get(feature)));
                        }
                    }
                }
            }
        }
        
        private static void parseLimit(HashMap<String,Integer> map, String limit) {
            String[] parts = limit.split(":");
            int max = -1;
            try {
                max = Integer.parseInt(parts[1]);
            }
            catch (NumberFormatException e) {
                // ignore, should log
            }
            if (max < 0)
                return;
            map.put(parts[0], max);
            
        }
        
        // mostly pawned off from LdapProvisioning.countAccounts(domain)
        private void buildDomainCounts(String domain, String defaultCos,
                Map<String,Integer> cosCount, Map<String,Integer> featureCount)
        throws ServiceException {
            String query = LdapFilter.allNonSystemAccounts();

            ZimbraLdapContext zlc = null;
            try {
                zlc = new ZimbraLdapContext();

                SearchControls searchControls = 
                    new SearchControls(SearchControls.SUBTREE_SCOPE, 0, 0, new String[] {
                            "zimbraId", "objectclass"}, false, false);

                NamingEnumeration<SearchResult> ne = null;

                String dn = mDIT.domainToAccountSearchDN(domain);
                int pageSize = 1000;
                byte[] cookie = null;
                do {
                    try {
                        zlc.setPagedControl(pageSize, cookie, true);
                        ne = zlc.searchDir(dn, query, searchControls);
                        while (ne != null && ne.hasMore()) {
                            SearchResult sr = ne.nextElement();
                            dn = sr.getNameInNamespace();
                            // skip admin accounts
                            if (dn.endsWith("cn=zimbra")) continue;
                            Attributes attrs = sr.getAttributes();
                            Attribute objectclass = attrs.get("objectclass");
                            if (objectclass.contains("zimbraAccount")) {
                                String cosId = defaultCos;
                                Attribute cosIdAttr = attrs.get("zimbracosid");
                                if (cosIdAttr != null)
                                    cosId = (String) cosIdAttr.get();
                                incrementCount(cosCount, cosId);

                                NamingEnumeration<? extends Attribute> e = attrs.getAll();
                                while (e.hasMore()) {
                                    Attribute at = e.next();
                                    String name = at.getID();
                                    if (name.toLowerCase().startsWith("zimbrafeature")
                                            && name.toLowerCase().endsWith("enabled"))
                                        incrementCount(featureCount, name);
                                }
                            }
                        }
                    } finally {
                        if (ne != null) ne.close();
                    }
                    cookie = zlc.getCookie();
                } while (cookie != null);
            } catch (NamingException e) {
                throw ServiceException.FAILURE("unable to count the users", e);
            } catch (IOException e) {
                throw ServiceException.FAILURE("unable to count all the users", e);
            } finally {
                ZimbraLdapContext.closeContext(zlc);
            }
        }
        private static void incrementCount(Map<String,Integer> map, String key) {
            if (!map.containsKey(key))
                map.put(key, 1);
            else
                map.put(key, map.get(key) + 1);
        }
    }
}
