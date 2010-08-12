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

public class WorkdayStartEndTime extends AttributeCallback {

    @Override
    public void preModify(Map context, String attrName, Object attrValue, Map attrsToModify, Entry entry, boolean isCreate)
    throws ServiceException {
        // Allow unsetting.
        if (attrValue == null)
            return;
        // Value must be in HHMM format, from 0000 to 2400.  2400 is the end of the current day and the start of the following day.
        boolean valid = false;
        if (attrValue instanceof String) {
            String digits = (String) attrValue;
            int len = digits.length();
            // Allow unsetting.
            if (len == 0)
                return;
            if (len == 4) {
                try {
                    int hh = Integer.parseInt(digits.substring(0, 2));
                    int mm = Integer.parseInt(digits.substring(2));
                    valid = (hh >= 0 && hh <= 23 && mm >= 0 && mm <= 59) || (hh == 24 && mm == 0);
                } catch (NumberFormatException e) {}
            }
        }
        if (!valid)
            throw ServiceException.INVALID_REQUEST(attrName + " must be a 4-digit string of hours and minutes (24-hour format)", null);
    }

    @Override
    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
    }
}
