package com.zimbra.cs.mailbox.im;

import java.util.Formatter;
import java.util.List;

import com.zimbra.cs.service.ServiceException;

public class IMSendMessageEvent implements IMEvent {
    
    IMAddr mFromAddr;
    String mThreadId;
    List<IMAddr> mTargets;
    IMMessage mMessage;
    
    IMSendMessageEvent(IMAddr fromAddr, String threadId, List<IMAddr> targets, IMMessage message) {
        mFromAddr = fromAddr;
        mThreadId = threadId;
        mTargets = targets;
        mMessage = message;
    }

    public void run() throws ServiceException {
        IMRouter.getInstance().onNewMessage(mFromAddr, mThreadId, mTargets, mMessage);
    }
    
    public String toString() {
        return new Formatter().format("IMSendMessageEvent: %s --> thread %s Message=%s", 
                mFromAddr, mThreadId, mMessage.toString()).toString();
    }
    

}
