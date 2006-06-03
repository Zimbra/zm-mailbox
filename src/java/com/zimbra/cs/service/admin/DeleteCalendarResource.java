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

import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.CalendarResourceBy;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author jhahm
 */
public class DeleteCalendarResource extends AdminDocumentHandler {

    /**
     * must be careful and only allow deletes domain admin has access to
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    /**
     * Deletes a calendar resource account and its mailbox.
     */
    public Element handle(Element request, Map<String, Object> context)
    throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();

        String id = request.getAttribute(AdminService.E_ID);

        // Confirm that the account exists and that the mailbox is located
        // on the current host
        CalendarResource resource = prov.get(CalendarResourceBy.ID, id);
        if (resource == null)
            throw AccountServiceException.NO_SUCH_CALENDAR_RESOURCE(id);

        if (!canAccessAccount(lc, resource))
            throw ServiceException.PERM_DENIED(
                    "cannot access calendar resource account");

        if (!Provisioning.onLocalServer(resource)) {
            // Request must be sent to the host that the mailbox is on, so that
            // the mailbox can be deleted
            throw ServiceException.WRONG_HOST(
                    resource.getAttr(Provisioning.A_zimbraMailHost), null);
        }
        Mailbox mbox = Mailbox.getMailboxByAccount(resource);

        prov.deleteCalendarResource(id);
        mbox.deleteMailbox();

        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
            new String[] {"cmd", "DeleteCalendarResource", "name",
                          resource.getName(), "id", resource.getId()}));

        Element response =
            lc.createElement(AdminService.DELETE_CALENDAR_RESOURCE_RESPONSE);
        return response;
    }
}
