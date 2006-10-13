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
import java.util.Collections;
import java.util.List;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.BrowseResult;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.BrowseResult.DomainItem;
import com.zimbra.cs.service.ServiceException;

public class BrowseWrapper extends PhantomResource {
	
	private enum BrowseBy {
		menu, date, type, sender
	}
	
	private BrowseBy mAction;
	
	BrowseWrapper(String uri, String owner) {
		this(uri, owner, parseUri(uri));
	}
	
	BrowseWrapper(String uri, String owner, List<String> tokens) {
		super(uri, owner, tokens);

		String name = mTokens.get(mTokens.size()-1);
		if (name.equals(BY_DATE)) {
			mAction = BrowseBy.date;
		} else if (name.equals(BY_TYPE)) {
			mAction = BrowseBy.type;
		} else if (name.equals(BY_SENDER)) {
			mAction = BrowseBy.sender;
		} else {
			mAction = BrowseBy.menu;
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
		try {
			switch (mAction) {
			case menu:
				return generateMenu(ctxt);
			case date:
				return browseByDate(ctxt);
			case sender:
				return browseBySender(ctxt);
			case type:
				return browseByType(ctxt);
			}
		} catch (ServiceException e) {
			ZimbraLog.dav.error("can't generate browse resource: uri="+getUri(), e);
		} catch (IOException e) {
			ZimbraLog.dav.error("can't generate browse resource: uri="+getUri(), e);
		}
		return Collections.emptyList();
	}
	
	private String generateUri(String path) {
		StringBuilder buf = new StringBuilder();
		buf.append(getUri());
		if (buf.charAt(buf.length()-1) != '/')
			buf.append("/");
		buf.append(path);
		return buf.toString();
	}
	
	private List<DavResource> generateMenu(DavContext ctxt) {
		ArrayList<DavResource> menu = new ArrayList<DavResource>();
		menu.add(new BrowseWrapper(generateUri(BY_DATE), getOwner()));
		menu.add(new BrowseWrapper(generateUri(BY_TYPE), getOwner()));
		menu.add(new BrowseWrapper(generateUri(BY_SENDER), getOwner()));
		return menu;
	}
	
	private List<DavResource> browseByDate(DavContext ctxt) {
		ArrayList<DavResource> res = new ArrayList<DavResource>();
		res.add(new SearchWrapper(generateUri(TODAY), getOwner()));
		res.add(new SearchWrapper(generateUri(WEEK), getOwner()));
		res.add(new SearchWrapper(generateUri(MONTH), getOwner()));
		res.add(new SearchWrapper(generateUri(ALL), getOwner()));
		return res;
	}
	
	private List<DavResource> browseBySender(DavContext ctxt) throws IOException, ServiceException {
		ArrayList<DavResource> res = new ArrayList<DavResource>();
		String user = ctxt.getUser();
		Provisioning prov = Provisioning.getInstance();
		Account account = prov.get(AccountBy.name, user);
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
		BrowseResult br = mbox.browse(ctxt.getOperationContext(), Mailbox.BROWSE_BY_DOMAINS);
		for (Object obj : br.getResult()) {
			if (obj instanceof DomainItem) {
				DomainItem di = (DomainItem) obj;
				if (di.isFrom())
					res.add(new SearchWrapper(generateUri(di.getDomain()), getOwner()));
			}
		}
		return res;
	}
	
	private List<DavResource> browseByType(DavContext ctxt) throws IOException, ServiceException {
		ArrayList<DavResource> res = new ArrayList<DavResource>();
		String user = ctxt.getUser();
		Provisioning prov = Provisioning.getInstance();
		Account account = prov.get(AccountBy.name, user);
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
		BrowseResult br = mbox.browse(ctxt.getOperationContext(), Mailbox.BROWSE_BY_ATTACHMENTS);
		for (Object obj : br.getResult()) {
			if (obj instanceof String) {
				String ctype = (String) obj;
				int index = ctype.indexOf('/');
				if (index != -1)
					ctype = ctype.substring(0,index) + "%2F" + ctype.substring(index+1);
				res.add(new SearchWrapper(generateUri(ctype), getOwner()));
			}
		}
		return res;
	}
}
