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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s):
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.session;

import com.zimbra.common.service.ServiceException;

/**
 * 
 */
public class WaitSetSession extends Session {
    
    WaitSetSession(String accountId, String sessionId) throws ServiceException {
        super(accountId, sessionId, Session.Type.WAITSET);
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.session.Session#cleanup()
     */
    @Override
    protected void cleanup() {
    // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.session.Session#getSessionIdleLifetime()
     */
    @Override
    protected long getSessionIdleLifetime() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see com.zimbra.cs.session.Session#notifyPendingChanges(com.zimbra.cs.session.PendingModifications)
     */
    @Override
    public void notifyPendingChanges(PendingModifications pns) {
    // TODO Auto-generated method stub

    }

}
