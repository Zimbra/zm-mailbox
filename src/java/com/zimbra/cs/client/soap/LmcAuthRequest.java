package com.liquidsys.coco.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.soap.DomUtil;
import com.liquidsys.coco.service.account.AccountService;
import com.liquidsys.coco.service.admin.AdminService;
import com.liquidsys.coco.service.admin.GetAccount;
import com.zimbra.soap.LiquidContext;
import com.liquidsys.coco.client.*;
import com.liquidsys.coco.service.ServiceException;

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
		String sessionId = DomUtil.getString(responseXML, LiquidContext.E_SESSION_ID, null);

		LmcAuthResponse responseObj = new LmcAuthResponse();
		LmcSession sess = new LmcSession(authToken, sessionId);
		responseObj.setSession(sess);
		return responseObj;
	}
}