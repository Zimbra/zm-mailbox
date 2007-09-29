/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import java.util.Formatter;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.service.im.IMService;
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
        Element toRet = parent.addElement(IMService.E_PRESENCE); 
        mPresence.toXml(toRet);
        toRet.addAttribute(IMService.A_FROM, mFromAddr.getAddr());
        return toRet;
    }
    
}
