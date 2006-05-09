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

import java.util.ArrayList;
import java.util.Formatter;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.im.IMService;
import com.zimbra.soap.Element;

public class IMProbeEvent extends IMEvent implements IMNotification {
    
    IMAddr mFromAddr; // send the reply to this person
    
    IMPresence mPresence = null; // for notification, this is set when we get a result
    
    IMProbeEvent(IMAddr fromAddr, IMAddr toAddr) {
        super(toAddr);
        mFromAddr = fromAddr;
    }
    
    public String toString() {
        IMAddr toAddr = mTargets.get(0);
        
        return new Formatter().format("PROBE(%s --> %s)", mFromAddr, toAddr).toString();
    }

    public void run() throws ServiceException {
        IMAddr toAddr = mTargets.get(0);
        
        ArrayList targets = new ArrayList(1);
        targets.add(mFromAddr);
        
        // fetch the presence from the sender
        Object lock = IMRouter.getInstance().getLock(toAddr);
        synchronized (lock) {
            IMPersona persona = IMRouter.getInstance().findPersona(null, toAddr, false);
            mPresence = persona.getEffectivePresence();
        }
        
        // we can immediately send it to the requestor here, since we are in the
        // event context and know we are holding no other locks
        lock = IMRouter.getInstance().getLock(mFromAddr);
        synchronized (lock) {
            IMPersona persona = IMRouter.getInstance().findPersona(null, mFromAddr, false);

            // yes, FROM the TO addr!
            persona.handlePresenceUpdate(toAddr, mPresence);
            persona.postIMNotification(this);
        }
    }
    
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.im.IMNotification#toXml(com.zimbra.soap.Element)
     */
    public Element toXml(Element parent) {
        Element toRet = parent.addElement(IMService.E_PRESENCE);
        mPresence.toXml(toRet);
        toRet.addAttribute(IMService.A_FROM, mFromAddr.getAddr());
        return toRet;
    }
    
}
