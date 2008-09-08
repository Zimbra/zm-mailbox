/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007 Zimbra, Inc.
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

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.FileBufferedWriter;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class SearchGal extends AccountDocumentHandler {
    
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(getZimbraSoapContext(context));

        if (!canAccessAccount(zsc, account))
            throw ServiceException.PERM_DENIED("can not access account");
        
        if (!zsc.getAuthToken().isAdmin() && !zsc.getAuthToken().isDomainAdmin()) {
            if (!account.getBooleanAttr(Provisioning.A_zimbraFeatureGalEnabled, false))
                throw ServiceException.PERM_DENIED("cannot search GAL");
        }
        
        String n = request.getAttribute(AccountConstants.E_NAME);
        while (n.endsWith("*"))
            n = n.substring(0, n.length() - 1);

        String typeStr = request.getAttribute(AccountConstants.A_TYPE, "all");
        Provisioning.GAL_SEARCH_TYPE type;
        if (typeStr.equals("all"))
            type = Provisioning.GAL_SEARCH_TYPE.ALL;
        else if (typeStr.equals("account"))
            type = Provisioning.GAL_SEARCH_TYPE.USER_ACCOUNT;
        else if (typeStr.equals("resource"))
            type = Provisioning.GAL_SEARCH_TYPE.CALENDAR_RESOURCE;
        else
            throw ServiceException.INVALID_REQUEST("Invalid search type: " + typeStr, null);

        Element response = zsc.createElement(AccountConstants.SEARCH_GAL_RESPONSE);
        
        Provisioning prov = Provisioning.getInstance();
        Domain d = prov.getDomain(account);
        SearchGalResult result = prov.searchGal(d, n, type, null);
        toXML(response, result);
        
        return response;
    }

    @Override
    public boolean needsAuth(Map<String, Object> context) {
        return true;
    }

    public static void toXML(Element response, SearchGalResult result) throws ServiceException {
        response.addAttribute(AccountConstants.A_MORE, result.getHadMore());
        response.addAttribute(AccountConstants.A_TOKENIZE_KEY, result.getTokenizeKey());
        
        addContacts(response, result);
    }
    
    public static void addContacts(Element response, SearchGalResult result) throws ServiceException {
        if (isLarge(result))
            response.setIsLarge();   
        
        if (!(result instanceof Provisioning.VisitorSearchGalResult)) {
            for (GalContact contact : result.getMatches())
                addContact(response, contact);
        }
    }
    
    public static void addContact(Element response, GalContact contact) {
        Element cn = response.addElement(MailConstants.E_CONTACT);
        cn.addAttribute(MailConstants.A_ID, contact.getId());
        Map<String, Object> attrs = contact.getAttrs();
        for (Map.Entry<String, Object> entry : attrs.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String[]) {
                String sa[] = (String[]) value;
                for (int i = 0; i < sa.length; i++)
                    cn.addKeyValuePair(entry.getKey(), sa[i], MailConstants.E_ATTRIBUTE, MailConstants.A_ATTRIBUTE_NAME);
            } else {
                cn.addKeyValuePair(entry.getKey(), (String) value, MailConstants.E_ATTRIBUTE, MailConstants.A_ATTRIBUTE_NAME);
            }
        }
    }
    
    /*
     * we've got a big result
     */
    private static boolean isLarge(SearchGalResult result) {
        /*
        <cn id="uid=user1,ou=people,dc=phoebe,dc=mac">
            <a n="workPhone">+1 650 555 1111</a>
            <a n="objectClass">organizationalPerson</a>
            <a n="objectClass">zimbraAccount</a>
            <a n="objectClass">amavisAccount</a>
            <a n="modifyTimeStamp">20080906173522Z</a>
            <a n="createTimeStamp">20080906173432Z</a>
            <a n="zimbraId">acc886ee-2f45-47c1-99f3-6f28703d1f13</a>
            <a n="fullName">Demo User One</a>
            <a n="email">user1@phoebe.mac</a>
            <a n="lastName">user1</a>
        </cn>
        */
        // average gal entry size in SOAP
        final int GAL_ENTRY_AVG_SIZE = 600;  // bytes
        int maxInMemSize = LC.soap_max_in_memory_buffer_size.intValueWithinRange(0, FileBufferedWriter.MAX_BUFFER_SIZE);
        int numEntries = result.getNumMatches();
        
        return numEntries * GAL_ENTRY_AVG_SIZE > maxInMemSize;
    }
    
    
    public static class GalContactVisitor implements GalContact.Visitor {
        Element mResponse;
        
        GalContactVisitor(Element response) {
            mResponse = response;
        }
        
        public void visit(GalContact gc) {
            addContact(mResponse, gc);
        }
    }
}