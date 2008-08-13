/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.property;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.resource.DavResource;
import com.zimbra.cs.dav.resource.UrlNamespace;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.ACL.Grant;

/*
 * The following are the required properties for supporting DAV access-control.
 * 
 * DAV:owner - RFC3744 section 5.1
 * DAV:group - RFC3744 section 5.2
 * DAV:supported-privilege-set - RFC3744 section 5.3
 * DAV:current-user-privilege-set - RFC3744 section 5.4
 * DAV:acl - RFC3744 section 5.5
 * DAV:acl-restrictions - RFC3744 section 5.6
 * DAV:inherited-acl-set- RFC3744 section 5.7
 * DAV:principal-collection-set - RFC3744 section 5.8
 */
public class Acl extends ResourceProperty {
	public static Set<ResourceProperty> getAclProperties(DavResource rs, Folder folder) throws ServiceException, DavException {
		HashSet<ResourceProperty> props = new HashSet<ResourceProperty>();
		if (folder == null)
			return props;
		
		String owner = rs.getOwner();
		ACL acl = folder.getEffectiveACL();
		props.add(getSupportedPrivilegeSet());
		if (folder != null) {
			props.add(getCurrentUserPrivilegeSet(acl, folder.getAccount()));
			props.add(getPrincipalCollectionSet(folder.getAccount()));
		}
		props.add(getAcl(acl, owner));
		props.add(getAclRestrictions());
		
		ResourceProperty p = new ResourceProperty(DavElements.E_OWNER);
		p.setProtected(true);
		Element href = p.addChild(DavElements.E_HREF);
		href.setText(UrlNamespace.getPrincipalUrl(owner));
		props.add(p);
		
		// empty properties
		p = new ResourceProperty(DavElements.E_GROUP);
		p.setProtected(true);
		props.add(p);
		p = new ResourceProperty(DavElements.E_INHERITED_ACL_SET);
		p.setProtected(true);
		props.add(p);
		return props;
	}
	
	public static ResourceProperty getAcl(ACL acl, String owner) {
		return new Acl(acl, owner);
	}
	public static ResourceProperty getSupportedPrivilegeSet() {
		return new SupportedPrivilegeSet();
	}
	
	public static ResourceProperty getCurrentUserPrivilegeSet(ACL acl, Account owner) {
		return new CurrentUserPrivilegeSet(acl, owner);
	}
	
	public static ResourceProperty getAclRestrictions() {
		return new AclRestrictions();
	}
	
	public static ResourceProperty getPrincipalCollectionSet(Account acct) {
		return new PrincipalCollectionSet(acct);
	}
	
	protected ACL mAcl;
	protected String mOwner;
	
	private Acl(ACL acl, String owner) {
		this(DavElements.E_ACL, acl, owner);
	}

	private Acl(QName name, ACL acl, String owner) {
		super(name);
		setProtected(true);
		mAcl = acl;
		mOwner = owner;
	}
	
	public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
		Element acl = super.toElement(ctxt, parent, true);
		
		if (mAcl == null)
			return acl;

        for (Grant g : mAcl.getGrants()) {
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
				case ACL.GRANTEE_KEY:
					// 30049 TODO
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
				
				addGrantDeny(ace, g, true);
			} catch (DavException e) {
				ZimbraLog.dav.error("can't add principal: grantee="+g.getGranteeId(), e);
			} catch (ServiceException e) {
				ZimbraLog.dav.error("can't add principal: grantee="+g.getGranteeId(), e);
			}
		}
		return acl;
	}

	protected Element addGrantDeny(Element ace, Grant g, boolean isGrant) {
		Element grant = isGrant ? ace.addElement(DavElements.E_GRANT) : ace.addElement(DavElements.E_DENY);
		addPrivileges(grant, g.getGrantedRights());
		return grant;
	}
	
	protected Element addPrivileges(Element grant, short rights) {
		if ((rights & ACL.RIGHT_READ) > 0) {
			grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_READ);
			grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_READ_CURRENT_USER_PRIVILEGE_SET);
			grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_READ_FREE_BUSY);
		}
		if ((rights & ACL.RIGHT_WRITE) > 0) {
			grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_BIND);
			grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_UNBIND);
			grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_WRITE);
			grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_WRITE_ACL);
			grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_WRITE_CONTENT);
			grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_WRITE_PROPERTIES);
		}
		if ((rights & ACL.RIGHT_ADMIN) > 0)
			grant.addElement(DavElements.E_PRIVILEGE).addElement(DavElements.E_UNLOCK);
		return grant;
	}
	
	static protected HashMap<String, Short> sRightsMap;
	
	static {
		sRightsMap = new HashMap<String, Short>();
		sRightsMap.put(DavElements.P_READ, ACL.RIGHT_READ);
		sRightsMap.put(DavElements.P_READ_CURRENT_USER_PRIVILEGE_SET, ACL.RIGHT_READ);
		sRightsMap.put(DavElements.P_READ_FREE_BUSY, ACL.RIGHT_READ);
		sRightsMap.put(DavElements.P_BIND, ACL.RIGHT_WRITE);
		sRightsMap.put(DavElements.P_UNBIND, ACL.RIGHT_WRITE);
		sRightsMap.put(DavElements.P_WRITE, ACL.RIGHT_WRITE);
		sRightsMap.put(DavElements.P_WRITE_ACL, ACL.RIGHT_WRITE);
		sRightsMap.put(DavElements.P_WRITE_CONTENT, ACL.RIGHT_WRITE);
		sRightsMap.put(DavElements.P_WRITE_PROPERTIES, ACL.RIGHT_WRITE);
		sRightsMap.put(DavElements.P_UNLOCK, ACL.RIGHT_WRITE); // XXX
	}
	
	private static class PrincipalCollectionSet extends ResourceProperty {
		public PrincipalCollectionSet(Account acct) {
			super(DavElements.E_PRINCIPAL_COLLECTION_SET);
			setProtected(true);
			Element e = org.dom4j.DocumentHelper.createElement(DavElements.E_HREF);
			try {
				e.setText(UrlNamespace.getPrincipalCollectionUrl(acct));
				mChildren.add(e);
			} catch (ServiceException ex) {
				ZimbraLog.dav.warn("can't generate principal-collection-url", ex);
			}
		}
	}
	private static class SupportedPrivilegeSet extends Acl {

		public SupportedPrivilegeSet() {
			super(DavElements.E_SUPPORTED_PRIVILEGE_SET, null, null);
		}
		
		public Element addPrivilege(Element parent, QName name, String description) {
			Element priv = parent.addElement(DavElements.E_SUPPORTED_PRIVILEGE);
			priv.addElement(DavElements.E_PRIVILEGE).addElement(name);
			Element desc = priv.addElement(DavElements.E_DESCRIPTION);
			desc.addAttribute(DavElements.E_LANG, DavElements.LANG_EN_US);
			desc.setText(description);
			
			return priv;
		}
		
		public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
			Element sps = super.toElement(ctxt, parent, true);
			if (nameOnly)
				return sps;

			if (mAcl == null)
				return sps;
			
			Element all = addPrivilege(sps, DavElements.E_ALL, "any operation");
			addPrivilege(all, DavElements.E_READ, "read calendar, attachment, notebook");
			addPrivilege(all, DavElements.E_WRITE, "add calendar appointment, upload attachment");
			addPrivilege(all, DavElements.E_UNLOCK, "unlock your own resources locked by someone else");
			
			return sps;
		}
	}
	
	private static class CurrentUserPrivilegeSet extends Acl {
		private String mOwnerId;
		public CurrentUserPrivilegeSet(ACL acl, Account owner) {
			super(DavElements.E_CURRENT_USER_PRIVILEGE_SET, acl, owner.getName());
			mOwnerId = owner.getId();
		}

		public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
			Element cups = super.toElement(ctxt, parent, true);
			if (nameOnly)
				return cups;

			if (mAcl == null) {
				// the requestor still has full permission if owner.
				if (ctxt.getAuthAccount().getId().equals(mOwnerId))
					addPrivileges(cups, (short)(ACL.RIGHT_READ | ACL.RIGHT_WRITE | ACL.RIGHT_DELETE | ACL.RIGHT_INSERT));
				return cups;
			}

            for (Grant g : mAcl.getGrants()) {
				try {
					short rights = g.getGrantedRights(ctxt.getAuthAccount());
					if (rights > 0) {
						addPrivileges(cups, rights);
						break;
					}
				} catch (ServiceException e) {
					ZimbraLog.dav.error("can't add principal: grantee="+g.getGranteeId(), e);
				}
			}
			
			return cups;
		}
	}
	/*
	 * DAV:acl-restrictions
	 * RFC3744 section 5.6
	 */
	private static class AclRestrictions extends Acl {
		public AclRestrictions() {
			super(DavElements.E_ACL_RESTRICTIONS, null, null);
		}
		public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
			Element ar = super.toElement(ctxt, parent, true);
			ar.addElement(DavElements.E_GRANT_ONLY);
			ar.addElement(DavElements.E_NO_INVERT);
			return ar;
		}
	}

	public static class Ace {
		private String mPrincipalUrl;
		private String mId;
		private short mRights;
		private byte mGranteeType;
		
		public Ace(Element a) throws DavException {
			Element elem = a.element(DavElements.E_PRINCIPAL);
			if (elem == null)
				throw new DavException("missing principal element", HttpServletResponse.SC_BAD_REQUEST);
			
			List<Element> principal = elem.elements();
			if (principal.size() != 1)
				throw new DavException("invalid principal element", HttpServletResponse.SC_BAD_REQUEST);
			for (Element p : principal) {
				QName name = p.getQName();
				if (name.equals(DavElements.E_HREF)) {
					mPrincipalUrl = elem.getText();
					mGranteeType = ACL.GRANTEE_USER;
					try {
						Account acc = UrlNamespace.getPrincipal(mPrincipalUrl);
						if (acc == null)
							throw new DavException("invalid principal: "+mPrincipalUrl, HttpServletResponse.SC_BAD_REQUEST);
						mId = acc.getId();
					} catch (ServiceException se) {
						throw new DavException("can't find principal: "+mPrincipalUrl, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, se);
					}
				} else if (name.equals(DavElements.E_ALL)) {
					mGranteeType = ACL.GRANTEE_PUBLIC;
				} else if (name.equals(DavElements.E_UNAUTHENTICATED)) {
					// XXX we don't yet support "only for unauthenticated users"
					mGranteeType = ACL.GRANTEE_PUBLIC;
				} else if (name.equals(DavElements.E_AUTHENTICATED)) {
					mGranteeType = ACL.GRANTEE_AUTHUSER;
				} else {
					throw new DavException("unsupported principal: "+name.getName(), HttpServletResponse.SC_NOT_IMPLEMENTED);
				}
			}
			if (elem != null)
				elem = elem.element(DavElements.E_HREF);
			

			mRights = (short)0;
			elem = a.element(DavElements.E_GRANT);
			if (elem == null)
				throw new DavException("missing grant element", HttpServletResponse.SC_BAD_REQUEST);
			List<Element> priv = elem.elements(DavElements.E_PRIVILEGE);
			for (Element p : priv) {
				List<Element> right = p.elements();
				if (right.size() != 1)
					throw new DavException("number of right elements contained in privilege element is not one", HttpServletResponse.SC_BAD_REQUEST);
				mRights |= sRightsMap.get(right.get(0).getName());
			}
		}
		
		public Ace(String id, short rights, byte type) {
			mId = id;
			mRights = rights;
			mGranteeType = type;
		}
		
		public String getPrincipalUrl() {
			if (mPrincipalUrl != null)
				return mPrincipalUrl;

			switch (mGranteeType) {
			case ACL.GRANTEE_USER:
				try {
					Account acct = Provisioning.getInstance().get(Provisioning.AccountBy.id, mId);
					mPrincipalUrl = UrlNamespace.getPrincipalCollectionUrl(acct);
				} catch (ServiceException se) {
					ZimbraLog.dav.warn("can't lookup account "+mId, se);
				}
				break;
			}
			return mPrincipalUrl;
		}
		
		public String getZimbraId() {
			return mId;
		}
		
		public byte getGranteeType() {
			return mGranteeType;
		}
		
		public short getRights() {
			return mRights;
		}
		
		public boolean hasHref() {
			return mGranteeType == ACL.GRANTEE_USER;
		}
	}
}
