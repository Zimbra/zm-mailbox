/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

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
