/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010 Zimbra, Inc.
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

import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.callback.CallbackContext.DataKey;

public class WhiteBlackList extends AttributeCallback {
    
    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry)
    throws ServiceException {
        
        if (entry != null && !(entry instanceof Account)) {
            return;
        }
        
        Account acct = (Account)entry;
        String max;
        
        if (context.isCreate() || entry == null) {
            if (Provisioning.A_amavisWhitelistSender.equalsIgnoreCase(attrName)) {
                max = context.getData(DataKey.MAIL_WHITELIST_MAX_NUM_ENTRIES);
            } else {
                max = context.getData(DataKey.MAIL_BLACKLIST_MAX_NUM_ENTRIES);
            }
        } else {
            if (Provisioning.A_amavisWhitelistSender.equalsIgnoreCase(attrName)) {
                max = acct.getAttr(Provisioning.A_zimbraMailWhitelistMaxNumEntries);
            } else {
                max = acct.getAttr(Provisioning.A_zimbraMailBlacklistMaxNumEntries);
            }
        }

        if (max != null) {
            check(max, acct, attrName, attrsToModify);
        }
        
        // if limit is not set, we take it as no limit and don't do any check.
    }
    
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }

    private void check(String max, Account acct, String attrName, Map attrsToModify) 
    throws ServiceException {
        
        // if max can't be parsed as an integer, NumberFormatException will be thrown
        // and the check will fail.   It is not likely to happen though, becasue the 
        // limit attrs are defined as integer.
        int numMax = Integer.valueOf(max);
        
        Object replace = attrsToModify.get(attrName);
        Object add = attrsToModify.get("+" + attrName);
        Object remove = attrsToModify.get("-" + attrName);
        
        //
        // we don't dedupe values in replace/add/remove for now, but LDAP does
        // so there might be a discrepency in max checking if there are dups 
        // in replace/add/remove.
        // will fix if there is a bug says so.
        //
        
        // this check is also done in LdapUtil.modifyAttrs, but we are in preModify 
        // and LdapUtil.modifyAttrs has not been called yet, so we check it here
        if ((add != null || remove != null) && replace != null)
            throw ServiceException.INVALID_REQUEST("can't mix +attrName/-attrName with attrName", null); 
        
        // now, we are either replacing, or adding/removing.
        Set<String> mods;
        if (replace != null) {
            // replacing
            mods = getMultiValueSet(replace);
            if (mods.size() > numMax)
                throwLimitExceeded(attrName, numMax);
        } else {
            // adding/removing
            if (acct == null) {
                // creating the entry, there is no existing values
                // just check the +'s
                mods = getMultiValueSet(add);
                if (mods.size() > numMax)
                    throwLimitExceeded(attrName, numMax);
            } else {
                // modifying an existing account
                Set<String> curValues = acct.getMultiAttrSet(attrName);
                int curNum = curValues.size();
                
                // see how many to add
                int numToAdd = 0;
                if (add != null) {
                    mods = getMultiValueSet(add);
                    for (String s : mods) {
                        if (!curValues.contains(s))
                            numToAdd++;
                    }
                }
                
                // see how many to remove
                int numToRemove = 0;
                if (remove != null) {
                    mods = getMultiValueSet(remove);
                    for (String s : mods) {
                        if (curValues.contains(s))
                            numToRemove++;
                    }
                }
                
                // if current values already exceeded the limit, just allow the removal
                // this can happen if the limit is changed after values were added
                if (curNum > numMax) {
                    // remove the adds from attrsToModify if there is any
                    if (add != null) {
                        ZimbraLog.account.warn("number of values for " + attrName + 
                                " already exceeded the limit: " + numMax + 
                                ", additional values are ignored");
                        attrsToModify.remove("+" + attrName);
                        
                        // if not also requesting for any removal, fail the request, since nothing has been done
                        if (remove == null)
                            throwLimitExceeded(attrName, numMax);
                        // else there is also some removal, let the removal through
                    }
                } else {
                    // see if the new number of values exceeds the limit
                    int newNum = curNum + numToAdd - numToRemove;
                    if (newNum > numMax)
                        throwLimitExceeded(attrName, numMax);
                }
            }
        }

    }
    
    private void throwLimitExceeded(String attrName, int numMax) throws ServiceException {
        throw ServiceException.INVALID_REQUEST("exceed limit for " + attrName + ", max is " + numMax, null);
    }

}
