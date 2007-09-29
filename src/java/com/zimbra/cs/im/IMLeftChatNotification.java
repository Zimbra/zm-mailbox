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
import com.zimbra.cs.service.im.IMService;
import com.zimbra.soap.Element;

public class IMLeftChatNotification implements IMNotification {

    IMAddr mFromAddr;
    String mThreadId;
    
    IMLeftChatNotification(IMAddr from, String threadId) {
        mFromAddr = from;
        mThreadId = threadId;
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
