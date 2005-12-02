package com.zimbra.cs.mailbox.im;

import java.util.Formatter;
import java.util.List;

import com.zimbra.cs.service.ServiceException;

public class IMLeftChatEvent implements IMEvent {

    IMAddr mFrom;
    String mThreadId;
    List<IMAddr> mTargets;
    
    IMLeftChatEvent(IMAddr from, String threadId, List<IMAddr> targets) {
        mFrom = from;
        mThreadId = threadId;
        mTargets = targets;
    }
            
    public void run() throws ServiceException {
        IMRouter.getInstance().onCloseChat(mFrom, mThreadId, mTargets);
    }
    
    public String toString() {
        return new Formatter().format("IMLeftChatEvent: From: %s  Thread: %s", 
                mFrom, mThreadId).toString();
    }
}
