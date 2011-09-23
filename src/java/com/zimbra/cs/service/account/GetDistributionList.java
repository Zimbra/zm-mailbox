/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011 Zimbra, Inc.
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

package com.zimbra.cs.service.account;

import java.util.Map;
import java.util.Set;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeClass;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;

import com.zimbra.soap.ZimbraSoapContext;

public class GetDistributionList extends AccountDocumentHandler {

    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        Account acct = getRequestedAccount(zsc);
        
        Group group = GetDistributionList.getGroup(request, acct, prov);
        
        Set<String> reqAttrs = getReqAttrs(request, 
                group.isDynamic() ?  AttributeClass.group : AttributeClass.distributionList);
        
        Element response = zsc.createElement(AccountConstants.GET_DISTRIBUTION_LIST_RESPONSE);
        com.zimbra.cs.service.admin.GetDistributionList.encodeDistributionList(
                response, group, true, reqAttrs, null);

        return response;
    }
    
    public static Group getGroup(Element request, Provisioning prov) 
    throws ServiceException {
        Element d = request.getElement(AccountConstants.E_DL);
        String key = d.getAttribute(AccountConstants.A_BY);
        String value = d.getText();
        
        // temporary fix for the caching bug
        // Group group = prov.getGroupBasic(Key.DistributionListBy.fromString(key), value);
        Group group = prov.getGroup(Key.DistributionListBy.fromString(key), value);
        
        if (group == null) {
            throw AccountServiceException.NO_SUCH_DISTRIBUTION_LIST(value);
        }
        
        return group;
    }
    
    public static Group getGroup(Element request, Account acct, Provisioning prov) 
    throws ServiceException {
        Group group = getGroup(request, prov);
        
        if (!AccessManager.getInstance().canAccessGroup(acct, group)) {
            throw ServiceException.PERM_DENIED(
                    "you do not have sufficient rights to access this distribution list");
        }
        
        return group;
    }
}

