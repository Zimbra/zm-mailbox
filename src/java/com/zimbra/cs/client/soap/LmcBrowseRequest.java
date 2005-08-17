package com.liquidsys.coco.client.soap;

import java.util.ArrayList;
import java.util.Iterator;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.zimbra.soap.DomUtil;
import com.liquidsys.coco.service.ServiceException;
import com.liquidsys.coco.service.mail.MailService;
import com.zimbra.soap.SoapParseException;
import com.liquidsys.coco.client.*;

public class LmcBrowseRequest extends LmcSoapRequest {

    private String mBrowseBy;
    
    public void setBrowseBy(String b) { mBrowseBy = b; }
    
    public String getBrowseBy() { return mBrowseBy; }
    
	protected Element getRequestXML() throws LmcSoapClientException {
        Element request = DocumentHelper.createElement(MailService.BROWSE_REQUEST);
        DomUtil.addAttr(request, MailService.A_BROWSE_BY, mBrowseBy);
        return request;
	}

    protected LmcBrowseData parseBrowseData(Element bdElem) {
    	LmcBrowseData bd = new LmcBrowseData();
        bd.setFlags(bdElem.attributeValue(MailService.A_BROWSE_DOMAIN_HEADER));
        bd.setData(bdElem.getText());
        return bd;
    }
    
	protected LmcSoapResponse parseResponseXML(Element parentElem)
			throws SoapParseException, ServiceException, LmcSoapClientException 
    {
		LmcBrowseResponse response = new LmcBrowseResponse();
        ArrayList bdArray = new ArrayList();
        for (Iterator ait = parentElem.elementIterator(MailService.E_BROWSE_DATA); ait.hasNext(); ) {
            Element a = (Element) ait.next();
            bdArray.add(parseBrowseData(a));
        }

        if (!bdArray.isEmpty()) {
            LmcBrowseData bds[] = new LmcBrowseData[bdArray.size()]; 
            response.setData((LmcBrowseData []) bdArray.toArray(bds));
        } 

        return response;
	}

}
