/*
 * Created on Mar 9, 2005
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.stats.StopWatch;
import com.zimbra.cs.util.LiquidLog;
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
            LiquidLog.security.info(LiquidLog.encodeAttrs(
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
