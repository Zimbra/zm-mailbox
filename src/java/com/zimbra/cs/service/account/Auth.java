/*
 * Created on May 26, 2004
 */
package com.liquidsys.coco.service.account;

import java.util.Map;

import com.liquidsys.coco.account.Account;
import com.liquidsys.coco.account.AccountServiceException;
import com.liquidsys.coco.account.AuthToken;
import com.liquidsys.coco.account.AuthTokenException;
import com.liquidsys.coco.account.Provisioning;
import com.liquidsys.coco.service.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.session.Session;
import com.liquidsys.coco.session.SessionCache;
import com.liquidsys.coco.util.LiquidLog;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.LiquidContext;
import com.zimbra.soap.SoapEngine;

/**
 * @author schemers
 */
public class Auth extends DocumentHandler  {

	// FIXME: config, sane value, default to 12 hours for now...
	private static final long DEFAULT_AUTH_LIFETIME = 60*60*12;
	
	public Element handle(Element request, Map context) throws ServiceException {
        LiquidContext lc = getLiquidContext(context);
        context.put(SoapEngine.IS_AUTH_COMMAND, "1");

		String name = request.getAttribute(AccountService.E_ACCOUNT);
		String password = request.getAttribute(AccountService.E_PASSWORD);
        Provisioning prov = Provisioning.getInstance();
		Account acct = prov.getAccountByName(name);
		if (acct == null)
			throw AccountServiceException.AUTH_FAILED(name);

		try {
		    prov.authAccount(acct, password);
            LiquidLog.security.info(LiquidLog.encodeAttrs(
                    new String[] {"cmd", "Auth","account", name}));
        } catch (ServiceException se) {
            LiquidLog.security.warn(LiquidLog.encodeAttrs(
                    new String[] {"cmd", "Auth","account", name, "error", se.getMessage()}));             
            throw se;
        }

        boolean isAdmin = "TRUE".equals(acct.getAttr(Provisioning.A_liquidIsAdminAccount));

        long lifetime = isAdmin ?
                acct.getTimeInterval(Provisioning.A_liquidAdminAuthTokenLifetime, DEFAULT_AUTH_LIFETIME*1000) :                                    
                acct.getTimeInterval(Provisioning.A_liquidAuthTokenLifetime, DEFAULT_AUTH_LIFETIME*1000);
                    
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
            if (context.get(SoapEngine.DONT_CREATE_SESSION) == null) {
                Session session = SessionCache.getInstance().getNewSession(acct.getId() + "", SessionCache.SESSION_SOAP);
                response.addAttribute(LiquidContext.E_SESSION_ID, session.getSessionId().toString(), Element.DISP_CONTENT);
            }
        } else
            response.addAttribute(AccountService.E_REFERRAL, acct.getAttr(Provisioning.A_liquidMailHost), Element.DISP_CONTENT);

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
