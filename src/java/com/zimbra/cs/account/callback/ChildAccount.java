/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010 Zimbra, Inc.
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

package com.zimbra.cs.account.callback;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.SetUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
 
public class ChildAccount extends AttributeCallback {

    private static final String KEY = ChildAccount.class.getName();
    
    public void preModify(Map context, String attrName, Object value,
                          Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {

        /*
         * This callback is for both zimbraPrefChildVisibleAccount and zimbraChildAccount, and it handles
         * both in one shot.  If we've been called just return.
         */ 
        Object done = context.get(KEY);
        if (done == null)
            context.put(KEY, KEY);
        else
            return;
        
        // the +/- has been striped off from attrName but we need that info, it is in attrsToModify
        
        MultiValueMod visibleChildrenMod = multiValueMod(attrsToModify, Provisioning.A_zimbraPrefChildVisibleAccount);
        MultiValueMod allChildrenMod = multiValueMod(attrsToModify, Provisioning.A_zimbraChildAccount);
        
        Set<String> visibleChildren = newValuesToBe(visibleChildrenMod, entry, Provisioning.A_zimbraPrefChildVisibleAccount);
        Set<String> allChildren = newValuesToBe(allChildrenMod, entry, Provisioning.A_zimbraChildAccount);
        
        if (allChildrenMod != null && allChildrenMod.deleting()) {
            attrsToModify.put(Provisioning.A_zimbraPrefChildVisibleAccount, "");
        } else {
            Set<String> vidsToRemove = new HashSet<String>();
            for (String vid : visibleChildren) {
                if (!allChildren.contains(vid)) {
                    /*
                     * if the request is removing children but not updating the visible children, 
                     * we remove the visible children that are no longer a child.
                     * otherwise, throw exception if the mod results into a situation where a 
                     * visible child is not one of the children.
                     */ 
                    if (allChildrenMod!=null && allChildrenMod.removing() && visibleChildrenMod==null)
                        vidsToRemove.add(vid);
                    else
                        throw ServiceException.INVALID_REQUEST("visible child id " + vid + " is not one of " + Provisioning.A_zimbraChildAccount, null);
                }
            }
    
            if (vidsToRemove.size() > 0)
                attrsToModify.put("-" + Provisioning.A_zimbraPrefChildVisibleAccount, vidsToRemove.toArray(new String[vidsToRemove.size()]));
        }
        
        // check circular relationship
        if (entry instanceof Account) {
            Provisioning prov = Provisioning.getInstance();
            Account parentAcct = (Account)entry;
            String parentId = parentAcct.getId();
            for (String childId : allChildren) {
                Account childAcct = prov.get(AccountBy.id, childId);
                if (childAcct == null)
                    throw AccountServiceException.NO_SUCH_ACCOUNT(childId);
                
                String[] children = childAcct.getChildAccount();
                for (String child : children) {
                    if (child.equals(parentId))
                        throw ServiceException.INVALID_REQUEST(
                                "child account " + childId + "(" + childAcct.getName() + ")"  +
                                " is parent of the parent account " + parentId + "(" + parentAcct.getName() + ")", 
                                null);
                }
            }
        }
    }
    


    
    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {

    }
}

