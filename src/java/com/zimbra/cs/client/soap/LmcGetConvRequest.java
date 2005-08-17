package com.liquidsys.coco.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.mail.MailService;
import com.liquidsys.coco.client.*;


public class LmcGetConvRequest extends LmcSoapRequest {

    private String mConvID;
    private String mMsgsToGet[];
    
    // There is a single conversation to get.  Must be present.
    public void setConvToGet(String f) { mConvID = f; }

    // Set the ID's of the msgs within the conversation to get.  Optional.
    public void setMsgsToGet(String m[]) { mMsgsToGet = m; }
    
    public String getConvToGet() { return mConvID; }

    public String[] getMsgsToGet() { return mMsgsToGet; }


    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.GET_CONV_REQUEST);

        // set the ID of the conversation to get
        Element convElement = DomUtil.add(request, MailService.E_CONV, "");
        DomUtil.addAttr(convElement, MailService.A_ID, mConvID);  

        // add message elements within the conversation element if desired
        if (mMsgsToGet != null) {
            for (int i = 0; i < mMsgsToGet.length; i++) {
                Element m = DomUtil.add(convElement, MailService.E_MSG, "");
                DomUtil.addAttr(m, MailService.A_ID, mMsgsToGet[i]);
            }
        }

        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException, LmcSoapClientException
    {
        // the response will always be exactly one conversation
        LmcConversation c = parseConversation(DomUtil.get(responseXML, MailService.E_CONV));
        LmcGetConvResponse response = new LmcGetConvResponse();
        response.setConv(c);
        return response;
    }

}
