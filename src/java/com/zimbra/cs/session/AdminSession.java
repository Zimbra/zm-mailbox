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
/*
 * Created on Aug 31, 2005
 */
package com.zimbra.cs.session;

import com.zimbra.cs.im.IMNotification;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.Constants;

/** @author dkarp */
public class AdminSession extends Session {

    private static final long ADMIN_SESSION_TIMEOUT_MSEC = 10 * Constants.MILLIS_PER_MINUTE;

    AdminSession(String accountId, String sessionId) throws ServiceException {
        super(accountId, sessionId, SessionCache.SESSION_ADMIN);
    }

    protected long getSessionIdleLifetime() {
        return ADMIN_SESSION_TIMEOUT_MSEC;
    }

    public void notifyPendingChanges(PendingModifications pns) { }
    
    public void notifyIM(IMNotification imn) { }

    protected void cleanup() {
    }
}
