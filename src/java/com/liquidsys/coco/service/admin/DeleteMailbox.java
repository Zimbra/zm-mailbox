/*
 * Created on Mar 9, 2005
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.mailbox.MailServiceException;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.stats.StopWatch;
import com.liquidsys.coco.util.LiquidLog;
import com.liquidsys.soap.LiquidContext;

/**
 * @author dkarp
 */
public class DeleteMailbox extends AdminDocumentHandler {

    private StopWatch sWatch = StopWatch.getInstance("DeleteMailbox");

    public Element handle(Element request, Map context) throws ServiceException {
        long startTime = sWatch.start();
        LiquidContext lc = getLiquidContext(context);

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
