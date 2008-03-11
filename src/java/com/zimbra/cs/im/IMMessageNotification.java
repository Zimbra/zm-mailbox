/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import java.util.Formatter;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.IMConstants;
import com.zimbra.common.soap.Element;

public class IMMessageNotification extends IMBaseMessageNotification {
    
    private IMMessage mMessage;
    private int mSeqNo;
    private String mToAddr;
    
    public IMMessageNotification(IMAddr fromAddr, String threadId, IMMessage message, int seqNo) {
        super(fromAddr.toString(), threadId, message.isTyping(), message.getTimestamp());
        mMessage = message;
        try { mToAddr = message.getTo().toString(); } catch (Exception e) {} // why in exception? figure out and comment me!  
    }
    
    public Element toXml(Element parent) throws ServiceException {
        Element e = super.toXml(parent);
        if (mToAddr != null) 
            e.addAttribute(IMConstants.A_TO, mToAddr);
        e.addAttribute(IMConstants.A_SEQ, mSeqNo);
        mMessage.toXml(e);
        return e;
    }
    
    public String toString() {
        return new Formatter().format("IMSendMessageEvent: %s --> thread %s Message=%s", 
                getFromAddr(), getThreadId(), mMessage.toString()).toString();
    }
    
    public final String getToAddr() { return mToAddr; }
}
