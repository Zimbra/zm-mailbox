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

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.common.soap.Element;

public class IMMessageNotification implements IMNotification {
    
    IMAddr mFromAddr;
    String mThreadId;
    IMMessage mMessage;
    int mSeqNo;
    
    public IMMessageNotification(IMAddr fromAddr, String threadId, IMMessage message, int seqNo) {
        mFromAddr = fromAddr;
        mThreadId = threadId;
        mMessage = message;
    }
    
    public Element toXml(Element parent) throws ServiceException {
        Element e = parent.addElement(IMConstants.E_MESSAGE);
        e.addAttribute(IMConstants.A_FROM, mFromAddr.toString());
        e.addAttribute(IMConstants.A_THREAD_ID, mThreadId);
        e.addAttribute(IMConstants.A_TIMESTAMP, mMessage.getTimestamp());
        
        e.addAttribute(IMConstants.A_SEQ, mSeqNo);
        
        mMessage.toXml(e);
        return e;
    }
    
    public String toString() {
        return new Formatter().format("IMSendMessageEvent: %s --> thread %s Message=%s", 
                mFromAddr, mThreadId, mMessage.toString()).toString();
    }
    
}
