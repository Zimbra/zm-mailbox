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
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Group;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.Right;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.UserRight;
import com.zimbra.cs.account.accesscontrol.Rights.User;
import com.zimbra.soap.ZimbraSoapContext;

public class DiscoverRights extends AccountDocumentHandler {

    /* can't do this, RightManager might not have been initialized
    private static final Set<? extends Right> DELEGATED_SEND_RIGHTS = 
        Sets.newHashSet(
                User.R_sendAs,
                User.R_sendOnBehalfOf,
                User.R_sendAsDistList,
                User.R_sendOnBehalfOfDistList);
    
    */
    
    private static final Set<String> DELEGATED_SEND_RIGHTS = 
        Sets.newHashSet(
                Right.RT_sendAs,
                Right.RT_sendOnBehalfOf,
                Right.RT_sendAsDistList,
                Right.RT_sendOnBehalfOfDistList);
    
    
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
        for (Element eRight : request.listElements(AccountConstants.E_RIGHT)) {
            UserRight r = rightMgr.getUserRight(eRight.getText());
            rights.add(r); 
        }
        
        AccessManager accessMgr = AccessManager.getInstance();
        Map<Right, Set<Entry>> discoveredRights = accessMgr.discoverRights(account, rights);
        
        Element response = zsc.createElement(AccountConstants.DISCOVER_RIGHTS_RESPONSE);
        for (Map.Entry<Right, Set<Entry>> targetsForRight : discoveredRights.entrySet()) {
            Right right = targetsForRight.getKey();
            Set<Entry> targets = targetsForRight.getValue();
            
            boolean isDelegatedSendRight = DELEGATED_SEND_RIGHTS.contains(right.getName());
            
            Element eTargets = response.addElement(AccountConstants.E_TARGETS);
            eTargets.addAttribute(AccountConstants.A_RIGHT, right.getName());
            
            for (Entry target : targets) {
                // support only account and group targets for now
                TargetType targetType = TargetType.getTargetType(target);
                Element eTarget = eTargets.addElement(AccountConstants.E_TARGET);
                eTarget.addAttribute(AccountConstants.A_TYPE, targetType.getCode());
                
                if (isDelegatedSendRight) {
                    if (target instanceof Account || target instanceof Group) {
                        NamedEntry entry = (NamedEntry) target;
                        String[] addrs = target.getMultiAttr(Provisioning.A_zimbraPrefAllowAddressForDelegatedSender);
                        eTarget.addAttribute(AccountConstants.A_ID, entry.getId());
                        if (addrs.length == 0) {
                            Element eEmail = eTarget.addElement(AccountConstants.E_EMAIL);
                            eEmail.addAttribute(AccountConstants.A_ADDR, entry.getName());
                        } else {
                            for (String addr : addrs) {
                                Element eEmail = eTarget.addElement(AccountConstants.E_EMAIL);
                                eEmail.addAttribute(AccountConstants.A_ADDR, addr);
                            }
                        }
                    } else {
                        throw ServiceException.FAILURE("internal error, target for " +
                                " delegated send rights must be account or group", null);
                    }
                } else {
                    if (target instanceof NamedEntry) {
                        NamedEntry entry = (NamedEntry) target;
                        eTarget.addAttribute(AccountConstants.A_ID, entry.getId());
                        eTarget.addAttribute(AccountConstants.A_NAME, entry.getName());
                    } else {
                        eTarget.addAttribute(AccountConstants.A_NAME, target.getLabel());
                    }
                }
            }
        }
        return response;
    }

}
