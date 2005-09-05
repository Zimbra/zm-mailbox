/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
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
 * Created on May 26, 2004
 */
package com.zimbra.soap;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.util.ZimbraLog;

/**
 * @author schemers
 */
public abstract class DocumentHandler {

    public abstract Element handle(Element request, Map context) throws ServiceException;

	public ZimbraContext getZimbraContext(Map context) {
		return (ZimbraContext) context.get(SoapEngine.ZIMBRA_CONTEXT);
	}

    public Account getRequestedAccount(ZimbraContext lc) throws ServiceException {
        String id = lc.getRequestedAccountId();

        Account acct = Provisioning.getInstance().getAccountById(id);
        if (acct == null)
            throw ServiceException.AUTH_EXPIRED();
        return acct;
    }

    public Mailbox getRequestedMailbox(ZimbraContext lc) throws ServiceException {
        String id = lc.getRequestedAccountId();
        Mailbox mbx = Mailbox.getMailboxByAccountId(id);
        if (mbx != null)
            ZimbraLog.addToContext(mbx);
	    return mbx; 
    }

	/** by default, document handlers require a valid auth token */
	public boolean needsAuth(Map context) {
		return true;
	}

	/** @return <code>true</code> if this is an administrative command */
	public boolean needsAdminAuth(Map context) {
		return false;
	}

    /** @return <code>true</code> if the operation is read-only, or
     *          <code>false</code> if the operation causes backend state change. */
    public boolean isReadOnly() {
    	return true;
    }

    /** @return Whether the client making the SOAP request is localhost. */
    protected boolean clientIsLocal(Map context) {
        HttpServletRequest req = (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST);
        if (req == null) return true;
        String peerIP = req.getRemoteAddr();
        return "127.0.0.1".equals(peerIP);
    }

    /** Fetches the in-memory {@link Session} object appropriate for this request.
     *  If none already exists, one is created.
     * 
     * @param context  The Map containing context information for this SOAP request.
     * @return A {@link com.zimbra.cs.session.SoapSession}. */
    public Session getSession(Map context) {
        return getSession(context, SessionCache.SESSION_SOAP);
    }

    /** Fetches or creates a {@link Session} object to persist and manage state
     *  between SOAP requests.
     * 
     * @param context      The Map containing context information for this SOAP request.
     * @param sessionType  The type of session needed.
     * @return An in-memory {@link Session} object of the specified type, fetched
     *         from the request's {@link ZimbraContext} object.  If no matching
     *         session already exists, a new one is created.
     * @see SessionCache SessionCache for valid values for <code>sessionType</code>. */
    protected Session getSession(Map context, int sessionType) {
        ZimbraContext lc = getZimbraContext(context);
        return (lc == null ? null : lc.getSession(sessionType));
    }
}
