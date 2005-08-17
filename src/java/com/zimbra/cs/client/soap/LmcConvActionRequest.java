package com.zimbra.cs.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;


public class LmcConvActionRequest extends LmcSoapRequest {

    private String mIDList;
    private String mOp;
    private String mTag;
    private String mFolder;
    private String mPosition;
    private String mContent;
    

    /**
     * Set the list of Conv ID's to operate on
     * @param idList - a list of the messages to operate on
     */
    public void setConvList(String idList) { mIDList = idList; }

    /**
     * Set the operation
     * @param op - the operation (delete, read, etc.)
     */
    public void setOp(String op) { mOp = op; }

    public void setTag(String t) { mTag = t; }
    public void setFolder(String f) { mFolder = f; }
    public void setPosition(String p) { mPosition = p; }
    public void setContent(String c) { mContent = c; }
    
    public String getConvList() { return mIDList; }
    public String getOp() { return mOp; }
    public String getTag() { return mTag; }
    public String getFolder() { return mFolder; }
    public String getContent() { return mContent; }
    public String getPosition() { return mPosition; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.CONV_ACTION_REQUEST);
        Element a = DomUtil.add(request, MailService.E_ACTION, "");
        DomUtil.addAttr(a, MailService.A_ID, mIDList);
        DomUtil.addAttr(a, MailService.A_OPERATION, mOp);
        DomUtil.addAttr(a, MailService.A_TAG, mTag);
        DomUtil.addAttr(a, MailService.A_FOLDER, mFolder);
        if (mContent != null)
        	DomUtil.add(a, MailService.E_CONTENT, mContent);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        LmcConvActionResponse response = new LmcConvActionResponse();
        Element a = DomUtil.get(responseXML, MailService.E_ACTION);
        response.setConvList(DomUtil.getAttr(a, MailService.A_ID));
        response.setOp(DomUtil.getAttr(a, MailService.A_OPERATION));
        return response;
    }

}
