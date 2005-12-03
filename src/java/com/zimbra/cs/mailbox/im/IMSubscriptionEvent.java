package com.zimbra.cs.mailbox.im;

import java.util.Formatter;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.soap.Element;

public class IMSubscriptionEvent implements IMEvent, IMNotification {
    
    static enum Op {
        SUBSCRIBE, UNSUBSCRIBE;
    }
    
    IMAddr mFromAddr; // person who RECEIVES the presence updates
    IMAddr mToAddr; // person who SENDS the presence updates
    Op mOp;
    
    IMSubscriptionEvent(IMAddr fromAddr, IMAddr toAddr, Op op) {
        mFromAddr = fromAddr;
        mToAddr = toAddr;
        mOp = op;
    }

    public void run() throws ServiceException {
        Mailbox mbox = IMRouter.getInstance().getMailboxFromAddr(mToAddr);
        synchronized (mbox) {
            IMPersona persona = IMRouter.getInstance().findPersona(null, mbox, false);
            switch (mOp) {
            case SUBSCRIBE:
                persona.handleAddIncomingSubscription(mFromAddr);
                break;
            case UNSUBSCRIBE:
                persona.handleRemoveIncomingSubscription(mFromAddr);
                break;
            }
        }
    }
    
    public String toString() {
        return new Formatter().format("IMSubscriptionEvent: %s --> %s Op=%s", mFromAddr, mToAddr, mOp.toString()).toString();
    }
    
    public Element toXml(Element parent) {
        if (mOp == Op.SUBSCRIBE) {
            Element toRet = parent.addElement("subscribe");
            toRet.addAttribute("from", mFromAddr.getAddr());
            return toRet;
        } else 
            return parent;
    }
}
