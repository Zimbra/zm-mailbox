package com.liquidsys.coco.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.liquidsys.soap.DomUtil;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.account.AccountService;
import com.liquidsys.soap.SoapParseException;
import com.liquidsys.coco.client.*;

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
