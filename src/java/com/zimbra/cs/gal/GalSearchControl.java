/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.gal;

import java.io.IOException;
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
import com.zimbra.cs.account.ZAttrProvisioning.GalMode;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.ldap.LdapUtil;
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
import com.zimbra.cs.mailbox.util.TypedIdList;

public class GalSearchControl {
	private GalSearchParams mParams;
	
	public GalSearchControl(GalSearchParams params) {
		mParams = params;
	}
	
	public void autocomplete() throws ServiceException {
		String query = mParams.getQuery();
		if (!query.endsWith("*"))
			mParams.setQuery(query+"*");
		try {
			accountSearch();
		} catch (GalAccountNotConfiguredException e) {
			// fallback to ldap search
			mParams.getResultCallback().reset(mParams);
			ldapSearch(GalOp.autocomplete);
		}
	}
	
	public void search() throws ServiceException {
		String query = mParams.getQuery();
		if (query == null)
			query = "*";
		else {
			if (!query.endsWith("*"))
				query = query+"*";
			if (!query.startsWith("*"))
				query = "*"+query;
		}
		mParams.setQuery(query);
		try {
			accountSearch();
		} catch (GalAccountNotConfiguredException e) {
			// fallback to ldap search
			mParams.getResultCallback().reset(mParams);
			ldapSearch(GalOp.search);
		}
	}
	
	public void sync() throws ServiceException {
		try {
			accountSync();
		} catch (GalAccountNotConfiguredException e) {
			// fallback to ldap search
			mParams.getResultCallback().reset(mParams);
			ldapSearch(GalOp.sync);
		}
	}
	
	private String[] getGalSyncAccounts() throws GalAccountNotConfiguredException, ServiceException {
        Domain d = mParams.getDomain();
        String[] accts = d.getGalAccountId();
        if (accts.length == 0)
        	throw new GalAccountNotConfiguredException();
        return accts;
	}
	
	private void generateSearchQuery(Account galAcct) throws ServiceException, GalAccountNotConfiguredException {
		String query = mParams.getQuery();
		Provisioning.GAL_SEARCH_TYPE type = mParams.getType();
		StringBuilder searchQuery = new StringBuilder();
        if (query != null)
        	searchQuery.append("(#email:").append(query).append(" OR #fileasstr:").append(query).append(" OR #fullname:").append(query).append(") AND");
        GalMode galMode = Provisioning.getInstance().getDomain(galAcct).getGalMode();
        boolean first = true;
		for (DataSource ds : galAcct.getAllDataSources()) {
			if (ds.getType() != DataSource.Type.gal)
				continue;
			// check if there was any successful import from gal
			if (ds.getAttr(Provisioning.A_zimbraGalLastSuccessfulSyncTimestamp, null) == null)
	        	throw new GalAccountNotConfiguredException();
			if (ds.getAttr(Provisioning.A_zimbraGalStatus).compareTo("enabled") != 0)
	        	throw new GalAccountNotConfiguredException();
			String galType = ds.getAttr(Provisioning.A_zimbraGalType);
			if (galMode == GalMode.ldap && galType.compareTo("zimbra") == 0)
				continue;
			if (galMode == GalMode.zimbra && galType.compareTo("ldap") == 0)
				continue;
			if (first) searchQuery.append("("); else searchQuery.append(" OR");
			first = false;
			searchQuery.append(" inid:").append(ds.getFolderId());
		}
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
	
	private HashSet<Integer> getDataSourceFolderIds(Account galAcct) throws ServiceException, GalAccountNotConfiguredException {
        HashSet<Integer> folderIds = new HashSet<Integer>();
        GalMode galMode = Provisioning.getInstance().getDomain(galAcct).getGalMode();
		for (DataSource ds : galAcct.getAllDataSources()) {
			if (ds.getType() != DataSource.Type.gal)
				continue;
			// check if there was any successful import from gal
			if (ds.getAttr(Provisioning.A_zimbraGalLastSuccessfulSyncTimestamp, null) == null)
	        	throw new GalAccountNotConfiguredException();
			if (ds.getAttr(Provisioning.A_zimbraGalStatus).compareTo("enabled") != 0)
	        	throw new GalAccountNotConfiguredException();
			String galType = ds.getAttr(Provisioning.A_zimbraGalType);
			if (galMode == GalMode.ldap && galType.compareTo("zimbra") == 0)
				continue;
			if (galMode == GalMode.zimbra && galType.compareTo("ldap") == 0)
				continue;
			if (folderIds != null)
				folderIds.add(ds.getFolderId());
		}
		return folderIds;
	}
	
	private void accountSearch() throws ServiceException, GalAccountNotConfiguredException {
        Provisioning prov = Provisioning.getInstance();
    	for (String galAccountId : getGalSyncAccounts()) {
    		Account galAcct = prov.getAccountById(galAccountId);
    		if (galAcct == null) {
    			ZimbraLog.gal.warn("GalSync account not found: "+galAccountId);
    			continue;
    		}
    		if (!galAcct.getAccountStatus().isActive()) {
    			ZimbraLog.gal.info("GalSync account "+galAccountId+" is in "+galAcct.getAccountStatus().name());
            	throw new GalAccountNotConfiguredException();
    		}
			if (Provisioning.onLocalServer(galAcct)) {
		        generateSearchQuery(galAcct);
		        if (!doLocalGalAccountSearch(galAcct))
		        	throw new GalAccountNotConfiguredException();
			} else {
				if (!proxyGalAccountSearch(galAcct))
		        	throw new GalAccountNotConfiguredException();
			}
    	}
	}
	
	private void accountSync() throws ServiceException, GalAccountNotConfiguredException {
        Provisioning prov = Provisioning.getInstance();
    	for (String galAccountId : getGalSyncAccounts()) {
    		Account galAcct = prov.getAccountById(galAccountId);
    		if (galAcct == null) {
    			ZimbraLog.gal.warn("GalSync account not found: "+galAccountId);
    			continue;
    		}
    		if (!galAcct.getAccountStatus().isActive()) {
    			ZimbraLog.gal.info("GalSync account "+galAccountId+" is in "+galAcct.getAccountStatus().name());
            	throw new GalAccountNotConfiguredException();
    		}
			if (Provisioning.onLocalServer(galAcct)) {
		        HashSet<Integer> folderIds = getDataSourceFolderIds(galAcct);
		        int syncToken = 0;
		        try {
		        	String token = mParams.getSyncToken();
		        	if (token != null)
		        		syncToken = Integer.parseInt(token);
		        } catch (NumberFormatException e) {
		        	// do a full sync
		        }
		        if (!doLocalGalAccountSync(galAcct, syncToken, folderIds))
		        	throw new GalAccountNotConfiguredException();
			} else {
				if (!proxyGalAccountSearch(galAcct))
		        	throw new GalAccountNotConfiguredException();
			}
    	}
	}
	
    private boolean doLocalGalAccountSearch(Account galAcct) {
		ZimbraQueryResults zqr = null;
		try {
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(galAcct);
			SearchParams searchParams = mParams.getSearchParams();
			zqr = mbox.search(SoapProtocol.Soap12, new Mailbox.OperationContext(mbox), searchParams);
            ResultsPager pager = ResultsPager.create(zqr, searchParams);
            GalSearchResultCallback callback = mParams.getResultCallback();
            int limit  = mParams.getLimit();
            int num = 0;
			while (pager.hasNext()) {
				if (num == limit)
					break;
                ZimbraHit hit = pager.getNextHit();
                if (hit instanceof ContactHit)
                	callback.handleContact(((ContactHit)hit).getContact());
                num++;
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
    
    private boolean doLocalGalAccountSync(Account galAcct, int syncToken, HashSet<Integer> folderIds) {
		try {
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(galAcct);
			Mailbox.OperationContext octxt = new Mailbox.OperationContext(mbox);
			Pair<List<Integer>,TypedIdList> changed = mbox.getModifiedItems(octxt, syncToken, MailItem.TYPE_CONTACT, folderIds);
            GalSearchResultCallback callback = mParams.getResultCallback();
			// XXX batch items
			for (int itemId : changed.getFirst()) {
				MailItem item = mbox.getItemById(octxt, itemId, MailItem.TYPE_CONTACT);
				if (item instanceof Contact)
					callback.handleContact((Contact)item);
			}
			// XXX deleted items
            callback.setNewToken(mbox.getLastChangeID());
		} catch (Exception e) {
			ZimbraLog.gal.warn("search on GalSync account failed for"+galAcct.getId(), e);
			return false;
		}
		return true;
    }
    
    private boolean proxyGalAccountSearch(Account targetAcct) {
		try {
			Provisioning prov = Provisioning.getInstance();
    		String serverUrl = URLUtil.getAdminURL(prov.getServerByName(targetAcct.getMailHost()));
			SoapHttpTransport transport = new SoapHttpTransport(serverUrl);
			transport.setAuthToken(AuthToken.getZimbraAdminAuthToken().toZAuthToken());
			transport.setTargetAcctId(targetAcct.getId());
			if (mParams.getSoapContext() != null)
				transport.setResponseProtocol(mParams.getSoapContext().getResponseProtocol());
			Element req = mParams.getRequest();
			if (req == null) {
				req = Element.create(SoapProtocol.Soap12, AccountConstants.SEARCH_GAL_REQUEST);
				req.addAttribute(AccountConstants.A_TYPE, "account");
				req.addAttribute(AccountConstants.A_NAME, mParams.getQuery());
			}
			Element resp = transport.invokeWithoutSession(req.detach());
			GalSearchResultCallback callback = mParams.getResultCallback();
			Iterator<Element> iter = resp.elementIterator(MailConstants.E_CONTACT);
			while (iter.hasNext())
				callback.handleElement(iter.next());
		} catch (IOException e) {
			ZimbraLog.gal.warn("remote search on GalSync account failed for"+targetAcct.getName(), e);
			return false;
		} catch (ServiceException e) {
			ZimbraLog.gal.warn("remote search on GalSync account failed for"+targetAcct.getName(), e);
			return false;
		}
		return true;
    }
    
    private void ldapSearch(GalOp op) throws ServiceException {
        GalMode galMode = mParams.getDomain().getGalMode();
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
    	mParams.createSearchConfig(op, type);
    	try {
        	LdapUtil.galSearch(mParams);
    	} catch (Exception e) {
    		throw ServiceException.FAILURE("ldap search failed", e);
    	}

    	boolean hadMore = mParams.getResult().getHadMore();
    	if (galMode != GalMode.both)
    		return;

    	// do the second query
    	mParams.createSearchConfig(op, GalType.ldap);
    	try {
        	LdapUtil.galSearch(mParams);
    	} catch (Exception e) {
    		throw ServiceException.FAILURE("ldap search failed", e);
    	}
    	hadMore |= mParams.getResult().getHadMore();
    	mParams.getResultCallback().setHasMoreResult(hadMore);
    }
    
	private static class GalAccountNotConfiguredException extends Exception {
		private static final long serialVersionUID = 679221874958248740L;

		public GalAccountNotConfiguredException() {
		}
	}
}
