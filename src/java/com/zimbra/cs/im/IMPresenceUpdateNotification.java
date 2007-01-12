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

import java.util.Formatter;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.soap.Element;

public class IMPresenceUpdateNotification implements IMNotification {

    IMAddr mFromAddr;
    IMPresence mPresence;
    
    IMPresenceUpdateNotification(IMAddr fromAddr, IMPresence presence)
    {
        mFromAddr = fromAddr;
        mPresence = presence;
    }
    
    public String toString() {
        return new Formatter().format("IMPresenceUpdateEvent: From: %s  Presence: %s", 
                mFromAddr, mPresence.toString()).toString();
    }
    
    public Element toXml(Element parent) {
        ZimbraLog.im.info(this.toString());
        Element toRet = parent.addElement(IMConstants.E_PRESENCE);
        mPresence.toXml(toRet);
        toRet.addAttribute(IMConstants.A_FROM, mFromAddr.getAddr());
        return toRet;
    }
    
}
