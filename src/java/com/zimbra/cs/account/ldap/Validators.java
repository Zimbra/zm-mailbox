/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.ldap;

import java.io.IOException;
import java.util.Arrays;
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
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CosBy;
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
        
        public void refresh() {
            setNextCheck(0);
        }
        
        private synchronized void setNextCheck(long nextCheck) {
            mNextCheck = nextCheck;
        }
        
        private synchronized long getNextCheck() {
            return mNextCheck;
        }
        
        public void validate(LdapProvisioning prov, String action, Object... args) throws ServiceException {
            if (args.length < 1) return;
            if (!action.equals("createAccount") || !(args[0] instanceof String))
                return;
            
            if (args.length > 1 && args[1] instanceof String[] &&
                    Arrays.asList((String[]) args[1]).contains(LdapProvisioning.C_zimbraCalendarResource)) {
                return; // as in LicenseManager, don't want to count calendar resources
            }
            
            if (args.length > 2 && args[2] instanceof Map) {
                Map<String,Object> acctAttrs = (Map) args[2];
                if (isSystemProperty(acctAttrs))
                    return;
            }
            
            String emailAddr = (String)args[0];
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
            if (now > getNextCheck()) {
                try {
                    mLastUserCount = prov.countAccounts(domain);
                } catch (ServiceException e) {
                    Throwable cause = e.getCause();
                    String causeMsg = cause.getMessage();
                    

                    if (causeMsg != null && causeMsg.contains("timeout"))
                        throw ServiceException.FAILURE("The directory may not be responding or is responding slowly.  " +
                                "The directory may need tuning or the LDAP read timeout may need to be raised.  " +
                                "Otherwise, removing the zimbraDomainMaxAccounts restriction will avoid this check.", e); 
                    else
                        throw ServiceException.FAILURE("Unable to count users for setting zimbraDomainMaxAccounts=" +  limit + "" +
                                " in domain " + d.getName(), e);

                }
                long nextCheck = (maxAccount - mLastUserCount) > NUM_ACCT_THRESHOLD ? 
                        LDAP_CHECK_INTERVAL : 0;
                setNextCheck(nextCheck);
            }
            
            if (maxAccount <= mLastUserCount)
                throw AccountServiceException.TOO_MANY_ACCOUNTS("domain="+domain+" ("+maxAccount+")");
        }
        
    }
    
    private static boolean isSystemProperty(Map<String,Object> attrs) {
        if (attrs == null)
            return false;
        
        Object o = attrs.get(Provisioning.A_zimbraIsSystemResource);
        if (o != null && "true".equalsIgnoreCase(o.toString()))
            return true; // is system resource, do not check
        
        // if we are restoring, the OC array would be empty and 
        // all object classes will be in the attr map.  
        // Skip license check if we are restoring a calendar resource
        o = attrs.get(Provisioning.A_objectClass);
        if (o instanceof String[]) {
            Set<String> ocs = new HashSet<String>(Arrays.asList((String[])o));
            if (ocs.contains(LdapProvisioning.C_zimbraCalendarResource))
                return true;
        }
        
        return false;
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
        
        public void refresh() {
            // do nothing
        }
        
        public void validate(LdapProvisioning prov, String action, Object... args) throws ServiceException {
            
            if (!"createAccountCheckDomainCosAndFeature".equals(action) &&
                    !"modifyAccountCheckDomainCosAndFeature".equals(action))
                return;
            
            if (args.length < 2)
                return;

            HashMap<String,Integer> cosCountMap = new HashMap<String,Integer>();
            HashMap<String,Integer> cosLimitMap = new HashMap<String,Integer>();
            HashMap<String,Integer> featureCountMap = new HashMap<String,Integer>();
            HashMap<String,Integer> featureLimitMap = new HashMap<String,Integer>();
            HashMap<String,Set<String>> cosFeatureMap = new HashMap<String,Set<String>>();

            String emailAddress = (String) args[0];
            if (emailAddress == null)
                return;

            Map<String, Object> attrs = (Map) args[1];
            if (isSystemProperty(attrs))
                return;
            
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
            if (defaultCosId == null) {
                Cos defaultCos = prov.get(CosBy.name, Provisioning.DEFAULT_COS_NAME);
                if (defaultCos != null)
                    defaultCosId = defaultCos.getId();
            }

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
            
            // populate count maps with the cos and features we are interested in
            for (Map.Entry<String,Integer> e : cosLimitMap.entrySet())
                cosCountMap.put(e.getKey(), 0);
            for (Map.Entry<String,Integer> e : featureLimitMap.entrySet())
                featureCountMap.put(e.getKey(), 0);
            
            String desiredCosId = (String) attrs.get(Provisioning.A_zimbraCOSId);
            if (desiredCosId == null)
                desiredCosId = defaultCosId;
            
            Set<String> cosFeatures = getCosFeatures(prov, cosFeatureMap, desiredCosId, defaultCosId);
            Set<String> desiredFeatures = new HashSet<String>();
            // add all new requested features
            for (Map.Entry<String,Object> entry : attrs.entrySet()) {
                String k = entry.getKey();
                if (featureLimitMap.containsKey(k)
                        && "true".equalsIgnoreCase(entry.getValue().toString())) {
                        desiredFeatures.add(k);
                }
            }
            // add all features in new cos
            if (cosFeatures != null) {
                for (String feature : cosFeatures) {
                    if (featureLimitMap.containsKey(feature))
                        desiredFeatures.add(feature);
                }
            }
            if (ZimbraLog.account.isDebugEnabled())
                ZimbraLog.account.debug("Desired features (incl. cos): %s + %s", desiredFeatures, cosFeatures);
            String originalCosId = null;
            // remove all features in old cos
            if (account != null) {
                originalCosId = account.getAttr(Provisioning.A_zimbraCOSId);
                // be sure to fall back to default cos ID if none is set
                // spurious counts will occur otherwise
                if (originalCosId == null)
                    originalCosId = defaultCosId;
                Set<String> features = getCosFeatures(prov, cosFeatureMap, originalCosId, defaultCosId);
                if (features != null)
                    desiredFeatures.removeAll(features);
            }
            // remove all features in old account
            if (desiredFeatures.size() > 0) {
                if (account != null) {
                    Map<String, Object> acctAttrs = account.getAttrs(false);
                    for (Iterator<String> i = desiredFeatures.iterator() ; i.hasNext(); ) {
                        String feature = i.next();
                        if (acctAttrs.containsKey(feature)
                                && "true".equalsIgnoreCase(acctAttrs.get(feature).toString()))
                            i.remove();
                    }
                }
            }
            if ((desiredCosId != null && !desiredCosId.equals(originalCosId)
                    && cosLimitMap.containsKey(desiredCosId)) || desiredFeatures.size() > 0) {
                if (ZimbraLog.account.isDebugEnabled()) {
                    ZimbraLog.account.debug("COS change info [%s:%s], desired features %s",
                            originalCosId, desiredCosId, desiredFeatures);
                }
                buildDomainCounts(prov, domainName, defaultCosId, cosCountMap, featureCountMap, cosFeatureMap);
                if (ZimbraLog.account.isDebugEnabled())
                    ZimbraLog.account.debug("COS/Feature limits: %s + %s", cosLimitMap, featureLimitMap);
                if (desiredCosId != null && !desiredCosId.equals(originalCosId)
                        && cosLimitMap.containsKey(desiredCosId)) {
                    if (cosCountMap.containsKey(desiredCosId)
                            && cosCountMap.get(desiredCosId) >= cosLimitMap.get(desiredCosId)) {
                        throw AccountServiceException.TOO_MANY_ACCOUNTS(
                                String.format("domain=%s[cos=%s,count=%d,limit=%d]",
                                        domainName, desiredCosId, cosCountMap.get(desiredCosId), cosLimitMap.get(desiredCosId)));
                    }
                }
                if (desiredFeatures.size() > 0) {
                    for (String feature : desiredFeatures) {
                        if (featureCountMap.containsKey(feature)
                                && featureCountMap.get(feature) >= featureLimitMap.get(feature)) {
                            throw AccountServiceException.TOO_MANY_ACCOUNTS(
                                    String.format("domain=%s[%s,count=%d,limit=%d]",
                                            domainName, feature, featureCountMap.get(feature), featureLimitMap.get(feature)));
                        }
                    }
                }
            }
        }
        
        private static Set<String> getCosFeatures(LdapProvisioning prov, Map<String,Set<String>> cosFeatureMap,
                String cosId, String defaultCosId)
        throws ServiceException {
            if (!cosFeatureMap.containsKey(cosId)) {
                Cos cos = null;
                if (cosId != null)
                    cos = prov.get(CosBy.id, cosId);
                if (cos == null) {
                    if (defaultCosId != null) {
                        ZimbraLog.account.debug("COS id %s not found, reverting to %s", cosId, defaultCosId);
                        return getCosFeatures(prov, cosFeatureMap, defaultCosId, null);
                    }
                    else {
                        ZimbraLog.account.debug("COS %s not found, bailing!", cosId);
                        return null;
                    }
                }
                Map<String,Object> cosAttrs = cos.getAttrs(true);
                Set<String> features = new HashSet<String>();
                for (Map.Entry<String,Object> entry : cosAttrs.entrySet()) {
                    String name = entry.getKey();
                    if (name.toLowerCase().startsWith("zimbrafeature")
                            && name.toLowerCase().endsWith("enabled")) {
                        Object value = entry.getValue();
                        if (value != null && "true".equalsIgnoreCase(value.toString()))
                            features.add(name);
                    }
                }
                cosFeatureMap.put(cosId, features);
            }
            return cosFeatureMap.get(cosId);
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
        private void buildDomainCounts(LdapProvisioning prov, String domain, String defaultCos,
                Map<String,Integer> cosCount, Map<String,Integer> featureCount, Map<String,Set<String>> cosFeatureMap)
        throws ServiceException {
            String query = LdapFilter.allNonSystemAccounts();

            ZimbraLdapContext zlc = null;
            try {
                zlc = new ZimbraLdapContext();

                SearchControls searchControls = 
                    new SearchControls(SearchControls.SUBTREE_SCOPE,
                            0, 0, null, false, false);

                NamingEnumeration<SearchResult> ne = null;

                String searchDN = prov.getDIT().domainToAccountSearchDN(domain);
                int pageSize = 1000;
                byte[] cookie = null;
                do {
                    try {
                        zlc.setPagedControl(pageSize, cookie, true);
                        ne = zlc.searchDir(searchDN, query, searchControls);
                        while (ne != null && ne.hasMore()) {
                            SearchResult sr = ne.nextElement();
                            String dn = sr.getNameInNamespace();
                            // skip admin accounts
                            if (dn.endsWith("cn=zimbra")) continue;

                            Attributes attrs = sr.getAttributes();
                            Attribute objectclass = attrs.get("objectclass");
                            if (objectclass == null) {
                                ZimbraLog.account.error("DN: " + dn + ": does not have objectclass!");
                                continue;
                            }
                            if (objectclass.contains("zimbraAccount")) {
                                String cosId = defaultCos;
                                Attribute cosIdAttr = attrs.get("zimbracosid");
                                if (cosIdAttr != null)
                                    cosId = (String) cosIdAttr.get();
                                // invalid COS id will revert to default COS id, however, this counter will count
                                // the invalid ID and not count the reverted default ID.  i.e. 100 accounts with
                                // invalid IDs will be counted as 100 accounts with invalid IDs and not properly
                                // counted as 100 accounts in the default COS
                                incrementCount(cosCount, cosId);
                                Set<String> cosFeatures = getCosFeatures(prov, cosFeatureMap, cosId, defaultCos);

                                NamingEnumeration<? extends Attribute> e = attrs.getAll();
                                Set<String> acctFeatures = new HashSet<String>();
                                while (e.hasMore()) {
                                    Attribute at = e.next();
                                    String name = at.getID();
                                    Object atValue = at.get();
                                    String value = null;
                                    if (atValue != null)
                                        value = at.get().toString();
                                    if (name.toLowerCase().startsWith("zimbrafeature")
                                            && name.toLowerCase().endsWith("enabled")
                                            && "true".equalsIgnoreCase(value))
                                        acctFeatures.add(name);
                                }
                                if (cosFeatures != null)
                                    acctFeatures.addAll(cosFeatures);
                                for (String feature : acctFeatures)
                                    incrementCount(featureCount, feature);
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
            if (ZimbraLog.account.isDebugEnabled())
                ZimbraLog.account.debug("COS/Feature counts: %s + %s", cosCount, featureCount);
        }
        private static void incrementCount(Map<String,Integer> map, String key) {
            if (key == null || !map.containsKey(key))
                return; // not something that we care about
            map.put(key, map.get(key) + 1);
        }
    }
}
