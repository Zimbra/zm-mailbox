/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2009, 2010, 2013, 2014, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.client.soap;

import java.util.Iterator;

import org.dom4j.Element;
import org.dom4j.DocumentHelper;

import com.zimbra.common.soap.DomUtil;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;


public class LmcCheckSpellingRequest extends LmcSoapRequest {

    private String mText;

    public LmcCheckSpellingRequest(String text) {
        mText = text;
    }

    public String getText() {
        return mText;
    }

    protected Element getRequestXML() {
        Element request = DocumentHelper.createElement(MailConstants.CHECK_SPELLING_REQUEST);
        request.addText(mText);
        return request;
    }

    protected LmcSoapResponse parseResponseXML(Element responseXML)
        throws ServiceException
    {
        boolean isAvailable = DomUtil.getAttrBoolean(responseXML, MailConstants.A_AVAILABLE);
        LmcCheckSpellingResponse response = new LmcCheckSpellingResponse(isAvailable);

        Iterator i = responseXML.elementIterator();
        while (i.hasNext()) {
            Element misspelled = (Element) i.next();
            String word = DomUtil.getAttr(misspelled, MailConstants.A_WORD);
            String suggestions = DomUtil.getAttr(misspelled, MailConstants.A_SUGGESTIONS);
            response.addMisspelled(word, suggestions.split(","));
        }

        return response;
    }
}
