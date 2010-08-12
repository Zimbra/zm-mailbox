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

public class WorkdaysMask extends AttributeCallback {

    @Override
    public void preModify(Map context, String attrName, Object attrValue, Map attrsToModify, Entry entry, boolean isCreate)
    throws ServiceException {
        // Allow unsetting.
        if (attrValue == null)
            return;
        // Value must be a 7-character string of "0" and "1" characters only.
        int numValidChars = 0;
        if (attrValue instanceof String) {
            String mask = (String) attrValue;
            int len = mask.length();
            // Allow unsetting.
            if (len == 0)
                return;
            if (len == 7) {
                for (int i = 0; i < len; ++i) {
                    char ch = mask.charAt(i);
                    if (ch == '1' || ch == '0')
                        ++numValidChars;
                }
            }
        }
        if (numValidChars != 7)
            throw ServiceException.INVALID_REQUEST(attrName + " must be a 7-character string of 1's and 0's only", null);
    }

    @Override
    public void postModify(Map context, String attrName, Entry entry, boolean isCreate) {
    }
}
