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

public class IMSendMessageEvent extends IMEvent {
    
    IMAddr mFromAddr;
    String mThreadId;
    IMMessage mMessage;
    
    IMSendMessageEvent(IMAddr fromAddr, String threadId, List<IMAddr> targets, IMMessage message) {
        super(targets);
        mFromAddr = fromAddr;
        mThreadId = threadId;
        mMessage = message;
    }
    
    /**
     * @author tim
     *
     * Use an inner class for the notification here since each target mbox's notification
     * has to be different (different sequence ID for each chat participant)
     * 
     */
    public class IMMessageNotification implements IMNotification {
        int mSeqNo;
        
        private IMMessageNotification(int seq) {
            mSeqNo = seq;
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
    }
    
    IMMessageNotification getNotificationEvent(int seqNo) {
        return new IMMessageNotification(seqNo);
    }
    
    protected void handleTarget(IMPersona persona) throws ServiceException {
        int seqNo = persona.handleMessage(mFromAddr, mThreadId, mMessage);
        persona.postIMNotification(new IMMessageNotification(seqNo));
    }
    
    public String toString() {
        return new Formatter().format("IMSendMessageEvent: %s --> thread %s Message=%s", 
                mFromAddr, mThreadId, mMessage.toString()).toString();
    }
    
}
