/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
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
