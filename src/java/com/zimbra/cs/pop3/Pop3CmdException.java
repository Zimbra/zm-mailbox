/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
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
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class Pop3CmdException extends Exception {
    
    public Pop3CmdException(String msg, Throwable cause) {
        super(msg, cause);
    }
    
    public Pop3CmdException(String msg) {
        super(msg);
    }
    
    /**
     * sanatize the result of getMessage for use in an -ERR response
     * 
     * @return
     */
    public String getResponse() {
        return getResponse(getMessage());
    }
    
    /**
     * sanatize the specified message for use in an -ERR response
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
