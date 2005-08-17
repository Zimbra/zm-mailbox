/*
 * Created on Jun 17, 2004
 */
package com.liquidsys.coco.service.admin;

import java.util.Map;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.AccountServiceException;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.mailbox.Mailbox;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.util.LiquidLog;
import com.liquidsys.soap.LiquidContext;

/**
 * @author schemers
 */
public class DeleteAccount extends AdminDocumentHandler {
    
    /**
     * Deletes an account and its mailbox.
     */
    public Element handle(Element request, Map context) throws ServiceException {
        
        LiquidContext lc = getLiquidContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        String id = request.getAttribute(AdminService.E_ID);
        
        // Confirm that the account exists and that the mailbox is located
        // on the current host
        Account account = prov.getAccountById(id);
        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(id);
        if (!account.isCorrectHost()) {
            // Request must be sent to the host that the mailbox is on, so that
            // the mailbox can be deleted
            throw AccountServiceException.WRONG_HOST(
                account.getAttr(Provisioning.A_liquidMailHost), null);
        }
        Mailbox mbox = Mailbox.getMailboxByAccount(account);
        
        prov.deleteAccount(id);
        mbox.deleteMailbox();
        
        LiquidLog.security.info(LiquidLog.encodeAttrs(
            new String[] {"cmd", "DeleteAccount","name", account.getName(), "id", account.getId()}));
        
        Element response = lc.createElement(AdminService.DELETE_ACCOUNT_RESPONSE);
        return response;
    }
    
}