/*
 * Created on Apr 2, 2005
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.account.AccountServiceException;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.zimbra.soap.LiquidContext;

/**
 * @author dkarp
 */
public class PurgeMessages extends AdminDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {
        LiquidContext lc = getLiquidContext(context);

        Element mreq = request.getOptionalElement(AdminService.E_MAILBOX);
        String[] accounts;
        if (mreq != null)
            accounts = new String[] { mreq.getAttribute(AdminService.A_ACCOUNTID) };
        else
            accounts = Mailbox.getAccountIds();

        Element response = lc.createElement(AdminService.PURGE_MESSAGES_RESPONSE);
        for (int i = 0; i < accounts.length; i++) {
            Mailbox mbox = Mailbox.getMailboxByAccountId(accounts[i]);
            try {
                mbox.purgeMessages(null);
            } catch (AccountServiceException ase) {
                // ignore NO_SUCH_ACCOUNT errors (logged by the mailbox)
                if (ase.getCode() != AccountServiceException.NO_SUCH_ACCOUNT)
                    throw ase;
            }

            Element mresp = response.addElement(AdminService.E_MAILBOX);
            mresp.addAttribute(AdminService.A_MAILBOXID, mbox.getId());
            mresp.addAttribute(AdminService.A_SIZE, mbox.getSize());
        }
        return response;
	}
}
