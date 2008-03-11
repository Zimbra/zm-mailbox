/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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

import org.xmpp.packet.PacketError;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.IMConstants;

/**
 * 
 */
public class IMBaseMessageNotification extends IMNotification {
    
    private String mFromAddr;
    private String mThreadId;
    private long mTimestamp;
    private boolean mTyping;
    private PacketError.Condition mErrorCondition = null; 

    public IMBaseMessageNotification(String fromAddr, String threadId, boolean typing, long timestamp) {
        mFromAddr = fromAddr;
        mThreadId = threadId;
        mTyping = typing;
        mTimestamp = timestamp;
    }
    
    public IMBaseMessageNotification(String fromAddr, String threadId, boolean typing, long timestamp, PacketError.Condition errorCondition) {
        mFromAddr = fromAddr;
        mThreadId = threadId;
        mTyping = typing;
        mTimestamp = timestamp;
        mErrorCondition = errorCondition;
    }
    
    
    /* @see com.zimbra.cs.im.IMNotification#toXml(com.zimbra.common.soap.Element) */
    @Override
    public Element toXml(Element parent) throws ServiceException {
        Element e = create(parent, IMConstants.E_MESSAGE);
        e.addAttribute(IMConstants.A_FROM, mFromAddr);
        e.addAttribute(IMConstants.A_THREAD_ID, mThreadId);
        if (mTyping)
            e.addElement(IMConstants.E_TYPING);
        e.addAttribute(IMConstants.A_TIMESTAMP, mTimestamp);
        if (mErrorCondition != null) {
            switch(mErrorCondition) {
                case recipient_unavailable:
                    e.addAttribute(IMConstants.A_ERROR, PacketError.Condition.recipient_unavailable.name());
                    break;
            }
        }
        return e;
    }

    public final String getFromAddr() { return mFromAddr; };
    public final String getThreadId() { return mThreadId; }
    public final boolean isTyping() { return mTyping; }
    public final long getTimestamp() { return mTimestamp; }
}
