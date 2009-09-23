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
import java.util.regex.Pattern;

public class ZPhone implements ToZJSONObject {

    public static final String INVALID_PHNUM_OWN_PHONE_NUMBER = "voice.INVALID_PHNUM_OWN_PHONE_NUMBER";
    public static final String INVALID_PHNUM_INTERNATIONAL_NUMBER = "voice.INVALID_PHNUM_INTERNATIONAL_NUMBER";
    public static final String INVALID_PHNUM_BAD_NPA = "voice.INVALID_PHNUM_BAD_NPA";
    public static final String INVALID_PHNUM_BAD_LINE = "voice.INVALID_PHNUM_BAD_LINE";
    public static final String INVALID_PHNUM_EMERGENCY_ASSISTANCE = "voice.INVALID_PHNUM_EMERGENCY_ASSISTANCE";
    public static final String INVALID_PHNUM_DIRECTORY_ASSISTANCE = "voice.INVALID_PHNUM_DIRECTORY_ASSISTANCE";
    public static final String INVALID_PHNUM_BAD_FORMAT = "voice.INVALID_PHNUM_BAD_FORMAT";
    public static final String VALID = "voice.OK";
	
    public static final Pattern CHECK_INTERNATIONAL = Pattern.compile("^0\\d*");
    public static final Pattern CHECK_NPA = Pattern.compile("^1?(900)|(500)|(700)|(976)");
    public static final Pattern CHECK_LINE = Pattern.compile("^1?\\d{3}555\\d*");
    public static final Pattern CHECK_EMERGENCY_ASSISTANCE = Pattern.compile("^1?911\\d*");
    public static final Pattern CHECK_DIRECTORY_ASSISTANCE = Pattern.compile("^1?411\\d*");
    public static final Pattern CHECK_FORMAT = Pattern.compile("^1?[2-9]\\d{9}$");

    private String mName;
    private String mCallerId;

    public ZPhone(String name, String callerId) throws ServiceException {
        mName = name;
        mCallerId = callerId != null && (callerId.equals(name) || callerId.equals("Unavailable")) ? null : callerId;
    }

    public ZPhone(String name) throws ServiceException {
        this(name, null);
    }

    public String getName() {
        return mName;
    }

    public String getDisplay() {
        return ZPhone.getDisplay(mName);
    }

    public String getValidity() {
        return ZPhone.validate(mName);
    }

    public String getCallerId() {
        return mCallerId;
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
            if (offset>0) {
                builder.append(name, 0, offset);
                builder.append("-");
            }
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
        if (display == null) {
            return display;
        }
        StringBuilder builder = new StringBuilder(display.length());
        for (int i = 0, count = display.length(); i < count; i++) {
            char ch = display.charAt(i);
            if (Character.isDigit(ch)) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    public static String validate(String number) {
        number = ZPhone.getName(number);

        if (number.length() == 0) {
            return ZPhone.INVALID_PHNUM_BAD_FORMAT;
        }

        if (number.charAt(0) == '1')
            number = number.substring(1);

        if (ZPhone.CHECK_INTERNATIONAL.matcher(number).matches()) {
            return ZPhone.INVALID_PHNUM_INTERNATIONAL_NUMBER;
        }
									
        if (ZPhone.CHECK_NPA.matcher(number).matches()) {
            return ZPhone.INVALID_PHNUM_BAD_NPA;
        }
																	
        if (ZPhone.CHECK_LINE.matcher(number).matches()) {
            return ZPhone.INVALID_PHNUM_BAD_LINE;
        }
																				
        if (ZPhone.CHECK_EMERGENCY_ASSISTANCE.matcher(number).matches()) {
            return ZPhone.INVALID_PHNUM_EMERGENCY_ASSISTANCE;
        }
																						
        if (ZPhone.CHECK_DIRECTORY_ASSISTANCE.matcher(number).matches()) {
            return ZPhone.INVALID_PHNUM_DIRECTORY_ASSISTANCE;
        }
																										    
        if (!ZPhone.CHECK_FORMAT.matcher(number).matches()) {
            return ZPhone.INVALID_PHNUM_BAD_FORMAT;
        }
        return ZPhone.VALID;
    }
}
