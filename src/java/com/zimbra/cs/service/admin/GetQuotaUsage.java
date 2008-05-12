/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.session.AdminSession;
import com.zimbra.cs.session.Session;

import com.zimbra.soap.ZimbraSoapContext;

public class GetQuotaUsage extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
    public static final String SORT_PERCENT_USED = "percentUsed";
    public static final String SORT_TOTAL_USED = "totalUsed";
    public static final String SORT_QUOTA_LIMIT = "quotaLimit";
    public static final String SORT_ACCOUNT = "account";
        
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        int limit = (int) request.getAttributeLong(AdminConstants.A_LIMIT, Integer.MAX_VALUE);
        if (limit == 0)
            limit = Integer.MAX_VALUE;
        int offset = (int) request.getAttributeLong(AdminConstants.A_OFFSET, 0);
        String domain = request.getAttribute(AdminConstants.A_DOMAIN, null);
        String sortBy = request.getAttribute(AdminConstants.A_SORT_BY, SORT_TOTAL_USED);
        final boolean sortAscending = request.getAttributeBool(AdminConstants.A_SORT_ASCENDING, false);

        if (!(sortBy.equals(SORT_TOTAL_USED) || sortBy.equals(SORT_PERCENT_USED) || sortBy.equals(SORT_QUOTA_LIMIT) || sortBy.equals(SORT_ACCOUNT)))
            throw ServiceException.INVALID_REQUEST("sortBy must be percentUsed or totalUsed", null);

        // if we are a domain admin only, restrict to domain
        if (isDomainAdminOnly(zsc)) {
            if (domain == null) {
                domain = getAuthTokenAccountDomain(zsc).getName();
            } else {
                if (!canAccessDomain(zsc, domain)) 
                    throw ServiceException.PERM_DENIED("can not access domain"); 
            }
        }

        Domain d = null;
        if (domain != null) {
            d = prov.get(DomainBy.name, domain);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domain);
        }

        
        QuotaUsageParams params = new QuotaUsageParams(d, sortBy,sortAscending);

        ArrayList<AccountQuota> quotas = params.doSearch();
       
        AdminSession session = (AdminSession) getSession(zsc, Session.Type.ADMIN);
        if (session != null) {
            QuotaUsageParams cachedParams = (QuotaUsageParams) session.getData("GetQuotaUsage");
            if (cachedParams == null || !cachedParams.equals(params)) {
                quotas = params.doSearch();
                session.setData("GetQuotaUsage", params);
            } else {
                quotas = cachedParams.doSearch();
            }
        } else {
            quotas = params.doSearch();
        }

        Element response = zsc.createElement(AdminConstants.GET_QUOTA_USAGE_RESPONSE);
        int i, limitMax = offset+limit;
        for (i=offset; i < limitMax && i < quotas.size(); i++) {
            AccountQuota quota = quotas.get(i);
            Element account = response.addElement(AdminConstants.E_ACCOUNT);
            account.addAttribute(AdminConstants.A_NAME, quota.name);
            account.addAttribute(AdminConstants.A_ID, quota.id);
            account.addAttribute(AdminConstants.A_QUOTA_USED, quota.quotaUsed);
            account.addAttribute(AdminConstants.A_QUOTA_LIMIT, quota.quotaLimit);
        }
        response.addAttribute(AdminConstants.A_MORE, i < quotas.size());
        response.addAttribute(AdminConstants.A_SEARCH_TOTAL, quotas.size());
        return response;
    }
    
    public static class AccountQuota {
        public String name;
        public String id;
        public long quotaLimit;
        public long sortQuotaLimit;        
        public long quotaUsed;
        public float percentQuotaUsed; 
    }

    public class QuotaUsageParams {    
        String mDomainId;
        String mSortBy;
        boolean mSortAscending;

        ArrayList<AccountQuota> mResult;
        
        QuotaUsageParams(Domain d, String sortBy, boolean sortAscending) {
            mDomainId = (d == null) ? "" : d.getId();
            mSortBy = (sortBy == null) ? "" : sortBy;
            mSortAscending = sortAscending;
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof QuotaUsageParams)) return false;
            if (o == this) return true;
            
            QuotaUsageParams other = (QuotaUsageParams) o; 
            return 
                mDomainId.equals(other.mDomainId) &&
                mSortBy.equals(other.mSortBy) &&
                mSortAscending == other.mSortAscending;
        }
        
        ArrayList<AccountQuota> doSearch() throws ServiceException {
            if (mResult != null) return mResult;

            ArrayList<AccountQuota> result = new ArrayList<AccountQuota>();
            
            String query = String.format("(zimbraMailHost=%s)", LOCAL_HOST);
            
            Provisioning prov = Provisioning.getInstance();
            int flags = Provisioning.SA_ACCOUNT_FLAG;
            List accounts;
            Domain d = mDomainId.equals("") ? null : prov.get(DomainBy.id, mDomainId);
            if (d != null) {
                accounts = prov.searchAccounts(d, query, null, null, true, flags);
            } else {
                accounts = prov.searchAccounts(query, null, null, true, flags);
            }

            Map<String, Long> quotaUsed = MailboxManager.getInstance().getMailboxSizes();
            
            for (Object obj: accounts) {
                if (!(obj instanceof Account))continue;
                AccountQuota aq = new AccountQuota();            
                Account acct = (Account) obj;
                aq.id = acct.getId();
                aq.name = acct.getName();
                aq.quotaLimit = acct.getLongAttr(Provisioning.A_zimbraMailQuota, 0);
                aq.sortQuotaLimit = aq.quotaLimit == 0 ? Long.MAX_VALUE : aq.quotaLimit;
                Long used = quotaUsed.get(acct.getId());
                aq.quotaUsed = used == null ? 0 : used;
                aq.percentQuotaUsed = aq.quotaLimit > 0 ? (aq.quotaUsed / (float)aq.quotaLimit) : 0;
                result.add(aq);
            }

            final boolean sortByTotal = mSortBy.equals(SORT_TOTAL_USED);
            final boolean sortByQuota = mSortBy.equals(SORT_QUOTA_LIMIT);
            final boolean sortByAccount = mSortBy.equals(SORT_ACCOUNT);

            Comparator<AccountQuota> comparator = new Comparator<AccountQuota>() {
                public int compare(AccountQuota a, AccountQuota b) {
                    int comp = 0;
                    if (sortByTotal) {
                        if (a.quotaUsed > b.quotaUsed) comp = 1;
                        else if (a.quotaUsed < b.quotaUsed) comp = -1;
                    } else if (sortByQuota) {
                        if (a.sortQuotaLimit > b.sortQuotaLimit) comp = 1;
                        else if (a.sortQuotaLimit < b.sortQuotaLimit) comp = -1;                            
                    } else if(sortByAccount) {
                    	comp=a.name.compareToIgnoreCase(b.name);
                    } else {
                        if (a.percentQuotaUsed > b.percentQuotaUsed) comp = 1;
                        else if (a.percentQuotaUsed < b.percentQuotaUsed) comp = -1;
                    }
                    return mSortAscending ? comp : -comp;
                }
            };
            Collections.sort(result, comparator);
            mResult = result;
            return mResult;
        }
    }
}
