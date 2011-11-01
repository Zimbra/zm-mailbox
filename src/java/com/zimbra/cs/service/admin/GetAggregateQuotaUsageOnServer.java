/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetAggregateQuotaUsageOnServer extends AdminDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        if (!AccessControlUtil.isGlobalAdmin(getAuthenticatedAccount(zsc))) {
            throw ServiceException.PERM_DENIED("only global admin is allowed");
        }

        Map<String, Long> domainAggrQuotaUsed = new HashMap<String, Long>();

        SearchAccountsOptions searchOpts = new SearchAccountsOptions();
        searchOpts.setIncludeType(IncludeType.ACCOUNTS_ONLY);
        searchOpts.setMakeObjectOpt(MakeObjectOpt.NO_DEFAULTS);

        Provisioning prov = Provisioning.getInstance();
        List<NamedEntry> accounts = prov.searchAccountsOnServer(prov.getLocalServer(), searchOpts);
        Map<String, Long> acctQuotaUsed = MailboxManager.getInstance().getMailboxSizes(accounts);
        for (NamedEntry ne : accounts) {
            if (!(ne instanceof Account)) {
                continue;
            }
            Account acct = (Account) ne;
            Long acctQuota = acctQuotaUsed.get(acct.getId());
            if (acctQuota == null) {
                acctQuota = 0L;
            }
            String domainId = acct.getDomainId();
            if (domainId == null) {
                continue;
            }
            Long aggrQuota = domainAggrQuotaUsed.get(domainId);
            domainAggrQuotaUsed.put(domainId, aggrQuota == null ? acctQuota : aggrQuota + acctQuota);
        }

        Element response = zsc.createElement(AdminConstants.GET_AGGR_QUOTA_USAGE_ON_SERVER_RESPONSE);
        for (String domainId : domainAggrQuotaUsed.keySet()) {
            Domain domain = prov.getDomainById(domainId);
            Element domainElt = response.addElement(AdminConstants.E_DOMAIN);
            domainElt.addAttribute(AdminConstants.A_NAME, domain.getName());
            domainElt.addAttribute(AdminConstants.A_ID, domainId);
            domainElt.addAttribute(AdminConstants.A_QUOTA_USED, domainAggrQuotaUsed.get(domainId));
        }
        return response;
    }
}
