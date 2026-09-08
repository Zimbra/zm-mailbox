/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2025 Synacor, Inc.
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
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import java.util.Map;

public class FcmConfigRestriction extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) throws ServiceException {
        if (isDomainCreateContext(context) || !(entry instanceof Config)) {
            throw ServiceException.PERM_DENIED(
                    String.format("'%s' can only be configured at the global config level.", attrName));
        }
    }

    private boolean isDomainCreateContext(CallbackContext context) {
        return context != null
                && context.isCreate()
                && Domain.class.equals(context.getCreatingEntryType());
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        // No post-modify actions required
    }
}
