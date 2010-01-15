/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2010 Zimbra, Inc.
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
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Provisioning.CountAccountResult;
import com.zimbra.cs.account.Provisioning.DomainBy;
import com.zimbra.soap.ZimbraSoapContext;

public class CountAccount extends AdminDocumentHandler {

    /**
     * must be careful and only allow access to domain if domain admin
     */
    public boolean domainAuthSufficient(Map context) {
        return true;
    }

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        Element d = request.getElement(AdminConstants.E_DOMAIN);
        String key = d.getAttribute(AdminConstants.A_BY);
        String value = d.getText();
        
        Domain domain = prov.get(DomainBy.fromString(key), value);

        if (domain == null)
            throw AccountServiceException.NO_SUCH_DOMAIN(value);

        if (isDomainAdminOnly(zsc) && !canAccessDomain(zsc, domain))
            throw ServiceException.PERM_DENIED("can not access domain");

        Element response = zsc.createElement(AdminConstants.COUNT_ACCOUNT_RESPONSE);
        CountAccountResult result = Provisioning.getInstance().countAccount(domain);
        toXML(response, result);
        return response;
    }
    
    private void toXML(Element parent, CountAccountResult result) {
        for (CountAccountResult.CountAccountByCos c : result.getCountAccountByCos()) {
            Element eCos = parent.addElement(AdminConstants.E_COS);
            eCos.addAttribute(AdminConstants.A_ID, c.getCosId());
            eCos.addAttribute(AdminConstants.A_NAME, c.getCosName());
            eCos.setText(String.valueOf(c.getCount()));
        }
    }
}
