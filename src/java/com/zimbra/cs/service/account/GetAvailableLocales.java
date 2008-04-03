/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.service.account;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.util.StringUtil;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

public class GetAvailableLocales extends AccountDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");

        Locale displayLocale = getDisplayLocale(account, context);

        // get installed locales, sorted
        Locale installedLocales[] = L10nUtil.getLocalesSorted(displayLocale);

        // get avail locales for this account/COS
        Set<String> allowedLocales = account.getMultiAttrSet(Provisioning.A_zimbraAvailableLocale);

        Locale[] availLocales = null;
        if (allowedLocales.size() > 0)
            availLocales = computeAvailLocales(installedLocales, allowedLocales);
        else
            availLocales = installedLocales; 

        Element response = zsc.createElement(AccountConstants.GET_AVAILABLE_LOCALES_RESPONSE);
        for (Locale locale : availLocales) {
            if (locale != null)
                ToXML.encodeLocale(response, locale, displayLocale);
            else
                break;
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
    
    private Locale[] computeAvailLocales(Locale[] installedLocales, Set<String> allowedLocales) {
        /*
         * available locales is the intersection of installedLocales and allowedLocales
         * 
         * for a locale in allowedLocales, we include all the sub locales, but not the more "generic" locales in the family
         * e.g. - if allowedLocales is fr, all the fr_* in installedLocales will be included
         *      - if allowedLocales is fr_CA, all the fr_CA_* in installedLocales will be included, 
         *        but not any of the fr_[non CA] or fr.
         */
    
        Locale[] availLocales = new Locale[installedLocales.length];
        int i = 0;
        for (Locale locale : installedLocales) {
            // locale ids are in language[_country[_variant]] format 
            // include it if it allows a more generic locale in the family
            String localeId = locale.toString();
            String language = locale.getLanguage();
            String country = locale.getCountry();
            String variant = locale.getVariant();
                
            if (!StringUtil.isNullOrEmpty(variant)) {
                if (allowedLocales.contains(language) || allowedLocales.contains(language+"_"+country) || allowedLocales.contains(localeId))
                    availLocales[i++] = locale;
            } else if (!StringUtil.isNullOrEmpty(country)) {
                if (allowedLocales.contains(language) || allowedLocales.contains(localeId))
                    availLocales[i++] = locale;
            } else {
                if (allowedLocales.contains(localeId))
                    availLocales[i++] = locale;
            }
        }
        
        return availLocales;
    }
}
