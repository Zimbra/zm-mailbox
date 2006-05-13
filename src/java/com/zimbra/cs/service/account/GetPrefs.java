/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class GetPrefs extends DocumentHandler  {

	/* (non-Javadoc)
	 * @see com.zimbra.soap.DocumentHandler#handle(org.dom4j.Element, java.util.Map)
	 */
	public Element handle(Element request, Map<String, Object> context) throws ServiceException {
		ZimbraSoapContext lc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(lc);

        Element response = lc.createElement(AccountService.GET_PREFS_RESPONSE);
        handle(request, response, acct);
        return response;
    }

	/**
	 * Pass in a request that optional has &lt;pre&gt; items as a filter, and
	 * fills in the response document with gathered prefs.
	 * 
	 * @param request 
	 * @param acct
	 * @param response
	 * @throws ServiceException
	 */
	public static void handle(Element request, Element response, Account acct) throws ServiceException {
		HashSet<String> specificPrefs = null;
		for (Iterator it = request.elementIterator(AccountService.E_PREF); it.hasNext(); ) {
			if (specificPrefs == null)
				specificPrefs = new HashSet<String>();
			Element e = (Element) it.next();
			String name = e.getAttribute(AccountService.A_NAME);
			if (name != null)
				specificPrefs.add(name);
		}
	
		Map map = null; 
		map = acct.getAttrs(true, true);
		
	    if (map != null) {
    	    for (Iterator mi = map.entrySet().iterator(); mi.hasNext(); ) {
    	        Map.Entry entry = (Entry) mi.next();
    	        // FIXME: this could contain a String[] instead...
    	        String key = (String) entry.getKey();
    	        if (specificPrefs != null && !specificPrefs.contains(key))
    	            continue;
    	        if (!key.startsWith("zimbraPref"))
    	            continue;
    	        Object value = entry.getValue();
    	        if (value instanceof String[]) {
    	            String sa[] = (String[]) value;
    	            for (int i = 0; i < sa.length; i++) {
	    	            Element pref = response.addElement(AccountService.E_PREF);
                        pref.addAttribute(AccountService.A_NAME, key);
                        pref.setText(sa[i]);
    	            }
    	        } else {
    	            Element pref = response.addElement(AccountService.E_PREF);
                    pref.addAttribute(AccountService.A_NAME, key);
                    pref.setText((String) value);
    	        }
    	    }
	    }
	}
}
