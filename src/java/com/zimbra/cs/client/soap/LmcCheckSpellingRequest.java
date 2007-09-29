/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client.soap;

import java.util.Iterator;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.soap.DomUtil;
import com.zimbra.common.service.ServiceException;
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
