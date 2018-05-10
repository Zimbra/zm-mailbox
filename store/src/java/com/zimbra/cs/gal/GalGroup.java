/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
package com.zimbra.cs.gal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.ThreadPool;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.EntryCacheDataKey;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CacheEntry;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.type.GalSearchType;


public abstract class GalGroup {

    public enum GroupInfo {
        IS_GROUP,   // address is a group
        CAN_EXPAND  // address is a group and the authed account has right to expand it
    };

    private static final Provisioning prov = Provisioning.getInstance();
    private static Map<String, DomainGalGroupCache> groups = new HashMap<String, DomainGalGroupCache>();
    private static ThreadPool syncGalGroupThreadPool = new ThreadPool("SyncGalGroup", 10);

    private interface GalGroupCache {
        boolean isInternalGroup(String addr);
        boolean isExternalGroup(String addr);
    }

    private static class GalGroupCacheFullException extends Exception {
    }

    public static void flushCache(CacheEntry[] domains) throws ServiceException {
        if (domains != null) {
            for (CacheEntry entry : domains) {
                Key.DomainBy domainBy = (entry.mEntryBy==Key.CacheEntryBy.id)? Key.DomainBy.id : Key.DomainBy.name;
                Domain domain = prov.get(Key.DomainBy.name, entry.mEntryIdentity);
                if (domain != null) {
                    GalGroup.flushCache(domain);
                }
            }
        } else {
            GalGroup.flushCache((Domain)null);
        }
    }

    /**
     * returns whether addr is a GAL(internal or external) group according to the GAL
     * setting on the domain of the requestedAcct.
     *
     * This method uses the GAL group cache, domain (of the requestedAcct) attribute
     * zimbraGalGroupIndicatorEnabled must be set to TRUE for this call to return meaningful answer.
     * If zimbraGalGroupIndicatorEnabled is FALSE, this method always returns false.
     *
     * @param addr
     * @param requestedAcct
     * @return
     */
    public static boolean isGroup(String addr, Account requestedAcct) {
        return GroupInfo.IS_GROUP == GalGroupInfoProvider.getInstance().getGroupInfo(addr, false, requestedAcct, null);
    }

    public static GroupInfo getGroupInfo(String addr, boolean needCanExpand, Account requestedAcct, Account authedAcct) {
        ZimbraLog.gal.trace("GalGroup - getting group info for addr [%s] requestedAcct [%s] authedAcct [%s]", addr, requestedAcct, authedAcct);
        Domain domain = null;
        try {
            domain = prov.getDomain(requestedAcct);
        } catch (ServiceException e) {
            ZimbraLog.gal.warn("GalGroup - unable to get domain for account " + requestedAcct, e);
        }

        if (domain == null || !domain.isGalGroupIndicatorEnabled()) {
            ZimbraLog.gal.trace("GalGroup - domain null or gal group indicator disabled [%s]", domain);
            return null;
        }

        GalGroupCache galGroup = null;

        try {
            galGroup = GalGroup.getGalGroupForDomain(requestedAcct, domain);
        } catch (GalGroupCacheFullException e) {
            ZimbraLog.gal.trace("GalGroup - cache full exception", e);
            return null;
        }

        if (galGroup == null) {
            // GalGroup for the domain is still syncing, do a GAL search
            // this should not happen once the syncing is finished
            galGroup = new EmailAddrGalGroupCache(addr, requestedAcct);
        }

        // we have a fully synced GalGroup object for the domain
        if (galGroup.isInternalGroup(addr)) {
            ZimbraLog.gal.trace("GalGroup - internal group");
            if (needCanExpand && canExpandGroup(prov, addr, authedAcct))
                return GroupInfo.CAN_EXPAND;
            else
                return GroupInfo.IS_GROUP;
        } else if (galGroup.isExternalGroup(addr)) {
            ZimbraLog.gal.trace("GalGroup - external group");
            if (needCanExpand)
                return GroupInfo.CAN_EXPAND;
            else
                return GroupInfo.IS_GROUP;
        }
        ZimbraLog.gal.trace("GalGroup - neither internal nor external group");

        return null;
    }

    public static synchronized void flushCache(Domain domain) {
        if (domain == null) {
            Map<String, DomainGalGroupCache> tmp = ImmutableMap.copyOf(groups);
            for (Map.Entry<String, DomainGalGroupCache> entry : tmp.entrySet()) {
                GalGroup.flushCache(entry.getKey(), entry.getValue());
            }
        } else {
            String domainName = domain.getName();
            DomainGalGroupCache galGroup = groups.get(domainName);
            GalGroup.flushCache(domain.getName(), galGroup);
        }
    }

    private static void flushCache(String domainName, DomainGalGroupCache galGroup) {
        if (galGroup == null) {
            ZimbraLog.gal.info("GalGroup - flushCache: no cache entry for domain " + domainName);
        } else if (galGroup.isSyncing()) {
            ZimbraLog.gal.info("GalGroup - flushCache: Still syncing GalGroup for domain " + domainName);
        } else {
            ZimbraLog.gal.info("GalGroup - flushCache: Flushing GalGroup for domain " + domainName);
            GalGroup.removeFromCache(domainName, "flush cache");
        }
    }

    private static synchronized GalGroupCache getGalGroupForDomain(Account requestedAcct, Domain domain)
    throws GalGroupCacheFullException{
        String domainName = domain.getName();
        DomainGalGroupCache galGroup = groups.get(domainName);

        if (galGroup == null) {
            // see if there is room for a new domain
            int maxDomains = LC.gal_group_cache_maxsize_domains.intValue();
            if (groups.size() >= maxDomains) {
                String msg = "GalGroup - group cache has reached maxsize of " +
                        maxDomains + " domains, group indicator for messages are temporarily unavailable " +
                        "for domain " + domainName;
                if (hadWarnedDomainForCacheFull(domain)) {
                    // log at debug level so we don't flood the log
                    ZimbraLog.gal.debug(msg);
                } else {
                    ZimbraLog.gal.warn(msg);
                    setHadWarnedDomainForCacheFull(domain);
                }
                throw new GalGroupCacheFullException();
            }

            //
            // add group cache for the domain
            //
            clearHadWarnedDomainForCacheFull(domain);

            galGroup = new DomainGalGroupCache(domainName);
            GalGroup.putInCache(domainName, galGroup);
            DomainGalGroupCache.SyncThread syncThread = new DomainGalGroupCache.SyncThread(domain, galGroup);
            syncGalGroupThreadPool.execute(syncThread);
        }

        if (galGroup.isSyncing()) {
            ZimbraLog.gal.debug("GalGroup - Still syncing GalGroup for domain " + domain.getName());
            return null;
        } else if (galGroup.isExpired()) {
            GalGroup.removeFromCache(domainName, "group cache expired");
            return null;
        } else {
            return galGroup;
        }
    }

    private static boolean hadWarnedDomainForCacheFull(Domain domain) {
        Boolean domainHadBeenWarned = (Boolean)domain.getCachedData(EntryCacheDataKey.DOMAIN_GROUP_CACHE_FULL_HAD_BEEN_WARNED.getKeyName());
        if (domainHadBeenWarned == null)
            return false;
        else
            return true;
    }

    private static void setHadWarnedDomainForCacheFull(Domain domain) {
        domain.setCachedData(EntryCacheDataKey.DOMAIN_GROUP_CACHE_FULL_HAD_BEEN_WARNED.getKeyName(), Boolean.TRUE);
    }

    private static void clearHadWarnedDomainForCacheFull(Domain domain) {
        domain.setCachedData(EntryCacheDataKey.DOMAIN_GROUP_CACHE_FULL_HAD_BEEN_WARNED.getKeyName(), null);
    }

    private static synchronized void putInCache(String domainName, DomainGalGroupCache galGroup) {
        ZimbraLog.gal.debug("GalGroup - adding GalGroup cache for domain " + domainName);
        groups.put(domainName, galGroup);
    }

    private static synchronized void removeFromCache(String domainName, String reason) {
        ZimbraLog.gal.debug("GalGroup - removing GalGroup cache for domain " + domainName + ", " + reason);
        groups.remove(domainName);
    }

    private static boolean canExpandGroup(Provisioning prov, String groupName, Account authedAcct) {
        try {
            // get the dl object for ACL checking
            ZimbraLog.gal.trace("GalGroup - canExpandGroup() account [%s] group [%s]",
                    authedAcct == null ? "null" : authedAcct.getName(), groupName);
            Group group = prov.getGroupBasic(Key.DistributionListBy.name, groupName);

            // the DL might have been deleted since the last GAL sync account sync, throw.
            // or should we just let the request through?
            if (group == null) {
                ZimbraLog.gal.warn("GalGroup - unable to find group " + groupName + " for permission checking");
                return false;
            }

            if (!AccessManager.getInstance().canDo(authedAcct, group, User.R_viewDistList, false)) {
                ZimbraLog.gal.trace("GalGroup - canDo returned false");
                return false;
            }

        } catch (ServiceException e) {
            ZimbraLog.gal.warn("GalGroup - unable to check permission for gal group expansion: " + groupName);
            return false;
        }
        ZimbraLog.gal.trace("GalGroup - canExpandGroup() true");
        return true;
    }

    private static class GalGroupCallback extends GalSearchResultCallback {
        protected GalGroupCallback(GalSearchParams params) {
            super(params);
        }

        protected boolean isZimbraInternalGroup(String email, String zimbraId) {
            return (zimbraId != null && prov.isDistributionList(email));
        }

        protected String getSingleAttr(Object contact, String attr) {
            if (contact instanceof Contact)
                return ((Contact)contact).get(attr);
            else if (contact instanceof GalContact)
                return ((GalContact)contact).getSingleAttr(attr);
            else
                return getSingleAttr((HashMap<String,Object>)contact, attr);
        }

        private String getSingleAttr(HashMap<String,Object> map, String attr) {
            Object val = map.get(attr);
            if (val instanceof String)
                return (String) val;
            else if (val instanceof String[])
                return ((String[])val)[0];
            else
                return null;
        }
    }

    /**
     * GAL group search result for a domain (all GAL groups in the domain's GAL)
     */
    private static class DomainGalGroupCache implements GalGroupCache {
        private String domainName; // for debugging purpose only
        private long lifeTime;
        private int max;
        private boolean isSyncing;
        private Set<String> internalGroups;
        private Set<String> externalGroups;


        private DomainGalGroupCache(String domainName) {
            lifeTime = 0;  // life starts when the sync is completed
            max = LC.gal_group_cache_maxsize_per_domain.intValue(); // 0 is unlimited

            this.domainName = domainName;
            isSyncing = true;
            internalGroups = new HashSet<String>();
            externalGroups = new HashSet<String>();
        }

        private synchronized boolean isSyncing() {
            return isSyncing;
        }

        private synchronized boolean isExpired() {
            return (!isSyncing && (lifeTime < System.currentTimeMillis()));
        }

        private synchronized void setDoneSyncing() {
            isSyncing = false;

            // start life, refresh interval must be between 15 min and 30 days
            lifeTime = System.currentTimeMillis() + (LC.gal_group_cache_maxage.intValueWithinRange(15, 43200) * Constants.MILLIS_PER_MINUTE);
        }

        // no need to synchronize because it can only be called from the syncing thread
        private void addInternalGroup(String addr) {
            if (reachedMax()) {
                ZimbraLog.gal.debug("GalGroup - NOT adding internal group: " + addr + ", limit (" + max + ") reached, domain=" + domainName);
                return;
            }

            ZimbraLog.gal.debug("GalGroup - Adding internal group: " + addr + ", domain=" + domainName);
            internalGroups.add(addr.toLowerCase());
        }

        // no need to synchronize because it can only be called from the syncing thread
        private void addExternalGroup(String addr) {
            if (reachedMax()) {
                ZimbraLog.gal.debug("GalGroup - NOT adding external group: " + addr + ", limit (" + max + ") reached, domain=" + domainName);
                return;
            }

            ZimbraLog.gal.debug("GalGroup - Adding external group: " + addr + ", domain=" + domainName);
            externalGroups.add(addr.toLowerCase());
        }

        private boolean reachedMax() {
            if (max == 0)
                return false;
            else
                return ((internalGroups.size() + externalGroups.size()) >= max);
        }

        // no need to synchronize because we would never modify/get the set concurrently.
        // No one would call this method when the GalGroup object is still syncing
        @Override
        public boolean isInternalGroup(String addr) {
            return internalGroups.contains(addr.toLowerCase());
        }

        // no need to synchronize because we would never modify/get the set concurrently.
        // No one would call this method when the GalGroup object is still syncing
        @Override
        public boolean isExternalGroup(String addr) {
            return externalGroups.contains(addr.toLowerCase());
        }

        private int getMax() {
            return max;
        }

        private static class SyncThread implements Runnable {
            AuthToken adminAuthToken; // admin auth token for the search
            Domain domain;
            DomainGalGroupCache galGroup;

            private SyncThread(Domain domain, DomainGalGroupCache galGroup) {
                this.domain = domain;
                this.galGroup = galGroup;
            }

            @Override
            public void run() {

                try {
                    Account admin = Provisioning.getInstance().get(AccountBy.adminName, LC.zimbra_ldap_user.value());
                    ZimbraLog.addAccountNameToContext(admin.getName());
                    adminAuthToken =  AuthProvider.getAuthToken(admin, true);

                    long startTime = System.currentTimeMillis();
                    ZimbraLog.gal.info("GalGroup - Start syncing gal groups for domain " + domain.getName());

                    sync();
                    galGroup.setDoneSyncing();

                    long elapsedTime = System.currentTimeMillis() - startTime;
                    ZimbraLog.gal.info("GalGroup - Finished syncing gal groups for domain " + domain.getName() +
                            ", elapsed time = " + elapsedTime + "msec");
                } catch (ServiceException e) {
                    ZimbraLog.gal.warn("GalGroup - failed to sync gal groups for domain " + domain.getName(), e);
                    GalGroup.removeFromCache(domain.getName(), "sync failed");
                }
            }

            // sync all groups in the domain's GAL
            private void sync() throws ServiceException {
                int offset = 0;
                int max = galGroup.getMax(); // 0 is unlimited
                int pageSize = 1000;
                int limit = (max == 0) ? pageSize : Math.min(pageSize, max); // page size for GAL sync account search

                GalSearchType searchType = GalSearchType.group;

                boolean hasMore = true;

                while (hasMore && !galGroup.reachedMax()) {
                    ZimbraSoapContext zsc = new ZimbraSoapContext(adminAuthToken, null, SoapProtocol.Soap12, SoapProtocol.Soap12);
                    GalSearchParams params = new GalSearchParams(domain, zsc);

                    // create a request in case the GAL sync account search needs to be proxied,
                    Element request = Element.create(SoapProtocol.Soap12, AdminConstants.SEARCH_GAL_REQUEST);
                    request.addAttribute(AdminConstants.A_DOMAIN, domain.getName());
                    request.addAttribute(AdminConstants.A_TYPE, searchType.name());
                    request.addAttribute(MailConstants.A_QUERY_OFFSET, offset);
                    request.addAttribute(MailConstants.A_QUERY_LIMIT, limit);
                    params.setRequest(request);

                    params.setType(searchType);
                    params.setLimit(limit);
                    params.setLdapLimit(max); // set ldap limit to configured max if the search falls back to ldap search
                    params.setResponseName(AdminConstants.SEARCH_GAL_RESPONSE);

                    SyncGalGroupCallback resultCallback = new SyncGalGroupCallback(params, galGroup);
                    params.setResultCallback(resultCallback);
                    GalSearchControl gal = new GalSearchControl(params);

                    ZimbraLog.gal.debug("GalGroup - searching GAL for groups: domain=" + domain.getName() + ", max="+ max + ", limit=" + limit + ", offset=" + offset);
                    gal.search();

                    offset += limit;

                    if (resultCallback.isPagingSupported())
                        hasMore = resultCallback.getHasMore();
                    else
                        hasMore = false;
                }
            }

            private static class SyncGalGroupCallback extends GalGroupCallback {
                private DomainGalGroupCache galGroup;
                private boolean hasMore;
                private boolean pagingSupported; // default to false

                // extra email fields on gal entries, e.g. aliases
                private String[] EXTRA_EMAIL_FIELDS = new String[] {
                        "email2", "email3", "email4", "email5", "email6", "email7", "email8", "email9",
                        "email10", "email11", "email12", "email13", "email14", "email15", "email16"
                };

                private SyncGalGroupCallback(GalSearchParams params, DomainGalGroupCache galGroup) {
                    super(params);
                    this.galGroup = galGroup;
                }

                @Override
                public Element handleContact(Contact contact) throws ServiceException {
                    if (!contact.isGroup())
                        return null;

                    String email = getSingleAttr(contact, ContactConstants.A_email);
                    String zimbraId = getSingleAttr(contact, ContactConstants.A_zimbraId);

                    if (email == null) {
                        ZimbraLog.gal.info("GalGroup - handle Contact: contact " +
                                contact.getFileAsString() + "(" + contact.getId() + ")" + " does not have an email address." +
                                " Not adding to gal group cache.");
                    } else {
                        addResult(email, zimbraId, contact);
                    }
                    return null;
                }

                @Override
                public void handleContact(GalContact galContact) throws ServiceException {
                    if (!galContact.isGroup())
                        return;

                    String email = getSingleAttr(galContact, ContactConstants.A_email);
                    String zimbraId = getSingleAttr(galContact, ContactConstants.A_zimbraId);

                    if (email == null) {
                        ZimbraLog.gal.info("GalGroup - handle GalContact: contact " +
                                galContact.getId() + " does not have an email address." +
                                " Not adding to gal group cache.");
                    } else {
                        addResult(email, zimbraId, galContact);
                    }
                }

                @Override
                public void handleElement(Element e) throws ServiceException {
                    HashMap<String,Object> contact = parseContactElement(e);
                    if (!Contact.isGroup(contact))
                        return;

                    String email = getSingleAttr(contact, ContactConstants.A_email);
                    String zimbraId = getSingleAttr(contact, ContactConstants.A_zimbraId);

                    if (email == null) {
                        ZimbraLog.gal.info("GalGroup - handle Element: contact " +
                                e.toString() + " does not have an email address." +
                                " Not adding to gal group cache.");
                    } else {
                        addResult(email, zimbraId, contact);
                    }
                }

                private void addResult(String email, String zimbraId, Object contact) {
                    if (isZimbraInternalGroup(email, zimbraId)) {
                        galGroup.addInternalGroup(email);
                        for (String extraEmailField : EXTRA_EMAIL_FIELDS) {
                            String extraEmail = getSingleAttr(contact, extraEmailField);
                            if (extraEmail != null)
                                galGroup.addInternalGroup(extraEmail);
                        }
                    } else {
                        galGroup.addExternalGroup(email);
                        for (String extraEmailField : EXTRA_EMAIL_FIELDS) {
                            String extraEmail = getSingleAttr(contact, extraEmailField);
                            if (extraEmail != null)
                                galGroup.addExternalGroup(extraEmail);
                        }
                    }
                }

                @Override
                public void setQueryOffset(int offset) {
                    pagingSupported = true;
                }

                @Override
                public void setHasMoreResult(boolean more) {
                    hasMore = more;
                }

                private boolean getHasMore() {
                    return hasMore;
                }

                private boolean isPagingSupported() {
                    return pagingSupported;
                }

            }
        }
    }

    /**
     * GAL group search result for an email address
     */
    private static class EmailAddrGalGroupCache implements GalGroupCache {
        SearchGroupCallback resultCallback;

        EmailAddrGalGroupCache(String addr, Account requestedAcct) {
            GalSearchParams params = new GalSearchParams(requestedAcct);
            params.setQuery(addr);
            params.setType(GalSearchType.group);
            params.setLimit(1);
            resultCallback = new SearchGroupCallback(params);
            params.setResultCallback(resultCallback);

            GalSearchControl gal = new GalSearchControl(params);
            try {
                gal.search();
            } catch (ServiceException e) {
                ZimbraLog.gal.warn("GalGroup - unable to search GAL group for addr:" + addr, e);
            }
        }

        @Override
        public boolean isInternalGroup(String addr) {
            return resultCallback.isInternalGroup();
        }

        @Override
        public boolean isExternalGroup(String addr) {
            return resultCallback.isExternalGroup();
        }

        private static class SearchGroupCallback extends GalGroupCallback {
            private boolean isInternalGroup;
            private boolean isExternalGroup;

            private SearchGroupCallback(GalSearchParams params) {
                super(params);
            }

            @Override
            public Element handleContact(Contact contact) throws ServiceException {
                if (!contact.isGroup())
                    return null;

                String email = getSingleAttr(contact, ContactConstants.A_email);
                String zimbraId = getSingleAttr(contact, ContactConstants.A_zimbraId);
                setResult(email, zimbraId);
                return null;
            }

            @Override
            public void handleContact(GalContact galContact) throws ServiceException {
                if (!galContact.isGroup())
                    return;

                String email = getSingleAttr(galContact, ContactConstants.A_email);
                String zimbraId = getSingleAttr(galContact, ContactConstants.A_zimbraId);
                setResult(email, zimbraId);
            }

            @Override
            public void handleElement(Element e) throws ServiceException {
                HashMap<String,Object> contact = parseContactElement(e);
                if (!Contact.isGroup(contact))
                    return;

                String email = getSingleAttr(contact, ContactConstants.A_email);
                String zimbraId = getSingleAttr(contact, ContactConstants.A_zimbraId);
                setResult(email, zimbraId);
            }

            private void setResult(String email, String zimbraId) {
                if (isZimbraInternalGroup(email, zimbraId))
                    isInternalGroup = true;
                else
                    isExternalGroup = true;
            }

            private boolean isInternalGroup() {
                return isInternalGroup;
            }

            private boolean isExternalGroup() {
                return isExternalGroup;
            }

        }
    }

}
