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
package com.zimbra.cs.dav.resource;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.ServiceException;

public abstract class MailItemResource extends DavResource {
	protected int  mId;
	protected byte mType;
	
	public MailItemResource(MailItem item) throws ServiceException {
		this(getItemPath(item), item);
	}
	
	public MailItemResource(String path, MailItem item) throws ServiceException {
		super(path, item.getAccount());
		mId = item.getId();
		mType = item.getType();
	}
	
	public MailItemResource(String path, String acct) {
		super(path, acct);
	}
	
	private static String getItemPath(MailItem item) throws ServiceException {
		String path = item.getPath();
		if (item.getType() == MailItem.TYPE_FOLDER && !path.endsWith("/"))
			return path + "/";
		return path;
	}
	
	public void delete(DavContext ctxt) throws DavException {
		if (mId == 0) 
			throw new DavException("cannot delete resource", HttpServletResponse.SC_FORBIDDEN, null);
		String user = ctxt.getUser();
		String path = ctxt.getPath();
		if (user == null || path == null)
			throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, null);
		try {
			Provisioning prov = Provisioning.getInstance();
			Account account = prov.get(AccountBy.name, user);
			if (account == null)
				throw new DavException("no such account "+user, HttpServletResponse.SC_NOT_FOUND, null);

			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			mbox.delete(ctxt.getOperationContext(), mId, mType);
		} catch (ServiceException e) {
			throw new DavException("cannot get item", HttpServletResponse.SC_NOT_FOUND, e);
		}
	}
}
