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
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.*;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class Auth extends AdminDocumentHandler {

	private static final long DEFAULT_AUTH_LIFETIME = 60*60*10;

    /** Returns (or creates) the in-memory {@link Session} object appropriate
     *  for this request.<p>
     * 
     *  Auth commands do not create a session by default, as issues with the 
     *  ordering of operations might cause the new session to be for the old
     *  credentials rather than for the new ones.
     * 
     * @return <code>null</code> in all cases */
    public Session getSession(Map context) {
        return null;
    }

	public Element handle(Element request, Map context) throws ServiceException {
        ZimbraContext lc = getZimbraContext(context);

        String name = request.getAttribute(AdminService.E_NAME);
		String password = request.getAttribute(AdminService.E_PASSWORD);
		Provisioning prov = Provisioning.getInstance();
		Account acct = null;
        if (name.indexOf("@") == -1) {
            acct = prov.getAdminAccountByName(name);
        } else {
            acct = prov.getAccountByName(name);
            if (acct != null && !acct.getBooleanAttr(Provisioning.A_zimbraIsAdminAccount, false)) {
                throw ServiceException.PERM_DENIED("not an admin account");
            }
        }

        if (acct == null)
			throw AccountServiceException.AUTH_FAILED(name);
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "AdminAuth","account", name})); 

        try {
            prov.authAccount(acct, password);
        } catch (ServiceException se) {
            ZimbraLog.security.warn(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "AdminAuth","account", name, "error", se.getMessage()}));             
            throw se;
        }

        Element response = lc.createElement(AdminService.AUTH_RESPONSE);
        long lifetime = acct.getTimeInterval(Provisioning.A_zimbraAdminAuthTokenLifetime, DEFAULT_AUTH_LIFETIME*1000);
        long expires = System.currentTimeMillis()+ lifetime;
        String token;
        AuthToken at = new AuthToken(acct, expires, true, null);
        try {
            token = at.getEncoded();
        } catch (AuthTokenException e) {
            throw  ServiceException.FAILURE("unable to encode auth token", e);
        }
        response.addAttribute(AdminService.E_AUTH_TOKEN, token, Element.DISP_CONTENT);
        response.addAttribute(AdminService.E_LIFETIME, lifetime, Element.DISP_CONTENT);
        Session session = lc.getNewSession(acct.getId(), SessionCache.SESSION_ADMIN);
        if (session != null)
            response.addAttribute(ZimbraContext.E_SESSION_ID, session.getSessionId(), Element.DISP_CONTENT);
		return response;
	}

    public boolean needsAuth(Map context) {
        // can't require auth on auth request
        return false;
    }

    public boolean needsAdminAuth(Map context) {
        // can't require auth on auth request
        return false;
    }
}
