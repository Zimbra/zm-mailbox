/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
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

/*
 * Created on Nov 26, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.zimbra.cs.pop3;

/**
 * @author schemers
 */
public class Pop3CmdException extends Exception {
    
    public Pop3CmdException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
    public Pop3CmdException(String msg) {
        super(msg);
    }
    
    /**
     * sanitize the result of getMessage for use in an -ERR response
     * 
     * @return
     */
    public String getResponse() {
        return getResponse(getMessage());
    }
    
    /**
     * sanitize the specified message for use in an -ERR response
     * 
     * @return
     */
    public static String getResponse(String msg) {
        String resp = msg;
        if (resp.length() > Pop3Handler.MAX_RESPONSE_TEXT)
            resp = resp.substring(0, Pop3Handler.MAX_RESPONSE_TEXT);
        if (resp.indexOf('\r') != -1)
            resp = resp.replaceAll("\r", " ");
        if (resp.indexOf('\n') != -1)
            resp = resp.replaceAll("\n", " ");        
        return resp;
    }
}
