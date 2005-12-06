package com.zimbra.cs.im;

import java.util.Formatter;
import java.util.List;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.im.IMService;
import com.zimbra.soap.Element;

public class IMPresenceUpdateEvent extends IMEvent implements IMNotification {

    IMAddr mFromAddr;
    IMPresence mPresence;
    
    IMPresenceUpdateEvent(IMAddr fromAddr, IMPresence presence, List<IMAddr> toAddrs)
    {
        super(toAddrs);
        mFromAddr = fromAddr;
        mPresence = presence;
    }
    
    protected void handleTarget(IMPersona persona) throws ServiceException {
        persona.handlePresenceUpdate(mFromAddr, mPresence);
        persona.postIMNotification(this);
    }
    

    public String toString() {
        return new Formatter().format("IMPresenceUpdateEvent: From: %s  Presence: %s", 
                mFromAddr, mPresence.toString()).toString();
    }
    
    public Element toXml(Element parent) {
        Element toRet = mPresence.toXml(parent);
        toRet.addAttribute(IMService.A_FROM, mFromAddr.getAddr());
        return toRet;
    }
    
}
