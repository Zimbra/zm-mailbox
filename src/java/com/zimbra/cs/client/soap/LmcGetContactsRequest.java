package com.liquidsys.coco.client.soap;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.mail.MailService;
import com.liquidsys.coco.client.*;
import com.liquidsys.soap.DomUtil;

public class LmcGetContactsRequest extends LmcSoapRequest {

    private LmcContactAttr mAttrs[];
    private String mIDList[];
    
    public void setContacts(String c[]) { mIDList = c; }
    public void setAttrs(LmcContactAttr attrs[]) { mAttrs = attrs; }
    
    public String[] getContacts() { return mIDList; }
    public LmcContactAttr[] getAttrs() { return mAttrs; }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.GET_CONTACTS_REQUEST);
        
        // emit contact attributes if any
        for (int i = 0; mAttrs != null && i < mAttrs.length; i++)
            addContactAttr(request, mAttrs[i]);
        
        // emit specified contacts if any
        for (int i = 0; mIDList != null && i < mIDList.length; i++) {
            Element newCN = DomUtil.add(request, MailService.E_CONTACT, "");
            DomUtil.addAttr(newCN, MailService.A_ID, mIDList[i]);
        }

        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException, LmcSoapClientException 
    {
        LmcGetContactsResponse response = new LmcGetContactsResponse();
        LmcContact cons[] = parseContactArray(responseXML);
        response.setContacts(cons);
        return response;
    }
}
