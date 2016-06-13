/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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
