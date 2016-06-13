/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Signature;
import com.zimbra.cs.account.callback.CallbackContext.DataKey;
 
public class MailSignature extends AttributeCallback {

    /**
     * check to make sure zimbraPrefMailSignature is shorter than the limit
     */
    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) 
    throws ServiceException {

        SingleValueMod mod = singleValueMod(attrName, value);
        if (mod.unsetting())
            return;
                
        if (entry != null && 
            !((entry instanceof Account)||(entry instanceof Identity)||(entry instanceof Signature))) {
            return;
        }

        long maxLen = -1;
        
        String maxInContext = context.getData(DataKey.MAX_SIGNATURE_LEN);
        if (maxInContext != null) {
            try {
                maxLen = Integer.parseInt(maxInContext);
            } catch (NumberFormatException e) {
                ZimbraLog.account.warn("encountered invalid " + 
                        DataKey.MAX_SIGNATURE_LEN.name() + ": " + maxInContext);
            }
        }

        if (maxLen == -1) {
            String maxInAttrsToModify = (String) attrsToModify.get(Provisioning.A_zimbraMailSignatureMaxLength);
            if (maxInAttrsToModify != null) {
                try {
                    maxLen = Integer.parseInt(maxInAttrsToModify);
                } catch (NumberFormatException e) {
                    ZimbraLog.account.warn("encountered invalid " + 
                            Provisioning.A_zimbraMailSignatureMaxLength + ": " +
                            maxInAttrsToModify);
                }
            }
        }

        if (maxLen == -1) {
            if (entry == null) {
                return;
            }
            
            Account account;
            if (entry instanceof Account) {
                account = (Account)entry;
            } else if (entry instanceof Identity) {
                account = ((Identity)entry).getAccount();
            } else if (entry instanceof Signature) {
                account = ((Signature)entry).getAccount();
            } else {
                return;
            }
            
            maxLen = account.getMailSignatureMaxLength();
        }

        // 0 means unlimited
        if (maxLen != 0 && ((String)value).length() > maxLen) {
            throw ServiceException.INVALID_REQUEST(
                    Provisioning.A_zimbraPrefMailSignature + 
                    " is longer than the limited value " + maxLen, null);
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}
