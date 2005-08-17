package com.liquidsys.coco.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.mail.MailService;
import com.zimbra.soap.DomUtil;

public class LmcContactActionRequest extends LmcSoapRequest {

    private String mIDList;

    private String mOp;

    private String mFolder;

    private String mTag;

    /**
     * Set the list of Contact ID's to operate on
     * @param idList - a list of the contacts to operate on
     */
    public void setIDList(String idList) {
    	mIDList = idList;
    }
    
    /**
     * Set the operation
     * @param op - the operation (delete, read, etc.).  It's up to the client
     * to put a "!" in front of the operation if negation is desired.
     */
    public void setOp(String op) {
    	mOp = op;
    }
    
    public void setTag(String t) {
    	mTag = t;
    }
    
    public void setFolder(String f) {
    	mFolder = f;
    }
    
    public String getIDList() {
    	return mIDList;
    }
    
    public String getOp() {
    	return mOp;
    }
    
    public String getFolder() {
    	return mFolder;
    }
    
    public String getTage() {
    	return mTag;
    }
    
    protected Element getRequestXML() {
    	Element request = DocumentHelper.createElement(MailService.CONTACT_ACTION_REQUEST);
    	Element a = DomUtil.add(request, MailService.E_ACTION, "");
    	DomUtil.addAttr(a, MailService.A_ID, mIDList);
    	DomUtil.addAttr(a, MailService.A_OPERATION, mOp);
    	addAttrNotNull(a, MailService.A_TAG, mTag);
    	addAttrNotNull(a, MailService.A_FOLDER, mFolder);
    	return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
        throws ServiceException 
    {
    	LmcContactActionResponse response = new LmcContactActionResponse();
    	Element a = DomUtil.get(responseXML, MailService.E_ACTION);
    	response.setIDList(DomUtil.getAttr(a, MailService.A_ID));
    	response.setOp(DomUtil.getAttr(a, MailService.A_OPERATION));
    	return response;
    }
}
