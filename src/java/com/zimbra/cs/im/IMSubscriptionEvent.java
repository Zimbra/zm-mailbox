package com.zimbra.cs.im;

import java.util.Formatter;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.im.IMService;
import com.zimbra.soap.Element;

public class IMSubscriptionEvent extends IMEvent implements IMNotification {
    
    static enum Op {
        SUBSCRIBE, UNSUBSCRIBE;
    }
    
    IMAddr mFromAddr; // person who RECEIVES the presence updates
    //IMAddr mToAddr; // person who SENDS the presence updates
    Op mOp;
    
    IMSubscriptionEvent(IMAddr fromAddr, IMAddr toAddr, Op op) {
        super(toAddr);
        mFromAddr = fromAddr;
        mOp = op;
    }
    
    protected void handleTarget(IMPersona persona) throws ServiceException {
        switch (mOp) {
        case SUBSCRIBE:
            persona.handleAddIncomingSubscription(mFromAddr);
            persona.postIMNotification(this);
            break;
        case UNSUBSCRIBE:
            persona.handleRemoveIncomingSubscription(mFromAddr);
            break;
        }
    }
    
    public String toString() {
        IMAddr toAddr = mTargets.get(0);
        return new Formatter().format("IMSubscriptionEvent: %s --> %s Op=%s", mFromAddr, toAddr, mOp.toString()).toString();
    }
    
    public Element toXml(Element parent) {
        if (mOp == Op.SUBSCRIBE) {
            Element toRet = parent.addElement(IMService.E_SUBSCRIBE);
            toRet.addAttribute(IMService.A_FROM, mFromAddr.getAddr());
            return toRet;
        } else 
            return parent;
    }
}
