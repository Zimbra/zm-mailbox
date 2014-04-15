/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2014 Zimbra Software, LLC.
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
import com.zimbra.common.util.DateUtil;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;

public class PositiveTimeInterval extends AttributeCallback {

    @SuppressWarnings("rawtypes")
    @Override
    public void preModify(CallbackContext context, String attrName,
            Object attrValue, Map attrsToModify, Entry entry)
            throws ServiceException {
        SingleValueMod mod = singleValueMod(attrName, attrValue);
        if (mod.setting()) {
            long interval = DateUtil.getTimeInterval(mod.value(), 0);
            if (interval <= 0) {
                throw ServiceException.INVALID_REQUEST("cannot set " + attrName + " less than or equal to 0", null);
            }
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}
