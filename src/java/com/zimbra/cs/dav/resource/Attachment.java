/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2009, 2010 Zimbra, Inc.
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;

/**
 * Attachment is a DAV resource that represents email attachments.
 * 
 * Using WebDAV client, email attachments in the mailbox can be browsed in
 * file manager like interface.  There are predefined phantom folders like
 * /attachment/by-type/image/today whose contents are dynamically updated
 * just like smart folders.  In those phantom folders the email attachments
 * are listed as individual files.
 * 
 * @author jylee
 *
 */
public class Attachment extends PhantomResource {

	private byte[] mContent;
	
	public Attachment(String uri, String owner, long date, int contentLength) {
		this(uri, owner, parseUri(uri));
		setCreationDate(date);
		setLastModifiedDate(date);
		setProperty(DavElements.P_GETCONTENTLENGTH, Integer.toString(contentLength));
	}
	
	public Attachment(String uri, String owner, List<String> tokens, DavContext ctxt) throws DavException {
		this(uri, owner, tokens);
		String user = ctxt.getUser();
		Provisioning prov = Provisioning.getInstance();
		
		String name = tokens.get(tokens.size()-1);
		StringBuilder query = new StringBuilder();
		boolean needQuotes = (name.indexOf(' ') > 0);
		
		// XXX filename: query won't match the attachments in winmail.dat
		// bug 11406
		query.append("filename:");
		if (needQuotes)
			query.append("'");
		query.append(name);
		if (needQuotes)
			query.append("'");
		ZimbraQueryResults zqr = null;
		boolean found = false;
		try {
			Account account = prov.get(AccountBy.name, user);
			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			// if more than one attachments with the same name, take the first one.
			zqr = mbox.search(ctxt.getOperationContext(), query.toString(), SEARCH_TYPES, SortBy.NAME_ASCENDING, 10);
			if (zqr.hasNext()) {
				ZimbraHit hit = zqr.getNext();
				if (hit instanceof MessageHit) {
					Message message = ((MessageHit)hit).getMessage();
					setCreationDate(message.getDate());
					setLastModifiedDate(message.getChangeDate());
					MimeMessage msg = message.getMimeMessage();
					List<MPartInfo> parts = Mime.getParts(msg);
					for (MPartInfo p : parts) {
						String fname = p.getFilename();
						if (name.equals(fname)) {
							String partName = p.getPartName();
							mContent = ByteUtil.getContent(Mime.getMimePart(msg, partName).getInputStream(), 0);
							setProperty(DavElements.P_GETCONTENTLENGTH, Integer.toString(mContent.length));
							setProperty(DavElements.P_GETCONTENTTYPE, p.getContentType());
							found = true;
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			ZimbraLog.dav.error("can't search for: attachment="+name, e);
		} finally {
			if (zqr != null)
				try {
					zqr.doneWithSearchResults();
				} catch (ServiceException e) {}
		}
		if (!found) 
			throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, null);
	}

	public Attachment(String uri, String owner, List<String> tokens) {
		super(uri, owner, tokens);
	}
	
	@Override
	public InputStream getContent(DavContext ctxt) {
		return new ByteArrayInputStream(mContent);
	}

	@Override
	public boolean isCollection() {
		return false;
	}

}
