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
package com.zimbra.cs.service.account;

import java.util.Map;

import com.zimbra.cs.account.*;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.Session;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

/**
 * @author schemers
 */
public class Auth extends DocumentHandler  {

	// FIXME: config, sane value, default to 12 hours for now...
	private static final long DEFAULT_AUTH_LIFETIME = 60*60*12;

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

		String name = request.getAttribute(AccountService.E_ACCOUNT);
		String password = request.getAttribute(AccountService.E_PASSWORD);
        Provisioning prov = Provisioning.getInstance();
		Account acct = prov.getAccountByName(name);
		if (acct == null)
			throw AccountServiceException.AUTH_FAILED(name);

		try {
		    prov.authAccount(acct, password);
            ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "Auth","account", name}));
        } catch (ServiceException se) {
            ZimbraLog.security.warn(ZimbraLog.encodeAttrs(
                    new String[] {"cmd", "Auth","account", name, "error", se.getMessage()}));             
            throw se;
        }

        boolean isAdmin = "TRUE".equals(acct.getAttr(Provisioning.A_zimbraIsAdminAccount));

        long lifetime = isAdmin ?
                acct.getTimeInterval(Provisioning.A_zimbraAdminAuthTokenLifetime, DEFAULT_AUTH_LIFETIME*1000) :                                    
                acct.getTimeInterval(Provisioning.A_zimbraAuthTokenLifetime, DEFAULT_AUTH_LIFETIME*1000);
                    
        long expires = System.currentTimeMillis()+ lifetime;

        AuthToken at = new AuthToken(acct, expires, isAdmin, null);
        String token;
        try {
            token = at.getEncoded();
        } catch (AuthTokenException e) {
            throw  ServiceException.FAILURE("unable to encode auth token", e);
        }

        Element response = lc.createElement(AccountService.AUTH_RESPONSE);
        response.addAttribute(AccountService.E_AUTH_TOKEN, token, Element.DISP_CONTENT);
        response.addAttribute(AccountService.E_LIFETIME, lifetime, Element.DISP_CONTENT);
        if (acct.isCorrectHost()) {
            Session session = lc.getNewSession(acct.getId(), SessionCache.SESSION_SOAP);
            if (session != null)
                ZimbraContext.encodeSession(response, session, true);
        } else
            response.addAttribute(AccountService.E_REFERRAL, acct.getAttr(Provisioning.A_zimbraMailHost), Element.DISP_CONTENT);

		Element prefsRequest = request.getOptionalElement(AccountService.E_PREFS);
		if (prefsRequest != null) {
			Element prefsResponse = response.addElement(AccountService.E_PREFS);
			GetPrefs.handle(request, prefsResponse, acct);
		}
		return response;
	}

	public boolean needsAuth(Map context) {
		return false;
	}
}
