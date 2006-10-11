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
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import org.xmpp.packet.JID;

public class IMAddr {
    private String mAddr;
    
    public IMAddr(String addr) {
        mAddr = addr;
    }
    
    public String getAddr() { return mAddr; }
    
    public String toString() { return mAddr; }
    
    public JID makeJID() {
        int domainSplit = mAddr.indexOf('@');
        
        if (domainSplit > 0) {
            String namePart = mAddr.substring(0, domainSplit);
            String domainPart = mAddr.substring(domainSplit+1);
            return new JID(namePart, domainPart, "");
        } else {
            return new JID(mAddr);
        }            
    }
    
    public boolean equals(Object other) {
        return (((IMAddr)other).mAddr).equals(mAddr);
    }
    
    public int hashCode() {
        return mAddr.hashCode();
    }
}
