/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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
import com.zimbra.cs.account.Entry;

public class WorkingHours extends AttributeCallback {

    // Value must be a comma-separated string whose parts are colon-separated strings.
    // Each comma-separated part specifies the working hours of a day of the week.
    // Each day of the week must be specified exactly once.
    // 
    @Override
    public void preModify(CallbackContext context, String attrName, Object attrValue, 
            Map attrsToModify, Entry entry)
    throws ServiceException {
        if (attrValue == null) {
            return;  // Allow unsetting.
        }
        if (!(attrValue instanceof String)) {
            throw ServiceException.INVALID_REQUEST(attrValue + " is a single-valued string", null);
        }
        String value = (String) attrValue;
        if (value.length() == 0) {
            return;  // Allow unsetting.
        }
        com.zimbra.cs.fb.WorkingHours.validateWorkingHoursPref(value);
    }

    @Override
    public void postModify(CallbackContext context, String attrName, Entry entry) {
    }
}
