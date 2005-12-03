package com.zimbra.cs.mailbox.im;

import java.util.ArrayList;
import java.util.Formatter;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;

public class IMProbeEvent implements IMEvent, IMNotification {
    
    IMAddr mFromAddr; // send the reply to this person
    IMAddr mToAddr; // person who's presence we want
    
    IMPresence mPresence = null; // for notification, this is set when we get a result
    
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
        
        // fetch the presence from the sender
        synchronized (mbox) {
            IMPersona persona = IMRouter.getInstance().findPersona(null, mbox, false);
            mPresence = persona.getMyPresence();
        }
        
        // we can immediately send it to the requestor here, since we are in the
        // event context and know we are holding no other locks
        mbox = IMRouter.getInstance().getMailboxFromAddr(mFromAddr);
        synchronized (mbox) {
            IMPersona persona = IMRouter.getInstance().findPersona(null, mbox, false);

            // yes, FROM the TO addr!
            persona.handlePresenceUpdate(mToAddr, mPresence);
            
            mbox.postIMNotification(this);
        }
    }
    
    
    public Element toXml(Element parent) {
        Element toRet = mPresence.toXml(parent);
        toRet.addAttribute("from", mFromAddr.getAddr());
        return toRet;
    }
    
}
