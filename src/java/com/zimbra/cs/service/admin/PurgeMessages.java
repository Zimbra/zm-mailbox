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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Apr 2, 2005
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
public class PurgeMessages extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element mreq = request.getOptionalElement(AdminService.E_MAILBOX);
        String[] accounts;
        if (mreq != null)
            accounts = new String[] { mreq.getAttribute(AdminService.A_ACCOUNTID) };
        else
            accounts = MailboxManager.getInstance().getAccountIds();

        Element response = lc.createElement(AdminService.PURGE_MESSAGES_RESPONSE);
        for (int i = 0; i < accounts.length; i++) {
            Mailbox mbox = null;
            try {
                mbox = MailboxManager.getInstance().getMailboxByAccountId(accounts[i]);
                mbox.purgeMessages(null);
            } catch (AccountServiceException ase) {
                // ignore NO_SUCH_ACCOUNT errors (logged by the mailbox)
                if (ase.getCode() != AccountServiceException.NO_SUCH_ACCOUNT)
                    throw ase;
            }
            if (mbox == null)
                continue;

            Element mresp = response.addElement(AdminService.E_MAILBOX);
            mresp.addAttribute(AdminService.A_MAILBOXID, mbox.getId());
            mresp.addAttribute(AdminService.A_SIZE, mbox.getSize());
        }
        return response;
	}
}
