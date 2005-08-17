/*
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.session.SoapSession;
import com.zimbra.cs.session.SessionCache;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.ZimbraContext;
import com.zimbra.soap.SoapEngine;

/**
 * @author schemers
 */
public class Auth extends AdminDocumentHandler {

	private static final long DEFAULT_AUTH_LIFETIME = 60*60*10;

	public Element handle(Element request, Map context) throws ServiceException {
		context.put(SoapEngine.IS_AUTH_COMMAND, "1");
        ZimbraContext lc = getZimbraContext(context);

        String name = request.getAttribute(AdminService.E_NAME);
		String password = request.getAttribute(AdminService.E_PASSWORD);
		Provisioning prov = Provisioning.getInstance();
		Account acct = null;
        if (name.indexOf("@") == -1) {
            acct = prov.getAdminAccountByName(name);
        } else {
            acct = prov.getAccountByName(name);
            if (acct != null && !acct.getBooleanAttr(Provisioning.A_liquidIsAdminAccount, false)) {
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
        long lifetime = acct.getTimeInterval(Provisioning.A_liquidAdminAuthTokenLifetime, DEFAULT_AUTH_LIFETIME*1000);
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
        if (context.get(SoapEngine.DONT_CREATE_SESSION) == null) {
            SoapSession session = (SoapSession) SessionCache.getInstance().getNewSession(acct.getId(), SessionCache.SESSION_SOAP);
            response.addAttribute(ZimbraContext.E_SESSION_ID, session.getSessionId().toString(), Element.DISP_CONTENT);
        }
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
