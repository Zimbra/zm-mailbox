/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.jsp.bean;

import java.util.List;

import com.zimbra.cs.zclient.ZEmailAddress;

public class BeanUtils {

    private static void addAddr(StringBuilder sb, ZEmailAddress email, int size) {
        if (email == null) return;
        if (sb.length() > 0) sb.append(", ");
        if (size > 1 && email.getDisplay() != null)
            sb.append(email.getDisplay());        
        else if (email.getPersonal() != null)
            sb.append(email.getPersonal());
        else if (email.getAddress() != null)
            sb.append(email.getAddress());
    }
    
    public static String getAddrs(List<ZEmailAddress> addrs) {
        if ( addrs == null) return "";
        int len = addrs.size();
        StringBuilder sb = new StringBuilder();
        for (ZEmailAddress addr: addrs) {
            addAddr(sb, addr, addrs.size());
        }
        return sb.toString();
    }
    
    public static String getAddr(ZEmailAddress addr) {
        if ( addr == null) return "";
        else if (addr.getPersonal() != null)
            return addr.getPersonal();
        else if (addr.getAddress() != null)
            return addr.getAddress();
        else
            return "";
    }

}
