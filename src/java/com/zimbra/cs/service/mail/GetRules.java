/*
 * Created on Nov 10, 2004
 */
package com.liquidsys.coco.service.mail;

import java.util.Map;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.filter.RuleManager;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.soap.DocumentHandler;
import com.liquidsys.soap.LiquidContext;

/**
 * @author kchen
 */
public class GetRules extends DocumentHandler {

    /* (non-Javadoc)
     * @see com.liquidsys.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
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
