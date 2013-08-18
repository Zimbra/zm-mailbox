/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2012, 2013 Zimbra Software, LLC.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.4 ("License"); you may not use this file except in
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
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class AutoProvLdapSearchFilter extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry)
            throws ServiceException {
        //reset zimbraAutoProvLastPolledTimestamp
        String oldValue = null;
        String newValue = null;
        if (entry != null) {
            oldValue = entry.getAttr(attrName, true);
        }
        SingleValueMod mod = singleValueMod(attrsToModify, attrName);
        newValue = mod.value();
        if (!StringUtil.equal(oldValue, newValue)) {
            attrsToModify.put(Provisioning.A_zimbraAutoProvLastPolledTimestamp, null);
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }

}
