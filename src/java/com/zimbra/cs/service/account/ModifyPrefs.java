/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.LiquidContext;
import com.zimbra.soap.WriteOpDocumentHandler;

/**
 * @author schemers
 */
public class ModifyPrefs extends WriteOpDocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {
		LiquidContext lc = getLiquidContext(context);
        Account acct = getRequestedAccount(lc);

        HashMap prefs = new HashMap();
        HashMap specialPrefs = new HashMap();
        
        for (Iterator it = request.elementIterator(AccountService.E_PREF); it.hasNext(); ) {
            Element e = (Element) it.next();
            String name = e.getAttribute(AccountService.A_NAME);
            String value = e.getText();
    		    if (!name.startsWith("liquidPref")) {
    		        throw ServiceException.INVALID_REQUEST("pref name must start with liquidPref", null);
    		    }
    		    prefs.put(name, value);
        }
        // call modifyAttrs and pass true to checkImmutable
        acct.modifyAttrs(prefs, true);
        Element response = lc.createElement(AccountService.MODIFY_PREFS_RESPONSE);
        return response;
	}
}
