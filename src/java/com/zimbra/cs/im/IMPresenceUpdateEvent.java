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
import java.util.List;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.im.IMService;
import com.zimbra.soap.Element;

public class IMPresenceUpdateEvent extends IMEvent implements IMNotification {

    IMAddr mFromAddr;
    IMPresence mPresence;
    
    IMPresenceUpdateEvent(IMAddr fromAddr, IMPresence presence, List<IMAddr> toAddrs)
    {
        super(toAddrs);
        mFromAddr = fromAddr;
        mPresence = presence;
    }
    
    protected void handleTarget(IMPersona persona) throws ServiceException {
        persona.handlePresenceUpdate(mFromAddr, mPresence);
        persona.postIMNotification(this);
    }
    

    public String toString() {
        return new Formatter().format("IMPresenceUpdateEvent: From: %s  Presence: %s", 
                mFromAddr, mPresence.toString()).toString();
    }
    
    public Element toXml(Element parent) {
        Element toRet = parent.addElement(IMService.E_PRESENCE); 
        mPresence.toXml(toRet);
        toRet.addAttribute(IMService.A_FROM, mFromAddr.getAddr());
        return toRet;
    }
    
}
