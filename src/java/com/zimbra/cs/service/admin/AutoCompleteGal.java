/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.List;
import java.util.Map;

import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.util.FileBufferedWriter;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.GalContact;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.cs.account.Provisioning.SearchGalResult;
import com.zimbra.cs.account.accesscontrol.AdminRight;
import com.zimbra.cs.account.accesscontrol.Rights.Admin;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class AutoCompleteGal extends AdminDocumentHandler {

    /**
     * must be careful and only return accounts a domain admin can see
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        String n = request.getAttribute(AdminConstants.E_NAME);

        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account acct = getRequestedAccount(getZimbraSoapContext(context));

        while (n.endsWith("*"))
            n = n.substring(0, n.length() - 1);

        String domain = request.getAttribute(AdminConstants.A_DOMAIN);
        String typeStr = request.getAttribute(AdminConstants.A_TYPE, "account");

        int max = (int) request.getAttributeLong(AdminConstants.A_LIMIT);
        Provisioning.GalSearchType type = Provisioning.GalSearchType.fromString(typeStr);
                
        Provisioning prov = Provisioning.getInstance();
        Domain d = prov.get(DomainBy.name, domain);
        if (d == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(domain);
        
        checkDomainRight(zsc, d, Admin.R_accessGAL); 

        Element response = zsc.createElement(AdminConstants.AUTO_COMPLETE_GAL_RESPONSE);

        SearchGalResult result = prov.autoCompleteGal(d, n, type, max);
        toXML(response, result);

        return response;
    }

    public boolean needsAuth(Map<String, Object> context) {
        return true;
    }

    @Override
    public void docRights(List<AdminRight> relatedRights, List<String> notes) {
        relatedRights.add(Admin.R_accessGAL);
    }

    public static void toXML(Element response, SearchGalResult result) throws ServiceException {
        response.addAttribute(AccountConstants.A_MORE, result.getHadMore());
        response.addAttribute(AccountConstants.A_TOKENIZE_KEY, result.getTokenizeKey());
        
        addContacts(response, result);
    }
    
    public static void addContacts(Element response, SearchGalResult result) throws ServiceException {
        
        ZimbraLog.gal.debug("GAL result total entries:" + result.getNumMatches());
        
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

}
