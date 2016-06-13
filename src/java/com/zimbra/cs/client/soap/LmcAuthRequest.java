/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.AccountConstants;
import com.zimbra.common.soap.AdminConstants;
import com.zimbra.common.soap.HeaderConstants;
import com.zimbra.cs.client.*;

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
		Element request = DocumentHelper.createElement(AccountConstants.AUTH_REQUEST);
		Element a = DomUtil.add(request, AccountConstants.E_ACCOUNT, mUsername);
        DomUtil.addAttr(a, AdminConstants.A_BY, AdminConstants.BY_NAME); // XXX should use a constant
		DomUtil.add(request, AccountConstants.E_PASSWORD, mPassword);
		return request;
	}

	protected LmcSoapResponse parseResponseXML(Element responseXML)
			throws ServiceException 
    {
		// get the auth token out, no default, must be present or a service exception is thrown
		String authToken = DomUtil.getString(responseXML, AccountConstants.E_AUTH_TOKEN);
		ZAuthToken zat = new ZAuthToken(null, authToken, null);
		// get the session id, if not present, default to null
		String sessionId = DomUtil.getString(responseXML, HeaderConstants.E_SESSION, null);

		LmcAuthResponse responseObj = new LmcAuthResponse();
		LmcSession sess = new LmcSession(zat, sessionId);
		responseObj.setSession(sess);
		return responseObj;
	}
}