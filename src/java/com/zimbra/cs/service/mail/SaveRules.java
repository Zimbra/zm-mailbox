/*
 * Created on Jan 7, 2005
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.ZimbraContext;
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
        ZimbraContext lc = getZimbraContext(context);
        // FIXME: need to check that account exists
        Account account = super.getRequestedAccount(lc);
        
        RuleManager mgr = RuleManager.getInstance();
        Element rulesElem = document.getElement(MailService.E_RULES);
        mgr.setXMLRules(account, rulesElem);
        
        Element response = lc.createElement(MailService.SAVE_RULES_RESPONSE);
        return response;
    }

}
