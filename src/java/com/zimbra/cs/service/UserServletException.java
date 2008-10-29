/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005 Zimbra, Inc.
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
package com.zimbra.cs.service;

import javax.servlet.http.HttpServletResponse;

public class UserServletException extends Exception {
    private int mCode;

    public UserServletException(int code, String message) {
        super(message);
        mCode = code;
    }
    
    public UserServletException(int code, String message, Throwable cause) {
        super(message, cause);
        mCode = code;
    }

    public int getHttpStatusCode() {
       return mCode;
    }

    public static UserServletException notImplemented(String message) {
        return new UserServletException(HttpServletResponse.SC_NOT_IMPLEMENTED, message);
    }

    public static UserServletException badRequest(String message) {
        return new UserServletException(HttpServletResponse.SC_BAD_REQUEST, message);
    }
    
}
