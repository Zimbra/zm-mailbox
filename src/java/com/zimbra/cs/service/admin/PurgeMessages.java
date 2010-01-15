/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010 Zimbra, Inc.
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

/*
 * Created on Apr 2, 2005
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.common.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author dkarp
 */
public class PurgeMessages extends AdminDocumentHandler {

	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element mreq = request.getOptionalElement(AdminConstants.E_MAILBOX);
        String[] accounts;
        if (mreq != null)
            accounts = new String[] { mreq.getAttribute(AdminConstants.A_ACCOUNTID) };
        else
            accounts = MailboxManager.getInstance().getAccountIds();

        Element response = lc.createElement(AdminConstants.PURGE_MESSAGES_RESPONSE);
        for (int i = 0; i < accounts.length; i++) {
            Mailbox mbox = null;
            try {
                mbox = MailboxManager.getInstance().getMailboxByAccountId(accounts[i]);
                mbox.purgeMessages(null);
            } catch (ServiceException e) {
                // ignore NO_SUCH_ACCOUNT and WRONG_HOST errors
                if (e.getCode() == ServiceException.WRONG_HOST) {
                    ZimbraLog.mailbox.warn("ignoring mailbox found on wrong host (not cleaned up after migrate?): " + accounts[i], e);
                    // fall through to the "continue" statement
                } else if (e.getCode() != AccountServiceException.NO_SUCH_ACCOUNT) {
                    throw e;
                }
            }
            if (mbox == null)
                continue;

            Element mresp = response.addElement(AdminConstants.E_MAILBOX);
            mresp.addAttribute(AdminConstants.A_MAILBOXID, mbox.getId());
            mresp.addAttribute(AdminConstants.A_SIZE, mbox.getSize());
        }
        return response;
	}
}
