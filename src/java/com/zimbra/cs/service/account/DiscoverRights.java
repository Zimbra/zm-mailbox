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

import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.UserRight;
import com.zimbra.soap.ZimbraSoapContext;

public class DiscoverRights extends AccountDocumentHandler {

    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account account = getRequestedAccount(zsc);

        if (!canAccessAccount(zsc, account)) {
            throw ServiceException.PERM_DENIED("can not access account");
        }
        
        RightManager rightMgr = RightManager.getInstance();
        Set<Right> rights = Sets.newHashSet();
        for (Element eRight : request.listElements(MailConstants.E_RIGHT)) {
            UserRight r = rightMgr.getUserRight(eRight.getText());
            rights.add(r); 
        }
        
        AccessManager accessMgr = AccessManager.getInstance();
        Map<Right, Set<Entry>> discoveredRights = accessMgr.discoverRights(account, rights);
        
        Element response = zsc.createElement(AccountConstants.DISCOVER_RIGHTS_RESPONSE);
        for (Map.Entry<Right, Set<Entry>> targetsForRight : discoveredRights.entrySet()) {
            Right right = targetsForRight.getKey();
            Set<Entry> targets = targetsForRight.getValue();
            
            Element eTargets = response.addElement(AccountConstants.E_TARGETS);
            eTargets.addAttribute(MailConstants.A_RIGHT, right.getName());
            
            for (Entry target : targets) {
                // support only account and group targets for now
                if (target instanceof Account) {
                    Account acct = (Account)target;
                    Element eTarget = eTargets.addElement(AccountConstants.E_ACCOUNT);
                    eTarget.addAttribute(AccountConstants.A_ID, acct.getId());
                    eTarget.addAttribute(AccountConstants.A_NAME, acct.getName());
                } else if (target instanceof Group) {
                    Group group = (Group)target;
                    Element eTarget = eTargets.addElement(AccountConstants.E_DL);
                    eTarget.addAttribute(AccountConstants.A_ID, group.getId());
                    eTarget.addAttribute(AccountConstants.A_NAME, group.getName());
                } else {
                    throw ServiceException.FAILURE("target type unsupported yet: " +
                            target.getLabel(), null);
                }
            }
        }
        return response;
    }

}
