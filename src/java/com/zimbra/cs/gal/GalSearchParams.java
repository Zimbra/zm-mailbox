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

import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.gal.GalUtil;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.soap.ZimbraSoapContext;

public class GalSearchParams {
	private GalSearchConfig mConfig;
	private Provisioning.GalSearchType mType;
	private int mLimit;
	private int mPageSize;
	private String mQuery;
	private GalSyncToken mSyncToken;
	private SearchGalResult mResult;
	private ZimbraSoapContext mSoapContext;
	
	private Account mAccount;
	private Account mGalSyncAccount;
	private Domain mDomain;
    private SearchParams mSearchParams;
    private GalSearchResultCallback mResultCallback;
    private Element mRequest;
    private QName mResponse;
    private DataSource mDataSource;
    private boolean mIdOnly;
    private GalOp mOp;
	
	public GalSearchParams(Account account) {
        mAccount = account;
        mResult = SearchGalResult.newSearchGalResult(null);
        mResponse = AccountConstants.SEARCH_GAL_RESPONSE;
	}
	
	public GalSearchParams(Account account, ZimbraSoapContext ctxt) {
		this(account);
        mSoapContext = ctxt;
	}
	
    public GalSearchParams(Domain domain, ZimbraSoapContext ctxt) {
    	mDomain = domain;
    	mSoapContext = ctxt;
    }
    
    public GalSearchParams(DataSource ds) throws ServiceException {
    	this(ds.getAccount());
    	mDataSource = ds;
		mConfig = GalSearchConfig.create(mDataSource);
    }
    
	public GalSearchConfig getConfig() {
		return mConfig;
	}
	
	public Provisioning.GalSearchType getType() {
		return mType;
	}

	public int getLimit() {
		return mLimit;
	}

	public int getPageSize() {
		return mPageSize;
	}

	public String getQuery() {
		return mQuery;
	}
	
	public String getSyncToken() {
		if (mSyncToken == null)
			return null;
		return mSyncToken.getLdapTimestamp();
	}
	
	public GalSyncToken getGalSyncToken() {
		return mSyncToken;
	}
	
	public SearchGalResult getResult() {
		return mResult;
	}
	
	public Account getAccount() {
		return mAccount;
	}
	
	public Account getGalSyncAccount() {
		return mGalSyncAccount;
	}
	
	public Domain getDomain() throws ServiceException {
		if (mDomain != null)
			return mDomain;
		return Provisioning.getInstance().getDomain(mAccount);
	}
	public ZimbraSoapContext getSoapContext() {
		return mSoapContext;
	}
	
	public AuthToken getAuthToken() {
	    if (mSoapContext == null)
	        return null;
	    else
	        return mSoapContext.getAuthToken();
	}
	
	public SearchParams getSearchParams() {
		return mSearchParams;
	}

	public GalSearchResultCallback getResultCallback() {
		if (mResultCallback == null)
			return createResultCallback();
		return mResultCallback;
	}
	
	public Element getRequest() {
		return mRequest;
	}
	
	public QName getResponseName() {
		return mResponse;
	}
    
    public GalOp getOp() {
        return mOp;
    }
	
	public boolean isIdOnly() {
		return mIdOnly;
	}
	
	public void setSearchConfig(GalSearchConfig config) {
		mConfig = config;
	}
	
	public void setType(Provisioning.GalSearchType type) {
		mType = type;
	}
	
	public void setLimit(int limit) {
		mLimit = limit;
	}
	
	public void setPageSize(int pageSize) {
		mPageSize = pageSize;
	}
	
	public void setQuery(String query) {
		mQuery = query;
	}
	
	public void setToken(String token) {
		mSyncToken = new GalSyncToken(token);
	}
	
	public void setGalResult(SearchGalResult result) {
		mResult = result;
	}
	
	public void createSearchParams(String searchQuery) {
		mSearchParams = new SearchParams();
		mSearchParams.setLimit(mLimit + 1);
		mSearchParams.setSortBy(SortBy.NAME_ASCENDING);
		mSearchParams.setQueryStr(searchQuery);
	    mSearchParams.setTypes(new byte[] { MailItem.TYPE_CONTACT });
	}
	
	public void parseSearchParams(Element request, String searchQuery) throws ServiceException {
		if (request == null || mSoapContext == null) {
			createSearchParams(searchQuery);
			return;
		}
		setRequest(request);
		mSearchParams = SearchParams.parse(request, mSoapContext, searchQuery);
		mSearchParams.setTypes(new byte[] { MailItem.TYPE_CONTACT });
		setLimit(mSearchParams.getLimit());
	}
	
	public void setResultCallback(GalSearchResultCallback callback) {
		mResultCallback = callback;
	}
	
	public GalSearchResultCallback createResultCallback() {
		mResultCallback = new GalSearchResultCallback(this);
		return mResultCallback;
	}
	
	public void setRequest(Element req) {
		mRequest = req;
	}
	public void setResponseName(QName response) {
		mResponse = response;
	}
	
	public void createSearchConfig(GalSearchConfig.GalType type) throws ServiceException {
		mConfig = GalSearchConfig.create(getDomain(), mOp, type, mType);
	}
	
	public String generateLdapQuery() throws ServiceException {
		assert(mConfig != null);
		String token = (mSyncToken != null) ? mSyncToken.getLdapTimestamp(mConfig.mTimestampFormat) : null;
		return GalUtil.expandFilter(mConfig.getTokenizeKey(), mConfig.getFilter(), mQuery, token, false);
	}
	
	public void setGalSyncAccount(Account acct) {
		mGalSyncAccount = acct;
	}
	
	public void setIdOnly(boolean idOnly) {
		mIdOnly = idOnly;
	}
	
	public void setOp(GalOp op) {
	    mOp = op;
	}
}