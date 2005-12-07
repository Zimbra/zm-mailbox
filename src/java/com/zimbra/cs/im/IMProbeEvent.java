package com.zimbra.cs.im;

import java.util.ArrayList;
import java.util.Formatter;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.im.IMService;
import com.zimbra.soap.Element;

public class IMProbeEvent extends IMEvent implements IMNotification {
    
    IMAddr mFromAddr; // send the reply to this person
    
    IMPresence mPresence = null; // for notification, this is set when we get a result
    
    IMProbeEvent(IMAddr fromAddr, IMAddr toAddr) {
        super(toAddr);
        mFromAddr = fromAddr;
    }
    
    public String toString() {
        IMAddr toAddr = mTargets.get(0);
        
        return new Formatter().format("PROBE(%s --> %s)", mFromAddr, toAddr).toString();
    }

    public void run() throws ServiceException {
        IMAddr toAddr = mTargets.get(0);
        
        ArrayList targets = new ArrayList(1);
        targets.add(mFromAddr);
        
        // fetch the presence from the sender
        Object lock = IMRouter.getInstance().getLock(toAddr);
        synchronized (lock) {
            IMPersona persona = IMRouter.getInstance().findPersona(null, toAddr, false);
            mPresence = persona.getMyPresence();
        }
        
        // we can immediately send it to the requestor here, since we are in the
        // event context and know we are holding no other locks
        lock = IMRouter.getInstance().getLock(mFromAddr);
        synchronized (lock) {
            IMPersona persona = IMRouter.getInstance().findPersona(null, mFromAddr, false);

            // yes, FROM the TO addr!
            persona.handlePresenceUpdate(toAddr, mPresence);
            persona.postIMNotification(this);
        }
    }
    
    
    /* (non-Javadoc)
     * @see com.zimbra.cs.im.IMNotification#toXml(com.zimbra.soap.Element)
     */
    public Element toXml(Element parent) {
        Element toRet = parent.addElement(IMService.E_PRESENCE);
        mPresence.toXml(toRet);
        toRet.addAttribute(IMService.A_FROM, mFromAddr.getAddr());
        return toRet;
    }
    
}
