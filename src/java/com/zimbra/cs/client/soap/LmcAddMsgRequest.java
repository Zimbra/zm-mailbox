package com.liquidsys.coco.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.client.*;
import com.liquidsys.coco.service.mail.MailService;

public class LmcAddMsgRequest extends LmcSoapRequest {

    private LmcMessage mMsg;
    
    /**
     * Set the message that will be added
     * @param m - the message to be added
     */
    public void setMsg(LmcMessage m) { mMsg = m; }

    public LmcMessage getMsg() { return mMsg; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.ADD_MSG_REQUEST);
        addMsg(request, mMsg, null, null, null);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        Element m = DomUtil.get(responseXML, MailService.E_MSG);
        LmcAddMsgResponse response = new LmcAddMsgResponse();
        response.setID(DomUtil.getAttr(m, MailService.A_ID));
        return response;
    }

}

