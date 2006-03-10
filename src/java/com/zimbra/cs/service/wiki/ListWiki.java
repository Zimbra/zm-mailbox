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

package com.zimbra.cs.service.wiki;

import java.util.Map;
import java.util.Set;

import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.WikiWord;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

public class ListWiki extends WikiDocumentHandler {

	@Override
	public Element handle(Element request, Map context) throws ServiceException {
		ZimbraContext lc = getZimbraContext(context);
		Wiki wiki = getRequestedWiki(request, lc);

        Set<String> wikiWords = wiki.listWiki();
        Element response = lc.createElement(MailService.LIST_WIKI_RESPONSE);
        for (String w : wikiWords) {
        	WikiWord ww = wiki.lookupWiki(w);
            Element m = response.addElement(MailService.E_WIKIWORD);
            m.addAttribute(MailService.A_NAME, w);
            m.addAttribute(MailService.A_VERSION, ww.lastRevision());
            m.addAttribute(MailService.A_CREATED_DATE, ww.getCreatedDate());
            m.addAttribute(MailService.A_MODIFIED_DATE, ww.getModifiedDate());
            m.addAttribute(MailService.A_CREATOR, ww.getCreator());
            m.addAttribute(MailService.A_LAST_EDITED_BY, ww.getLastEditor());
            m.addAttribute(MailService.A_FOLDER, ww.getFolderId());
        }
        return response;
	}
}
