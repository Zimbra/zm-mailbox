/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011 Zimbra, Inc.
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
package com.zimbra.cs.gal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Strings;
import com.google.common.io.Closeables;
import com.zimbra.common.account.ZAttrProvisioning.GalMode;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DistributionListBy;
import com.zimbra.soap.admin.type.DataSourceType;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;
import com.zimbra.cs.gal.GalSearchConfig.GalType;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.index.ContactHit;
import com.zimbra.cs.index.ResultsPager;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.type.GalSearchType;

public class GalSearchControl {
    private GalSearchParams mParams;

    public GalSearchControl(GalSearchParams params) {
        mParams = params;
    }

    private void checkFeatureEnabled(String extraFeatAttr) throws ServiceException {
        AuthToken authToken = mParams.getAuthToken();
        boolean isAdmin = authToken == null ? false : AuthToken.isAnyAdmin(authToken);

        // admin is always allowed.
        if (isAdmin)
            return;

        // check feature enabling attrs
        Account acct = mParams.getAccount();
        if (acct == null) {
            if (authToken != null)
                acct = Provisioning.getInstance().get(AccountBy.id, authToken.getAccountId());

            if (acct == null)
                throw ServiceException.PERM_DENIED("unable to get account for GAL feature checking");
        }

        if (!acct.getBooleanAttr(Provisioning.A_zimbraFeatureGalEnabled, false))
            throw ServiceException.PERM_DENIED("GAL feature (" + Provisioning.A_zimbraFeatureGalEnabled + ") is not enabled");

        if (extraFeatAttr != null) {
            if (!acct.getBooleanAttr(extraFeatAttr, false))
                throw ServiceException.PERM_DENIED("GAL feature (" + extraFeatAttr + ") is not enabled");
        }
    }

    public void autocomplete() throws ServiceException {

        checkFeatureEnabled(Provisioning.A_zimbraFeatureGalAutoCompleteEnabled);

        mParams.setOp(GalOp.autocomplete);

        Account requestedAcct = mParams.getAccount();

        boolean useGalSyncAcct = requestedAcct == null ? true :
            requestedAcct.isGalSyncAccountBasedAutoCompleteEnabled();

        if (useGalSyncAcct) {
            try {
                Account galAcct = mParams.getGalSyncAccount();
                if (galAcct == null)
                    galAcct = getGalSyncAccount();
                accountSearch(galAcct);
                return;
            } catch (GalAccountNotConfiguredException e) {
            }
        }
        // fallback to ldap search
        String query = Strings.nullToEmpty(mParams.getQuery());
        mParams.setQuery(query.replaceFirst("[*]*$", "*"));
        mParams.getResultCallback().reset(mParams);
        ldapSearch();
    }

    public void search() throws ServiceException {

        checkFeatureEnabled(null);

        String query = mParams.getQuery();
        // '.' is a special operator that matches everything.
        // We don't support it in auto-complete.
        if (".".equals(query)) {
            mParams.setQuery(null);
        }
        mParams.setOp(GalOp.search);
        try {
            Account galAcct = mParams.getGalSyncAccount();
            if (galAcct == null)
                galAcct = getGalSyncAccount();
            accountSearch(galAcct);
        } catch (GalAccountNotConfiguredException e) {
            query = Strings.nullToEmpty(query);
            // fallback to ldap search
            if (!query.endsWith("*"))
                query = query + "*";
            if (!query.startsWith("*"))
                query = "*" + query;
            mParams.setQuery(query);
            mParams.getResultCallback().reset(mParams);
            ldapSearch();
        }
    }

    private static HashSet<String> SyncClients;

    static {
        SyncClients = new HashSet<String>();
    }

    public void sync() throws ServiceException {

        checkFeatureEnabled(Provisioning.A_zimbraFeatureGalSyncEnabled);

        String id = Thread.currentThread().getName() + " / " + mParams.getUserInfo();
        int capacity = mParams.getDomain().getGalSyncMaxConcurrentClients();
        boolean limitReached = true;
        
        mParams.setQuery("");
        mParams.setOp(GalOp.sync);
        mParams.setFetchGroupMembers(true);
        mParams.setNeedSMIMECerts(true);
        Account galAcct = mParams.getGalSyncAccount();
        GalSyncToken gst = mParams.getGalSyncToken();
        
        // if the presented sync token is old LDAP timestamp format, we need to sync
        // against LDAP server to keep the client up to date.
        boolean useGalSyncAccount = mParams.isIdOnly() && gst.doMailboxSync();
        if (useGalSyncAccount) {
            try {
                synchronized (SyncClients) {
                    // allow the sync only when the # of sync clients
                    // are within the capacity.
                    if (SyncClients.size() < capacity) {
                        SyncClients.add(id);
                        limitReached = false;
                    }
                }
                if (limitReached) {
                    logCurrentSyncClients();
                    // return "no change".
                    mParams.getResultCallback().setNewToken(mParams.getGalSyncToken());
                    return;
                }
                if (galAcct == null)
                    galAcct = getGalSyncAccountForSync();                
                accountSync(galAcct);
                // account based sync was finished
                return;
            } catch (GalAccountNotConfiguredException e) {
                // if there was an error in GAL sync account based sync,
                // fallback to ldap search
                mParams.getResultCallback().reset(mParams);
            } finally {
                synchronized (SyncClients) {
                    SyncClients.remove(id);
                }
            }
        }
        ldapSearch();
    }

    private void logCurrentSyncClients() {
        if (!ZimbraLog.galconcurrency.isDebugEnabled())
            return;
        StringBuilder buf = new StringBuilder();
        buf.append("limit reached, turning away ").append(mParams.getUserInfo());
        buf.append(", busy sync clients:");
        synchronized (SyncClients) {
            for (String id : SyncClients) {
                buf.append(" [").append(id).append("]");
            }
        }
        ZimbraLog.galconcurrency.debug(buf.toString());
    }
    
    private Account getGalSyncAccountForSync() throws GalAccountNotConfiguredException, ServiceException {
        // If the client has already synced with a galsync account use the same
        // account for subsequent syncs.
        GalSyncToken gst = mParams.getGalSyncToken();
        if (gst == null || gst.isEmpty() || !gst.doMailboxSync())
            return getGalSyncAccount();        
        Domain d = mParams.getDomain();
        String[] accts = d.getGalAccountId();
        if (accts.length == 0)
            throw new GalAccountNotConfiguredException();
        Account ret = null;
        for (String acctId : accts) {
            if (gst.getChangeId(acctId) > 0) {
                Account a = Provisioning.getInstance().getAccountById(acctId);
                if (a != null && isValidGalSyncAccount(a))
                    ret = a;
                break;
            }    
        }
        if (ret == null)
            throw new GalAccountNotConfiguredException();
        return ret;
    }
    
    private Account getGalSyncAccount() throws GalAccountNotConfiguredException, ServiceException {
        Domain d = mParams.getDomain();
        String[] accts = d.getGalAccountId();
        if (accts.length == 0)
            throw new GalAccountNotConfiguredException();
        Provisioning prov = Provisioning.getInstance();
        Account ret = null;
        for (String acctId : accts) {
            Account a = prov.getAccountById(acctId);
            if (a == null)
                continue;
            if (isValidGalSyncAccount(a)) {
                ret = a;
                if (Provisioning.onLocalServer(a))
                    break;
            }
        }        
        if (ret == null)
            throw new GalAccountNotConfiguredException();
        return ret;
    }
    
    private boolean isValidGalSyncAccount(Account a) throws ServiceException {
        for (DataSource ds : a.getAllDataSources()) {
            if (ds.getType() != DataSourceType.gal)
                continue;
            // check if there was any successful import from gal
            if (ds.getAttr(Provisioning.A_zimbraGalLastSuccessfulSyncTimestamp, null) == null)
                return false;
            if (ds.getAttr(Provisioning.A_zimbraGalStatus).compareTo("enabled") != 0)
                return false;
            if (ds.getAttr(Provisioning.A_zimbraDataSourceEnabled).compareTo("TRUE") != 0)
                return false;
        }
        return true;
    }

    private void generateSearchQuery(Account galAcct) throws ServiceException {
        String query = mParams.getQuery();
        String searchByDn = mParams.getSearchEntryByDn();
        
        GalSearchType type = mParams.getType();
        StringBuilder searchQuery = new StringBuilder();
        
        if (searchByDn != null) {
            searchQuery.append("#dn:(" + searchByDn + ")");
        } else if (!Strings.isNullOrEmpty(query)) {
            searchQuery.append("contact:\"");
            searchQuery.append(query.replace("\"", "\\\"")); // escape quotes
            searchQuery.append("\" AND");
        }

        GalSearchQueryCallback queryCallback = mParams.getExtraQueryCallback();
        if (queryCallback != null) {
            String extraQuery = queryCallback.getMailboxSearchQuery();
            if (extraQuery != null) {
                ZimbraLog.gal.debug("extra search query: " + extraQuery);
                searchQuery.append(" (").append(extraQuery).append(") AND");
            }
        }

        GalMode galMode = mParams.getDomain().getGalMode();
        boolean first = true;
        for (DataSource ds : galAcct.getAllDataSources()) {
            if (ds.getType() != DataSourceType.gal)
                continue;
            String galType = ds.getAttr(Provisioning.A_zimbraGalType);
            if (galMode == GalMode.ldap && galType.compareTo("zimbra") == 0)
                continue;
            if (galMode == GalMode.zimbra && galType.compareTo("ldap") == 0)
                continue;
            if (first) searchQuery.append("("); else searchQuery.append(" OR");
            first = false;
            searchQuery.append(" inid:").append(ds.getFolderId());
        }
        if (!first)
            searchQuery.append(")");
        switch (type) {
        case resource:
            searchQuery.append(" AND #zimbraAccountCalendarUserType:RESOURCE");
            break;
        case group:
            searchQuery.append(" AND #type:group");
            break;
        case account:
            searchQuery.append(" AND !(#zimbraAccountCalendarUserType:RESOURCE)");
            break;
        case all:
            break;
        }
        ZimbraLog.gal.debug("query: %s", searchQuery);
        mParams.parseSearchParams(mParams.getRequest(), searchQuery.toString());
    }

    private boolean generateLocalResourceSearchQuery(Account galAcct) throws ServiceException {
        String query = mParams.getQuery();
        StringBuilder searchQuery = new StringBuilder();
        if (!Strings.isNullOrEmpty(query)) {
            searchQuery.append("contact:\"");
            searchQuery.append(query.replace("\"", "\\\"")); // escape quotes
            searchQuery.append("\" AND");
        }
        searchQuery.append(" #zimbraAccountCalendarUserType:RESOURCE");
        for (DataSource ds : galAcct.getAllDataSources()) {
            if (ds.getType() != DataSourceType.gal)
                continue;
            String galType = ds.getAttr(Provisioning.A_zimbraGalType);
            if (galType.compareTo("ldap") == 0)
                continue;
            searchQuery.append(" AND (");
            searchQuery.append(" inid:").append(ds.getFolderId());
            searchQuery.append(")");
            ZimbraLog.gal.debug("query: "+searchQuery.toString());
            mParams.parseSearchParams(mParams.getRequest(), searchQuery.toString());
            return true;
        }
        return false;
    }

    private void accountSearch(Account galAcct) throws ServiceException, GalAccountNotConfiguredException {
        if (!galAcct.getAccountStatus().isActive()) {
            ZimbraLog.gal.info("GalSync account "+galAcct.getId()+" is in "+galAcct.getAccountStatus().name());
            throw new GalAccountNotConfiguredException();
        }
        if (Provisioning.onLocalServer(galAcct)) {
            if (needResources()) {
                if (generateLocalResourceSearchQuery(galAcct) &&
                        !doLocalGalAccountSearch(galAcct))
                    throw new GalAccountNotConfiguredException();
            }
            generateSearchQuery(galAcct);
            if (!doLocalGalAccountSearch(galAcct))
                throw new GalAccountNotConfiguredException();
        } else {
            if (!proxyGalAccountSearch(galAcct))
                throw new GalAccountNotConfiguredException();
        }
    }

    private void accountSync(Account galAcct) throws ServiceException, GalAccountNotConfiguredException {
        if (!galAcct.getAccountStatus().isActive()) {
            ZimbraLog.gal.info("GalSync account "+galAcct.getId()+" is in "+galAcct.getAccountStatus().name());
            throw new GalAccountNotConfiguredException();
        }
        if (Provisioning.onLocalServer(galAcct)) {
            if (!doLocalGalAccountSync(galAcct))
                throw new GalAccountNotConfiguredException();
        } else {
            if (!proxyGalAccountSearch(galAcct))
                throw new GalAccountNotConfiguredException();
        }
    }

    private boolean doLocalGalAccountSearch(Account galAcct) {
        ZimbraQueryResults zqr = null;
        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(galAcct);
            SearchParams searchParams = mParams.getSearchParams();
            zqr = mbox.index.search(SoapProtocol.Soap12, new OperationContext(mbox), searchParams);
            ResultsPager pager = ResultsPager.create(zqr, searchParams);
            GalSearchResultCallback callback = mParams.getResultCallback();
            int num = 0;
            while (pager.hasNext()) {
                ZimbraHit hit = pager.getNextHit();
                if (hit instanceof ContactHit) {
                    Element contactElem = callback.handleContact(((ContactHit)hit).getContact());
                    if (contactElem != null)
                        contactElem.addAttribute(MailConstants.A_SORT_FIELD, hit.getSortField(pager.getSortOrder()).toString());
                }
                num++;
                if (num == mParams.getLimit())
                    break;
            }
            callback.setSortBy(zqr.getSortBy().toString());
            callback.setQueryOffset(searchParams.getOffset());
            callback.setHasMoreResult(pager.hasNext());
        } catch (Exception e) {
            ZimbraLog.gal.warn("search on GalSync account failed for %s", galAcct.getId(), e);
            return false;
        } finally {
            Closeables.closeQuietly(zqr);
        }
        return true;
    }

    private boolean doLocalGalAccountSync(Account galAcct) {
        GalSyncToken token = mParams.getGalSyncToken();
        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(galAcct);
            OperationContext octxt = new OperationContext(mbox);
            GalSearchResultCallback callback = mParams.getResultCallback();
            HashSet<Integer> folderIds = new HashSet<Integer>();
            Domain domain = mParams.getDomain();
            GalMode galMode = domain.getGalMode();
            String syncToken = null;

            // bug 46608
            // first do local resources sync if galMode == ldap
            if (galMode == GalMode.ldap &&
                    domain.isGalAlwaysIncludeLocalCalendarResources()) {
                for (DataSource ds : galAcct.getAllDataSources()) {
                    if (ds.getType() != DataSourceType.gal)
                        continue;
                    String galType = ds.getAttr(Provisioning.A_zimbraGalType);
                    if (galType.compareTo("ldap") == 0)
                        continue;
                    int fid = ds.getFolderId();
                    DataSourceItem folderMapping = DbDataSource.getMapping(ds, fid);
                    if (folderMapping.md == null)
                        continue;
                    folderIds.add(fid);
                    syncToken = LdapUtil.getEarlierTimestamp(syncToken, folderMapping.md.get(GalImport.SYNCTOKEN));
                    if (mParams.isIdOnly() && token.doMailboxSync()) {
                        int changeId = token.getChangeId(galAcct.getId());
                        Pair<List<Integer>,TypedIdList> changed = mbox.getModifiedItems(octxt, changeId,
                                MailItem.Type.CONTACT, folderIds);

                        int count = 0;
                        for (int itemId : changed.getFirst()) {
                            MailItem item = mbox.getItemById(octxt, itemId, MailItem.Type.CONTACT);
                            if (item instanceof Contact) {
                                Contact c = (Contact)item;
                                String accountType = c.get("zimbraAccountCalendarUserType");
                                if (accountType != null &&
                                        accountType.equals("RESOURCE"))
                                    callback.handleContact(c);
                            }
                            count++;
                            if (count % 100 == 0)
                                ZimbraLog.gal.debug("processing resources #"+count);
                        }
                    }
                    break;
                }
            }

            folderIds.clear();
            for (DataSource ds : galAcct.getAllDataSources()) {
                if (ds.getType() != DataSourceType.gal)
                    continue;
                String galType = ds.getAttr(Provisioning.A_zimbraGalType);
                if (galMode == GalMode.ldap && galType.compareTo("zimbra") == 0)
                    continue;
                if (galMode == GalMode.zimbra && galType.compareTo("ldap") == 0)
                    continue;
                int fid = ds.getFolderId();
                DataSourceItem folderMapping = DbDataSource.getMapping(ds, fid);
                if (folderMapping.md == null)
                    continue;
                folderIds.add(fid);
                syncToken = LdapUtil.getEarlierTimestamp(syncToken, folderMapping.md.get(GalImport.SYNCTOKEN));
            }
            if (mParams.isIdOnly() && token.doMailboxSync()) {
                int changeId = token.getChangeId(galAcct.getId());
                List<Integer> deleted = null;
                if (changeId > 0) {
                    try {
                        deleted = mbox.getTombstones(changeId).getAll();
                    } catch (MailServiceException e) {
                        if (MailServiceException.MUST_RESYNC == e.getCode()) {
                            ZimbraLog.gal.warn("sync token too old, deleted items will not be handled", e);
                        } else {
                            throw e;
                        }
                    }
                }
                
                Pair<List<Integer>,TypedIdList> changed = mbox.getModifiedItems(octxt, changeId,
                        MailItem.Type.CONTACT, folderIds);

                int count = 0;
                for (int itemId : changed.getFirst()) {
                    MailItem item = mbox.getItemById(octxt, itemId, MailItem.Type.CONTACT);
                    if (item instanceof Contact)
                        callback.handleContact((Contact)item);
                    count++;
                    if (count % 100 == 0)
                        ZimbraLog.gal.debug("processing #"+count);
                }

                if (deleted != null) {
                    for (int itemId : deleted) {
                        callback.handleDeleted(new ItemId(galAcct.getId(), itemId));
                    }
                }
                
            }
            GalSyncToken newToken = new GalSyncToken(syncToken, galAcct.getId(), mbox.getLastChangeID());
            ZimbraLog.gal.debug("computing new sync token for "+galAcct.getId()+": "+newToken);
            callback.setNewToken(newToken);
            callback.setHasMoreResult(false);
        } catch (Exception e) {
            ZimbraLog.gal.warn("search on GalSync account failed for "+galAcct.getId(), e);
            return false;
        }
        return true;
    }

    private boolean proxyGalAccountSearch(Account galSyncAcct) {
        try {
            Provisioning prov = Provisioning.getInstance();
            String serverUrl = URLUtil.getAdminURL(prov.getServerByName(galSyncAcct.getMailHost()));
            SoapHttpTransport transport = new SoapHttpTransport(serverUrl);
            AuthToken auth = mParams.getAuthToken();
            transport.setAuthToken((auth == null) ? AuthProvider.getAdminAuthToken().toZAuthToken() : auth.toZAuthToken());

            ZimbraSoapContext zsc = mParams.getSoapContext();
            if (zsc != null) {
                transport.setResponseProtocol(zsc.getResponseProtocol());

                String requestedAcctId = zsc.getRequestedAccountId();
                String authTokenAcctId = zsc.getAuthtokenAccountId();
                if (requestedAcctId != null && !requestedAcctId.equalsIgnoreCase(authTokenAcctId))
                    transport.setTargetAcctId(requestedAcctId);
            }

            Element req = mParams.getRequest();
            if (req == null) {
                req = Element.create(SoapProtocol.Soap12, AccountConstants.SEARCH_GAL_REQUEST);
                req.addAttribute(AccountConstants.A_TYPE, mParams.getType().toString());
                req.addAttribute(AccountConstants.A_LIMIT, mParams.getLimit());
                req.addAttribute(AccountConstants.A_NAME, mParams.getQuery());
                req.addAttribute(AccountConstants.A_REF, mParams.getSearchEntryByDn());
            }
            req.addAttribute(AccountConstants.A_GAL_ACCOUNT_ID, galSyncAcct.getId());
            Element resp = transport.invokeWithoutSession(req.detach());
            GalSearchResultCallback callback = mParams.getResultCallback();

            if (callback.passThruProxiedGalAcctResponse()) {
                callback.handleProxiedResponse(resp);
                return true;
            }

            Iterator<Element> iter = resp.elementIterator(MailConstants.E_CONTACT);
            while (iter.hasNext())
                callback.handleElement(iter.next());
            iter = resp.elementIterator(MailConstants.E_DELETED);
            while (iter.hasNext())
                callback.handleElement(iter.next());
            String newTokenStr = resp.getAttribute(MailConstants.A_TOKEN, null);
            if (newTokenStr != null) {
                GalSyncToken newToken = new GalSyncToken(newTokenStr);
                ZimbraLog.gal.debug("computing new sync token for proxied account "+galSyncAcct.getId()+": "+newToken);
                callback.setNewToken(newToken);
            }
            boolean hasMore =  resp.getAttributeBool(MailConstants.A_QUERY_MORE, false);
            callback.setHasMoreResult(hasMore);
            if (hasMore) {
                callback.setSortBy(resp.getAttribute(MailConstants.A_SORTBY));
                callback.setQueryOffset((int)resp.getAttributeLong(MailConstants.A_QUERY_OFFSET));
            }
        } catch (SoapFaultException e) {
            GalSearchResultCallback callback = mParams.getResultCallback();
            if (callback.passThruProxiedGalAcctResponse()) {
                Element fault = e.getFault();
                callback.handleProxiedResponse(fault);

                // if the callback says pass thru, it is up to the callback to take full
                // responsibility for the result.
                // return true so we do *not* fallback to do the ldap search.
                return true;
            } else {
                ZimbraLog.gal.warn("remote search on GalSync account failed for " + galSyncAcct.getName(), e);
                return false;
            }
        } catch (Exception e) {
            ZimbraLog.gal.warn("remote search on GalSync account failed for " + galSyncAcct.getName(), e);
            return false;
        }

        return true;
    }

    public void ldapSearch() throws ServiceException {
        Domain domain = mParams.getDomain();
        GalMode galMode = domain.getGalMode();
        GalSearchType stype = mParams.getType();
        Provisioning prov = Provisioning.getInstance();

        if (needResources()) {
            mParams.setType(GalSearchType.resource);
            mParams.createSearchConfig(GalType.zimbra);
            try {
                prov.searchGal(mParams);
            } catch (Exception e) {
                throw ServiceException.FAILURE("ldap search failed", e);
            }
            mParams.setType(stype);
        }

        Integer ldapLimit = mParams.getLdapLimit();
        int limit;
        if (ldapLimit == null)
            limit = mParams.getLimit();
        else
            limit = ldapLimit;

        // restrict to domain config if we are not syncing, and there is no specific ldap limit set
        if (limit == 0 && GalOp.sync != mParams.getOp() && ldapLimit == null) {
            limit = domain.getGalMaxResults();
        }
        mParams.setLimit(limit);

        if (galMode == GalMode.both) {
            // make two gal searches for 1/2 results each
            mParams.setLimit(limit / 2);
        }
        GalType type = GalType.ldap;
        if (galMode != GalMode.ldap) {
            // do zimbra gal search
            type = GalType.zimbra;
        }
        mParams.createSearchConfig(type);
        try {
            prov.searchGal(mParams);
        } catch (Exception e) {
            throw ServiceException.FAILURE("ldap search failed", e);
        }

        boolean hadMore = mParams.getResult().getHadMore();
        String newToken = mParams.getResult().getToken();
        if (mParams.getResult().getTokenizeKey() != null)
            hadMore = true;
        if (galMode == GalMode.both) {
            // do the second query
            mParams.createSearchConfig(GalType.ldap);
            try {
                prov.searchGal(mParams);
            } catch (Exception e) {
                throw ServiceException.FAILURE("ldap search failed", e);
            }
            hadMore |= mParams.getResult().getHadMore();
            newToken = LdapUtil.getLaterTimestamp(newToken, mParams.getResult().getToken());
            if (mParams.getResult().getTokenizeKey() != null)
                hadMore = true;
        }

        if (mParams.getOp() == GalOp.sync)
            mParams.getResultCallback().setNewToken(newToken);
        mParams.getResultCallback().setHasMoreResult(hadMore);
    }


    // bug 46608
    // do zimbra resources search if galMode == ldap
    private boolean needResources() throws ServiceException {
        Domain domain = mParams.getDomain();
        return (domain.getGalMode() == GalMode.ldap &&
                (GalSearchType.all == mParams.getType() || GalSearchType.resource == mParams.getType()) &&
                domain.isGalAlwaysIncludeLocalCalendarResources());
    }

    private static class GalAccountNotConfiguredException extends Exception {
        private static final long serialVersionUID = 679221874958248740L;

        public GalAccountNotConfiguredException() {
        }
    }

    public static boolean canExpandGalGroup(String groupName, String groupId, Account authedAcct) {

        if (groupName == null || authedAcct == null)
            return false;

        // if no groupId we consider it's an external group, no ACL checking
        if (groupId == null)
            return true;

        // check feature enabled
        // if (!authedAcct.)
        //     return false;

        // check permission if is is a Zimbra DL
        Provisioning prov = Provisioning.getInstance();

        if (prov.isDistributionList(groupName)) {  // quick check to see if this is a zimbra group

            try {
                // get the dl object for ACL checking
                DistributionList dl = prov.getDLBasic(Key.DistributionListBy.id, groupId);

                // the DL might have been deleted since the last GAL sync account sync, throw.
                // or should we just let the request through?
                if (dl == null) {
                    ZimbraLog.gal.warn("unable to find distribution list " + groupName + "(" + groupId + ") for permission checking");
                    return false;
                }

                if (!AccessManager.getInstance().canDo(authedAcct, dl, User.R_viewDistList, false))
                    return false;

            } catch (ServiceException e) {
                ZimbraLog.gal.warn("unable to check permission for gal group expansion: " + groupName);
                return false;
            }
        }

        return true;
    }

}
