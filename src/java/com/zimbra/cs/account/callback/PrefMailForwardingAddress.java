/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.callback.CallbackContext.DataKey;

public class PrefMailForwardingAddress extends AttributeCallback {

    /*
     * for modifying, we can get the max len from entry.getAccount
     * 
     * if the account entry is being created, or if someone is setting it (not a valid supported case) 
     * directly with createAccount, entry will be null, and we cannot do entry.getAccount to get the 
     * max length.  So we pass the max length in the context.
     * 
     * FIXME: currently callback keys are not set in LdapProvisioning because preModify is called before
     *        cos is decided.
     */
    
    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry)
    throws ServiceException {
        
        if (entry != null && !(entry instanceof Account)) {
            return;
        }
        
        SingleValueMod mod = singleValueMod(attrsToModify, attrName);
        if (mod.unsetting())
            return;
        
        int maxLen = -1;
        int maxAddrs = -1;
        
        String maxLenInCtxt = context.getData(DataKey.MAIL_FORWARDING_ADDRESS_MAX_LEN);
        if (maxLenInCtxt != null) {
            try {
                maxLen = Integer.parseInt(maxLenInCtxt);
            } catch (NumberFormatException e) {
                ZimbraLog.account.warn("encountered invalid " + 
                        DataKey.MAIL_FORWARDING_ADDRESS_MAX_LEN.name() + ": " + maxLenInCtxt);
            }
        }
        
        String maxAddrsInCtxt = context.getData(DataKey.MAIL_FORWARDING_ADDRESS_MAX_NUM_ADDRS);
        if (maxAddrsInCtxt != null) {
            try {
                maxLen = Integer.parseInt(maxAddrsInCtxt);
            } catch (NumberFormatException e) {
                ZimbraLog.account.warn("encountered invalid " + 
                        DataKey.MAIL_FORWARDING_ADDRESS_MAX_NUM_ADDRS.name() + ": " + maxAddrsInCtxt);
            }
        } 
        
        // if we don't have a good max len/addrs value in the context, get it from the entry
        if (entry == null)
            return;
        
        Account account = (Account)entry;
        if (maxLen == -1)
            maxLen = account.getMailForwardingAddressMaxLength();
        
        if (maxAddrs == -1)
            maxAddrs = account.getMailForwardingAddressMaxNumAddrs();
        
        String newValue = mod.value();
        
        if (newValue.length() > maxLen) {
            throw ServiceException.INVALID_REQUEST("value is too long, the limit(" + 
                    Provisioning.A_zimbraMailForwardingAddressMaxLength + ") is " + 
                    maxLen, null);
        }
        
        String[] addrs = newValue.split(",");
        if (addrs.length > maxAddrs) {
            throw ServiceException.INVALID_REQUEST("value is too long, the limit(" + 
                    Provisioning.A_zimbraMailForwardingAddressMaxNumAddrs + ") is " + 
                    maxAddrs, null);
        }
    }
    
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }

}
