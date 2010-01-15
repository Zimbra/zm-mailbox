/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010 Zimbra, Inc.
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
package com.zimbra.cs.util.yauth;

import com.zimbra.common.service.ServiceException;

@SuppressWarnings("serial")
public class AuthenticationException extends ServiceException {
    private final ErrorCode code;
    private String captchaUrl;
    private String captchaData;

    public AuthenticationException(ErrorCode code, String msg) {
        super(msg, "yauth." + code.name(), false);
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
