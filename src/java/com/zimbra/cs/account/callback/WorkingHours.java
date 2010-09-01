/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2010 Zimbra, Inc.
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
import com.zimbra.cs.account.Entry;

public class WorkingHours extends AttributeCallback {

    // Value must be a comma-separated string whose parts are colon-separated strings.
    // Each comma-separated part specifies the working hours of a day of the week.
    // Each day of the week must be specified exactly once.
    // 
    @Override
    public void preModify(Map context, String attrName, Object attrValue, Map attrsToModify, Entry entry, boolean isCreate)
    throws ServiceException {
        if (attrValue == null) return;  // Allow unsetting.
        if (!(attrValue instanceof String))
            throw ServiceException.INVALID_REQUEST(attrValue + " is a single-valued string", null);
        String value = (String) attrValue;
        if (value.length() == 0) return;  // Allow unsetting.
        com.zimbra.cs.fb.WorkingHours.validateWorkingHoursPref(value);
    }

    @Override
    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
    }
}
