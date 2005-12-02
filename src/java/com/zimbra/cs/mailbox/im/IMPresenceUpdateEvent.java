package com.zimbra.cs.mailbox.im;

import java.util.Formatter;
import java.util.List;

import com.zimbra.cs.service.ServiceException;

public class IMPresenceUpdateEvent implements IMEvent {

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
        IMRouter.getInstance().onPresenceUpdate(mFromAddr, mPresence, mToAddrs);
    }

    public String toString() {
        return new Formatter().format("IMPresenceUpdateEvent: From: %s  Presence: %s", 
                mFromAddr, mPresence.toString()).toString();
    }
    
}
