package com.liquidsys.coco.client.soap;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.mail.MailService;
import com.liquidsys.coco.client.*;

public class LmcCreateFolderRequest extends LmcSoapRequest {

    private String mName;
    private String mParentID;
    

    /**
     * Set the ID of the parent of this new folder
     * @param n - the ID of the parent folder
     */
    public void setParentID(String id) { mParentID = id; }

    /**
     * Set the name of the folder to be created
     * @param n - the name of the new folder
     */
    public void setName(String n) { mName = n; }
    
    public String getParentID() { return mParentID; }

    public String getName() { return mName; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.CREATE_FOLDER_REQUEST);
        Element f = DomUtil.add(request, MailService.E_FOLDER, "");  
        DomUtil.addAttr(f, MailService.A_NAME, mName);
        DomUtil.addAttr(f, MailService.A_FOLDER, mParentID);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        Element folderElem = DomUtil.get(responseXML, MailService.E_FOLDER);
        LmcFolder f = parseFolder(folderElem);
        LmcCreateFolderResponse response = new LmcCreateFolderResponse();
        response.setFolder(f);
        return response;
    }

}
