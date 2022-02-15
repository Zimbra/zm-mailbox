/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpException;

import com.google.common.base.Strings;
import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.ZAttrProvisioning.GalMode;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.Element.ContainerException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapFaultException;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.cs.account.gal.GalOp;
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
import com.zimbra.soap.admin.type.DataSourceType;
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
                accountSearch(galAcct, true);
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
        search(true);
    }

    public void search(boolean wildCardSearch) throws ServiceException {
        checkFeatureEnabled(null);
        mParams.setWildCardSearch(wildCardSearch);
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
            accountSearch(galAcct, true);
        } catch (GalAccountNotConfiguredException e) {
            query = Strings.nullToEmpty(query);
            // fallback to ldap search
            if (wildCardSearch) {
                if (!query.endsWith("*")) {
                    query = query + "*";
                }
                if (!query.startsWith("*")) {
                    query = "*" + query;
                }
            }
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

        synchronized (SyncClients) {
            // allow the sync only when the # of sync clients
            // are within the capacity.
            if (capacity == 0 || SyncClients.size() < capacity) {
                SyncClients.add(id);
                limitReached = false;
            }
        }
        if (limitReached) {
            logCurrentSyncClients();
            // return "no change".
            mParams.getResultCallback().setNewToken(mParams.getGalSyncToken());
            mParams.getResultCallback().setThrottled(true);
            return;
        }

        try {
            mParams.setQuery("");
            mParams.setOp(GalOp.sync);
            mParams.setFetchGroupMembers(true);
            mParams.setNeedSMIMECerts(true);
            Account galAcct = mParams.getGalSyncAccount();
            GalSyncToken gst = mParams.getGalSyncToken();
            Domain domain = mParams.getDomain();

            if (mParams.getOp() == GalOp.sync) {
                int limit = mParams.getLimit();
                int domainLimit = domain.getGalSyncSizeLimit();
                // Use the lower value in case of non zero values of limit and domainLimit
                if (limit != 0 && domainLimit != 0) {
                    limit = limit > domainLimit ? domainLimit : limit;
                } else if (limit == 0) {
                    //if limit is zero use domainLimit
                    limit = domainLimit;
                }
                mParams.setLimit(limit);
            }

            // if the presented sync token is old LDAP timestamp format, we need to sync
            // against LDAP server to keep the client up to date.
            boolean useGalSyncAccount = gst.doMailboxSync() && (mParams.isIdOnly() || domain.isLdapGalSyncDisabled());
            if (useGalSyncAccount) {
                ZimbraLog.mailbox.debug("sync against galsync account");
                try {
                    if (galAcct == null)
                        galAcct = getGalSyncAccountForSync();
                    ZimbraLog.mailbox.debug("start syncing galsync account %s", galAcct.getId());
                    accountSync(galAcct);
                    // account based sync was finished
                    return;
                } catch (GalAccountNotConfiguredException e) {
                    // if there was an error in GAL sync account based sync,
                    // fallback to ldap search
                    mParams.getResultCallback().reset(mParams);
                }
            }
            if (mParams.isIdOnly() || domain.isLdapGalSyncDisabled()) {
                // add recommendation to perform fullsync if there is a valid GSA.
                try {
                    if (getGalSyncAccount() != null) {
                        mParams.getResultCallback().setFullSyncRecommended(true);
                    }
                } catch (GalAccountNotConfiguredException e) {}
            }
            if (domain.isLdapGalSyncDisabled()) {
                // return the same sync token.
                mParams.getResultCallback().setNewToken(mParams.getGalSyncToken());
                return;
            }
            ldapSearch();
        }  finally {
            synchronized (SyncClients) {
                SyncClients.remove(id);
            }
        }
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

   public Account getGalSyncAccount() throws GalAccountNotConfiguredException, ServiceException {
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
        String searchQuery = getSearchQuery(galAcct, true);
        mParams.parseSearchParams(mParams.getRequest(), searchQuery);
    }

    private String getSearchQuery(Account galAcct, boolean addInId) throws ServiceException {
        String query = mParams.getQuery();
        String searchByDn = mParams.getSearchEntryByDn();
        String galSearchQuery = mParams.getGalSearchQuery();

        GalSearchType type = mParams.getType();
        StringBuilder searchQuery = new StringBuilder();

        if (searchByDn != null) {
            searchQuery.append("#dn:(" + searchByDn + ")");
        } else if (!Strings.isNullOrEmpty(galSearchQuery)) {
            searchQuery.append(galSearchQuery.replace("\"", "\\\"")); // escape quotes
        } else if (!Strings.isNullOrEmpty(query)) {
            searchQuery.append("contact:\"");
            searchQuery.append(query.replace("\"", "\\\"")); // escape quotes
            searchQuery.append("\"");
        }

        GalSearchQueryCallback queryCallback = mParams.getExtraQueryCallback();
        if (queryCallback != null) {
            String extraQuery = queryCallback.getMailboxSearchQuery();
            if (extraQuery != null) {
                ZimbraLog.gal.debug("extra search query: " + extraQuery);
                if (!StringUtil.isNullOrEmpty(searchQuery.toString())) {
                    searchQuery.append(" AND");
                }
                searchQuery.append(" (").append(extraQuery).append(")");
            }
        }

        boolean first = true;
        boolean needClosing = false;
        if (addInId && galAcct != null) {
            GalMode galMode = mParams.getDomain().getGalMode();
            for (DataSource ds : galAcct.getAllDataSources()) {
                if (ds.getType() != DataSourceType.gal)
                    continue;
                String galType = ds.getAttr(Provisioning.A_zimbraGalType);
                if (galMode == GalMode.ldap && galType.compareTo("zimbra") == 0)
                    continue;
                if (galMode == GalMode.zimbra && galType.compareTo("ldap") == 0)
                    continue;
                if (!StringUtil.isNullOrEmpty(searchQuery.toString())) {
                    if (first) {
                        searchQuery.append(" AND (");
                        needClosing = true;
                    } else {
                        searchQuery.append(" OR");
                    }
                }
                first = false;
                searchQuery.append(" inid:").append(ds.getFolderId());
            }
        }
        if (needClosing)
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
        return searchQuery.toString();
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

    public void addressListSearch(Account galAcct, boolean genQuery) throws ServiceException, GalAccountNotConfiguredException {
        accountSearch(galAcct, false);
    }

    private void accountSearch(Account galAcct, boolean generateQuery) throws ServiceException, GalAccountNotConfiguredException {
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
            if (generateQuery) {
                generateSearchQuery(galAcct);
            }

            if (!doLocalGalAccountSearch(galAcct))
                throw new GalAccountNotConfiguredException();
        } else {
            try {
                if (!proxyGalAccountSearch(galAcct, false))
                    throw new GalAccountNotConfiguredException();
            } catch (IOException | HttpException e) {
                ZimbraLog.gal.warn("remote search on GalSync account failed for " + galAcct.getName(), e);
                // let the request fallback to ldap based search
                throw new GalAccountNotConfiguredException();
            }
        }
    }

    private void accountSync(Account galAcct) throws ServiceException, GalAccountNotConfiguredException {
        if (!galAcct.getAccountStatus().isActive()) {
            ZimbraLog.gal.info("GalSync account "+galAcct.getId()+" is in "+galAcct.getAccountStatus().name());
            throw new GalAccountNotConfiguredException();
        }
        if (Provisioning.onLocalServer(galAcct)) {
            doLocalGalAccountSync(galAcct);
        } else {
            try {
                if (!proxyGalAccountSearch(galAcct, true))
                    throw new GalAccountNotConfiguredException();
            } catch (IOException | HttpException e) {
                ZimbraLog.gal.warn("remote sync on GalSync account failed for " + galAcct.getName(), e);
                // remote server may be down, return the same sync token so that client can try again.
                mParams.getResultCallback().setNewToken(mParams.getGalSyncToken());
            }
        }
    }

    private boolean doLocalGalAccountSearch(Account galAcct) {
        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(galAcct);
            SearchParams searchParams = mParams.getSearchParams();
            try (ZimbraQueryResults zqr = mbox.index.search(SoapProtocol.Soap12,
                new OperationContext(mbox), searchParams)) {
                ResultsPager pager = ResultsPager.create(zqr, searchParams);
                GalSearchResultCallback callback = mParams.getResultCallback();
                int num = 0;
                while (pager.hasNext()) {
                    ZimbraHit hit = pager.getNextHit();
                    if (hit instanceof ContactHit) {
                        Element contactElem = callback
                            .handleContact(((ContactHit) hit).getContact());
                        if (contactElem != null)
                            contactElem.addAttribute(MailConstants.A_SORT_FIELD,
                                hit.getSortField(pager.getSortOrder()).toString());
                    }
                    num++;
                    if (num == mParams.getLimit())
                        break;
                }
                callback.setSortBy(zqr.getSortBy().toString());
                callback.setQueryOffset(searchParams.getOffset());
                callback.setHasMoreResult(pager.hasNext());
            }
        } catch (Exception e) {
            ZimbraLog.gal.warn("search on GalSync account failed for %s", galAcct.getId(), e);
            return false;
        } 
        return true;
    }

    private void doLocalGalAccountSync(Account galAcct) throws ServiceException {
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(galAcct);
        OperationContext octxt = new OperationContext(mbox);
        GalSearchResultCallback callback = mParams.getResultCallback();
        Domain domain = mParams.getDomain();
        GalMode galMode = domain.getGalMode();
        int changeId = mParams.getGalSyncToken().getChangeId(galAcct.getId());

        // bug 46608
        // sync local resources from first datasource if galMode is ldap
        // and zimbraGalAlwaysIncludeLocalCalendarResources is set for the domain
        boolean syncLocalResources = (galMode == GalMode.ldap && domain.isGalAlwaysIncludeLocalCalendarResources());

        Set<Integer> folderIds = new HashSet<Integer>();
        String syncToken = null;
        for (DataSource ds : galAcct.getAllDataSources()) {
            if (ds.getType() != DataSourceType.gal) {
                ZimbraLog.gal.trace("skipping datasource %s: wrong type %s expected %s", ds.getName(), ds.getType(), DataSourceType.gal);
                continue;
            }

            if (galMode != null) {
                if(!(galMode.isBoth() || galMode.toString().equals(ds.getAttr(Provisioning.A_zimbraGalType)))) {
                    ZimbraLog.gal.debug("skipping datasource %s: wrong zimbraGalType %s expected %s", ds.getName(), ds.getAttr(Provisioning.A_zimbraGalType), galMode.toString());
                    continue;
                }
            }

            int fid = ds.getFolderId();
            DataSourceItem folderMapping = DbDataSource.getMapping(ds, fid);
            if (folderMapping.md == null) {
                ZimbraLog.gal.debug("skipping datasource %s: no folder mapping", ds.getName());
                continue;
            }
            folderIds.add(fid);
            syncToken = LdapUtil.getEarlierTimestamp(syncToken, folderMapping.md.get(GalImport.SYNCTOKEN));

            if (syncLocalResources) {
                doLocalGalAccountSync(callback, mbox, octxt, changeId, folderIds, syncToken, mParams.getLimit(),
                        Provisioning.A_zimbraAccountCalendarUserType, "RESOURCE", mParams.isGetCount());
                syncLocalResources = false;
            }
        }
        if (folderIds.isEmpty()) {
            throw ServiceException.FAILURE("no gal datasource with mapped folder found", null);
        }
        if (syncToken == null) {
            throw ServiceException.FAILURE("no gal datasource with sync token found", null);
        }

        doLocalGalAccountSync(callback, mbox, octxt, changeId, folderIds, syncToken, mParams.getLimit(), mParams.isGetCount());

        List<Integer> deleted = null;
        if (callback.getResponse() != null && !callback.getResponse().getAttributeBool(MailConstants.A_QUERY_MORE) && changeId > 0) {
            try {
                TypedIdList tIdList =  mbox.getTombstones(changeId);
                deleted = tIdList.getAllIds();
                int deletedChangeId = tIdList.getMaxModSequence();
                ZimbraLog.gal.debug("deleted change id = %s", deletedChangeId);
                if (deletedChangeId > changeId) {
                    GalSyncToken newToken = new GalSyncToken(syncToken, mbox.getAccountId(), deletedChangeId);
                    ZimbraLog.gal.debug("computing new sync token for %s:%s", mbox.getAccountId(), newToken);
                    callback.setNewToken(newToken);
                }
            } catch (MailServiceException e) {
                if (MailServiceException.MUST_RESYNC == e.getCode()) {
                    ZimbraLog.gal.warn("sync token too old, deleted items will not be handled", e);
                } else {
                    throw e;
                }
            }
        }

        if (deleted != null) {
            for (int itemId : deleted) {
                callback.handleDeleted(new ItemId(galAcct.getId(), itemId));
            }
        }
    }

    private void doLocalGalAccountSync(GalSearchResultCallback callback, Mailbox mbox,
        OperationContext octxt, int changeId, Set<Integer> folderIds, String syncToken, int limit, boolean getCount) throws ServiceException {
        doLocalGalAccountSync(callback, mbox, octxt, changeId, folderIds, syncToken, limit, null, null, getCount);
    }

    private void doLocalGalAccountSync(GalSearchResultCallback callback, Mailbox mbox,
        OperationContext octxt, int changeId, Set<Integer> folderIds, String syncToken, int limit,
            String filterAttr, String filterValue, boolean getCount) throws ServiceException {
        ZimbraLog.gal.info("Using limit %d for gal account sync", limit);
        Pair<List<Integer>,TypedIdList> changed = mbox.getModifiedItems(octxt, changeId,
                0, MailItem.Type.CONTACT, folderIds, -1, limit);

        if (getCount) {
            int remain = 0;
            if (limit != 0) {
                int total = mbox.getModifiedItemsCount(octxt, changeId, 0, MailItem.Type.CONTACT, folderIds);
                remain = total > limit ? total - limit : 0;
                ZimbraLog.gal.debug("totalCount: %d", total);
            }
            callback.setRemain(remain);
            ZimbraLog.gal.debug("remain: %d, limit: %d", remain, limit);
        }

        int count = 0;
        boolean hasMore = false;
        for (int itemId : changed.getFirst()) {
            try {
                MailItem item = mbox.getItemById(octxt, itemId, MailItem.Type.CONTACT);
                if (item instanceof Contact) {
                    Contact c = (Contact)item;
                    if (filterAttr != null && !filterValue.equals(c.get(filterAttr))) {
                        continue;
                    }

                    callback.handleContact(c);

                    count++;
                    if (count % 100 == 0) {
                        ZimbraLog.gal.trace("processing #%s", count);
                    }

                    changeId = item.getModifiedSequence();
                    if (count == limit) {
                        hasMore = true;
                        break;
                    }
                }
            } catch (MailServiceException mse) {
                if (MailServiceException.NO_SUCH_ITEM.equals(mse.getId())) {
                    ZimbraLog.gal.warn("skipping item %d due to no such item; probably deleted during sync", itemId, mse);
                } else {
                    throw mse;
                }
            }
        }

        GalSyncToken newToken = new GalSyncToken(syncToken, mbox.getAccountId(), changeId);
        ZimbraLog.gal.debug("computing new sync token for %s:%s", mbox.getAccountId(), newToken);
        callback.setNewToken(newToken);
        callback.setHasMoreResult(hasMore);
    }

    private boolean proxyGalAccountSearch(Account galSyncAcct, boolean sync) throws IOException, ServiceException, ContainerException, HttpException {
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
                req = Element.create(mParams.getProxyProtocol(), AccountConstants.SEARCH_GAL_REQUEST);
                req.addAttribute(AccountConstants.A_TYPE, mParams.getType().toString());
                req.addAttribute(AccountConstants.A_LIMIT, mParams.getLimit());
                req.addAttribute(AccountConstants.A_NAME, mParams.getQuery());
                req.addAttribute(AccountConstants.A_REF, mParams.getSearchEntryByDn());
                req.addAttribute(AccountConstants.A_GET_COUNT, mParams.isGetCount());
            }
            req.addAttribute(AccountConstants.A_GAL_ACCOUNT_ID, galSyncAcct.getId());
            req.addAttribute(AccountConstants.A_GAL_ACCOUNT_PROXIED, true);

            if (sync && mParams.getGalSyncToken() != null) {
               req.addAttribute(MailConstants.A_TOKEN, mParams.getGalSyncToken().toString());
               ZimbraLog.gal.debug("setting token for proxied request %s", mParams.getGalSyncToken().toString());
            }

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
            if (mParams.isGetCount()) {
                callback.setRemain(resp.getAttributeInt(MailConstants.A_REMAIN, 0));
            }
            if (hasMore && !sync) {
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

        // Return the GAL definition last modified time so that clients can use it to decide if fullsync is required.
        if ((mParams.getOp() == GalOp.sync) && (mParams.getResultCallback() != null)) {
            String galLastModified = domain.getGalDefinitionLastModifiedTimeAsString();
            if (galLastModified != null) {
                mParams.getResultCallback().setGalDefinitionLastModified(galLastModified);
            }
        }

        ZimbraLog.gal.info("Using limit %d for ldapSearch", limit);
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

        GalSyncToken galSyncToken = mParams.getGalSyncToken();
        if (galSyncToken != null) {
            mParams.setLdapTimeStamp(galSyncToken.getIntLdapTs());
            mParams.setLdapMatchCount(galSyncToken.getIntLdapMatchCount());
            mParams.setLdapHasMore(galSyncToken.intLdapHasMore());
            mParams.setMaxLdapTimeStamp(galSyncToken.getIntMaxLdapTs());
        }

        try {
            prov.searchGal(mParams);
        } catch (Exception e) {
            throw ServiceException.FAILURE("ldap search failed", e);
        }

        String resultToken = null;
        boolean intLdapHasMore = false;
        boolean extLdapHasMore = false;
        if (mParams.getResult() != null) {
            intLdapHasMore = mParams.getResult().getHadMore();
            if (mParams.getOp() == GalOp.sync) {
                resultToken = getLdapSearchResultToken(mParams.getResult(), "");
            }
        }

        if (galMode == GalMode.both) {
            // do the second query
            mParams.createSearchConfig(GalType.ldap);
            if (galSyncToken != null) { 
                mParams.setLdapTimeStamp(galSyncToken.getExtLdapTs());
                mParams.setLdapMatchCount(galSyncToken.getExtLdapMatchCount());
                mParams.setLdapHasMore(galSyncToken.extLdapHasMore());
                mParams.setMaxLdapTimeStamp(galSyncToken.getExtMaxLdapTs());
            }

            try {
                prov.searchGal(mParams);
            } catch (Exception e) {
                throw ServiceException.FAILURE("ldap search failed", e);
            }

            if (mParams.getResult() != null) {
                extLdapHasMore = mParams.getResult().getHadMore();
                if (mParams.getOp() == GalOp.sync) {
                    resultToken = getLdapSearchResultToken(mParams.getResult(), resultToken);
                }
            }
        }

        if (mParams.getResultCallback() != null) {
            if (mParams.getOp() == GalOp.sync) {
                mParams.getResultCallback().setNewToken(resultToken);
            }
            mParams.getResultCallback().setHasMoreResult(intLdapHasMore || extLdapHasMore);
        }
    }


    // bug 46608
    // do zimbra resources search if galMode == ldap
    private boolean needResources() throws ServiceException {
        Domain domain = mParams.getDomain();
        return (domain.getGalMode() == GalMode.ldap &&
                (GalSearchType.all == mParams.getType() || GalSearchType.resource == mParams.getType()) &&
                domain.isGalAlwaysIncludeLocalCalendarResources());
    }

    public static class GalAccountNotConfiguredException extends Exception {
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
                Group group = prov.getGroupBasic(Key.DistributionListBy.id, groupId);

                // the DL might have been deleted since the last GAL sync account sync, throw.
                // or should we just let the request through?
                if (group == null) {
                    ZimbraLog.gal.warn("unable to find group " + groupName
                            + "(" + groupId + ") for permission checking");
                    return false;
                }

                if (!AccessManager.getInstance().canDo(authedAcct, group, User.R_viewDistList, false)) {
                    return false;
                }

            } catch (ServiceException e) {
                ZimbraLog.gal.warn("unable to check permission for gal group expansion: " + groupName);
                return false;
            }
        }

        return true;
    }

    private String getLdapSearchResultToken(SearchGalResult result, String initialString) {
        StringBuilder buf;
        if(StringUtils.isEmpty(initialString)) {
           buf = new StringBuilder();
        } else {
           buf = new StringBuilder(initialString);
           buf.append("_");
        }
        buf.append(result.getLdapTimeStamp()).append("_").append(result.getLdapMatchCount()).append("_").append(BooleanUtils.toInteger(result.getHadMore()));
        buf.append("_").append(result.getToken());
        return buf.toString();
    }

    public String getGalQuery() throws ServiceException {
        return getSearchQuery(null, false);
    }
}
