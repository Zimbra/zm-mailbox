/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2010 Zimbra, Inc.
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
