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

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.WikiWord;
import com.zimbra.soap.DocumentHandler;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraContext;

public abstract class WikiDocumentHandler extends DocumentHandler {

	protected String getAuthor(ZimbraContext lc) throws ServiceException {
		return lc.getAuthtokenAccount().getName();
	}
	
	protected Wiki getRequestedWikiNotebook(Element request, ZimbraContext lc) throws ServiceException {
		for (Element elem : request.listElements()) {
	        int fid = (int)elem.getAttributeLong(MailService.A_FOLDER, 0);
	        if (fid != 0)
	    		return Wiki.getInstance(lc.getAuthtokenAccount(), fid);
		}
		return Wiki.getInstance(lc.getAuthtokenAccount());
	}
	
	protected void validateRequest(Wiki wiki, int itemId, long ver, String wikiWord) throws ServiceException {
		if (itemId == 0 || ver == 0) {
			if (itemId != 0 || ver != 0) {
				throw new IllegalArgumentException("either itemId or version is zero");
			}
			if (wiki.lookupWiki(wikiWord) != null) {
				throw MailServiceException.ALREADY_EXISTS("wiki word "+wikiWord+" in folder "+wiki.getWikiFolderId());
			}
		} else {
			WikiWord ww = wiki.lookupWiki(wikiWord);
			if (ww.getId() != itemId) {
				throw MailServiceException.INVALID_ID(itemId);
			}
			if (ww.getLastRevision() != ver) {
				throw MailServiceException.MODIFY_CONFLICT();
			}
		}
	}
}
