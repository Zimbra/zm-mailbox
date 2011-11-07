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

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.account.Key.DomainBy;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.EmailUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class AllowFromAddress extends AttributeCallback {

    /**
     * zimbraAllowFromAddress may not contain the address of an internal account or a distribution list
     */
    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) 
    throws ServiceException {

        MultiValueMod mod = multiValueMod(attrsToModify, Provisioning.A_zimbraAllowFromAddress);
        if (mod != null && (mod.adding() || mod.replacing())) {
            for (String addr : mod.valuesSet()) {
                checkAddress(addr);
            }
        }
    }

    private void checkAddress(String addr) throws ServiceException {
        Provisioning prov = Provisioning.getInstance();
        String domain = EmailUtil.getValidDomainPart(addr);
        if (domain != null) {  // addresses in non-local domains are allowed
            Domain internalDomain = prov.getDomain(DomainBy.name, domain, true);
            if (internalDomain != null) {
                if (prov.isDistributionList(addr)) {
                    throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraAllowFromAddress +
                            " may not contain a distribution list: " + addr, null);
                }
                Account acct = prov.get(AccountBy.name, addr);
                if (acct != null) {
                    throw ServiceException.INVALID_REQUEST(Provisioning.A_zimbraAllowFromAddress +
                            " may not contain an internal account: " + addr, null);
                }
            }
        }
    }
    
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}
