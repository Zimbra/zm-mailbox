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
import java.util.Iterator;
import java.util.List;

import org.dom4j.Element;

import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.ACL.Grant;
import com.zimbra.cs.service.ServiceException;

public class Collection extends DavResource {

	private int mId;
	private ACL mAcl;
	
	public Collection(Folder f, Account acct) throws ServiceException {
		super(getFolderPath(f.getPath()), acct);
		setCreationDate(f.getDate());
		setLastModifiedDate(f.getChangeDate());
		setProperty(DavElements.P_DISPLAYNAME, f.getSubject());
		setProperty(DavElements.P_GETCONTENTLENGTH, "0");
		mId = f.getId();
		mAcl = f.getPermissions();
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

	private Element addPrivileges(Element prop, Grant g) {
		short rights = g.getGrantedRights();
		if ((rights & ACL.RIGHT_READ) > 0)
			prop.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_READ);
		if ((rights & ACL.RIGHT_WRITE) > 0)
			prop.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_WRITE);
		if ((rights & ACL.RIGHT_ADMIN) > 0)
			prop.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_UNLOCK);
		return prop;
	}
	
	public Element addCurrentUserPrivilegeSet(DavContext ctxt, Element prop) {
		Element ps = super.addCurrentUserPrivilegeSet(ctxt, prop);
		
		Iterator<ACL.Grant> iter = mAcl.grantIterator();
		while (iter.hasNext()) {
			Grant g = iter.next();
			try {
				if (g.getGrantedRights(ctxt.getAuthAccount()) > 0) {
					addPrivileges(ps, g);
					break;
				}
			} catch (ServiceException e) {
				ZimbraLog.dav.error("can't add principal: grantee="+g.getGranteeId(), e);
			}
		}
		return ps;
	}
	
	public Element addAcl(DavContext ctxt, Element prop) {
		Element acl = super.addAcl(ctxt, prop);
		
		Iterator<ACL.Grant> iter = mAcl.grantIterator();
		while (iter.hasNext()) {
			Grant g = iter.next();
			try {
				if (g.getGrantedRights(ctxt.getAuthAccount()) == 0)
					continue;
				Element ace = acl.addElement(DavElements.E_ACE);
				Element principal = ace.addElement(DavElements.E_PRINCIPAL);
				Element e;
				switch (g.getGranteeType()) {
				case ACL.GRANTEE_USER:
				case ACL.GRANTEE_GUEST:
					// maybe use different href for guest and internal users.
					e = principal.addElement(DavElements.E_HREF);
					e.setText(UrlNamespace.getAclUrl(g.getGranteeId(), UrlNamespace.ACL_USER));
					break;
				case ACL.GRANTEE_AUTHUSER:
					principal.addElement(DavElements.E_AUTHENTICATED);
					break;
				case ACL.GRANTEE_COS:
					e = principal.addElement(DavElements.E_HREF);
					e.setText(UrlNamespace.getAclUrl(g.getGranteeId(), UrlNamespace.ACL_COS));
					break;
				case ACL.GRANTEE_DOMAIN:
					e = principal.addElement(DavElements.E_HREF);
					e.setText(UrlNamespace.getAclUrl(g.getGranteeId(), UrlNamespace.ACL_DOMAIN));
					break;
				case ACL.GRANTEE_GROUP:
					e = principal.addElement(DavElements.E_HREF);
					e.setText(UrlNamespace.getAclUrl(g.getGranteeId(), UrlNamespace.ACL_GROUP));
					break;
				case ACL.GRANTEE_PUBLIC:
					principal.addElement(DavElements.E_UNAUTHENTICATED);
					break;
				}
				
				Element grant = ace.addElement(DavElements.E_GRANT);
				addPrivileges(grant, g);
			} catch (DavException e) {
				ZimbraLog.dav.error("can't add principal: grantee="+g.getGranteeId(), e);
			} catch (ServiceException e) {
				ZimbraLog.dav.error("can't add principal: grantee="+g.getGranteeId(), e);
			}
		}
		return acl;
	}
}
