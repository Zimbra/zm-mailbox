/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

import java.util.EnumSet;

import org.apache.commons.lang.StringUtils;
import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.SoapProtocol;
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
    private SoapProtocol mProxyProtocol;
    private QName mResponse;
    private DataSource mDataSource;
    private boolean mIdOnly;
    private boolean mNeedCanExpand;
    private boolean mNeedIsOwner;
    private MemberOfSelector mNeedIsMember;
    private boolean mNeedSMIMECerts;
    private boolean mFetchGroupMembers;
    private boolean mWildCardSearch = true;
    private String ldapTimeStamp = "";
    private String maxLdapTimeStamp = "";
    private int ldapMatchCount = 0;
    private boolean ldapHasMore = true;
    private boolean getCount = false;
    private boolean expandQuery = true;
    private String galSearchQuery;

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
        return !StringUtils.isEmpty(ldapTimeStamp) ? ldapTimeStamp : (mSyncToken != null ? mSyncToken.getLdapTimestamp() : null);
    }

    public String getMaxLdapTimeStamp() {
        return maxLdapTimeStamp;
    }

    public void setMaxLdapTimeStamp(String maxldapTimeStamp) {
       this.maxLdapTimeStamp = maxldapTimeStamp;
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

    public SoapProtocol getProxyProtocol() {
        return mProxyProtocol == null ? SoapProtocol.Soap12 : mProxyProtocol;
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
        // bug 69338
        // SearchParams.parse relies on A_SEARCH_TYPES on the request to determine search type, 
        // which will then determine if cursor should be used to narrow db query.  
        // If A_SEARCH_TYPES is not set, default type is conversation, cursor is not used to 
        // narrow db query for conversations.   We do not require clients to set types 
        // on GAL soap APIs.  Set it to "contact" here.
        request.addAttribute(MailConstants.A_SEARCH_TYPES, MailItem.Type.CONTACT.toString());
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
    
    public void setProxyProtocol(SoapProtocol proxyProtocol) {
        mProxyProtocol = proxyProtocol;
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
        String token = (mSyncToken != null) ? mSyncToken.getLdapTimestamp(mConfig.mTimestampFormat, ldapTimeStamp) : null;

        String extraQuery = null;
        if (GalSearchConfig.GalType.zimbra == mConfig.getGalType() && mExtraQueryCallback != null) {
            extraQuery = mExtraQueryCallback.getZimbraLdapSearchQuery();
        }
        String filterTemplate = mConfig.getFilter();
        if (!mWildCardSearch) {
           filterTemplate = filterTemplate.replaceAll("\\*", "");
        }
        return GalUtil.expandFilter(mConfig.getTokenizeKey(), filterTemplate, mQuery, token, extraQuery, ldapHasMore);
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

    public boolean isWildCardSearch() {
        return mWildCardSearch;
    }

    public void setWildCardSearch(boolean wildCardSearch) {
        mWildCardSearch = wildCardSearch;
    }

    public String getLdapTimeStamp() {
        return ldapTimeStamp;
    }

    public void setLdapTimeStamp(String ldapTimeStamp) {
        this.ldapTimeStamp = ldapTimeStamp;
    }

    public int getLdapMatchCount() {
        return ldapMatchCount;
    }

    public void setLdapMatchCount(int ldapMatchCount) {
        this.ldapMatchCount = ldapMatchCount;
    }

    public boolean ldapHasMore() {
        return ldapHasMore;
    }

    public void setLdapHasMore(boolean ldapHasMore) {
        this.ldapHasMore = ldapHasMore;
    }

    public void setGetCount(boolean getCount) {
        this.getCount = getCount;
    }

    public boolean isGetCount() {
        return getCount;
    }

    public String getUserInfo() {
        if (mAccount != null) {
            return mAccount.getName() + " (" + ((mUserAgent == null) ? "" : mUserAgent) + ")";
        } else {
            return " (" + ((mUserAgent == null) ? "" : mUserAgent) + ")";
        }
    }

    public boolean isExpandQuery() {
        return expandQuery;
    }

    public void setExpandQuery(boolean expandQuery) {
        this.expandQuery = expandQuery;
    }

	public String getGalSearchQuery() {
		return galSearchQuery;
	}

	public void setGalSearchQuery(String galSearchQuery) {
		this.galSearchQuery = galSearchQuery;
	}

}
