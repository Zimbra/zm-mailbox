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

package com.zimbra.cs.account.callback;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.SetUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
 
public class ChildAccount extends AttributeCallback {

    private static final String KEY = ChildAccount.class.getName();
    
    public void preModify(Map context, String attrName, Object value,
                          Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {

        /*
         * This callback is for both imbraPrefChildVisibleAccount and zimbraChildAccount, and it handles
         * both in one shot.  If we've been called just return.
         */ 
        Object done = context.get(KEY);
        if (done == null)
            context.put(KEY, KEY);
        else
            return;
        
        // the +/- has been striped off from attrName but we need that info, it is in attrsToModify
        
        MultiValueMod visibleChildrenMod = getMultiValue(attrsToModify, Provisioning.A_zimbraPrefChildVisibleAccount);
        MultiValueMod allChildrenMod = getMultiValue(attrsToModify, Provisioning.A_zimbraChildAccount);
        
        Set<String> visibleChildren = newValuesToBe(visibleChildrenMod, entry, Provisioning.A_zimbraPrefChildVisibleAccount);
        Set<String> allChildren = newValuesToBe(allChildrenMod, entry, Provisioning.A_zimbraChildAccount);
        
        if (allChildrenMod != null && allChildrenMod.deletingall()) {
            attrsToModify.put(Provisioning.A_zimbraPrefChildVisibleAccount, "");
        } else {
            Set<String> vidsToRemove = new HashSet<String>();
            for (String vid : visibleChildren) {
                if (!allChildren.contains(vid)) {
                    /*
                     * if the request is removing children but not updating the visible children, we remove the visible children 
                     * that are no longer a child.
                     * otherwise, we throw exception if the mod result into a situation where a visible child is not one of the children.
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
    }
    
    private Set<String> newValuesToBe(MultiValueMod mod, Entry entry, String attrName) {
        Set<String> newValues = null; 
        if (entry != null && entry instanceof Account) {
            Account acct = (Account)entry;
            Set<String> curValues = acct.getMultiAttrSet(attrName);
    
            if (mod == null) {
                newValues = curValues;
            } else {
                if (mod.adding()) {
                    newValues = new HashSet<String>();
                    SetUtil.union(newValues, curValues, mod.valuesSet());
                } else if (mod.removing()) {
                    newValues = SetUtil.subtract(curValues, mod.valuesSet());
                } else if (mod.deletingall()) {
                    newValues = new HashSet<String>();
                } else {
                    newValues = mod.valuesSet();
                }
            }
        } else {
            if (mod == null)
                newValues = new HashSet<String>();
            else
                newValues = mod.valuesSet();
        }
        
        return newValues;
    }

    
    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {

    }
}

