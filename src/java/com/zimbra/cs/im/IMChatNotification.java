/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.2 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import java.util.Formatter;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.IMConstants;

public abstract class IMChatNotification extends IMNotification {
    
    IMAddr mFromAddr;
    String mThreadId;
    
    IMChatNotification(IMAddr from, String threadId) {
        mFromAddr = from;
        mThreadId = threadId;
    }
    

    public String toString() {
        return new Formatter().format("Addr: %s  Thread: %s",
                mFromAddr, mThreadId).toString();
    }

    /* (non-Javadoc)
    * @see com.zimbra.cs.im.IMNotification#toXml(com.zimbra.common.soap.Element)
    */
    public Element toXml(Element parent) {
        parent.addAttribute(IMConstants.A_THREAD_ID, mThreadId);
        parent.addAttribute(IMConstants.A_ADDRESS, mFromAddr.getAddr());
        return parent;
    }
}
