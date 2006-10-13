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
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.service.ServiceException;

public class SearchWrapper extends PhantomResource {

	protected static final long DAY_IN_MS = 24 * 60 * 60 * 1000;
	protected static final long WEEK_IN_MS = 7 * DAY_IN_MS;
	protected static final long MONTH_IN_MS = 4 * WEEK_IN_MS;
	
	private String mContentType;
	private String mQuery;
	
	public SearchWrapper(String uri, String owner) {
		this(uri, owner, parseUri(uri));
	}
	
	public SearchWrapper(String uri, String owner, List<String> tokens) {
		super(uri, owner, tokens);
		mContentType = null;
		mQuery = "has:attachment";
		int size = tokens.size();
		String category = tokens.get(size-2);
		String item = tokens.get(size-1);
		createQuery(category, item);
	}

	private void createQuery(String category, String item) {
		if (category.equals(BrowseWrapper.BY_DATE)) {
			if (item.equals(TODAY))
				mQuery = "has:attachment after:\"-1day\"";
			else if (item.equals(WEEK))
				mQuery = "has:attachment after:\"-1week\"";
			else if (item.equals(MONTH))
				mQuery = "has:attachment after:\"-1month\"";
			
		} else if (category.equals(BrowseWrapper.BY_SENDER)) {
			mQuery = "has:attachment from:(@"+item+")";
		} else if (category.equals(BrowseWrapper.BY_TYPE)) {
			mQuery = "attachment:\""+item+"\"";
			if (!item.equals("any"))
				mContentType = item;
		}
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
		String user = ctxt.getUser();
		Provisioning prov = Provisioning.getInstance();
		ZimbraQueryResults zqr = null;
		try {
			Account account = prov.get(AccountBy.name, user);
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			zqr = mbox.search(ctxt.getOperationContext(), mQuery, SEARCH_TYPES, MailboxIndex.SortBy.NAME_ASCENDING, 1000);
			while (zqr.hasNext()) {
                ZimbraHit hit = zqr.getNext();
                if (hit instanceof MessageHit)
                    addAttachmentResources((MessageHit) hit, children);
			}
		} catch (Exception e) {
			ZimbraLog.dav.error("can't search: uri="+getUri(), e);
		} finally {
			if (zqr != null)
				try {
					zqr.doneWithSearchResults();
				} catch (ServiceException e) {}
		}
		return children;
	}
	
	private void addAttachmentResources(MessageHit hit, List<DavResource> children) {
		try {
			Message msg = hit.getMessage();
			List<MPartInfo> parts = Mime.getParts(msg.getMimeMessage());
			for (MPartInfo p : parts) {
				String name = p.getFilename();
				String ct = p.getContentType();
				if (mContentType != null && ct != null && !ct.startsWith(mContentType))
					continue;
				// XXX get the size of the attachment so we can set content-length header,
				// otherwise zero length might confuse webdav client.
				if (name != null && name.length() > 0)
					children.add(new Attachment(getUri()+name, getOwner(), msg.getDate(), 0));
			}
		} catch (Exception e) {
			ZimbraLog.dav.error("can't get attachments from msg: itemid:"+hit.getItemId(), e);
		}
	}
}
