/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapHttpTransport;
import com.zimbra.common.soap.SoapProtocol;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.GAL_SEARCH_TYPE;
import com.zimbra.cs.account.ZAttrProvisioning.GalMode;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.ldap.LdapUtil;
import com.zimbra.cs.db.DbDataSource;
import com.zimbra.cs.db.DbDataSource.DataSourceItem;
import com.zimbra.cs.gal.GalSearchConfig.GalType;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.index.ContactHit;
import com.zimbra.cs.index.ResultsPager;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.util.TypedIdList;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;

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
	    
		String query = mParams.getQuery();
		if (query == null)
			query = "";
		if (query.endsWith("*"))
			query = query.substring(0, query.length()-1);
		mParams.setQuery(query);
		mParams.setOp(GalOp.autocomplete);
		if (mParams.getAccount().isGalSyncAccountBasedAutoCompleteEnabled()) {
			try {
				Account galAcct = mParams.getGalSyncAccount();
				if (galAcct != null) {
					accountSearch(galAcct);
				} else {
					for (Account galAccount : getGalSyncAccounts()) {
						accountSearch(galAccount);
					}
				}
				return;
			} catch (GalAccountNotConfiguredException e) {
			}
		}
		// fallback to ldap search
		mParams.setQuery(query+"*");
		mParams.getResultCallback().reset(mParams);
		mParams.setLimit(100);
		ldapSearch();
	}
	
	public void search() throws ServiceException {
	    
	    checkFeatureEnabled(null);
	    
		String query = mParams.getQuery();
		if (query == null)
			query = "";
		mParams.setQuery(query);
		mParams.setOp(GalOp.search);
		try {
			Account galAcct = mParams.getGalSyncAccount();
			if (galAcct != null) {
				accountSearch(galAcct);
			} else {
				for (Account galAccount : getGalSyncAccounts()) {
		    		accountSearch(galAccount);
		    	}
			}
		} catch (GalAccountNotConfiguredException e) {
			// fallback to ldap search
			if (!query.endsWith("*"))
				query = query+"*";
			if (!query.startsWith("*"))
				query = "*"+query;
			mParams.setQuery(query);
			mParams.getResultCallback().reset(mParams);
			mParams.setLimit(100);
			ldapSearch();
		}
	}
	
	public void sync() throws ServiceException {
	    
	    checkFeatureEnabled(Provisioning.A_zimbraFeatureGalSyncEnabled);
	    
		mParams.setQuery("");
		mParams.setOp(GalOp.sync);
		Account galAcct = mParams.getGalSyncAccount();
		try {
			if (galAcct != null) {
				accountSync(galAcct);
			} else {
				for (Account galAccount : getGalSyncAccounts()) {
					accountSync(galAccount);
				}
			}
			// account based sync was finished
			// if the presented sync token is old LDAP timestamp format, we need to sync
			// against LDAP server to keep the client up to date.
			GalSyncToken gst = mParams.getGalSyncToken();
			if (mParams.isIdOnly() && gst.doMailboxSync())
				return;
		} catch (GalAccountNotConfiguredException e) {
			// fallback to ldap search
			mParams.getResultCallback().reset(mParams);
		}
		ldapSearch();
	}
	
	private Account[] getGalSyncAccounts() throws GalAccountNotConfiguredException, ServiceException {
        Domain d = mParams.getDomain();
        String[] accts = d.getGalAccountId();
        if (accts.length == 0)
        	throw new GalAccountNotConfiguredException();
        Provisioning prov = Provisioning.getInstance();
        ArrayList<Account> accounts = new ArrayList<Account>();
        for (String acctId : accts) {
        	Account a = prov.getAccountById(acctId);
        	if (a == null)
            	throw new GalAccountNotConfiguredException();
    		for (DataSource ds : a.getAllDataSources()) {
    			if (ds.getType() != DataSource.Type.gal)
    				continue;
    			// check if there was any successful import from gal
    			if (ds.getAttr(Provisioning.A_zimbraGalLastSuccessfulSyncTimestamp, null) == null)
    	        	throw new GalAccountNotConfiguredException();
    			if (ds.getAttr(Provisioning.A_zimbraGalStatus).compareTo("enabled") != 0)
    	        	throw new GalAccountNotConfiguredException();
    			if (ds.getAttr(Provisioning.A_zimbraDataSourceEnabled).compareTo("TRUE") != 0)
    	        	throw new GalAccountNotConfiguredException();
    		}
    		accounts.add(a);
        }
        return accounts.toArray(new Account[0]);
	}
	
	private void generateSearchQuery(Account galAcct) throws ServiceException, GalAccountNotConfiguredException {
		String query = mParams.getQuery();
		Provisioning.GAL_SEARCH_TYPE type = mParams.getType();
		StringBuilder searchQuery = new StringBuilder();
        if (query.length() > 0)
        	searchQuery.append("contact:(").append(query).append(") AND");
        GalMode galMode = mParams.getDomain().getGalMode();
        boolean first = true;
		for (DataSource ds : galAcct.getAllDataSources()) {
			if (ds.getType() != DataSource.Type.gal)
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
		case CALENDAR_RESOURCE:
			searchQuery.append(" AND #zimbraAccountCalendarUserType:RESOURCE");
			break;
		case USER_ACCOUNT:
			searchQuery.append(" AND !(#zimbraAccountCalendarUserType:RESOURCE)");
			break;
		case ALL:
			break;
		}
		ZimbraLog.gal.debug("query: "+searchQuery.toString());
        mParams.parseSearchParams(mParams.getRequest(), searchQuery.toString());
	}
	
    private boolean generateLocalResourceSearchQuery(Account galAcct) throws ServiceException, GalAccountNotConfiguredException {
        String query = mParams.getQuery();
        StringBuilder searchQuery = new StringBuilder();
        if (query.length() > 0)
            searchQuery.append("contact:(").append(query).append(") AND");
        searchQuery.append(" #zimbraAccountCalendarUserType:RESOURCE");
        for (DataSource ds : galAcct.getAllDataSources()) {
            if (ds.getType() != DataSource.Type.gal)
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
            // bug 46608
            // include local resource in the search result if galMode is set to ldap.
		    Domain domain = mParams.getDomain();
		    if (domain.getGalMode() == GalMode.ldap &&
		            domain.isGalAlwaysIncludeLocalCalendarResources()) {
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
			zqr = mbox.search(SoapProtocol.Soap12, new OperationContext(mbox), searchParams);
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
			ZimbraLog.gal.warn("search on GalSync account failed for "+galAcct.getId(), e);
			return false;
		} finally {
			if (zqr != null) 
				try { zqr.doneWithSearchResults(); } catch (ServiceException e) {}
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
                    if (ds.getType() != DataSource.Type.gal)
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
                        Pair<List<Integer>,TypedIdList> changed = mbox.getModifiedItems(octxt, changeId, MailItem.TYPE_CONTACT, folderIds);

                        int count = 0;
                        for (int itemId : changed.getFirst()) {
                            MailItem item = mbox.getItemById(octxt, itemId, MailItem.TYPE_CONTACT);
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
                if (ds.getType() != DataSource.Type.gal)
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
                List<Integer> deleted = mbox.getTombstones(changeId).getAll();
                Pair<List<Integer>,TypedIdList> changed = mbox.getModifiedItems(octxt, changeId, MailItem.TYPE_CONTACT, folderIds);

                int count = 0;
                for (int itemId : changed.getFirst()) {
                    MailItem item = mbox.getItemById(octxt, itemId, MailItem.TYPE_CONTACT);
                    if (item instanceof Contact)
                        callback.handleContact((Contact)item);
                    count++;
                    if (count % 100 == 0)
                        ZimbraLog.gal.debug("processing #"+count);
                }

                if (changeId > 0)
                    for (int itemId : deleted)
                        callback.handleDeleted(new ItemId(galAcct.getId(), itemId));
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
    
    private boolean proxyGalAccountSearch(Account targetAcct) {
		try {
			Provisioning prov = Provisioning.getInstance();
    		String serverUrl = URLUtil.getAdminURL(prov.getServerByName(targetAcct.getMailHost()));
			SoapHttpTransport transport = new SoapHttpTransport(serverUrl);
			transport.setAuthToken(AuthProvider.getAdminAuthToken().toZAuthToken());
			transport.setTargetAcctId(targetAcct.getId());
			if (mParams.getSoapContext() != null)
				transport.setResponseProtocol(mParams.getSoapContext().getResponseProtocol());
			Element req = mParams.getRequest();
			if (req == null) {
				req = Element.create(SoapProtocol.Soap12, AccountConstants.SEARCH_GAL_REQUEST);
				req.addAttribute(AccountConstants.A_TYPE, "account");
				req.addAttribute(AccountConstants.A_NAME, mParams.getQuery());
			}
			req.addAttribute(AccountConstants.A_ID, targetAcct.getId());
			Element resp = transport.invokeWithoutSession(req.detach());
			GalSearchResultCallback callback = mParams.getResultCallback();
			Iterator<Element> iter = resp.elementIterator(MailConstants.E_CONTACT);
			while (iter.hasNext())
				callback.handleElement(iter.next());
			iter = resp.elementIterator(MailConstants.E_DELETED);
			while (iter.hasNext())
				callback.handleElement(iter.next());
			String newTokenStr = resp.getAttribute(MailConstants.A_TOKEN, null);
			if (newTokenStr != null) {
				GalSyncToken newToken = new GalSyncToken(newTokenStr);
				ZimbraLog.gal.debug("computing new sync token for proxied account "+targetAcct.getId()+": "+newToken);
	            callback.setNewToken(newToken);
			}
			boolean hasMore =  resp.getAttributeBool(MailConstants.A_QUERY_MORE, false);
			if (hasMore) {
			    callback.setHasMoreResult(true);
			    callback.setSortBy(resp.getAttribute(MailConstants.A_SORTBY));
			    callback.setQueryOffset((int)resp.getAttributeLong(MailConstants.A_QUERY_OFFSET));
			}
		} catch (Exception e) {
			ZimbraLog.gal.warn("remote search on GalSync account failed for"+targetAcct.getName(), e);
			return false;
		}
		return true;
    }
    
    private void ldapSearch() throws ServiceException {
        Domain domain = mParams.getDomain();
        GalMode galMode = domain.getGalMode();
        Provisioning.GAL_SEARCH_TYPE stype = mParams.getType();

        // bug 46608
        // first do local resources search if galMode == ldap
        // and operation is search or sync.
        if (mParams.getOp() != GalOp.autocomplete &&
                stype != GAL_SEARCH_TYPE.USER_ACCOUNT &&
                galMode == GalMode.ldap &&
                domain.isGalAlwaysIncludeLocalCalendarResources()) {
            mParams.setType(GAL_SEARCH_TYPE.CALENDAR_RESOURCE);
            mParams.createSearchConfig(GalType.zimbra);
            try {
                LdapUtil.galSearch(mParams);
            } catch (Exception e) {
                throw ServiceException.FAILURE("ldap search failed", e);
            }
            mParams.setType(stype);
        }
        int limit = mParams.getLimit();
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
        	LdapUtil.galSearch(mParams);
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
            	LdapUtil.galSearch(mParams);
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
    
	private static class GalAccountNotConfiguredException extends Exception {
		private static final long serialVersionUID = 679221874958248740L;

		public GalAccountNotConfiguredException() {
		}
	}
}
