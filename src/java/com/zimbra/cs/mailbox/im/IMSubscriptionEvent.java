package com.zimbra.cs.mailbox.im;

import java.util.Formatter;

import com.zimbra.cs.service.ServiceException;

public class IMSubscriptionEvent implements IMEvent {
    
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
        IMRouter.getInstance().onSubscriptionEvent(mFromAddr, mToAddr, mOp);
    }
    
    public String toString() {
        return new Formatter().format("IMSubscriptionEvent: %s --> %s Op=%s", mFromAddr, mToAddr, mOp.toString()).toString();
    }

}
