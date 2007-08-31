/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2007 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.account.callback;

import java.util.ArrayList;
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
 
public class ChildAccount implements AttributeCallback {

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
        
        Set<String> vidsToRemove = new HashSet<String>();
        for (String vid : visibleChildren) {
            if (!allChildren.contains(vid)) {
                /*
                 * if the request is removing children and not updating the visible children, we remove the visible children 
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
            attrsToModify.put("-" + Provisioning.A_zimbraPrefChildVisibleAccount, vidsToRemove);
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
    
    static class MultiValueMod {
        // only one of add, removing, replacing can be true
        boolean mAdding;
        boolean mRemoving;
        boolean mReplacing;
        List<String> mValues;
        
        public boolean adding() { return mAdding; }
        public boolean removing() { return mRemoving; }
        public boolean replacing() { return mReplacing; }
        public List<String> values() { return mValues; }
        public Set<String> valuesSet() { return new HashSet<String>(mValues); }
    }
    
    protected MultiValueMod getMultiValue(Map attrsToModify, String attrName) {
        MultiValueMod mvm = new MultiValueMod();
        Object v = attrsToModify.get(attrName);
        if (v != null) {
            mvm.mReplacing = true;
        }
        if (v == null) {
            v = attrsToModify.get("+" + attrName);
            if (v != null)
                mvm.mAdding = true;
        }
        if (v == null) {
            v = attrsToModify.get("-" + attrName);
            if (v != null)
                mvm.mRemoving = true;
        }
        
        if (v != null)
            mvm.mValues = getMultiValue(v);
        
        return (v == null?null:mvm);
    }
    
    private List<String> getMultiValue(Object v) {
        
        List<String> list = null;
        
        // Convert array to List so it can be treated as a Collection
        if (v instanceof Object[]) {
            Object[] oa = (Object[]) v;
            list = new ArrayList<String>(oa.length);
            for (int i=0; i<oa.length; i++)
                list.add(oa[i] == null ? null : oa[i].toString());
            
        } else if (v instanceof Collection) {
            Collection c = (Collection) v;
            list = new ArrayList<String>(c.size());
            int i = 0;
            for (Object o : c)
                list.add(o == null ? null : o.toString());
            
        } else {
            list = new ArrayList<String>(1);
            list.add(v.toString());
        }
        
        return list;
    }

    
    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {

    }
}
