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

import java.util.EnumSet;

import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.gal.GalOp;
import com.zimbra.cs.account.gal.GalUtil;
import com.zimbra.cs.index.SearchParams;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.account.type.MemberOfSelector;
import com.zimbra.soap.type.GalSearchType;

public class GalSearchParams {
    private GalSearchConfig mConfig;
    private GalSearchType mType = GalSearchType.account;
    private int mLimit;
    private Integer mLdapLimit; // ldap search does not support paging, allow a different limit for ldap search
    private int mPageSize;
    private String mQuery;
    private String mSearchEntryByDn;  // if not null, search the entry by a DN instead of using query
    private GalSyncToken mSyncToken;
    private SearchGalResult mResult;
    private ZimbraSoapContext mSoapContext;

    private Account mAccount;
    private String mUserAgent;
    private Account mGalSyncAccount;
    private Domain mDomain;
    private SearchParams mSearchParams;
    private GalSearchResultCallback mResultCallback;
    private GalSearchQueryCallback mExtraQueryCallback;
    private Element mRequest;
    private QName mResponse;
    private DataSource mDataSource;
    private boolean mIdOnly;
    private boolean mNeedCanExpand;
    private boolean mNeedIsOwner;
    private MemberOfSelector mNeedIsMember;
    private boolean mNeedSMIMECerts;
    private boolean mFetchGroupMembers;
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

    public GalSearchType getType() {
        return mType;
    }

    public int getLimit() {
        return mLimit;
    }

    public Integer getLdapLimit() {
        return mLdapLimit;
    }

    public int getPageSize() {
        return mPageSize;
    }

    public String getQuery() {
        return mQuery;
    }

    public String getSearchEntryByDn() {
        return mSearchEntryByDn;
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

        Domain domain = Provisioning.getInstance().getDomain(mAccount);
        if (domain != null)
            return domain;

        Account galSyncAcct = getGalSyncAccount();
        if (galSyncAcct != null)
            domain = Provisioning.getInstance().getDomain(galSyncAcct);

        if (domain != null)
            return domain;

        throw ServiceException.FAILURE("Unable to get domain", null);
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

    public Account getAuthAccount() throws ServiceException {
        if (mSoapContext == null)
            return getAccount();
        else
            return Provisioning.getInstance().get(AccountBy.id, mSoapContext.getAuthtokenAccountId());
    }

    public SearchParams getSearchParams() {
        return mSearchParams;
    }

    public GalSearchResultCallback getResultCallback() {
        if (mResultCallback == null)
            return createResultCallback();
        return mResultCallback;
    }

    public GalSearchQueryCallback getExtraQueryCallback() {
        return mExtraQueryCallback;
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

    public boolean getNeedCanExpand() {
        return mNeedCanExpand;
    }
    
    public boolean getNeedIsOwner() {
        return mNeedIsOwner;
    }
    
    public MemberOfSelector getNeedIsMember() {
        return mNeedIsMember;
    }
    
    public boolean getNeedSMIMECerts() {
        return mNeedSMIMECerts;
    }

    public void setSearchConfig(GalSearchConfig config) {
        mConfig = config;
    }

    public void setType(GalSearchType type) {
        mType = type;
    }

    public void setLimit(int limit) {
        mLimit = limit;
    }

    public void setLdapLimit(int limit) {
        mLdapLimit = limit;
    }

    public void setPageSize(int pageSize) {
        mPageSize = pageSize;
    }

    public void setQuery(String query) {
        mQuery = query;
    }

    public void setSearchEntryByDn(String dn) {
        mSearchEntryByDn = dn;
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
        mSearchParams.setSortBy(SortBy.NAME_ASC);
        mSearchParams.setQueryString(searchQuery);
        mSearchParams.setTypes(EnumSet.of(MailItem.Type.CONTACT));
    }

    public void parseSearchParams(Element request, String searchQuery) throws ServiceException {
        if (request == null || mSoapContext == null) {
            createSearchParams(searchQuery);
            return;
        }
        setRequest(request);
        mSearchParams = SearchParams.parse(request, mSoapContext, searchQuery);
        mSearchParams.setTypes(EnumSet.of(MailItem.Type.CONTACT));
        setLimit(mSearchParams.getLimit());
    }

    public void setResultCallback(GalSearchResultCallback callback) {
        mResultCallback = callback;
    }

    public GalSearchResultCallback createResultCallback() {
        mResultCallback = new GalSearchResultCallback(this);
        return mResultCallback;
    }

    public void setExtraQueryCallback(GalSearchQueryCallback callback) {
        mExtraQueryCallback = callback;
    }

    public void setRequest(Element req) {
        mRequest = req;
    }
    public void setResponseName(QName response) {
        mResponse = response;
    }

    public void createSearchConfig(GalSearchConfig.GalType type) throws ServiceException {
        mConfig = GalSearchConfig.create(getDomain(), mOp, type, mType);
        mConfig.getRules().setFetchGroupMembers(mFetchGroupMembers);
        mConfig.getRules().setNeedSMIMECerts(mNeedSMIMECerts);
    }

    public String generateLdapQuery() throws ServiceException {
        assert(mConfig != null);
        String token = (mSyncToken != null) ? mSyncToken.getLdapTimestamp(mConfig.mTimestampFormat) : null;

        String extraQuery = null;
        if (GalSearchConfig.GalType.zimbra == mConfig.getGalType() && mExtraQueryCallback != null) {
            extraQuery = mExtraQueryCallback.getZimbraLdapSearchQuery();
        }
        return GalUtil.expandFilter(mConfig.getTokenizeKey(), mConfig.getFilter(), mQuery, token, extraQuery);
    }

    public void setGalSyncAccount(Account acct) {
        mGalSyncAccount = acct;
    }

    public void setIdOnly(boolean idOnly) {
        mIdOnly = idOnly;
    }

    public void setNeedCanExpand(boolean needCanExpand) {
        mNeedCanExpand = needCanExpand;
    }

    public void setNeedIsOwner(boolean needOwnerOf) {
        mNeedIsOwner = needOwnerOf;
    }

    public void setNeedIsMember(MemberOfSelector needMemberOf) {
        mNeedIsMember = needMemberOf;
    }

    public void setNeedSMIMECerts(boolean needSMIMECerts) {
        mNeedSMIMECerts = needSMIMECerts;
    }

    
    public void setFetchGroupMembers(boolean fetchGroupMembers) {
        mFetchGroupMembers = fetchGroupMembers;
    }

    public void setOp(GalOp op) {
        mOp = op;
    }

    public void setUserAgent(String ua) {
        mUserAgent = ua;
    }

    public String getUserInfo() {
        if (mAccount != null) {
            return mAccount.getName() + " (" + ((mUserAgent == null) ? "" : mUserAgent) + ")";
        } else {
            return " (" + ((mUserAgent == null) ? "" : mUserAgent) + ")";
        }
    }
}
