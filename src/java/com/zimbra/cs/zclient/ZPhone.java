/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.zclient;

import com.zimbra.common.service.ServiceException;
import org.json.JSONException;

public class ZPhone implements ToZJSONObject {

	private String mName;

	public ZPhone(String name) throws ServiceException {
		mName = name;
	}

	public String getName() {
		return mName;
	}

	public String getDisplay() {
		return ZPhone.getDisplay(mName);
	}

    public ZJSONObject toZJSONObject() throws JSONException {
        ZJSONObject zjo = new ZJSONObject();
        zjo.put("name", mName);
        return zjo;
    }

    public String toString() {
        return ZJSONObject.toString(this);
    }

    public static String getDisplay(String name) {
        // Handles familiar usa-style numbers only for now...
        int offset = 0;
        boolean doIt = false;
        if (name.length() == 10) {
            doIt = true;
        } else if ((name.length() == 11) && (name.charAt(0) == '1')) {
            doIt = true;
            offset = 1;
        }
        if (doIt) {
            StringBuilder builder = new StringBuilder();
            builder.append('(');
            builder.append(name, offset, offset + 3);
            builder.append(") ");
            builder.append(name, offset + 3, offset + 6);
            builder.append('-');
            builder.append(name, offset + 6, offset + 10);
            return builder.toString();
        } else {
            return name;
        }
	}

    public static String getName(String display) {
        StringBuilder builder = new StringBuilder(display.length());
        for (int i = 0, count = display.length(); i < count; i++) {
            char ch = display.charAt(i);
            if (Character.isDigit(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }
}
