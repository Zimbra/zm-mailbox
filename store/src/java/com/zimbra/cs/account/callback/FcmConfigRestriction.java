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
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Cos;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Entry;
import java.util.Map;

public class FcmConfigRestriction extends AttributeCallback {
    /**
     * Resolves a human-readable configuration level name from the given entry type.
     *
     * @param entry the LDAP entry being modified
     * @return a lowercase label identifying the entry level (e.g. "domain", "cos", "account", "server")
     */
    private String resolveEntryLevel(Entry entry) {
        if (entry instanceof Domain) {
            return "domain";
        } else if (entry instanceof Cos) {
            return "cos (class of service)";
        } else if (entry instanceof Account) {
            return "account";
        }
        return entry.getClass().getSimpleName().toLowerCase();
    }

    @Override
    public void preModify(CallbackContext context, String attrName, Object value,
            Map attrsToModify, Entry entry) throws ServiceException {
        if (!(entry instanceof Config)) {
            String level = resolveEntryLevel(entry);
            throw ServiceException.PERM_DENIED(
                    String.format("'%s' cannot be configured at the %s level. "
                            + "Please use global config instead.", attrName, level));
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
        // No post-modify actions required
    }
}
