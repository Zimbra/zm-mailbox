package com.zimbra.cs.im;

import java.util.List;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.im.IMService;
import com.zimbra.soap.Element;

public class IMEnteredChatEvent extends IMEvent implements IMNotification {
    
    private String mThreadId;
    private IMAddr mFromAddr;
    private IMAddr mParam;
    
    IMEnteredChatEvent(IMAddr fromAddr, String threadId, List<IMAddr>targets, IMAddr param) {
        super(targets);
        
        mThreadId = threadId;
        mFromAddr = fromAddr;
        mParam = param;
    }
    
    protected void handleTarget(IMPersona persona) throws ServiceException {
        persona.handleAddChatUser(mThreadId, mParam);
        persona.postIMNotification(this);
    }
    
    public Element toXml(Element parent) throws ServiceException {
        Element toRet = parent.addElement(IMService.E_ENTEREDCHAT);
        toRet.addAttribute(IMService.A_ADDRESS, mParam.getAddr());
        return toRet;
    }

}
