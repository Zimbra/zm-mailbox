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
import java.io.InputStream;
import java.util.ArrayList;

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
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.service.AuthProvider;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.zclient.ZFolder;
import com.zimbra.cs.zclient.ZMailbox;

public class User extends Principal {

    public User(DavContext ctxt, Account account, String url) throws ServiceException {
    	super(account, url);
    	mAccount = account;
        String user = getOwner();
        addProperty(CalDavProperty.getCalendarHomeSet(user));
        if (ctxt.getAuthAccount().equals(account)) {
            addProperty(CalDavProperty.getScheduleInboxURL(user));
            addProperty(CalDavProperty.getScheduleOutboxURL(user));
            addProperty(CalDavProperty.getCalendarUserType(this));
            addProperty(VersioningProperty.getSupportedReportSet());
            addProperty(new CalendarProxyReadFor(mAccount));
            addProperty(new CalendarProxyWriteFor(mAccount));
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
		try {
			proxies.add(new CalendarProxyRead(getOwner(), mUri));
			proxies.add(new CalendarProxyWrite(getOwner(), mUri));
		} catch (ServiceException e) {
		}
		return proxies;
	}
    
    @Override
    public void delete(DavContext ctxt) throws DavException {
        throw new DavException("cannot delete this resource", HttpServletResponse.SC_FORBIDDEN, null);
    }

    @Override
    public InputStream getContent(DavContext ctxt) throws IOException,
            DavException {
        return null;
    }

    @Override
    public boolean isCollection() {
        return true;
    }
    
    private class CalendarProxyRead extends Principal {
    	public CalendarProxyRead(String user, String url) throws ServiceException {
    		super(user, url+"calendar-proxy-read");
            addResourceType(DavElements.E_CALENDAR_PROXY_READ);
            addProperty(VersioningProperty.getSupportedReportSet());
    		addProperty(Acl.getPrincipalUrl(this));
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
            		ZFolder folder = zmbx.getFolderById(new ItemId(mp.getOwnerId(), mp.getRemoteId()).toString(mAccount));
            		mps.add(new Pair<Mountpoint,ZFolder>(mp, folder));
            	}
        	} catch (ServiceException se) {
        		ZimbraLog.dav.warn("can't get mailbox", se);
        	}
    		return mps;
    	}
    }
    
    private class CalendarProxyReadFor extends ProxyProperty {
    	public CalendarProxyReadFor(Account acct) {
    		super(DavElements.E_CALENDAR_PROXY_READ_FOR);
    	}
        @Override
    	public Element toElement(DavContext ctxt, Element parent, boolean nameOnly) {
			Element proxy = super.toElement(ctxt, parent, true);
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
