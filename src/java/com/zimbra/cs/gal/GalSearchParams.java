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

import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
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
	private Provisioning.GAL_SEARCH_TYPE mType;
	private int mLimit;
	private int mPageSize;
	private String mQuery;
	private String mToken;
	private SearchGalResult mResult;
	private ZimbraSoapContext mSoapContext;
	
	private Account mAccount;
    private SearchParams mSearchParams;
    private GalSearchResultCallback mResultCallback;
    private Element mRequest;
    private QName mResponse;
    private DataSource mDataSource;
	
	public GalSearchParams(Account account) {
        mAccount = account;
        mResult = SearchGalResult.newSearchGalResult(null);
	}
	
	public GalSearchParams(Account account, ZimbraSoapContext ctxt) {
		this(account);
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
	
	public Provisioning.GAL_SEARCH_TYPE getType() {
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
		return mToken;
	}
	
	public SearchGalResult getResult() {
		return mResult;
	}
	
	public Account getAccount() {
		return mAccount;
	}
	
	public ZimbraSoapContext getSoapContext() {
		return mSoapContext;
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
	
	public void setSearchConfig(GalSearchConfig config) {
		mConfig = config;
	}
	
	public void setType(Provisioning.GAL_SEARCH_TYPE type) {
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
		mToken = token;
	}
	
	public void setGalResult(SearchGalResult result) {
		mResult = result;
	}
	
	public void createSearchParams(String searchQuery) {
		mSearchParams = new SearchParams();
		mSearchParams.setLimit(mLimit);
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
	
	public void createSearchConfig(GalOp op, GalSearchConfig.GalType type) throws ServiceException {
		mConfig = GalSearchConfig.create(mAccount, op, type);
	}
	
	public String generateLdapQuery() throws ServiceException {
		assert(mConfig != null);
        return GalUtil.expandFilter(mConfig.getTokenizeKey(), mConfig.getFilter(), mQuery, mToken, false);
	}
}