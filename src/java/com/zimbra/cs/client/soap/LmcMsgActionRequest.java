package com.zimbra.cs.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;

public class LmcMsgActionRequest extends LmcSoapRequest {

    private String mIDList;
    private String mOp;
    private String mTag;
    private String mFolder;
    

    /**
     * Set the list of Msg ID's to operate on
     * @param idList - a list of the messages to operate on
     */
    public void setMsgList(String idList) { mIDList = idList; }

    /**
     * Set the operation
     * @param op - the operation (delete, read, etc.)
     */
    public void setOp(String op) { mOp = op; }

    public void setTag(String t) { mTag = t; }
    
    public void setFolder(String f) { mFolder = f; }
    
    public String getMsgList() { return mIDList; }
    public String getOp() { return mOp; }
    public String getTag() { return mTag; }
    public String getFolder() { return mFolder; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.MSG_ACTION_REQUEST);
        Element a = DomUtil.add(request, MailService.E_ACTION, "");
        DomUtil.addAttr(a, MailService.A_ID, mIDList);
        DomUtil.addAttr(a, MailService.A_OPERATION, mOp);
        DomUtil.addAttr(a, MailService.A_TAG, mTag);
        DomUtil.addAttr(a, MailService.A_FOLDER, mFolder);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        LmcMsgActionResponse response = new LmcMsgActionResponse();
        Element a = DomUtil.get(responseXML, MailService.E_ACTION);
        response.setMsgList(DomUtil.getAttr(a, MailService.A_ID));
        response.setOp(DomUtil.getAttr(a, MailService.A_OPERATION));
        return response;
    }

}
