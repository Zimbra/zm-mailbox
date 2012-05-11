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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.DynamicGroup;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;

public class MemberURL extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry)
    throws ServiceException {

        if (context.isDoneAndSetIfNot(MemberURL.class)) {
            return;
        }

        if (context.isCreate()) {
            // memberURL can be set to anything during create
            // zimbraIsACLGroup will be checked in createDynamicGroup
            return;
        }

        // not creating, ensure zimbraIsACLGroup must be FALSE
        boolean isACLGroup = entry.getBooleanAttr(Provisioning.A_zimbraIsACLGroup, true);

        if (isACLGroup) {
            throw ServiceException.INVALID_REQUEST("cannot modify " + Provisioning.A_memberURL +
                    " when " +  Provisioning.A_zimbraIsACLGroup + " is TRUE", null);
        }

    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}
