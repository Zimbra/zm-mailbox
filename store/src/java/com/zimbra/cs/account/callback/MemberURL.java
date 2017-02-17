/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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
