package com.zimbra.cs.im;

import org.xmpp.packet.Packet;

import com.zimbra.cs.service.ServiceException;

public class IMXmppEvent extends IMEvent {

    Packet mPacket;
    
    IMXmppEvent(IMAddr target, Packet packet) {
        super(target);
        mPacket = packet;
    }
    
    protected void handleTarget(IMPersona persona) throws ServiceException {
        persona.process(mPacket);
    }
    
}
