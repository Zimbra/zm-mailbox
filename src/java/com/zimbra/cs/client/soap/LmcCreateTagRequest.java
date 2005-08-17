package com.zimbra.cs.client.soap;

import org.dom4j.Element;
import com.zimbra.cs.service.ServiceException;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.client.*;

public class LmcCreateTagRequest extends LmcSoapRequest {

    private String mName;
    private String mColor;
    

    public void setName(String n) { mName = n; }
    public void setColor(String c) { mColor = c; }

    public String getName() { return mName; }
    public String getColor() { return mColor; }


    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.CREATE_TAG_REQUEST);
        Element t = DomUtil.add(request, MailService.E_TAG, "");  
        DomUtil.addAttr(t, MailService.A_NAME, mName);
        DomUtil.addAttr(t, MailService.A_COLOR, mColor);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        Element tagElem = DomUtil.get(responseXML, MailService.E_TAG);
        LmcTag f = parseTag(tagElem);
        LmcCreateTagResponse response = new LmcCreateTagResponse();
        response.setTag(f);
        return response;
    }

}
