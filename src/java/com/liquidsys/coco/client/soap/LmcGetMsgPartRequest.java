package com.liquidsys.coco.client.soap;

import org.dom4j.Element;

import com.liquidsys.coco.service.ServiceException;


public class LmcGetMsgPartRequest extends LmcSoapRequest {

    private String mMsgID;
    private String mPartName;
    

    /**
     * Set the ID of the msg that has the target MIME part
     * @param n - the ID of the msg
     */
    public void setMsgID(String id) { mMsgID = id; }

    /**
     * Set the name of the message part to retrieve.
     * @param n - the name of the message part.
     */
    public void setPartName(String n) { mPartName = n; }
    
    public String getMsgID() { return mMsgID; }

    public String getPartName() { return mPartName; }

    protected Element getRequestXML() throws LmcSoapClientException {
        throw new LmcSoapClientException("this command not implemented by server");
        /*
        Element request = DocumentHelper.createElement("XXX there is no GetMsgPartRequest");
        Element m = DomUtil.add(request, MailService.E_MSG, "");
        DomUtil.addAttr(m, MailService.A_ID, mMsgID);
        Element mp = DomUtil.add(m, MailService.E_MIMEPART, "");
        DomUtil.addAttr(mp, MailService.A_PART, mPartName);
        return request;
        */
  
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException, LmcSoapClientException
    {
        LmcGetMsgPartResponse response = new LmcGetMsgPartResponse();
        response.setMessage(parseMessage(responseXML));
        return response;
    }
}
