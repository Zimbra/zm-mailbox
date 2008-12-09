package com.zimbra.cs.im;

import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.IMConstants;

public class IMChatCloseNotification extends IMNotification {
    String mThreadId;
    
    IMChatCloseNotification(String threadId) {
        mThreadId = threadId;
    }
    
    public Element toXml(Element parent) {
        Element elt = create(parent, "chatclosed");
        elt.addAttribute(IMConstants.A_THREAD_ID, mThreadId);
        return elt;
    }

}
