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

import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Signature;
 
public class MailSignature extends AttributeCallback {
    
    /*
     * for modifying signature, we can get the max len from entry.getAccount
     * 
     * if the signature entry is being created, or if someone is setting it (not a valid supported case) directly with createAccount, 
     * the entry field will be null, and we cannot do entry.getAccount to get the max signature length.  So we pass the max length 
     * in the context.
     */
    public static final String CALLBACK_KEY_MAX_SIGNATURE_LEN = "KEY_MAX_SIGNATURE_LEN";

    /**
     * check to make sure zimbraPrefMailSignature is shorter than the limit
     */
    public void preModify(Map context, String attrName, Object value,
            Map attrsToModify, Entry entry, boolean isCreate) throws ServiceException {

        SingleValueMod mod = singleValueMod(attrName, value);
        if (mod.unsetting())
            return;
                
        if (entry != null && !((entry instanceof Account)||(entry instanceof Identity)||(entry instanceof Signature))) 
            return;
        
        long maxLen = -1;
        
        String maxInContext = (String)context.get(CALLBACK_KEY_MAX_SIGNATURE_LEN);
        if (maxInContext != null) {
            try {
                maxLen = Integer.parseInt(maxInContext);
            } catch (NumberFormatException e) {
                ZimbraLog.account.warn("encountered invalid " + CALLBACK_KEY_MAX_SIGNATURE_LEN + ": " + maxInContext);
            }
        }

        if (maxLen == -1) {
            String maxInAttrsToModify = (String) attrsToModify.get(Provisioning.A_zimbraMailSignatureMaxLength);
            if (maxInAttrsToModify != null) {
                try {
                    maxLen = Integer.parseInt(maxInAttrsToModify);
                } catch (NumberFormatException e) {
                    ZimbraLog.account.warn("encountered invalid " + Provisioning.A_zimbraMailSignatureMaxLength + ": " + maxInAttrsToModify);
                }
            }
        }

        if (maxLen == -1) {
            if (entry == null)
                return;
            
            Account account;
            if (entry instanceof Account)
                account = (Account)entry;
            else if (entry instanceof Identity)
                account = ((Identity)entry).getAccount();
            else if (entry instanceof Signature)
                account = ((Signature)entry).getAccount();
            else
                return;
                
            maxLen = account.getMailSignatureMaxLength();
        }

        // 0 means unlimited
        if (maxLen != 0 && ((String)value).length() > maxLen)
            throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraPrefMailSignature + " is longer than the limited value " + maxLen, null);
    }

    /**
     * need to keep track in context on whether or not we have been called yet, only 
     * reset info once
     */

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {

    }
}
