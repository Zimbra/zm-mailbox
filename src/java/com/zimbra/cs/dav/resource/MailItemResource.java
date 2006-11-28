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

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;

public abstract class MailItemResource extends DavResource {
	protected int  mFolderId;
	protected int  mId;
	protected byte mType;
	protected String mEtag;
	
	public MailItemResource(MailItem item) throws ServiceException {
		this(getItemPath(item), item);
	}
	
	public MailItemResource(String path, MailItem item) throws ServiceException {
		super(path, item.getAccount());
		mFolderId = item.getFolderId();
		mId = item.getId();
		mType = item.getType();
		mEtag = "\""+Long.toString(item.getChangeDate())+"\"";
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

	public boolean hasEtag() {
		return true;
	}
	
	public String getEtag() {
		return mEtag;
	}
	
	protected Mailbox getMailbox(DavContext ctxt) throws ServiceException, DavException {
		String user = ctxt.getUser();
		if (user == null)
			throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, null);
		Provisioning prov = Provisioning.getInstance();
		Account account = prov.get(AccountBy.name, user);
		if (account == null)
			throw new DavException("no such account "+user, HttpServletResponse.SC_NOT_FOUND, null);
		return MailboxManager.getInstance().getMailboxByAccount(account);
	}
	
	public void delete(DavContext ctxt) throws DavException {
		if (mId == 0) 
			throw new DavException("cannot delete resource", HttpServletResponse.SC_FORBIDDEN, null);
		try {
			Mailbox mbox = getMailbox(ctxt);
			mbox.delete(ctxt.getOperationContext(), mId, mType);
		} catch (ServiceException se) {
			throw new DavException("cannot delete item", HttpServletResponse.SC_FORBIDDEN, se);
		}
	}
	
	public void move(DavContext ctxt, Collection dest) throws DavException {
		if (mFolderId == dest.getId())
			return;
		try {
			Mailbox mbox = getMailbox(ctxt);
			mbox.move(ctxt.getOperationContext(), mId, mType, dest.getId());
		} catch (ServiceException se) {
			throw new DavException("cannot move item", HttpServletResponse.SC_FORBIDDEN, se);
		}
	}
	
	public MailItemResource copy(DavContext ctxt, Collection dest) throws DavException {
		try {
			Mailbox mbox = getMailbox(ctxt);
			MailItem item = mbox.copy(ctxt.getOperationContext(), mId, mType, dest.getId());
			return UrlNamespace.getResourceFromMailItem(ctxt, item);
		} catch (IOException e) {
			throw new DavException("cannot copy item", HttpServletResponse.SC_FORBIDDEN, e);
		} catch (ServiceException se) {
			throw new DavException("cannot copy item", HttpServletResponse.SC_FORBIDDEN, se);
		}
	}
	
	public void rename(DavContext ctxt, String newName) throws DavException {
		try {
			if (isCollection()) {
				Mailbox mbox = getMailbox(ctxt);
				mbox.renameFolder(ctxt.getOperationContext(), mId, newName);
			} else {
				Mailbox mbox = getMailbox(ctxt);
				MailItem item = mbox.getItemById(ctxt.getOperationContext(), mId, mType);
				if (item instanceof Document) {
					Document doc = (Document) item;
					doc.rename(newName);
				}
			}
		} catch (ServiceException se) {
			throw new DavException("cannot copy item", HttpServletResponse.SC_FORBIDDEN, se);
		}
	}
	
	int getId() {
		return mId;
	}
}
