package com.zimbra.cs.mailbox.im;

import com.zimbra.cs.service.ServiceException;

public interface IMEvent {
    
    /**
     * Asynchronous IM event which gets run by the IM Router thread.
     * 
     * To avoid deadlocks, Events MUST NOT hold more than one mailbox lock
     * at any time. 
     * 
     * @throws ServiceException
     */
    void run() throws ServiceException;
}
