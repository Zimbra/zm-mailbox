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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.ServiceException;

public class Collection extends DavResource {

	private int mId;
	
	public Collection(Folder f, Account acct) throws ServiceException {
		super(getFolderPath(f.getPath()), acct);
		setCreationDate(f.getDate());
		setLastModifiedDate(f.getChangeDate());
		setProperty(DavElements.P_DISPLAYNAME, f.getSubject());
		setProperty(DavElements.P_GETCONTENTLENGTH, "0");
		mId = f.getId();
	}
	
	private Collection(String name, String acct) {
		super(name, acct);
		long now = System.currentTimeMillis();
		setCreationDate(now);
		setLastModifiedDate(now);
		setProperty(DavElements.P_DISPLAYNAME, name.substring(1));
		setProperty(DavElements.P_GETCONTENTLENGTH, "0");
	}
	
	private static String getFolderPath(String path) {
		if (path.endsWith("/"))
			return path;
		return path + "/";
	}
	
	@Override
	public InputStream getContent() throws IOException, DavException {
		return null;
	}

	@Override
	public boolean isCollection() {
		return true;
	}
	
	@Override
	public List<DavResource> getChildren(DavContext ctxt) throws DavException {
		ArrayList<DavResource> children = new ArrayList<DavResource>();

		try {
			List<MailItem> items = getChildrenMailItem(ctxt);
			for (MailItem item : items) {
				DavResource rs = UrlNamespace.getResourceFromMailItem(null, item);
				if (rs != null)
					children.add(rs);
			}
		} catch (ServiceException e) {
			ZimbraLog.dav.error("can't get children from folder: id="+mId, e);
		}
		if (mId == Mailbox.ID_FOLDER_USER_ROOT) {
			children.add(new Collection(UrlNamespace.ATTACHMENTS_PREFIX, getOwner()));
		}
		return children;
	}
	
	private List<MailItem> getChildrenMailItem(DavContext ctxt) throws ServiceException {
		String user = ctxt.getUser();
		Provisioning prov = Provisioning.getInstance();
		Account account = prov.get(AccountBy.name, user);
		//if (account == null)
		//throw new DavException("no such account "+user, HttpServletResponse.SC_NOT_FOUND, null);

		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

		List<MailItem> ret = new ArrayList<MailItem>();
		
		// XXX aggregate into single call
		for (MailItem f : mbox.getItemList(ctxt.getOperationContext(), MailItem.TYPE_FOLDER, mId)) {
			byte view = ((Folder)f).getDefaultView();
			if (view == MailItem.TYPE_DOCUMENT || view == MailItem.TYPE_WIKI)
				ret.add(f);
		}
		//ret.addAll(mbox.getItemList(ctxt.getOperationContext(), MailItem.TYPE_MOUNTPOINT, mId));
		ret.addAll(mbox.getItemList(ctxt.getOperationContext(), MailItem.TYPE_DOCUMENT, mId));
		ret.addAll(mbox.getItemList(ctxt.getOperationContext(), MailItem.TYPE_WIKI, mId));
		return ret;
	}
}
