/*
 * Created on Jan 7, 2005
 */
package com.liquidsys.coco.service.mail;

import java.util.Map;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.filter.RuleManager;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.zimbra.soap.LiquidContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author kchen
 */
public class SaveRules extends WriteOpDocumentHandler {

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element document, Map context)
            throws ServiceException {
        LiquidContext lc = getLiquidContext(context);
        // FIXME: need to check that account exists
        Account account = super.getRequestedAccount(lc);
        
        RuleManager mgr = RuleManager.getInstance();
        Element rulesElem = document.getElement(MailService.E_RULES);
        mgr.setXMLRules(account, rulesElem);
        
        Element response = lc.createElement(MailService.SAVE_RULES_RESPONSE);
        return response;
    }

}
