package com.zimbra.cs.client.soap;

import java.util.Iterator;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;


public class LmcCheckSpellingRequest extends LmcSoapRequest {

    private String mText;

    public LmcCheckSpellingRequest(String text) {
        mText = text;
    }
    
    public String getText() {
        return mText;
    }
    
    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailService.CHECK_SPELLING_REQUEST);
        request.addText(mText);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML) 
        throws ServiceException
    {
        boolean isAvailable = DomUtil.getAttrBoolean(responseXML, MailService.A_AVAILABLE);
        LmcCheckSpellingResponse response = new LmcCheckSpellingResponse(isAvailable);
        
        Iterator i = responseXML.elementIterator();
        while (i.hasNext()) {
            Element misspelled = (Element) i.next();
            String word = DomUtil.getAttr(misspelled, MailService.A_WORD);
            String suggestions = DomUtil.getAttr(misspelled, MailService.A_SUGGESTIONS);
            response.addMisspelled(word, suggestions.split(","));
        }
        
        return response;
    }
}
