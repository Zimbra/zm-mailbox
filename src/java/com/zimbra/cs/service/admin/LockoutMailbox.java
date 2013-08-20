/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.CalendarResourceBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.soap.ZimbraSoapContext;

public class LockoutMailbox extends AdminDocumentHandler {

    protected void checkRights(ZimbraSoapContext lc, Map<String, Object> context, Account account)
    throws ServiceException {
        Provisioning prov = Provisioning.getInstance();

        if (account.isCalendarResource()) {
            CalendarResource cr = prov.get(CalendarResourceBy.id, account.getId());
            checkCalendarResourceRight(lc, cr, Admin.R_moveCalendarResourceMailbox);
        } else {
            checkAccountRight(lc, account, Admin.R_moveAccountMailbox);
        }
        Server localServer = prov.getLocalServer();
        checkRight(lc, context, localServer, Admin.R_moveMailboxFromServer);
    }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = super.getZimbraSoapContext(context);
        String email = request.getElement(AdminConstants.E_ACCOUNT).getAttribute(AdminConstants.A_NAME);

        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.name, email);
        if (account == null) {
            throw ServiceException.FAILURE("Account " + email + " not found", null);
        }
        // Reload account to pick up any changes made by other servers.
        prov.reload(account);

        // check rights
        checkRights(zsc, context, account);

        String method = request.getAttribute(AdminConstants.A_OPERATION, AdminConstants.A_START);

        if (method.equalsIgnoreCase(AdminConstants.A_START)) {
            MailboxManager.getInstance().lockoutMailbox(account.getId());
        } else if (method.equalsIgnoreCase(AdminConstants.A_END)) {
            MailboxManager.getInstance().undoLockout(account.getId());
        } else {
            throw ServiceException.FAILURE("Unknown lockout method " + method, null);
        }

        return zsc.createElement(AdminConstants.LOCKOUT_MAILBOX_RESPONSE);
    }
}
