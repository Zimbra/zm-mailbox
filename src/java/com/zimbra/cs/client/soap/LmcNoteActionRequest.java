package com.liquidsys.coco.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.mail.MailService;
import com.liquidsys.soap.DomUtil;

public class LmcNoteActionRequest extends LmcSoapRequest {

    private String mIDList;
    private String mOp;
    private String mTargetFolder;
    private String mColor;
    private String mTag;
    

    /**
     * Set the list of Note ID's to operate on
     * @param idList - a list of the notes to operate on
     */
    public void setNoteList(String idList) { mIDList = idList; }

    /**
     * Set the operation
     * @param op - the operation (delete, read, etc.)
     */
    public void setOp(String op) { mOp = op; }

    public void setTag(String t) { mTag = t; }
    public void setTargetFolder(String f) { mTargetFolder = f; }
    public void setColor(String c) { mColor = c; }

    
    public String getNoteList() { return mIDList; }
    public String getOp() { return mOp; }
    public String getTargetFolder() { return mTargetFolder; }
    public String getColor() { return mColor; }
    public String getTag() { return mTag; }


    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.NOTE_ACTION_REQUEST);
        Element a = DomUtil.add(request, MailService.E_ACTION, "");
        DomUtil.addAttr(a, MailService.A_ID, mIDList);
        DomUtil.addAttr(a, MailService.A_OPERATION, mOp);
        DomUtil.addAttr(a, MailService.A_TAG, mTag);
        DomUtil.addAttr(a, MailService.A_FOLDER, mTargetFolder);
        DomUtil.addAttr(a, MailService.A_COLOR, mColor);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        LmcNoteActionResponse response = new LmcNoteActionResponse();
        Element a = DomUtil.get(responseXML, MailService.E_ACTION);
        response.setNoteList(DomUtil.getAttr(a, MailService.A_ID));
        response.setOp(DomUtil.getAttr(a, MailService.A_OPERATION));
        return response;
    }

}
