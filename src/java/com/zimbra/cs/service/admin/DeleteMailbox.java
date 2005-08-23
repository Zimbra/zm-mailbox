/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Mar 9, 2005
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.ZimbraContext;

/**
 * @author dkarp
 */
public class DeleteMailbox extends AdminDocumentHandler {

    private StopWatch sWatch = StopWatch.getInstance("DeleteMailbox");

    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        ZimbraContext lc = getZimbraContext(context);

        try {
            Element mreq = request.getElement(AdminService.E_MAILBOX);
            String accountId = mreq.getAttribute(AdminService.A_ACCOUNTID);

            Mailbox mbox = Mailbox.getMailboxByAccountId(accountId, false);
            int mailboxId = -1;
            if (mbox != null) {
                mailboxId = mbox.getId();
                mbox.deleteMailbox();
            }

            String idString = (mbox == null) ?
                "<no mailbox for account " + accountId + ">" : Integer.toString(mailboxId);
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "DeleteMailbox","id", idString}));
            
            Element response = lc.createElement(AdminService.DELETE_MAILBOX_RESPONSE);
            if (mbox != null)
                response.addElement(AdminService.E_MAILBOX)
                        .addAttribute(AdminService.A_MAILBOXID, mailboxId);
            return response;
        } finally {
            sWatch.stop(startTime);
        }
    }
}
