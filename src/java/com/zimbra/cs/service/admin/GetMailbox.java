/*
 * Created on Mar 9, 2005
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.LiquidContext;

/**
 * @author dkarp
 */
public class GetMailbox extends AdminDocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException {
        LiquidContext lc = getLiquidContext(context);

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
