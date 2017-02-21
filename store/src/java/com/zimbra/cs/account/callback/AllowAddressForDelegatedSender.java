/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2011, 2013, 2014, 2016 Synacor, Inc.
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
import java.util.Set;

import com.google.common.collect.Sets;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AliasedEntry;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;
import com.zimbra.cs.account.Group;

/**
 * Ensure values for zimbraPrefAllowAddressForDelegatedSender must be either the entry's
 * primary address or one of the aliases.
 *
 */
public class AllowAddressForDelegatedSender extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry)
    throws ServiceException {
        
        if (context.isDoneAndSetIfNot(AllowAddressForDelegatedSender.class)) {
            return;
        }
        
        Set<String> allowedValues;
        if (entry == null) {
            allowedValues = Sets.newHashSet();
            String entryName = context.getCreatingEntryName();
            if (entryName != null) {
                allowedValues.add(entryName);
            }
        } else {
            if (!(entry instanceof Account) && !(entry instanceof Group)) {
                return;
            }
            allowedValues = ((AliasedEntry) entry).getAllAddrsSet();
        }
        
        Object replace = attrsToModify.get(attrName);
        Object add = attrsToModify.get("+" + attrName);
        
        // can't replace and add in one command
        Set<String> values = getMultiValueSet((replace != null) ? replace : add);
        for (String value : values) {
            if (!StringUtil.isNullOrEmpty(value) && !allowedValues.contains(value)) {
                throw ServiceException.INVALID_REQUEST("value is not one of the addresses of the entry: " +
                        value, null);
            }
        }
    }
    
    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }


}
