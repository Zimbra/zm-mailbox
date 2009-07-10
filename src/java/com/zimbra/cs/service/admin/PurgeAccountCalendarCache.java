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
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.cache.CalendarCacheManager;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.soap.ZimbraSoapContext;

public class PurgeAccountCalendarCache extends AdminDocumentHandler {

    private static final String[] TARGET_ACCOUNT_PATH = new String[] { AdminConstants.A_ID };
    @Override protected String[] getProxiedAccountPath()  { return TARGET_ACCOUNT_PATH; }

    /**
     * must be careful and only allow renames from/to domains a domain admin can see
     */
    @Override public boolean domainAuthSufficient(Map<String, Object> context) {
        return true;
    }

    @Override public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String id = request.getAttribute(AdminConstants.A_ID);
        Account account = prov.get(AccountBy.id, id, zsc.getAuthToken());
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(id);

        if (account.isCalendarResource()) {
            // need a CalendarResource instance for RightChecker
            CalendarResource resource = prov.get(CalendarResourceBy.id, id);
            checkCalendarResourceRight(zsc, resource, Admin.R_purgeCalendarResourceCalendarCache);
        } else
            checkAccountRight(zsc, account, Admin.R_purgeAccountCalendarCache);
        
        if (!Provisioning.onLocalServer(account))
            throw ServiceException.WRONG_HOST(account.getAttr(Provisioning.A_zimbraMailHost), null);

        CalendarCacheManager calCache = CalendarCacheManager.getInstance();
        ZimbraLog.calendar.info("Purging calendar cache for account " + account.getName());
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccountId(account.getId(), false);
        if (mbox != null)
            calCache.purgeMailbox(mbox);
        Element response = zsc.createElement(AdminConstants.PURGE_ACCOUNT_CALENDAR_CACHE_RESPONSE);
        return response;
    }
    
    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_purgeAccountCalendarCache);
        relatedRights.add(Admin.R_purgeCalendarResourceCalendarCache);
    }
}
