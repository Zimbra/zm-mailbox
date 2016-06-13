/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
