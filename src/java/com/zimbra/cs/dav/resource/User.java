/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;
import org.dom4j.QName;

import com.zimbra.common.auth.ZAuthToken;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.property.Acl;
import com.zimbra.cs.dav.property.CalDavProperty;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.dav.property.VersioningProperty;
import com.zimbra.cs.mailbox.ACL;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.ACL.Grant;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

public class User extends Principal {

    public User(DavContext ctxt, Account account, String url) throws ServiceException {
    	super(account, url);
    	mAccount = account;
        String user = getOwner();
        addProperty(CalDavProperty.getCalendarHomeSet(user));
        addProperty(CalDavProperty.getCalendarUserType(this));
        addProperty(CalDavProperty.getScheduleInboxURL(user));
        addProperty(CalDavProperty.getScheduleOutboxURL(user));
        if (ctxt.getAuthAccount().equals(account)) {
            addProperty(VersioningProperty.getSupportedReportSet());
            if (ctxt.useIcalDelegation()) {
                addProperty(new CalendarProxyReadFor(mAccount));
                addProperty(new CalendarProxyWriteFor(mAccount));
                addProperty(new ProxyGroupMembership());
            }
        }
		addProperty(Acl.getPrincipalUrl(this));
        ArrayList<String> addrs = new ArrayList<String>();
        for (String addr : account.getMailDeliveryAddress())
            addrs.add(addr);
        for (String alias : account.getMailAlias())
            addrs.add(alias);
        addrs.add(url);
        addProperty(CalDavProperty.getCalendarUserAddressSet(addrs));
        setProperty(DavElements.E_HREF, url);
        String cn = account.getAttr(Provisioning.A_cn);
        if (cn == null)
            cn = account.getName();
        setProperty(DavElements.E_DISPLAYNAME, cn);
        mUri = url;
    }
    
    @Override
	public java.util.Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
		ArrayList<DavResource> proxies = new ArrayList<DavResource>();
		if (ctxt.useIcalDelegation()) {
			try {
				proxies.add(new CalendarProxyRead(getOwner(), mUri));
				proxies.add(new CalendarProxyWrite(getOwner(), mUri));
			} catch (ServiceException e) {
			}
		}
		return proxies;
	}
    
    @Override
    public void delete(DavContext ctxt) throws DavException {
        throw new DavException("cannot delete this resource", HttpServletResponse.SC_FORBIDDEN, null);
    }

    @Override
    public boolean isCollection() {
        return true;
    }
    
    @Override
	public void patchProperties(DavContext ctxt, Collection<Element> set, Collection<QName> remove) throws DavException, IOException {
    	if (true)
    		return;
    	// supporting ACL manipulation in CalDAV will be confusing.  In Zimbra model we send out
    	// share notification email, and recipient actively accepts the share.  But iCal
    	// supports proxy read/write groups and memberships of each users.  We can partly
    	// support iCal models for existing shares.  for new shares, we can try sending out
    	// new share notification email, but that doesn't work well with YCC who don't
    	// support emails today.  punt this until later.
    	String path = ctxt.getPath();
    	if (!path.endsWith(CALENDAR_PROXY_READ + "/") &&
    			!path.endsWith(CALENDAR_PROXY_WRITE + "/")) {
    		return;
    	}
    	short perm = path.endsWith(CALENDAR_PROXY_READ + "/") ? ACL.RIGHT_READ : ACL.RIGHT_READ | ACL.RIGHT_WRITE;
		for (Element setElem : set) {
			if (setElem.getQName().equals(DavElements.E_GROUP_MEMBER_SET)) {
			    @SuppressWarnings("unchecked")
				Iterator hrefs = setElem.elementIterator(DavElements.E_HREF);
				while (hrefs.hasNext()) {
					Element href = (Element) hrefs.next();
					String principalPath = href.getText();
					DavResource principal = null;
					try {
						UrlNamespace.getPrincipalAtUrl(ctxt, principalPath);
					} catch (DavException e) {
	            		ZimbraLog.dav.warn("can't find principal at %s", path);
						continue;
					}
					if (!(principal instanceof User)) {
	            		ZimbraLog.dav.warn("not a user principal path %s", path);
						continue;
					}
					Account target = ((User)principal).mAccount;
		        	try {
		            	Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);
		            	// share default Calendar folder and Tasks folder
		            	mbox.grantAccess(ctxt.getOperationContext(), Mailbox.ID_FOLDER_CALENDAR, target.getId(), ACL.GRANTEE_USER, perm, null);
		            	mbox.grantAccess(ctxt.getOperationContext(), Mailbox.ID_FOLDER_TASKS, target.getId(), ACL.GRANTEE_USER, perm, null);
		        	} catch (ServiceException se) {
	            		ZimbraLog.dav.warn("can't modify acl on %s for %s", mAccount.getName(), path);
		        	}
				}
			}
		}
	}
	
    private static final String CALENDAR_PROXY_READ  = "calendar-proxy-read";
    private static final String CALENDAR_PROXY_WRITE = "calendar-proxy-write";
    
    private class CalendarProxyRead extends Principal {
    	public CalendarProxyRead(String user, String url) throws ServiceException {
    		super(user, url+"calendar-proxy-read");
            addResourceType(DavElements.E_CALENDAR_PROXY_READ);
            addProperty(VersioningProperty.getSupportedReportSet());
    		addProperty(Acl.getPrincipalUrl(this));
    		addProperty(new ProxyGroupMemberSet(true));
    	}
        @Override
        public boolean isCollection() {
            return true;
        }
    }
    
    private class CalendarProxyWrite extends Principal {
    	public CalendarProxyWrite(String user, String url) throws ServiceException {
    		super(user, url+"calendar-proxy-write");
            addResourceType(DavElements.E_CALENDAR_PROXY_WRITE);
            addProperty(VersioningProperty.getSupportedReportSet());
    		addProperty(Acl.getPrincipalUrl(this));
    		addProperty(new ProxyGroupMemberSet(false));
    	}
        @Override
        public boolean isCollection() {
            return true;
        }
    }
    
    private class ProxyProperty extends ResourceProperty {
    	public ProxyProperty(QName name) {
    		super(name);
    	}
    	protected ArrayList<Pair<Mountpoint,ZFolder>> getMountpoints(DavContext ctxt) {
    		ArrayList<Pair<Mountpoint,ZFolder>> mps = new ArrayList<Pair<Mountpoint,ZFolder>>();
        	try {
            	Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);
            	for (MailItem item : mbox.getItemList(ctxt.getOperationContext(), MailItem.TYPE_MOUNTPOINT)) {
            		Mountpoint mp = (Mountpoint)item;
            		ZAuthToken zat = AuthProvider.getAuthToken(ctxt.getAuthAccount()).toZAuthToken();
            		ZMailbox zmbx = RemoteCollection.getRemoteMailbox(zat, mp.getOwnerId());
            		ZFolder folder = zmbx.getFolderById(mp.getTarget().toString(mAccount));
            		mps.add(new Pair<Mountpoint,ZFolder>(mp, folder));
            	}
        	} catch (ServiceException se) {
        		ZimbraLog.dav.warn("can't get mailbox", se);
        	}
    		return mps;
    	}
    }
    
    private class ProxyGroupMembership extends ProxyProperty {
    	public ProxyGroupMembership() {
    		super(DavElements.E_GROUP_MEMBERSHIP);
    		setProtected(true);
    	}
        @Override
    	public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
			Element group = super.toElement(ctxt, parent, true);
			if (nameOnly)
				return group;
        	ArrayList<Pair<Mountpoint,ZFolder>> mps = getMountpoints(ctxt);
        	for (Pair<Mountpoint,ZFolder> folder : mps) {
        		try {
        			short rights = ACL.stringToRights(folder.getSecond().getEffectivePerms());
        			if ((rights & ACL.RIGHT_WRITE) > 0) {
        				Account owner = Provisioning.getInstance().get(AccountBy.id, folder.getFirst().getOwnerId());
        				if (owner != null)
        					group.addElement(DavElements.E_HREF).setText(UrlNamespace.getPrincipalUrl(mAccount, owner)+"calendar-proxy-write");
        			} else if ((rights & ACL.RIGHT_READ) > 0) {
        				Account owner = Provisioning.getInstance().get(AccountBy.id, folder.getFirst().getOwnerId());
        				if (owner != null)
        					group.addElement(DavElements.E_HREF).setText(UrlNamespace.getPrincipalUrl(mAccount, owner)+"calendar-proxy-read");
        			}
        		} catch (ServiceException se) {
            		ZimbraLog.dav.warn("can't convert rights", se);
        		}
        	}
    		return group;
        }
    }
    private class ProxyGroupMemberSet extends ResourceProperty {
    	public ProxyGroupMemberSet(boolean readOnly) {
    		super(DavElements.E_GROUP_MEMBER_SET);
    		mReadOnly = readOnly;
    		setProtected(true);
    	}
    	private boolean mReadOnly;
        @Override
    	public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
        	Element group = super.toElement(ctxt, parent, nameOnly);
        	if (nameOnly)
        		return group;
        	try {
            	Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(mAccount);
            	Folder f = mbox.getFolderById(ctxt.getOperationContext(), Mailbox.ID_FOLDER_CALENDAR);
            	if (f == null)
            		return group;
            	ACL acl = f.getEffectiveACL();
            	if (acl == null)
            		return group;
            	for (Grant g : acl.getGrants()) {
            		if (g.getGranteeType() != ACL.GRANTEE_USER)
            			continue;
            		boolean match = mReadOnly ?
            			(g.getGrantedRights() & ACL.RIGHT_READ) != 0 && (g.getGrantedRights() & ACL.RIGHT_WRITE) == 0 :
            			(g.getGrantedRights() & ACL.RIGHT_WRITE) != 0;
            		if (match) {
        				Account user = Provisioning.getInstance().get(AccountBy.id, g.getGranteeId());
            			group.addElement(DavElements.E_HREF).setText(UrlNamespace.getPrincipalUrl(mAccount, user));
            		}
            	}
        	} catch (ServiceException se) {
        		ZimbraLog.dav.warn("can't get mailbox", se);
        	}
        	return group;
        }
    }
    private class CalendarProxyReadFor extends ProxyProperty {
    	public CalendarProxyReadFor(Account acct) {
    		super(DavElements.E_CALENDAR_PROXY_READ_FOR);
    	}
        @Override
    	public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
			Element proxy = super.toElement(ctxt, parent, true);
			if (nameOnly)
				return proxy;
        	ArrayList<Pair<Mountpoint,ZFolder>> mps = getMountpoints(ctxt);
        	for (Pair<Mountpoint,ZFolder> folder : mps) {
        		try {
        			short rights = ACL.stringToRights(folder.getSecond().getEffectivePerms());
        			if ((rights & ACL.RIGHT_READ) > 0) {
        				Account owner = Provisioning.getInstance().get(AccountBy.id, folder.getFirst().getOwnerId());
        				if (owner != null)
        					proxy.addElement(DavElements.E_HREF).setText(UrlNamespace.getPrincipalUrl(mAccount, owner));
        			}
        		} catch (ServiceException se) {
            		ZimbraLog.dav.warn("can't convert rights", se);
        		}
        	}
    		return proxy;
    	}
    }
    
    private class CalendarProxyWriteFor extends ProxyProperty {
    	public CalendarProxyWriteFor(Account acct) {
    		super(DavElements.E_CALENDAR_PROXY_WRITE_FOR);
    	}
        @Override
    	public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
			Element proxy = super.toElement(ctxt, parent, true);
			if (nameOnly)
				return proxy;
        	ArrayList<Pair<Mountpoint,ZFolder>> mps = getMountpoints(ctxt);
        	for (Pair<Mountpoint,ZFolder> folder : mps) {
        		try {
        			short rights = ACL.stringToRights(folder.getSecond().getEffectivePerms());
        			if ((rights & ACL.RIGHT_WRITE) > 0) {
        				Account owner = Provisioning.getInstance().get(AccountBy.id, folder.getFirst().getOwnerId());
        				if (owner != null)
        					proxy.addElement(DavElements.E_HREF).setText(UrlNamespace.getPrincipalUrl(mAccount, owner));
        			}
        		} catch (ServiceException se) {
            		ZimbraLog.dav.warn("can't convert rights", se);
        		}
        	}
    		return proxy;
    	}
    }
}
