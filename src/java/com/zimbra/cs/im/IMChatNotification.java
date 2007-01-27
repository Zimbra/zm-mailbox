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
