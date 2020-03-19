/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 *
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchAccountsOptions;
import com.zimbra.cs.account.SearchAccountsOptions.IncludeType;
import com.zimbra.cs.account.SearchDirectoryOptions.MakeObjectOpt;
import com.zimbra.cs.account.accesscontrol.AccessControlUtil;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAggregateQuotaUsageOnServer extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        if (!AccessControlUtil.isGlobalAdmin(getAuthenticatedAccount(zsc))) {
            throw ServiceException.PERM_DENIED("only global admin is allowed");
        }

        final Map<String, Long> domainAggrQuotaUsed = new HashMap<String, Long>();

        SearchAccountsOptions searchOpts = new SearchAccountsOptions();
        searchOpts.setIncludeType(IncludeType.ACCOUNTS_ONLY);
        searchOpts.setMakeObjectOpt(MakeObjectOpt.NO_DEFAULTS);

        final String[] returnAttrs = { Provisioning.A_zimbraId, Provisioning.A_zimbraDomainId };
        searchOpts.setReturnAttrs(returnAttrs);

        Provisioning prov = Provisioning.getInstance();
        final List<Domain> domains = prov.getAllDomains();

        for (Domain domain : domains) {
            searchOpts = new SearchAccountsOptions(domain);
            searchOpts.setIncludeType(IncludeType.ACCOUNTS_ONLY);
            searchOpts.setMakeObjectOpt(MakeObjectOpt.NO_DEFAULTS);
            searchOpts.setReturnAttrs(returnAttrs);
            searchOpts.setDomain(domain);
            DomainQuotaVisitor visitor = new DomainQuotaVisitor();
            prov.searchAccountsOnServer(prov.getLocalServer(), searchOpts, visitor);

            final Long quotaUsed = visitor.getQuotaUsed();
            domainAggrQuotaUsed.put(domain.getId(), quotaUsed);
        }

        final Element response = zsc.createElement(AdminConstants.GET_AGGR_QUOTA_USAGE_ON_SERVER_RESPONSE);
        for (String domainId : domainAggrQuotaUsed.keySet()) {
            final Domain domain = prov.getDomainById(domainId);
            final Element domainElt = response.addNonUniqueElement(AdminConstants.E_DOMAIN);
            domainElt.addAttribute(AdminConstants.A_NAME, domain.getName());
            domainElt.addAttribute(AdminConstants.A_ID, domainId);
            domainElt.addAttribute(AdminConstants.A_QUOTA_USED, domainAggrQuotaUsed.get(domainId));
        }

        return response;
    }

    public static class DomainQuotaVisitor implements NamedEntry.Visitor {

        private static final int DEFAULT_BATCH_SIZE = 250;

        private final List<NamedEntry> accounts = new ArrayList<NamedEntry>();
        private final int batchSize;
        private Long quotaUsed = 0L;
        private int count = 0;

        public DomainQuotaVisitor() {
            this.batchSize = DEFAULT_BATCH_SIZE;
        }

        public DomainQuotaVisitor(int batchSize) {
            this.batchSize = batchSize;
        }

        @Override
        public void visit(NamedEntry entry) throws ServiceException {
            if (!(entry instanceof Account)) {
                return;
            }

            this.accounts.add(entry);

            count++;
            if (count % batchSize == 0) {

                final Map<String, Long> batchAcctQuotaUsed = MailboxManager.getInstance().getMailboxSizes(accounts);

                for (NamedEntry ne : accounts) {
                    final Account acct = (Account) ne;
                    Long acctQuota = batchAcctQuotaUsed.get(acct.getId());
                    if (acctQuota == null) {
                        acctQuota = 0L;
                    }

                    this.quotaUsed += acctQuota;
                }

                this.accounts.clear();
            }
        }

        public Long getQuotaUsed() {
            return quotaUsed;
        }

    }
}
