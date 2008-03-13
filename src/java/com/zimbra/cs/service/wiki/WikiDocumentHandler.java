/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
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
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.wiki;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.common.soap.Element;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.mail.MailDocumentHandler;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.wiki.Wiki;
import com.zimbra.cs.wiki.Wiki.WikiContext;
import com.zimbra.soap.ZimbraSoapContext;

public abstract class WikiDocumentHandler extends MailDocumentHandler {
    private static final String[] TARGET_ID_PATH = new String[] { MailConstants.E_WIKIWORD, MailConstants.A_ID };
    private static final String[] TARGET_FOLDER_PATH = new String[] { MailConstants.E_WIKIWORD, MailConstants.A_FOLDER };
    protected String[] getProxiedIdPath(Element request)     {
    	String id = getXPath(request, TARGET_ID_PATH);
    	if (id == null)
    		return TARGET_FOLDER_PATH;
    	return TARGET_ID_PATH; 
    }
    protected boolean checkMountpointProxy(Element request)  { return true; }

	protected String getAuthor(ZimbraSoapContext zsc) throws ServiceException {
		return getAuthenticatedAccount(zsc).getName();
	}
	
	protected ItemId getRequestedFolder(Element request, ZimbraSoapContext zsc) throws ServiceException {
		for (Element elem : request.listElements()) {
	        String fid = elem.getAttribute(MailConstants.A_FOLDER, null);
	        if (fid != null) {
	        	return new ItemId(fid, zsc);
	        }
		}
		return null;
	}
	
	protected Wiki getRequestedWikiNotebook(Element request, ZimbraSoapContext zsc, OperationContext octxt) throws ServiceException {
		ItemId fid = getRequestedFolder(request, zsc);
		Account requestedAccount = Provisioning.getInstance().get(AccountBy.id, zsc.getRequestedAccountId(), zsc.getAuthToken());
		String accountId = requestedAccount.getId();
		WikiContext ctxt = new WikiContext(octxt, zsc.getAuthToken());
		if (fid == null) {
			return Wiki.getInstance(ctxt, accountId);
		} else if (!fid.belongsTo(requestedAccount)) {
			accountId = fid.getAccountId();
		}
		return Wiki.getInstance(ctxt, accountId, fid.getId());
	}
	
	protected void checkEnabled(ZimbraSoapContext zsc) throws ServiceException {
		Account requestedAccount = getRequestedAccount(zsc);
		if (!requestedAccount.getBooleanAttr("zimbraFeatureNotebookEnabled", false))
			throw WikiServiceException.NOT_ENABLED();
	}
}
