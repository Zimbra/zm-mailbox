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
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.ServiceException;

public class UrlNamespace {
	public static final String DAV_PATH = "/service/dav";
	
	public static final String ATTACHMENTS_PREFIX = "/attachments";
	
	public static void createResource(DavContext ctxt) throws DavException, IOException {
		String user = ctxt.getUser();
		String target = ctxt.getPath();
		
		if (user == null || target == null)
			throw new DavException("invalid uri", HttpServletResponse.SC_NOT_ACCEPTABLE, null);
		
		Provisioning prov = Provisioning.getInstance();
		Mailbox mbox = null;
		try {
			Account account = prov.get(AccountBy.name, user);
			if (account == null)
				throw new DavException("no such account "+user, HttpServletResponse.SC_NOT_FOUND, null);

			mbox = MailboxManager.getInstance().getMailboxByAccount(account);
		} catch (ServiceException e) {
			throw new DavException("cannot get mailbox", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
		}

		String author = ctxt.getAuthAccount().getName();
		String ctype = ctxt.getRequest().getContentType();
		int clen = ctxt.getRequest().getContentLength();
		byte[] data = ByteUtil.getContent(ctxt.getRequest().getInputStream(), clen);
		if (ctype == null)
			ctype = URLConnection.getFileNameMap().getContentTypeFor(target);
		if (ctype == null)
			ctype = DavProtocol.DEFAULT_CONTENT_TYPE;
		try {
			// add a revision if the resource already exists
			MailItem item = mbox.getItemByPath(ctxt.getOperationContext(), target, 0, false);
			if (item.getType() != MailItem.TYPE_DOCUMENT && item.getType() != MailItem.TYPE_WIKI)
				throw new DavException("no DAV resource for "+MailItem.getNameForType(item.getType()), HttpServletResponse.SC_NOT_ACCEPTABLE, null);
			mbox.addDocumentRevision(ctxt.getOperationContext(), (Document)item, data, author);
			ctxt.setStatus(HttpServletResponse.SC_OK);
		} catch (ServiceException e) {
			if (!(e instanceof NoSuchItemException))
				throw new DavException("cannot get item ", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
			
			// create
			int index = target.lastIndexOf("/");
			if (index == -1)
				throw new DavException("invalid uri", HttpServletResponse.SC_NOT_ACCEPTABLE, null);
			String name= target.substring(index+1);
			try {
				String path = target.substring(0, index);
				MailItem folder = mbox.getItemByPath(ctxt.getOperationContext(), path, 0, false);
				mbox.createDocument(ctxt.getOperationContext(), folder.getId(), name, ctype, author, data, null);
				ctxt.setStatus(HttpServletResponse.SC_CREATED);
			} catch (ServiceException se) {
				throw new DavException("cannot create ", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se);
			}
		}
	}
	
	public static DavResource getResource(DavContext ctxt) throws DavException {
		return getResourceAt(ctxt, ctxt.getPath());
	}
	
	public static DavResource getResourceAt(DavContext ctxt, String path) throws DavException {
		String target = path;
		if (target == null)
			throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, null);
		
		DavResource resource = null;
		
		if (target.startsWith(ATTACHMENTS_PREFIX)) {
			resource = getPhantomResource(ctxt);
		} else {
			MailItem item = getMailItem(ctxt, target);
			resource = getResourceFromMailItem(item);
		}
		
		if (resource == null)
			throw new DavException("no DAV resource for "+target, HttpServletResponse.SC_NOT_FOUND, null);
		
		return resource;
	}
	
	public static void deleteResource(DavContext ctxt) throws DavException {
		String user = ctxt.getUser();
		if (user == null)
			throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, null);
		try {
			Provisioning prov = Provisioning.getInstance();
			Account account = prov.get(AccountBy.name, user);
			if (account == null)
				throw new DavException("no such account "+user, HttpServletResponse.SC_NOT_FOUND, null);

			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			MailItem item = mbox.getItemByPath(ctxt.getOperationContext(), ctxt.getPath(), 0, false);
			mbox.delete(ctxt.getOperationContext(), item.getId(), item.getType());
		} catch (ServiceException e) {
			throw new DavException("cannot get item", HttpServletResponse.SC_NOT_FOUND, e);
		}
	}
	
	public static String getHomeUrl(String user) throws DavException {
		StringBuilder buf = new StringBuilder();
		buf.append(DAV_PATH).append("/").append(user);
		try {
			Provisioning prov = Provisioning.getInstance();
			return URLUtil.getMailURL(prov.getLocalServer(), buf.toString(), true);
		} catch (ServiceException e) {
			throw new DavException("cannot create URL for user "+user, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}
	
	public static final String ACL_USER   = "/acl/user";
	public static final String ACL_GROUP  = "/acl/group";
	public static final String ACL_COS    = "/acl/cos";
	public static final String ACL_DOMAIN = "/acl/domain";
	
	public static String getAclUrl(String principal, String type) throws DavException {
		StringBuilder buf = new StringBuilder();
		buf.append(type).append("/").append(principal);
		try {
			Provisioning prov = Provisioning.getInstance();
			return URLUtil.getMailURL(prov.getLocalServer(), buf.toString(), true);
		} catch (ServiceException e) {
			throw new DavException("cannot create ACL URL for principal "+principal, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}
	
	public static String getResourceUrl(DavResource rs) throws DavException {
		return getHomeUrl(rs.getOwner()) + rs.getUri();
	}
	
	public static DavResource getResourceFromMailItem(MailItem item) throws DavException {
		DavResource resource = null;
		byte itemType = item.getType();
		
		try {
			switch (itemType) {
			case MailItem.TYPE_FOLDER :
			case MailItem.TYPE_MOUNTPOINT :
				Folder f = (Folder) item;
				if (f.getDefaultView() == MailItem.TYPE_APPOINTMENT)
					resource = new CalendarCollection((Folder)item);
				else
					resource = new Collection((Folder)item);
				break;
			case MailItem.TYPE_WIKI :
			case MailItem.TYPE_DOCUMENT :
				resource = new Notebook((Document)item);
				break;
			case MailItem.TYPE_APPOINTMENT :
				resource = new CalendarObject((Appointment)item);
				break;
			}
		} catch (ServiceException e) {
			resource = null;
			ZimbraLog.dav.info("cannot create DavResource", e);
		}
		return resource;
	}
	
	private static DavResource getPhantomResource(DavContext ctxt) throws DavException {
		DavResource resource;
		String user = ctxt.getUser();
		String target = ctxt.getPath();
		
		ArrayList<String> tokens = new ArrayList<String>();
		StringTokenizer tok = new StringTokenizer(target, "/");
		int numTokens = tok.countTokens();
		while (tok.hasMoreTokens()) {
			tokens.add(tok.nextToken());
		}
		
		//
		// return BrowseResource
		//
		// /attachments/
		// /attachments/by-date/
		// /attachments/by-type/
		// /attachments/by-sender/

		//
		// return SearchResource
		//
		// /attachments/by-date/today/
		// /attachments/by-type/image%2Fgif/
		// /attachments/by-sender/zimbra.com/
		
		//
		// return AttachmentResource
		//
		// /attachments/by-date/today/image.gif
		// /attachments/by-type/image%2Fgif/image.gif
		// /attachments/by-sender/zimbra.com/image.gif

		switch (numTokens) {
		case 1:
		case 2:
			resource = new BrowseWrapper(target, user, tokens);
			break;
		case 3:
			resource = new SearchWrapper(target, user, tokens);
			break;
		case 4:
			resource = new Attachment(target, user, tokens, ctxt);
			break;
		default:
			resource = null;
		}
		
		return resource;
	}
	
	private static MailItem getMailItem(DavContext ctxt, String path) throws DavException {
		try {
			String user = ctxt.getUser();
			Provisioning prov = Provisioning.getInstance();
			Account account = prov.get(AccountBy.name, user);
			if (account == null)
				throw new DavException("no such account "+user, HttpServletResponse.SC_NOT_FOUND, null);

			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);

			Mailbox.OperationContext octxt = ctxt.getOperationContext();
			int index = path.lastIndexOf('/');
			Folder f = null;
			if (index != -1) {
				try {
					f = mbox.getFolderByPath(octxt, path.substring(0, index));
				} catch (MailServiceException.NoSuchItemException e) {
				}
			}
			if (f != null && 
					f.getDefaultView() == MailItem.TYPE_APPOINTMENT && 
					path.endsWith(CalendarObject.CAL_EXTENSION)) {
				String uid = path.substring(index + 1, path.length() - CalendarObject.CAL_EXTENSION.length());
				return mbox.getAppointmentByUid(octxt, uid);
			}
			return mbox.getItemByPath(octxt, path, 0, false);
		} catch (ServiceException e) {
			throw new DavException("cannot get item", HttpServletResponse.SC_NOT_FOUND, e);
		}
	}
}
