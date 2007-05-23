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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.account;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.util.L10nUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

public class GetLocales extends AccountDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(lc);
        
        Locale displayLocale = getDisplayLocale(acct, context);
        Locale locales[] = L10nUtil.getLocalesSorted(displayLocale);
        Element response = lc.createElement(AccountConstants.GET_LOCALES_RESPONSE);
        for (Locale locale : locales) {
            ToXML.encodeLocale(response, locale, displayLocale);
        }
        return response;
    }
    
    private Locale getDisplayLocale(Account acct, Map<String, Object> context) throws ServiceException {
        
        // use zimbraPrefLocale is it is present 
        String locale = acct.getAttr(Provisioning.A_zimbraPrefLocale, false);
        
        // otherwise use Accept-Language header
        if (StringUtil.isNullOrEmpty(locale)) {
            HttpServletRequest req = (HttpServletRequest)context.get(SoapServlet.SERVLET_REQUEST);
            if (req != null)
                locale = req.getHeader("Accept-Language");
            //TODO need to handle multiple languages with quality value and use the one with the highest quality value
        }
        
        // otherwise use Provisioning.getLocale();
        if (StringUtil.isNullOrEmpty(locale))
            return Provisioning.getInstance().getLocale(acct);
        else
            return L10nUtil.lookupLocale(locale);
    }
}
