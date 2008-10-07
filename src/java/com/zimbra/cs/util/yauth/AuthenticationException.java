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

import java.io.IOException;

public class AuthenticationException extends IOException {
    private final ErrorCode code;
    private String captchaUrl;
    private String captchaData;

    public AuthenticationException(ErrorCode code, String msg) {
        super(msg);
        this.code = code;
    }

    public AuthenticationException(ErrorCode code) {
        this(code, code.getDescription());
    }
    
    public ErrorCode getErrorCode() {
        return code;
    }

    public void setCaptchaUrl(String url) {
        captchaUrl = url;
    }

    public void setCaptchaData(String data) {
        captchaData = data;
    }

    public String getCaptchaUrl() { return captchaUrl; }
    public String getCaptchaData() { return captchaData; }
}
