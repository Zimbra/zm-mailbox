/*
 * ***** BEGIN LICENSE BLOCK *****
 *
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008 Zimbra, Inc.
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
package com.zimbra.cs.util.yauth;

import java.util.Map;
import java.util.HashMap;

/**
 * Yahoo authentication error codes.
 */
public enum ErrorCode {
    INVALID_PASSWORD("InvalidPassword", "Invalid Password"),
    UNDER_AGE_USER("UnderAgeUser", "User is under age"),
    CAPTCHA_REQUIRED("CaptchaRequired", "Captcha is required"),
    USER_MUST_LOGIN("UserMustLogin", "Browser login required"),
    LOGIN_DOESNT_EXIST("LoginDoesntExist", "User login does not exist"),
    LOCKED_USER("LockedUser", "User is in locked state"),
    TEMP_ERROR("TempError", "Temporary Error"),
    HTTPS_REQUIRED("HttpsRequired", "This webservice call requires HTTPS"),
    TOKEN_REQUIRED("TokenRequired", "Invalid (missing) token"),
    INVALID_APP_ID("InvalidAppId", "Invalid (missing) appid"),
    INVALID_TOKEN("InvalidToken", "Invalid (missing) token"),
    INVALID_LOGIN_OR_PASSWORD("InvalidLoginOrPassword", "Invalid (missing) login or password"),
    INVALID_CAPTCHA_WORD_LEN("InvalidCaptchaWWordLen", "Invalid (missing) captchaword"),
    INVALID_CAPTCHA_DATA("InvalidCaptchaData", "Invalid (missing) captchadata"),
    INVALID_CAPTCHA("InvalidCaptcha", "Validation of captcha failed"),
    DEACTIVATED_APP_ID("DeactivatedAppId", "Application id disabled"),
    GENERIC_ERROR("GenericError", "Unspecified error");

    private static final Map<String, ErrorCode> byName;

    static {
        byName = new HashMap<String, ErrorCode>();
        for (ErrorCode error : values()) {
            byName.put(error.name, error);
        }
    }

    public static ErrorCode get(String name) {
        return byName.get(name);
    }
    
    private final String name;
    private final String description;
    
    private ErrorCode(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
}
