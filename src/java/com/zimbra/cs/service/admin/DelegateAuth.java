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
 * Created on May 26, 2004
 */
package com.zimbra.cs.service.admin;

import java.util.Map;

import com.zimbra.cs.account.AccountServiceException;
import com.zimbra.cs.account.AuthToken;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.AuthTokenException;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

/**
 * @author schemers
 */
public class DelegateAuth extends AdminDocumentHandler {

    // default is one hour
	private static final long DEFAULT_AUTH_LIFETIME = 60*60*1;

    public static final String BY_NAME = "name";
    public static final String BY_ID = "id";

	public Element handle(Element request, Map context) throws ServiceException {
        ZimbraSoapContext lc = getZimbraSoapContext(context);

        Element a = request.getElement(AdminService.E_ACCOUNT);
        String key = a.getAttribute(AdminService.A_BY);
        String value = a.getText();

        long lifetime = request.getAttributeLong(AdminService.A_DURATION, DEFAULT_AUTH_LIFETIME) * 1000;
        
		Provisioning prov = Provisioning.getInstance();
        
        Account account = null;

        if (key.equals(BY_NAME)) {
            account = prov.getAccountByName(value);
        } else if (key.equals(BY_ID)) {
            account = prov.getAccountById(value);
        } else {
            throw ServiceException.INVALID_REQUEST("unknown value for by: "+key, null);
        }

        if (account == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(value);
        
        ZimbraLog.security.info(ZimbraLog.encodeAttrs(
                new String[] {"cmd", "DelegateAuth","accountId", account.getId()})); 

        Element response = lc.createElement(AdminService.DELEGATE_AUTH_RESPONSE);
        long maxLifetime = account.getTimeInterval(Provisioning.A_zimbraAuthTokenLifetime, DEFAULT_AUTH_LIFETIME*1000); 

        // take the min of requested lifetime vs maxLifetime
        long expires = System.currentTimeMillis()+ Math.min(lifetime, maxLifetime);
        String token;
        Account adminAcct = prov.getAccountById(lc.getAuthtokenAccountId());
        if (adminAcct == null)
            throw AccountServiceException.NO_SUCH_ACCOUNT(lc.getAuthtokenAccountId());

        AuthToken at = new AuthToken(account, expires, false, adminAcct); 
        try {
            token = at.getEncoded();
        } catch (AuthTokenException e) {
            throw  ServiceException.FAILURE("unable to encode auth token", e);
        }
        response.addAttribute(AdminService.E_AUTH_TOKEN, token, Element.DISP_CONTENT);
        response.addAttribute(AdminService.E_LIFETIME, lifetime, Element.DISP_CONTENT);
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
    
    /*
    public static void main(String args[]) throws ServiceException, AuthTokenException {
        Account acct = Provisioning.getInstance().getAccountByName("user2@slapshot.example.zimbra.com");
        Account admin = Provisioning.getInstance().getAccountByName("admin@slapshot.example.zimbra.com");        
        AuthToken at = new AuthToken(acct, System.currentTimeMillis()+DEFAULT_AUTH_LIFETIME*1000, false, admin);
        String token = at.getEncoded();
        System.out.println(token);
    }
    */
}
