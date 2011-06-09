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

package com.zimbra.cs.account.callback;

import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.SignatureBy;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.DataSource;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Signature;

/**
 * Callback to ensure a signature being used for auto-accept/decline has non-empty text/plain content.
 */
public class PlainTextSignature extends AttributeCallback {

    public void preModify(Map context, String attrName, Object value, Map attrsToModify, Entry entry, boolean isCreate)
    throws ServiceException {
        SingleValueMod mod = singleValueMod(attrName, value);
        if (mod.unsetting())
            return;

        Account account;
        if (entry instanceof Account)
            account = (Account) entry;
        else if (entry instanceof Identity)
            account = ((Identity) entry).getAccount();
        else if (entry instanceof DataSource)
            account = ((DataSource) entry).getAccount();
        else
            return;

        Signature sig = Provisioning.getInstance().get(account, Key.SignatureBy.id, mod.value());
        if (sig == null) {
            throw ServiceException.INVALID_REQUEST("No such signature " + mod.value() + " for account " + account.getName(), null);
        }
        String sigAttr = Signature.mimeTypeToAttrName(MimeConstants.CT_TEXT_PLAIN);
        String plainSig = sig.getAttr(sigAttr, null);
        if (StringUtil.isNullOrEmpty(plainSig)) {
            throw ServiceException.INVALID_REQUEST("Signature " + mod.value() + " must have plain text content", null);
        }
    }

    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
    }
}
