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

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.AccessManager;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.accesscontrol.RightManager;
import com.zimbra.cs.account.accesscontrol.TargetType;
import com.zimbra.cs.account.accesscontrol.UserRight;
import com.zimbra.cs.util.AccountUtil;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.soap.type.TargetBy;

public class CheckRights extends AccountDocumentHandler {

    private static class RequestedTarget {
        private Entry targetEntry;
        private TargetType targetType;
        private TargetBy targetBy;
        private String targetKey;
        private List<UserRight> rights = Lists.newArrayList();
        
        private RequestedTarget(Entry targetEntry, TargetType targetType, 
                TargetBy targetBy, String targetKey) {
            this.targetEntry = targetEntry;
            this.targetType = targetType;
            this.targetBy = targetBy;
            this.targetKey = targetKey;
        }
        
        private void addRight(UserRight right) {
            rights.add(right);
        }
        
        private Entry getTargetEntry() {
            return targetEntry;
        }
        
        private TargetType getTargetType() {
            return targetType;
        }
        
        private TargetBy getTargetBy() {
            return targetBy;
        }
        
        private String getTargetKey() {
            return targetKey;
        }
        
        private List<UserRight> getRights() {
            return rights;
        }
    };
    
    @Override
    public Element handle(Element request, Map<String, Object> context)
            throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Provisioning prov = Provisioning.getInstance();
        
        List<RequestedTarget> requestedTargets = Lists.newArrayList();
        
        for (Element eTarget : request.listElements(AccountConstants.E_TARGET)) {
            TargetType targetType = TargetType.fromCode(eTarget.getAttribute(AccountConstants.A_TYPE));
            TargetBy targetBy = TargetBy.fromString(eTarget.getAttribute(AccountConstants.A_BY));
            String key = eTarget.getAttribute(AccountConstants.A_KEY);
            
            Entry entry = findEntry(prov, targetType, targetBy, key);
            
            RequestedTarget target = new RequestedTarget(entry, targetType, targetBy, key);
            requestedTargets.add(target);
            for (Element eRight : eTarget.listElements(AccountConstants.E_RIGHT)) {
                // can only be user right, not admim rights
                target.addRight(RightManager.getInstance().getUserRight(eRight.getText()));
            }
            
            if (target.getRights().size() == 0) {
                throw ServiceException.INVALID_REQUEST(
                        "missing right for target: " + key, null);
            }
        }
        
        Element response = zsc.createElement(AccountConstants.CHECK_RIGHTS_RESPONSE);
        
        AccessManager accessMgr = AccessManager.getInstance();
        for (RequestedTarget target : requestedTargets) {
            Entry targetEntry = target.getTargetEntry();
            
            Element eTarget = response.addElement(AccountConstants.E_TARGET);
            eTarget.addAttribute(AccountConstants.A_TYPE, target.getTargetType().getCode());
            eTarget.addAttribute(AccountConstants.A_BY, target.getTargetBy().name());
            eTarget.addAttribute(AccountConstants.A_KEY, target.getTargetKey());
            
            boolean combinedResult = true;
            
            for (UserRight right : target.getRights()) {
                boolean allow = accessMgr.canDo(zsc.getAuthToken(), targetEntry, right, false);
                
                if (allow &&
                    DiscoverRights.isDelegatedSendRight(right) &&
                    TargetBy.name == target.getTargetBy()) {
                    allow = AccountUtil.isAllowedSendAddress((NamedEntry) targetEntry, target.getTargetKey());
                }
                eTarget.addElement(AccountConstants.E_RIGHT).
                        addAttribute(AccountConstants.A_ALLOW, allow).
                        setText(right.getName());
                combinedResult = combinedResult & allow;
            }
            eTarget.addAttribute(AccountConstants.A_ALLOW, combinedResult);
        }
        return response;
    }
    
    private Entry findEntry(Provisioning prov, TargetType targetType, TargetBy targetBy, String key) 
    throws ServiceException {
        Entry entry = null;
        switch (targetType) {
            case account:
            case calresource:
            case dl:
            case group:
            case domain:
                entry = TargetType.lookupTarget(prov, targetType, targetBy, key, false);
                break;
            default:
                throw ServiceException.INVALID_REQUEST(
                        "unsupported target type: " + targetType.getCode(), null);
        }
        
        if (entry == null && TargetBy.id == targetBy) {
            throw ServiceException.INVALID_REQUEST("no such entry: " + key, null);
        }
        
        /*
         * if entry is null, the target could be an external user, let it fall through
         * to return the default permission.
         */
        return entry;
    }

}
