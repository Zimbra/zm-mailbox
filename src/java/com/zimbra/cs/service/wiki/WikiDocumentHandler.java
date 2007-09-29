/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
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
package com.zimbra.cs.service.wiki;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.service.mail.MailDocumentHandler;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.wiki.Wiki;
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
		Account requestedAccount = Provisioning.getInstance().get(AccountBy.id,
																	lc.getRequestedAccountId());
		String accountId = requestedAccount.getId();
		WikiContext ctxt = new WikiContext(lc.getOperationContext(), lc.getRawAuthToken());
		if (fid == null) {
			return Wiki.getInstance(ctxt, accountId);
		} else if (!fid.belongsTo(requestedAccount)) {
			accountId = fid.getAccountId();
		}
		return Wiki.getInstance(ctxt, accountId, fid.getId());
	}
	
	protected void checkEnabled(ZimbraSoapContext lc) throws ServiceException {
		Account requestedAccount = Provisioning.getInstance().get(AccountBy.id,
				lc.getRequestedAccountId());
		if (!requestedAccount.getBooleanAttr("zimbraFeatureNotebookEnabled", false))
			throw WikiServiceException.NOT_ENABLED();
	}
}
