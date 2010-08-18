/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.3 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.index.BrowseTerm;
import com.zimbra.cs.mailbox.BrowseResult;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.BrowseResult.DomainItem;

/**
 * BrowseWrapper is used to generate the phantom folder hierarchy.
 * 
 * @author jylee
 *
 */
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
		if (tokens.size() == 1) {
			mAction = BrowseBy.menu;
		} else if (name.equals(BY_DATE)) {
			mAction = BrowseBy.date;
		} else if (name.equals(BY_TYPE)) {
			mAction = BrowseBy.type;
		} else if (name.equals(BY_SENDER)) {
			mAction = BrowseBy.sender;
		} else {
			mAction = BrowseBy.date;
		}
	}
	
	@Override
	public boolean isCollection() {
		return true;
	}
	
	@Override
	public List<DavResource> getChildren(DavContext ctxt) {
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
		res.add(new SearchWrapper(generateUri(YEAR), getOwner()));
		//res.add(new SearchWrapper(generateUri(ALL), getOwner()));
		return res;
	}
	
	private List<DavResource> browseBySender(DavContext ctxt) throws IOException, ServiceException {
		ArrayList<DavResource> res = new ArrayList<DavResource>();
		String user = ctxt.getUser();
		Provisioning prov = Provisioning.getInstance();
		Account account = prov.get(AccountBy.name, user);
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
		BrowseResult br = mbox.browse(ctxt.getOperationContext(), Mailbox.BrowseBy.domains, "", 0);
		for (BrowseTerm bt : br.getResult()) {
		    if (bt instanceof DomainItem) {
		        DomainItem di = (DomainItem) bt;
		        if (di.isFrom())
		            res.add(new BrowseWrapper(generateUri(di.getDomain()), getOwner()));
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
		BrowseResult br = mbox.browse(ctxt.getOperationContext(), Mailbox.BrowseBy.attachments, "", 0);
		for (BrowseTerm bt: br.getResult()) {
		    String ctype = bt.term;
		    int index = ctype.indexOf('/');
		    if (index != -1 || ctype.equals("message") || ctype.equals("none")) {
		        continue;
		        // the client still gets confused about having a slash in the
		        // path even after encoding
		        //ctype = ctype.substring(0,index) + "%2F" + ctype.substring(index+1);
		    }
		    res.add(new BrowseWrapper(generateUri(ctype), getOwner()));
		}
		res.add(new BrowseWrapper(generateUri("word"), getOwner()));
		res.add(new BrowseWrapper(generateUri("excel"), getOwner()));
		res.add(new BrowseWrapper(generateUri("ppt"), getOwner()));
		res.add(new BrowseWrapper(generateUri("pdf"), getOwner()));
		return res;
	}
}
