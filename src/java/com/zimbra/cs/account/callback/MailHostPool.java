/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
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

import java.util.List;
import java.util.Map;

import com.zimbra.common.account.Key;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class MailHostPool extends AttributeCallback {

    /**
     * check to make sure zimbraMailHostPool points to a valid server id
     */
    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) 
    throws ServiceException {
        
        MultiValueMod mod = multiValueMod(attrsToModify, Provisioning.A_zimbraMailHostPool);
        
        if (mod.adding() || mod.replacing()) {
            Provisioning prov = Provisioning.getInstance();
            List<String> pool = mod.values();
            for (String host : pool) {
                if (host == null || host.equals("")) continue;
                if (prov.get(Key.ServerBy.id, host) == null) {
                    throw ServiceException.INVALID_REQUEST(
                            "specified " + Provisioning.A_zimbraMailHostPool +
                            " does not correspond to a valid server: "+host, null);
                }
            }
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}
