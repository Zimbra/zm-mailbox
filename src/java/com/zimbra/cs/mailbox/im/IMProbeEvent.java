package com.zimbra.cs.mailbox.im;

import java.util.ArrayList;
import java.util.Formatter;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;

public class IMProbeEvent implements IMEvent {
    
    IMAddr mFromAddr; // send the reply to this person
    IMAddr mToAddr; // person who's presence we want
    
    IMProbeEvent(IMAddr fromAddr, IMAddr toAddr) {
        mFromAddr = fromAddr;
        mToAddr = toAddr;
    }
    
    public String toString() {
        return new Formatter().format("PROBE(%s --> %s)", mFromAddr, mToAddr).toString();
    }

    public void run() throws ServiceException {
        Mailbox mbox = IMRouter.getInstance().getMailboxFromAddr(mToAddr);
        ArrayList targets = new ArrayList(1);
        targets.add(mFromAddr);
        
        IMPresence presence;
        synchronized (mbox) {
            IMPersona persona = IMRouter.getInstance().findPersona(null, mbox, false);
            presence = persona.getMyPresence();
        }
            
        IMRouter.getInstance().onPresenceUpdate(mToAddr, presence, targets);
    }

}
