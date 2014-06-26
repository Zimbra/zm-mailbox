/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2013, 2014 Zimbra, Inc.
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <http://www.gnu.org/licenses/>.
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
