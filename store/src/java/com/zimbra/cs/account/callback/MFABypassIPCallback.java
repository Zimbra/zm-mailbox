/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2026 Synacor, Inc.
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
 *
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.account.callback;

import java.util.Collection;
import java.util.Map;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.CidrMatcher;
import com.zimbra.common.util.StringUtil;
import com.zimbra.cs.account.AttributeCallback;
import com.zimbra.cs.account.Entry;

/**
 * Rejects unparseable ranges on {@code zimbraMFAbyPassIP} at write time.
 *
 * Without this, a mistyped range would be silently ignored during authentication, quietly
 * changing which users are challenged for a second factor with no error to show for it.
 */
public class MFABypassIPCallback extends AttributeCallback {

    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue,
            Map attrsToModify, Entry entry) throws ServiceException {
        if (attrValue == null) {
            return;
        }
        if (attrValue instanceof String) {
            validate(attrName, (String) attrValue);
        } else if (attrValue instanceof String[]) {
            for (String value : (String[]) attrValue) {
                validate(attrName, value);
            }
        } else if (attrValue instanceof Collection) {
            for (Object value : (Collection<?>) attrValue) {
                if (value != null) {
                    validate(attrName, value.toString());
                }
            }
        }
    }

    private void validate(String attrName, String value) throws ServiceException {
        // Unsetting arrives as an empty value; there is nothing to check.
        if (StringUtil.isNullOrEmpty(value)) {
            return;
        }
        if (!CidrMatcher.isValid(value)) {
            throw ServiceException.INVALID_REQUEST(attrName + " value '" + value
                    + "' is not a valid IPv4 or IPv6 CIDR range, e.g. 10.0.0.0/8 or 2001:db8::/32", null);
        }
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}
