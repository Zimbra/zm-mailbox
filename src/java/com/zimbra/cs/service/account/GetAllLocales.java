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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.account;

import java.util.Locale;
import java.util.Map;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.L10nUtil;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAllLocales extends DocumentHandler {

    public Element handle(Element request, Map context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraContext(context);
        Locale locales[] = L10nUtil.getAllLocalesSorted();
        Element response = lc.createElement(AccountService.GET_ALL_LOCALES_RESPONSE);
        for (Locale locale : locales) {
            ToXML.encodeLocale(response, locale);
        }
        return response;
    }
}
