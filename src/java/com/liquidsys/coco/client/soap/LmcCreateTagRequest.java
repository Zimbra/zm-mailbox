package com.liquidsys.coco.client.soap;

import org.dom4j.Element;
import com.liquidsys.coco.service.ServiceException;
import org.dom4j.DocumentHelper;

import com.liquidsys.soap.DomUtil;
import com.liquidsys.coco.service.mail.MailService;
import com.liquidsys.coco.client.*;

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
