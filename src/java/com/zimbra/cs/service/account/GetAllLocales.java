/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.account;

import java.util.Locale;
import java.util.Map;

import com.zimbra.cs.util.L10nUtil;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAllLocales extends AccountDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) {
        ZimbraSoapContext lc = getZimbraSoapContext(context);
        Locale locales[] = L10nUtil.getAllLocalesSorted();
        Element response = lc.createElement(AccountService.GET_ALL_LOCALES_RESPONSE);
        for (Locale locale : locales) {
            ToXML.encodeLocale(response, locale);
        }
        return response;
    }
}
