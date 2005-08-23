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
import com.zimbra.soap.ZimbraContext;

/**
 * @author dkarp
 */
public class GetMailbox extends AdminDocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);

        Element mreq = request.getElement(AdminService.E_MAILBOX);
        String accountId = mreq.getAttribute(AdminService.A_ACCOUNTID);

        Mailbox mbox = Mailbox.getMailboxByAccountId(accountId);
        Element response = lc.createElement(AdminService.GET_MAILBOX_RESPONSE);
        Element m = response.addElement(AdminService.E_MAILBOX);
        m.addAttribute(AdminService.A_MAILBOXID, mbox.getId());
        m.addAttribute(AdminService.A_SIZE, mbox.getSize());
        return response;
    }
}
