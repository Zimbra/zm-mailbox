package com.liquidsys.coco.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.mail.MailService;
import com.liquidsys.coco.client.*;


public class LmcGetFolderRequest extends LmcSoapRequest {

    private String mFolderID;
    
    public void setFolderToGet(String f) { mFolderID = f; }
    
    public String getFolderToGet() { return mFolderID; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.GET_FOLDER_REQUEST);
        if (mFolderID != null) {
            Element folder = DomUtil.add(request, MailService.E_FOLDER, "");
            DomUtil.addAttr(folder, MailService.A_FOLDER, mFolderID);
        }
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        // LmcGetFolderResponse always has the 1 top level folder
        Element fElem = DomUtil.get(responseXML, MailService.E_FOLDER);
        LmcFolder f = parseFolder(fElem);
        
        LmcGetFolderResponse response = new LmcGetFolderResponse();
        response.setRootFolder(f);
        return response;
    }

}
