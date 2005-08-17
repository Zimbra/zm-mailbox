package com.zimbra.cs.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;


public class LmcGetMsgRequest extends LmcSoapRequest {

    private String mMsgID;
    private String mRead;
    
    // There is a single msg to get.  Must be present.
    public void setMsgToGet(String f) { mMsgID = f; }

    // Optionally set read
    public void setRead(String r) { mRead = r; }
    
    public String getMsgToGet() { return mMsgID; }

    public String getRead() { return mRead; }


    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.GET_MSG_REQUEST);  
        Element m = DomUtil.add(request, MailService.E_MSG, "");
        DomUtil.addAttr(m, MailService.A_ID, mMsgID);
        addAttrNotNull(m, MailService.A_MARK_READ, mRead);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException, LmcSoapClientException
    {
        LmcGetMsgResponse response = new LmcGetMsgResponse();
        response.setMsg(parseMessage(DomUtil.get(responseXML, MailService.E_MSG)));
        return response;
    }
}
