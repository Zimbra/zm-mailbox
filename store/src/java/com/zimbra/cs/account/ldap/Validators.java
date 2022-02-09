/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.account.ldap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.ldap.IAttributes;
import com.zimbra.cs.ldap.SearchLdapOptions.SearchLdapVisitor;
import com.zimbra.cs.ldap.ZLdapFilter;
import com.zimbra.cs.ldap.ZLdapFilterFactory;
import com.zimbra.soap.admin.type.CountObjectsType;

final public class Validators {

    private Validators() {
    }

    // cache the result for 1 min unless the count is within 5 of the limit.
    public static class DomainAccountValidator implements Provisioning.ProvisioningValidator {
        private static final long LDAP_CHECK_INTERVAL  = 60 * 1000;  // 1 min
        private static final long NUM_ACCT_THRESHOLD = 5;

        private long mNextCheck;
        private long mLastUserCount; // PFN: this isn't counted per-domain, is it?

        @Override
        public void refresh() {
            setNextCheck(0);
        }

        private synchronized void setNextCheck(long nextCheck) {
            mNextCheck = nextCheck;
        }

        private synchronized long getNextCheck() {
            return mNextCheck;
        }

        @Override
        public void validate(Provisioning prov, String action, Object... args) throws ServiceException {
            if (args.length < 1) return;
            if (!(action.equals(CREATE_ACCOUNT) || action.equals(RENAME_ACCOUNT)) || !(args[0] instanceof String))
                return;

            if (args.length > 1 && args[1] instanceof String[] &&
                    Arrays.asList((String[]) args[1]).contains(AttributeClass.OC_zimbraCalendarResource)) {
                return; // as in LicenseManager, don't want to count calendar resources
            }

            if (args.length > 2 && args[2] instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> acctAttrs = (Map<String, Object>) args[2];
                if (isSystemProperty(acctAttrs)) {
                    return;
                }
                if (isExternalVirtualAccount(acctAttrs)) {
                    return;
                }
            }

            String emailAddr = (String)args[0];
            String domain = null;
            int index = emailAddr.indexOf('@');
            if (index != -1)
                domain = emailAddr.substring(index+1);

            if (domain == null)
                return;

            Domain d = prov.get(Key.DomainBy.name, domain);
            if (d == null)
                return;

            String limit = d.getAttr(Provisioning.A_zimbraDomainMaxAccounts);
            if (limit == null)
                return;

            long maxAccount = Long.parseLong(limit);
            long now = System.currentTimeMillis();
            if (now > getNextCheck()) {
                try {
                    mLastUserCount = prov.countObjects(CountObjectsType.internalUserAccount, d, null);
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
            if (ocs.contains(AttributeClass.OC_zimbraCalendarResource))
                return true;
        }

        return false;
    }

    private static boolean isExternalVirtualAccount(Map<String, Object> attrs) {
        if (attrs == null) {
            return false;
        }
        Object o = attrs.get(Provisioning.A_zimbraIsExternalVirtualAccount);
        return o != null && "true".equalsIgnoreCase(o.toString());
    }


    /**
     * Validate that we are not exceeding max feature and cos counts for the given domain.
     * <p>
     * arg is an Object[] consisting of:
     *
     * @author pfnguyen
     */
    public static class DomainMaxAccountsValidator implements Provisioning.ProvisioningValidator {

        @Override
        public void refresh() {
            // do nothing
        }

        @Override
        public void validate(Provisioning prov, String action, Object... args) throws ServiceException {

            if (!CREATE_ACCOUNT_CHECK_DOMAIN_COS_AND_FEATURE.equals(action) &&
                    !RENAME_ACCOUNT_CHECK_DOMAIN_COS_AND_FEATURE.equals(action) &&
                    !MODIFY_ACCOUNT_CHECK_DOMAIN_COS_AND_FEATURE.equals(action)) {
                return;
            }

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

            @SuppressWarnings("unchecked")
            Map<String, Object> attrs = (Map<String, Object>) args[1];
            if (isSystemProperty(attrs)) {
                return;
            }
            if (isExternalVirtualAccount(attrs)) {
                return;
            }

            Account account = null;
            if (args.length == 3)
                account = (Account) args[2];
            String domainName = null;
            int index = emailAddress.indexOf('@');
            if (index != -1)
                domainName = emailAddress.substring(index+1);

            if (domainName == null)
                return;

            Domain domain = prov.get(Key.DomainBy.name, domainName);
            if (domain == null)
                return;

            String defaultCosId = domain.getAttr(Provisioning.A_zimbraDomainDefaultCOSId);
            if (defaultCosId == null) {
                Cos defaultCos = prov.get(Key.CosBy.name, Provisioning.DEFAULT_COS_NAME);
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

            boolean isModifyingCosId = (attrs != null && attrs.get(Provisioning.A_zimbraCOSId) != null);
            boolean isCreatingEntry = CREATE_ACCOUNT_CHECK_DOMAIN_COS_AND_FEATURE.equals(action);

            String desiredCosId = null;

            if (isModifyingCosId || isCreatingEntry) {
                if (attrs != null) {
                    desiredCosId = (String) attrs.get(Provisioning.A_zimbraCOSId);
                }
                if (desiredCosId == null) {
                    desiredCosId = defaultCosId;
                }
            } else {
                // we are not modifying cos for the account, and
                // we are not creating, account must not be null
                if (account != null) {
                    desiredCosId = account.getCOS().getId();
                } else {
                    // really an internal error
                    // at some point we should probably cleanup the validate interface/code
                    throw ServiceException.FAILURE("account object is null", null);
                }
            }

            Set<String> cosFeatures = getCosFeatures(prov, cosFeatureMap, desiredCosId, defaultCosId);
            Set<String> desiredFeatures = new HashSet<String>();
            // add all new requested features
            if (attrs != null) {
                for (Map.Entry<String,Object> entry : attrs.entrySet()) {
                    String k = entry.getKey();
                    if (featureLimitMap.containsKey(k)
                            && "true".equalsIgnoreCase(entry.getValue().toString())) {
                            desiredFeatures.add(k);
                    }
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

        private static Set<String> getCosFeatures(Provisioning prov, Map<String,Set<String>> cosFeatureMap,
                String cosId, String defaultCosId)
        throws ServiceException {
            if (!cosFeatureMap.containsKey(cosId)) {
                Cos cos = null;
                if (cosId != null)
                    cos = prov.get(Key.CosBy.id, cosId);
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

        private static class BuildDomainCounts extends SearchLdapVisitor {

            private Provisioning prov;
            private String domain;
            private String defaultCos;
            private Map<String,Integer> cosCount;
            private Map<String,Integer> featureCount;
            private Map<String,Set<String>> cosFeatureMap;

            private BuildDomainCounts(Provisioning prov, String domain, String defaultCos,
                    Map<String,Integer> cosCount, Map<String,Integer> featureCount,
                    Map<String,Set<String>> cosFeatureMap) {
                this.prov = prov;
                this.domain = domain;
                this.defaultCos = defaultCos;
                this.cosCount = cosCount;
                this.featureCount = featureCount;
                this.cosFeatureMap = cosFeatureMap;
            }

            void search() throws ServiceException {
                LdapProv ldapProv = (LdapProv) prov;
                String searchBaseDN = ldapProv.getDIT().domainToAccountSearchDN(domain);
                ZLdapFilter query = ZLdapFilterFactory.getInstance().allNonSystemInternalAccounts();

                ldapProv.searchLdapOnReplica(searchBaseDN, query, null, this);
                ZimbraLog.account.debug("COS/Feature counts: %s + %s", cosCount, featureCount);
            }

            @Override
            public void visit(String dn, Map<String, Object> attrs, IAttributes ldapAttrs) {
                try {
                    visitInternal(dn, attrs, ldapAttrs);
                } catch (ServiceException e) {
                    ZimbraLog.account.error("encountered error, entry skipped ", e);
                }
            }

            private void visitInternal(String dn, Map<String, Object> attrs, IAttributes ldapAttrs) throws ServiceException {

                List<String> objectclass = ldapAttrs.getMultiAttrStringAsList(
                        Provisioning.A_objectClass, IAttributes.CheckBinary.NOCHECK);
                if (objectclass == null || objectclass.size() == 0) {
                    ZimbraLog.account.error("DN: " + dn + ": does not have objectclass!");
                    return;
                }

                if (objectclass.contains(AttributeClass.OC_zimbraAccount)) {
                    String cosId = ldapAttrs.getAttrString(Provisioning.A_zimbraCOSId);
                    if (cosId == null) {
                        cosId = defaultCos;
                    }

                    // invalid COS id will revert to default COS id, however, this counter will count
                    // the invalid ID and not count the reverted default ID.  i.e. 100 accounts with
                    // invalid IDs will be counted as 100 accounts with invalid IDs and not properly
                    // counted as 100 accounts in the default COS
                    incrementCount(cosCount, cosId);
                    Set<String> cosFeatures = getCosFeatures(prov, cosFeatureMap, cosId, defaultCos);

                    Set<String> acctFeatures = new HashSet<String>();
                    for (Map.Entry<String, Object> attr : attrs.entrySet()) {
                        String attrName = attr.getKey();
                        Object attrValue = attr.getValue();

                        String value = null;
                        if (attrValue instanceof String) {
                            value = (String) attrValue;
                        }

                        if (attrName.toLowerCase().startsWith("zimbrafeature")
                                && attrName.toLowerCase().endsWith("enabled")
                                && "true".equalsIgnoreCase(value))
                            acctFeatures.add(attrName);
                    }
                    if (cosFeatures != null)
                        acctFeatures.addAll(cosFeatures);
                    for (String feature : acctFeatures)
                        incrementCount(featureCount, feature);
                }
            }
        }

        private static void incrementCount(Map<String,Integer> map, String key) {
            if (key == null || !map.containsKey(key))
                return; // not something that we care about
            map.put(key, map.get(key) + 1);
        }

        // new way to search LDAP after the SDK work
        private void buildDomainCounts(Provisioning prov, String domain, String defaultCos,
                Map<String,Integer> cosCount, Map<String,Integer> featureCount, Map<String,Set<String>> cosFeatureMap)
        throws ServiceException {
            BuildDomainCounts counts = new BuildDomainCounts(prov, domain, defaultCos,
                    cosCount, featureCount, cosFeatureMap);
            counts.search();
        }


        //
        // old code before LDAP SDK refactoring - keep in tact for now.
        // There are some existing bugs predate the SDK work.
        // keep the old code for now so when bugs are reported we can quickly test
        // to tell if a bug is a regression introduced by the SDK word.
        //
        //
        // mostly pawned off from LdapProvisioning.countAccounts(domain)
        /*
        private void buildDomainCounts(Provisioning prov, String domain, String defaultCos,
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

                // TODO: remove dependency on LDAP
                String searchDN = ((LdapProvisioning) prov).getDIT().domainToAccountSearchDN(domain);
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
        */

    }
}
