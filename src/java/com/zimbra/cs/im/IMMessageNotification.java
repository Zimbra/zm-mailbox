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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.service.im.IMService;
import com.zimbra.soap.Element;

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
        Element e = parent.addElement(IMService.E_MESSAGE);
        e.addAttribute(IMService.A_FROM, mFromAddr.toString());
        e.addAttribute(IMService.A_THREAD_ID, mThreadId);
        e.addAttribute(IMService.A_TIMESTAMP, mMessage.getTimestamp());
        
        e.addAttribute(IMService.A_SEQ, mSeqNo);
        
        mMessage.toXml(e);
        return e;
    }
    
    public String toString() {
        return new Formatter().format("IMSendMessageEvent: %s --> thread %s Message=%s", 
                mFromAddr, mThreadId, mMessage.toString()).toString();
    }
    
}
