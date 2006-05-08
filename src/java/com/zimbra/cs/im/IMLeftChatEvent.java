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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
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

public class IMLeftChatEvent extends IMEvent implements IMNotification {

    IMAddr mFromAddr;
    String mThreadId;
    
    IMLeftChatEvent(IMAddr from, String threadId, List<IMAddr> targets) {
        super(targets);
        mFromAddr = from;
        mThreadId = threadId;
    }
            
    /* (non-Javadoc)
     * @see com.zimbra.cs.im.IMEvent#handleTarget(com.zimbra.cs.im.IMPersona)
     */
    protected void handleTarget(IMPersona persona) throws ServiceException {
        persona.handleLeftChat(mFromAddr, mThreadId);
        persona.postIMNotification(this);
    }
        
    public String toString() {
        return new Formatter().format("IMLeftChatEvent: From: %s  Thread: %s", 
                mFromAddr, mThreadId).toString();
    }
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.im.IMNotification#toXml(com.zimbra.soap.Element)
     */
    public Element toXml(Element parent) {
        Element toRet = parent.addElement(IMService.E_LEFTCHAT);
        toRet.addAttribute(IMService.A_THREAD_ID, mThreadId);
        toRet.addAttribute(IMService.A_ADDRESS, mFromAddr.getAddr());
        return toRet;
    }
}
