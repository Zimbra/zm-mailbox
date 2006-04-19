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

package com.zimbra.cs.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.soap.DomUtil;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.cs.service.admin.AdminService;
import com.zimbra.cs.service.admin.GetAccount;
import com.zimbra.soap.ZimbraSoapContext;
import com.zimbra.cs.client.*;
import com.zimbra.cs.service.ServiceException;

public class LmcAuthRequest extends LmcSoapRequest {

	private String mUsername;

	private String mPassword;

	public void setUsername(String u) {
		mUsername = u;
	}

	public void setPassword(String p) {
		mPassword = p;
	}

	public String getUsername() {
		return mUsername;
	}

	public String getPassword() {
		return mPassword;   // high security interface
	} 

	protected Element getRequestXML() {
		Element request = DocumentHelper.createElement(AccountService.AUTH_REQUEST);
		Element a = DomUtil.add(request, AccountService.E_ACCOUNT, mUsername);
        DomUtil.addAttr(a, AdminService.A_BY, GetAccount.BY_NAME); // XXX should use a constant
		DomUtil.add(request, AccountService.E_PASSWORD, mPassword);
		return request;
	}

	protected LmcSoapResponse parseResponseXML(Element responseXML)
			throws ServiceException 
    {
		// get the auth token out, no default, must be present or a service exception is thrown
		String authToken = DomUtil.getString(responseXML, AccountService.E_AUTH_TOKEN);
		// get the session id, if not present, default to null
		String sessionId = DomUtil.getString(responseXML, ZimbraSoapContext.E_SESSION_ID, null);

		LmcAuthResponse responseObj = new LmcAuthResponse();
		LmcSession sess = new LmcSession(authToken, sessionId);
		responseObj.setSession(sess);
		return responseObj;
	}
}