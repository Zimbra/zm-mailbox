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

package com.zimbra.common.util;

import javax.servlet.http.HttpServletRequest;


public class TrustedNetwork {
    
    private static final String IP_LOCALHOST = "127.0.0.1"; 
    
    /*
     * returns if an ip is in trusted network
     */
    public static boolean isIpTrusted(String ip) {
        if (StringUtil.isNullOrEmpty(ip))
            return false;
        
        // For now the only trusted ip is localhost
        return isLocalhost(ip);
    }

    public static boolean isLocalhost(String ip) {
        return IP_LOCALHOST.equals(ip);
    }

}
