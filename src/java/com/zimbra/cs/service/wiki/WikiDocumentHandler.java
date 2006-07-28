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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.wiki;

import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.ServiceException.Argument;
import com.zimbra.cs.service.mail.MailDocumentHandler;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.WikiPage;
import com.zimbra.cs.wiki.Wiki.WikiContext;
import com.zimbra.soap.Element;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class WikiDocumentHandler extends MailDocumentHandler {
    private static final String[] TARGET_ID_PATH = new String[] { MailService.E_WIKIWORD, MailService.A_ID };
    private static final String[] TARGET_FOLDER_PATH = new String[] { MailService.E_WIKIWORD, MailService.A_FOLDER };
    protected String[] getProxiedIdPath(Element request)     {
    	String id = getXPath(request, TARGET_ID_PATH);
    	if (id == null)
    		return TARGET_FOLDER_PATH;
    	return TARGET_ID_PATH; 
    }
    protected boolean checkMountpointProxy(Element request)  { return true; }

	protected String getAuthor(ZimbraSoapContext lc) throws ServiceException {
		return lc.getAuthtokenAccount().getName();
	}
	
	protected ItemId getRequestedFolder(Element request, ZimbraSoapContext lc) throws ServiceException {
		for (Element elem : request.listElements()) {
	        String fid = elem.getAttribute(MailService.A_FOLDER, null);
	        if (fid != null) {
	        	return new ItemId(fid, lc);
	        }
		}
		return null;
	}
	
	protected Wiki getRequestedWikiNotebook(Element request, ZimbraSoapContext lc) throws ServiceException {
		ItemId fid = getRequestedFolder(request, lc);
		String accountId = lc.getAuthtokenAccountId();
		WikiContext ctxt = new WikiContext(lc.getOperationContext(), lc.getRawAuthToken());
		if (fid == null) {
			return Wiki.getInstance(ctxt, accountId);
		} else if (!fid.belongsTo(lc.getAuthtokenAccount())) {
			accountId = fid.getAccountId();
		}
		return Wiki.getInstance(ctxt, accountId, fid.getId());
	}
	
	protected void validateRequest(ZimbraSoapContext lc, Wiki wiki, int itemId, long ver, String wikiWord) throws ServiceException {
		if (itemId == 0 || ver == 0) {
			if (itemId != 0 || ver != 0) {
				throw new IllegalArgumentException("either itemId or version is zero");
			}
			WikiPage ww = wiki.lookupWiki(wikiWord);
			if (ww != null) {
				throw MailServiceException.ALREADY_EXISTS("wiki word "+wikiWord+" in folder "+wiki.getWikiFolderId(),
						new Argument(MailService.A_ID, ww.getId(), Argument.Type.IID),
						new Argument(MailService.A_VERSION, ww.getLastRevision(), Argument.Type.NUM));
			}
		} else {
			WikiPage ww = wiki.lookupWiki(wikiWord);
			if (ww == null) {
				throw MailServiceException.MODIFY_CONFLICT(
						new Argument(MailService.A_ID, itemId, Argument.Type.IID),
						new Argument(MailService.A_VERSION, ver, Argument.Type.NUM));
			}
			ItemId iid = new ItemId(ww.getId(), lc);
			if (iid.getId() != itemId) {
				throw MailServiceException.INVALID_ID(itemId);
			}
			if (ww.getLastRevision() != ver) {
				throw MailServiceException.MODIFY_CONFLICT(
						new Argument(MailService.A_ID, ww.getId(), Argument.Type.IID),
						new Argument(MailService.A_VERSION, ww.getLastRevision(), Argument.Type.NUM));
			}
		}
	}
}
