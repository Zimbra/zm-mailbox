package com.zimbra.cs.im;

import com.zimbra.cs.service.ServiceException;

public class IMXmppTextEvent extends IMEvent {
    
    String mString;
    
    IMXmppTextEvent(IMAddr target, String text) {
        super(target);
        mString = text;
    }
    
    protected void handleTarget(IMPersona persona) throws ServiceException {
        persona.process(mString);
    }
}
