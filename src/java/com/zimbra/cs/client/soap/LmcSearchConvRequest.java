package com.zimbra.cs.client.soap;

import org.dom4j.Element;

import com.zimbra.soap.DomUtil;
import com.zimbra.cs.service.mail.MailService;

public class LmcSearchConvRequest extends LmcSearchRequest {

    private String mConvID;
    
    public void setConvID(String c) { mConvID = c; }
    
    public String getConvID() { return mConvID; }
    
	protected Element getRequestXML() {
        // the request XML is the same as for search, with a conversation ID added
        Element response = createQuery(MailService.SEARCH_CONV_REQUEST);
        DomUtil.addAttr(response, MailService.A_CONV_ID, mConvID);
        return response;
	}


}
