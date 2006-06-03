/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CosBy;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.AdminSession;
import com.zimbra.cs.session.SessionCache;

import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetQuotaUsage extends AdminDocumentHandler {

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";
    
    public static final String SORT_PERCENT_USED = "percentUsed";
    public static final String SORT_TOTAL_USED = "totalUsed";
    public static final String SORT_QUOTA_LIMIT = "quotaLimit";
        
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {

        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        int limit = (int) request.getAttributeLong(AdminService.A_LIMIT, Integer.MAX_VALUE);
        if (limit == 0)
            limit = Integer.MAX_VALUE;
        int offset = (int) request.getAttributeLong(AdminService.A_OFFSET, 0);        
        String domain = request.getAttribute(AdminService.A_DOMAIN, null);
        String cos = request.getAttribute(AdminService.A_COS, null);
//        String attrsStr = request.getAttribute(AdminService.A_ATTRS, null);
        String sortBy = request.getAttribute(AdminService.A_SORT_BY, SORT_TOTAL_USED);        
        final boolean sortAscending = request.getAttributeBool(AdminService.A_SORT_ASCENDING, false);        

        if (!(sortBy.equals(SORT_TOTAL_USED) || sortBy.equals(SORT_PERCENT_USED) || sortBy.equals(SORT_QUOTA_LIMIT)))
            throw ServiceException.INVALID_REQUEST("sortBy must be percentUsed or totalUsed", null);

//        int flags = Provisioning.SA_ACCOUNT_FLAG;
        
//        String[] attrs = attrsStr == null ? null : attrsStr.split(",");

        // if we are a domain admin only, restrict to domain
        if (isDomainAdminOnly(lc)) {
            if (domain == null) {
                domain = getAuthTokenAccountDomain(lc).getName();
            } else {
                if (!canAccessDomain(lc, domain)) 
                    throw ServiceException.PERM_DENIED("can not access domain"); 
            }
        }

        Domain d = null;
        if (domain != null) {
            d = prov.get(DomainBy.NAME, domain);
            if (d == null)
                throw AccountServiceException.NO_SUCH_DOMAIN(domain);
        }

        Cos c = null;
        if (cos != null) {
            c = prov.get(CosBy.NAME, cos);
            if (c == null)
                throw AccountServiceException.NO_SUCH_COS(cos);
        }
        
        QuotaUsageParams params = new QuotaUsageParams(d, c, sortBy,sortAscending);

        ArrayList<AccountQuota> quotas = params.doSearch();
       
        AdminSession session = (AdminSession) lc.getSession(SessionCache.SESSION_ADMIN);
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

        Element response = lc.createElement(AdminService.GET_QUOTA_USAGE_RESPONSE);
        int i, limitMax = offset+limit;
        for (i=offset; i < limitMax && i < quotas.size(); i++) {
            AccountQuota quota = quotas.get(i);
            Element account = response.addElement(AdminService.E_ACCOUNT);
            account.addAttribute(AdminService.A_NAME, quota.name);
            account.addAttribute(AdminService.A_ID, quota.id);
            account.addAttribute(AdminService.A_QUOTA_USED, quota.quotaUsed);
            account.addAttribute(AdminService.A_QUOTA_LIMIT, quota.quotaLimit);
        }
        response.addAttribute(AdminService.A_MORE, i < quotas.size());
        response.addAttribute(AdminService.A_SEARCH_TOTAL, quotas.size());        
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
        String mCosId;
        String mCosName;    
        String mSortBy;
        boolean mSortAscending;

        ArrayList<AccountQuota> mResult;
        
        QuotaUsageParams(Domain d, Cos c, String sortBy, boolean sortAscending) {
            mDomainId = (d == null) ? "" : d.getId();
            mCosId = (c == null) ? "" : c.getId();
            mCosName = (c == null) ? "" : c.getName();        
            mSortBy = (sortBy == null) ? "" : sortBy;
            mSortAscending = sortAscending;
        }
        
        public boolean equals(Object o) {
            if (!(o instanceof QuotaUsageParams)) return false;
            if (o == this) return true;
            
            QuotaUsageParams other = (QuotaUsageParams) o; 
            return 
                mDomainId.equals(other.mDomainId) &&
                mCosId.equals(other.mCosId) &&
                mSortBy.equals(other.mSortBy) &&
                mSortAscending == other.mSortAscending;
        }
        
        ArrayList<AccountQuota> doSearch() throws ServiceException {
            if (mResult != null) return mResult;

            ArrayList<AccountQuota> result = new ArrayList<AccountQuota>();
            
            String query = null;
            if (!mCosId.equals("")) {
                if (mCosName.equals(Provisioning.DEFAULT_COS_NAME)) {
                    query = String.format("(&(zimbraMailHost=%s)((zimbraCOSId=%s)|(!(zimbraCOSId=*))))", LOCAL_HOST, mCosId);
                } else {
                    query = String.format("(&(zimbraMailHost=%s)(zimbraCOSId=%s))", LOCAL_HOST, mCosId);
                }
            } else {
                query = String.format("(zimbraMailHost=%s)", LOCAL_HOST);
            }
            
            Provisioning prov = Provisioning.getInstance();
            int flags = Provisioning.SA_ACCOUNT_FLAG;
            List accounts;
            Domain d = mDomainId.equals("") ? null : prov.get(DomainBy.ID, mDomainId);
            if (d != null) {
                accounts = prov.searchAccounts(d, query, null, null, true, flags);
            } else {
                accounts = prov.searchAccounts(query, null, null, true, flags);
            }

            Map<String, Long> quotaUsed = Mailbox.getMailboxSizes();
            
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

            Comparator<AccountQuota> comparator = new Comparator<AccountQuota>() {
                public int compare(AccountQuota a, AccountQuota b) {
                    int comp = 0;
                    if (sortByTotal) {
                        if (a.quotaUsed > b.quotaUsed) comp = 1;
                        else if (a.quotaUsed < b.quotaUsed) comp = -1;
                    } else if (sortByQuota) {
                        if (a.sortQuotaLimit > b.sortQuotaLimit) comp = 1;
                        else if (a.sortQuotaLimit < b.sortQuotaLimit) comp = -1;                            
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
