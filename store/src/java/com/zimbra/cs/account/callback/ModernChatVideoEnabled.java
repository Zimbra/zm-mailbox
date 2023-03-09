/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2022 Synacor, Inc.
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
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.callback.CallbackContext;
import com.zimbra.cs.extension.ZimbraExtensionNotification;

public class ModernChatVideoEnabled extends AttributeCallback {

    private String oldValue = null;
    private String newValue = null;
    
    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, Map attrsToModify, Entry entry)
            throws ServiceException {
        
        SingleValueMod mod = singleValueMod(attrName, attrValue);
        if (mod.unsetting()) {
            return;
        }
        
        if (entry != null) {
            oldValue = entry.getAttr(attrName, true);
        }
        newValue = mod.value();
        
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        try {
            if (context.isDoneAndSetIfNot(ModernChatVideoEnabled.class)) {
                return;
            }
            ZimbraLog.misc.debug("attrName %s, oldValue %s, newValue %s", attrName, oldValue, newValue);
            ZimbraExtensionNotification.notifyExtension("com.zimbra.cs.account.callback.ModernChatVideoEnabled:enabled",
                    entry, attrName, oldValue, newValue);
        } catch (ServiceException e) {
            ZimbraLog.misc.warn("Unable to set zimbraFeatureModernVideoEnabled " + e);
        }
    }
    
}