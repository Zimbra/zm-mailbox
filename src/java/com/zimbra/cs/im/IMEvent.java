/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.im;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;

public abstract class IMEvent implements Runnable {
    
    protected List<IMAddr> mTargets;
    
    protected IMEvent(IMAddr target) {
        mTargets = new ArrayList<IMAddr>(1);
        mTargets.add(target);
    }
    
    /**
     * Asynchronous IM event which gets run by the IM Router thread.
     * 
     * To avoid deadlocks, Events MUST NOT hold more than one mailbox lock
     * at any time. 
     * 
     * @throws ServiceException
     */
    public void run() {
        for (IMAddr addr : mTargets) {
            try {
                if (addr.getAddr().indexOf('@') > 0) {
                    IMPersona persona  = IMRouter.getInstance().findPersona(null, addr);
                    if (persona != null) {
                        synchronized (persona.getLock()) {
                            handleTarget(persona);
                        }
                    } else {
                        ZimbraLog.im.debug("Ignoring IMEvent for "+addr.toString()+" (could not find Mailbox): "+
                                    this.toString());
                    }
                } else {
                    ZimbraLog.im.debug("Ignoring IMEvent for "+addr.toString()+" (addr has no domain): "+
                                this.toString());
                }
            } catch (Exception e) {
                ZimbraLog.im.debug("Caught exception running event: "+this+" except="+e);
                e.printStackTrace();
            }
        }
    }
        
    protected void handleTarget(IMPersona persona) throws ServiceException {}
}
