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
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException.NoSuchItemException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.service.ServiceException;

public class UrlNamespace {
	public static final String DAV_PATH = "/service/dav";
	public static final String DEFAULT_APP_PATH = "/pub";
	
	private static class ResourceRef {
		public String user;
		public String app;
		public String target;
	}
	
	private static ResourceRef parseResourcePath(DavContext ctxt) {
		String uri = ctxt.getUri();
		if (uri.length() <= 1)
			return null;
		
		int index = uri.indexOf('/', 1);
		if (index == -1)
			return null;
		
		ResourceRef ref = new ResourceRef();
		ref.user = uri.substring(1, index);
		
		int begin = index + 1;
		index = uri.indexOf('/', begin);
		if (index == -1) {
			ref.app = uri.substring(begin);
			ref.target = "/";
		} else {
			ref.app = uri.substring(begin, index);
			ref.target = uri.substring(index);
		}
		ctxt.setUser(ref.user);
		return ref;
	}
	
	public static void createResource(DavContext ctxt) throws DavException, IOException {
		ResourceRef ref = parseResourcePath(ctxt);
		if (ref == null)
			throw new DavException("invalid uri", HttpServletResponse.SC_NOT_ACCEPTABLE, null);
		
		String user = ref.user;
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
			ctype = URLConnection.getFileNameMap().getContentTypeFor(ref.target);
		if (ctype == null)
			ctype = DavProtocol.DEFAULT_CONTENT_TYPE;
		try {
			// add a revision if the resource already exists
			MailItem item = mbox.getItemByPath(ctxt.getOperationContext(), ref.target, 0, false);
			if (item.getType() != MailItem.TYPE_DOCUMENT && item.getType() != MailItem.TYPE_WIKI)
				throw new DavException("no DAV resource for "+MailItem.getNameForType(item.getType()), HttpServletResponse.SC_NOT_ACCEPTABLE, null);
			mbox.addDocumentRevision(ctxt.getOperationContext(), (Document)item, data, author);
			ctxt.setStatus(HttpServletResponse.SC_OK);
		} catch (ServiceException e) {
			if (!(e instanceof NoSuchItemException))
				throw new DavException("cannot get item ", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, null);
			
			// create
			int index = ref.target.lastIndexOf("/");
			if (index == -1)
				throw new DavException("invalid uri", HttpServletResponse.SC_NOT_ACCEPTABLE, null);
			String name= ref.target.substring(index+1);
			try {
				String path = ref.target.substring(0, index);
				MailItem folder = mbox.getItemByPath(ctxt.getOperationContext(), path, 0, false);
				mbox.createDocument(ctxt.getOperationContext(), folder.getId(), name, ctype, author, data, null);
				ctxt.setStatus(HttpServletResponse.SC_CREATED);
			} catch (ServiceException se) {
				throw new DavException("cannot create ", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se);
			}
		}
	}
	
	public static DavResource getResource(DavContext ctxt) throws DavException {
		ResourceRef ref = parseResourcePath(ctxt);
		if (ref == null)
			throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, null);
		MailItem item = getMailItem(ctxt, ref.target);
		DavResource resource = getResourceFromMailItem(ref.target, item);
		
		if (resource == null)
			throw new DavException("no DAV resource for "+MailItem.getNameForType(item.getType()), HttpServletResponse.SC_NOT_FOUND, null);
		
		return resource;
	}
	
	public static void deleteResource(DavContext ctxt) throws DavException {
		ResourceRef ref = parseResourcePath(ctxt);
		if (ref == null)
			throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, null);
		try {
			String user = ctxt.getUser();
			Provisioning prov = Provisioning.getInstance();
			Account account = prov.get(AccountBy.name, user);
			if (account == null)
				throw new DavException("no such account "+user, HttpServletResponse.SC_NOT_FOUND, null);

			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			MailItem item = mbox.getItemByPath(ctxt.getOperationContext(), ref.target, 0, false);
			mbox.delete(ctxt.getOperationContext(), item.getId(), item.getType());
		} catch (ServiceException e) {
			throw new DavException("cannot get item", HttpServletResponse.SC_NOT_FOUND, e);
		}
	}
	
	public static List<DavResource> getChildren(DavContext ctxt, DavResource parent) throws DavException {
		ArrayList<DavResource> children = new ArrayList<DavResource>();
		if (parent.isCollection()) {
			MailItem parentItem = getMailItem(ctxt, parent.getUri());
			byte itemType = parentItem.getType();
			if (itemType != MailItem.TYPE_FOLDER && itemType != MailItem.TYPE_MOUNTPOINT)
				return children;
			
			List<MailItem> items = getMailItemsInFolder(ctxt, parentItem.getId());
			for (MailItem item : items) {
				DavResource rs = getResourceFromMailItem(null, item);
				if (rs != null)
					children.add(rs);
			}
		}
		return children;
	}
	
	private static String getHomeUrl(String user) throws DavException {
		StringBuilder buf = new StringBuilder();
		buf.append(DAV_PATH).append("/");
		buf.append(user).append(DEFAULT_APP_PATH);
		try {
			Provisioning prov = Provisioning.getInstance();
			return URLUtil.getMailURL(prov.getLocalServer(), buf.toString(), true);
		} catch (ServiceException e) {
			throw new DavException("cannot create URL for user "+user, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}
	
	public static String getResourceUrl(DavResource rs) throws DavException {
		return getHomeUrl(rs.getOwner()) + rs.getUri();
	}
	
	private static DavResource getResourceFromMailItem(String target, MailItem item) throws DavException {
		DavResource resource = null;
		byte itemType = item.getType();
		
		try {
			Account acct = item.getAccount();
			String owner = acct.getName();
			Provisioning prov = Provisioning.getInstance();
	        Config config = prov.getConfig();
	        String defaultDomain = config.getAttr(Provisioning.A_zimbraDefaultDomainName, null);
	        if (defaultDomain != null && defaultDomain.equalsIgnoreCase(acct.getDomainName()))
	        	owner = owner.substring(0, owner.indexOf('@'));
			switch (itemType) {
			case (MailItem.TYPE_FOLDER) :
			case (MailItem.TYPE_MOUNTPOINT) :
				resource = new FolderResource((Folder)item, owner);
			break;
			case (MailItem.TYPE_WIKI) :
			case (MailItem.TYPE_DOCUMENT) :
				resource = new NotebookResource((Document)item, owner);
			break;
			}
		} catch (ServiceException e) {
			resource = null;
			ZimbraLog.dav.info("cannot create DavResource", e);
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
			
			return mbox.getItemByPath(ctxt.getOperationContext(), path, 0, false);
		} catch (ServiceException e) {
			throw new DavException("cannot get item", HttpServletResponse.SC_NOT_FOUND, e);
		}
	}
	
	private static List<MailItem> getMailItemsInFolder(DavContext ctxt, int fid) throws DavException {
		try {
			String user = ctxt.getUser();
			Provisioning prov = Provisioning.getInstance();
			Account account = prov.get(AccountBy.name, user);
			if (account == null)
				throw new DavException("no such account "+user, HttpServletResponse.SC_NOT_FOUND, null);

			Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
			
			// XXX aggregate into single call
			List<MailItem> ret = new ArrayList<MailItem>();
			ret.addAll(mbox.getItemList(ctxt.getOperationContext(), MailItem.TYPE_FOLDER, fid));
			ret.addAll(mbox.getItemList(ctxt.getOperationContext(), MailItem.TYPE_MOUNTPOINT, fid));
			ret.addAll(mbox.getItemList(ctxt.getOperationContext(), MailItem.TYPE_DOCUMENT, fid));
			ret.addAll(mbox.getItemList(ctxt.getOperationContext(), MailItem.TYPE_WIKI, fid));
			return ret;
		} catch (ServiceException e) {
			throw new DavException("cannot get items", HttpServletResponse.SC_NOT_FOUND, e);
		}
	}
}
