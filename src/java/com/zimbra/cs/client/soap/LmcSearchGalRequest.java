package com.zimbra.cs.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.soap.DomUtil;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.account.AccountService;
import com.zimbra.soap.SoapParseException;
import com.zimbra.cs.client.*;

public class LmcSearchGalRequest extends LmcSoapRequest {

    private String mName;
    
    public void setName(String n) { mName = n; }
    
    public String getName() { return mName; }
    
	protected Element getRequestXML() throws LmcSoapClientException {
		Element request = DocumentHelper.createElement(AccountService.SEARCH_GAL_REQUEST);
		DomUtil.add(request, AccountService.E_NAME, mName);
        return request;
    }

	protected LmcSoapResponse parseResponseXML(Element responseXML)
	    throws SoapParseException, ServiceException, LmcSoapClientException 
    {
        LmcContact contacts[] = parseContactArray(responseXML);
        LmcSearchGalResponse sgResp = new LmcSearchGalResponse();
        sgResp.setContacts(contacts);
        return sgResp;
	}

}
