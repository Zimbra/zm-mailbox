package com.zimbra.cs.im;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;

public abstract class IMEvent {
    
    protected List<IMAddr> mTargets;
    
    protected IMEvent(IMAddr target) {
        mTargets = new ArrayList(1);
        mTargets.add(target);
    }
    
    protected IMEvent(List<IMAddr> target) {
        mTargets = target;
    }
    
    /**
     * Asynchronous IM event which gets run by the IM Router thread.
     * 
     * To avoid deadlocks, Events MUST NOT hold more than one mailbox lock
     * at any time. 
     * 
     * @throws ServiceException
     */
    void run() throws ServiceException {
        for (IMAddr addr : mTargets) {
            try {
                Object lock = IMRouter.getInstance().getLock(addr);
                synchronized (lock) {
                    IMPersona persona = IMRouter.getInstance().findPersona(null, addr, false);
                    handleTarget(persona);
                }
            } catch (Exception e) {
                ZimbraLog.im.debug("Caught exception running event: "+this+" except="+e);
                e.printStackTrace();
            }
        }
    }
    
    protected void handleTarget(IMPersona persona) throws ServiceException {}
}
