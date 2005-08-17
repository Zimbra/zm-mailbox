/*
 * Created on Nov 10, 2004
 */
package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.filter.RuleManager;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.LiquidContext;

/**
 * @author kchen
 */
public class GetRules extends DocumentHandler {

    /* (non-Javadoc)
     * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
     */
    public Element handle(Element document, Map context)
            throws ServiceException {

		LiquidContext lc = getLiquidContext(context);
		// FIXME: need to check that account exists
        Account account = super.getRequestedAccount(lc);
        
        Element response = lc.createElement(MailService.GET_RULES_RESPONSE);
        RuleManager mgr = RuleManager.getInstance();
        Element rules = mgr.getRulesAsXML(response, account);
        response.addUniqueElement(rules);
        return response;
    }

}
