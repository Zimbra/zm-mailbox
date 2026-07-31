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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Provisioning;
import java.util.HashMap;
import java.util.Map;

public class SamlTestReset extends AttributeCallback {
    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry) throws ServiceException {
        // no pre-modify validation needed
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        // fire only once per batch
        if (context.isDoneAndSetIfNot(SamlTestReset.class)) {
            return;
        }
        if (entry == null) {
            return;
        }
        // clear all 3 test attributes — invalidate ongoing test + stale results
        Map<String, Object> attrs = new HashMap<>();
        attrs.put(Provisioning.A_zimbraSamlTestTimestamp, "");
        attrs.put(Provisioning.A_zimbraSamlTestErrorMessage, "");
        attrs.put(Provisioning.A_zimbraSamlTestNonce, "");
        try {
            Provisioning.getInstance().modifyAttrs(entry, attrs);
        } catch (ServiceException e) {
            ZimbraLog.account.warn("Failed to reset SAML test status on " + entry.getLabel(), e);
        }
    }
}
