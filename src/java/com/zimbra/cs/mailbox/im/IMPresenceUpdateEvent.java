package com.zimbra.cs.mailbox.im;

import java.util.Formatter;
import java.util.List;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;

public class IMPresenceUpdateEvent implements IMEvent, IMNotification {

    IMAddr mFromAddr;
    IMPresence mPresence;
    List<IMAddr> mToAddrs;
    
    IMPresenceUpdateEvent(IMAddr fromAddr, IMPresence presence, List<IMAddr> toAddrs)
    {
        mFromAddr = fromAddr;
        mPresence = presence;
        mToAddrs = toAddrs;
    }
    
    public void run() throws ServiceException {
        for (IMAddr addr : mToAddrs) {
            Mailbox mbox = IMRouter.getInstance().getMailboxFromAddr(addr);
            synchronized (mbox) {
                IMPersona persona = IMRouter.getInstance().findPersona(null, mbox, false);
                persona.handlePresenceUpdate(mFromAddr, mPresence);
                mbox.postIMNotification(this);
            }
        }
    }

    public String toString() {
        return new Formatter().format("IMPresenceUpdateEvent: From: %s  Presence: %s", 
                mFromAddr, mPresence.toString()).toString();
    }
    
    public Element toXml(Element parent) {
        Element toRet = mPresence.toXml(parent);
        toRet.addAttribute("from", mFromAddr.getAddr());
        return toRet;
    }
    
}
