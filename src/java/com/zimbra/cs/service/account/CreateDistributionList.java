/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import com.zimbra.common.account.Key;
import com.zimbra.common.account.ZAttrProvisioning.DistributionListSubscriptionPolicy;
import com.zimbra.common.account.ZAttrProvisioning.DistributionListUnsubscriptionPolicy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.GranteeType;
import com.zimbra.soap.ZimbraSoapContext;

public class CreateDistributionList extends AccountDocumentHandler {
    
    static final DistributionListSubscriptionPolicy 
            DEFAULT_SUBSCRIPTION_POLICY = DistributionListSubscriptionPolicy.REJECT;
    
    static final DistributionListUnsubscriptionPolicy 
            DEFAULT_UNSUBSCRIPTION_POLICY = DistributionListUnsubscriptionPolicy.REJECT;

    public Element handle(Element request, Map<String, Object> context) 
    throws ServiceException {
        
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        String name = request.getAttribute(AccountConstants.E_NAME).toLowerCase();
        
        if (!AccessManager.getInstance().canCreateGroup(zsc.getAuthToken(), name)) {
            throw ServiceException.PERM_DENIED("you do not have sufficient rights to create distribution list");
        }
        
        Map<String, Object> attrs = AccountService.getAttrs(request, true, AccountConstants.A_N);
        
        if (attrs.get(Provisioning.A_zimbraDistributionListSubscriptionPolicy) == null) {
            attrs.put(Provisioning.A_zimbraDistributionListSubscriptionPolicy, 
                    DEFAULT_SUBSCRIPTION_POLICY.name());
        }
        
        if (attrs.get(Provisioning.A_zimbraDistributionListUnsubscriptionPolicy) == null) {
            attrs.put(Provisioning.A_zimbraDistributionListUnsubscriptionPolicy, 
                    DEFAULT_UNSUBSCRIPTION_POLICY.name());
        }
        
        
        boolean dynamic = request.getAttributeBool(AccountConstants.A_DYNAMIC, false);
        
        Group group = prov.createGroup(name, attrs, dynamic);
        
        // make creator a owner of the DL
        Account authedAcct = getAuthenticatedAccount(zsc);
        DistributionListAction.AddOwnerHandler.addOwner(prov, group,
                GranteeType.GT_USER, Key.GranteeBy.id, authedAcct.getId());
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                 new String[] {"cmd", "CreateDistributionList", "name", name}, attrs));         

        Element response = zsc.createElement(AccountConstants.CREATE_DISTRIBUTION_LIST_RESPONSE);
        
        // get the group again so ACL is on the instance
        group = prov.getGroup(Key.DistributionListBy.id, group.getId());
        com.zimbra.cs.service.admin.GetDistributionList.encodeDistributionList(response, group);

        return response;
    }

}

