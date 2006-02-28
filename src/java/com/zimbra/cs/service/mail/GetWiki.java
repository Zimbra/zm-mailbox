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

package com.zimbra.cs.service.mail;

import java.util.Map;

import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.WikiWord;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

public class GetWiki extends DocumentHandler {

	public Element handle(Element request, Map context) throws ServiceException {
		ZimbraContext lc = getZimbraContext(context);
        OperationContext octxt = lc.getOperationContext();
        Element eword = request.getElement(MailService.E_WIKIWORD);
        String word = eword.getAttribute(MailService.A_NAME);
        int rev = (int)eword.getAttributeLong(MailService.A_VERSION, -1);

        Element response = lc.createElement(MailService.GET_WIKI_RESPONSE);

        WikiWord w = Wiki.getInstance().lookupWiki(word);
        if (w == null) {
        	// error handling here
        	return response;
        }
        if (rev > 0) {
            ToXML.encodeWiki(response, lc, w.getWikiItem(octxt), rev);
        } else {
            ToXML.encodeWiki(response, lc, w.getWikiItem(octxt));
        }
        return response;
	}
}
