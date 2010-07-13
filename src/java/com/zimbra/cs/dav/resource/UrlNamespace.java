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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.util.MapUtil;

import com.zimbra.common.mailbox.ContactConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.Provisioning.AccountBy;
import com.zimbra.cs.account.Server;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.httpclient.URLUtil;
import com.zimbra.cs.index.ContactHit;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.calendar.Invite;

/**
 * UrlNamespace provides a mapping from a URL to a DavResource.
 * 
 * @author jylee
 *
 */
public class UrlNamespace {
	public static final String ATTACHMENTS_PREFIX = "/attachments";
	
	public static class UrlComponents {
	    public String user;
	    public String path;
	}
	
	public static UrlComponents parseUrl(String url) {
	    UrlComponents uc = new UrlComponents();
	    
	    int index = url.indexOf(DavServlet.DAV_PATH);
	    if (index >= 0) {
            url = url.substring(index + DavServlet.DAV_PATH.length());
            int delim = url.indexOf('/', 1);
            if (delim > 0) {
                uc.user = url.substring(1, delim);
                url = url.substring(delim);
            }
	    }
        uc.path = url;
        try {
            uc.path = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ZimbraLog.dav.debug("can't decode url %s", url, e);
        }

	    return uc;
	}
	
	/* Returns Collection at the specified URL. */
	public static Collection getCollectionAtUrl(DavContext ctxt, String url) throws DavException {
	    UrlComponents uc = parseUrl(url);
		int lastPos = uc.path.length() - 1;
		if (uc.path.endsWith("/"))
			lastPos--;
		int index = uc.path.lastIndexOf('/', lastPos);
		String path;
		if (index == -1)
			path = "/";
		else
			path = uc.path.substring(0, index);
		String user = uc.user;
		if (user == null)
		    user = ctxt.getUser();
		DavResource rsc = getResourceAt(new DavContext(ctxt, path), user, path);
		if (rsc instanceof Collection)
			return (Collection)rsc;
		throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, null);
	}

	/* Returns DavResource at the specified URL. */
	public static DavResource getResourceAtUrl(DavContext ctxt, String url) throws DavException {
        if (url.indexOf(PRINCIPALS_PATH) >= 0)
            return getPrincipalAtUrl(ctxt, url);
	    UrlComponents uc = parseUrl(url);
		if (uc.user == null || uc.path == null)
            throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, null);
		DavResource rs = getResourceAt(ctxt, uc.user, uc.path);
		rs.mUri = uc.path;
		return rs;
	}
	
    public static DavResource getPrincipalAtUrl(DavContext ctxt, String url) throws DavException {
        ZimbraLog.dav.debug("getPrincipalAtUrl");
        String name = ctxt.getAuthAccount().getName();
        if (url != null) {
            int index = url.indexOf(PRINCIPALS_PATH);
            if (index == -1 || url.endsWith(PRINCIPALS_PATH))
                try {
                    return new Principal(ctxt.getAuthAccount(), url);
                } catch (ServiceException se) {
                    throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, se);
                }
            index += PRINCIPALS_PATH.length();
            name = url.substring(index);
            if (name.indexOf('/') > 0)
                name = name.substring(0, name.indexOf('/'));
            ZimbraLog.dav.debug("name: "+name);
        } else {
            url = "/";
        }
            
        try {
            Account a = Provisioning.getInstance().get(Provisioning.AccountBy.name, name);
            if (a == null)
                throw new DavException("user not found", HttpServletResponse.SC_NOT_FOUND, null);
            return new User(ctxt, a, url);
        } catch (ServiceException se) {
            throw new DavException("user not found", HttpServletResponse.SC_NOT_FOUND, null);
        }
    }
    
    public static DavResource getPrincipal(DavContext ctxt, Account acct) throws DavException {
        try {
            return new User(ctxt, acct, getPrincipalUrl(acct.getName()));
        } catch (ServiceException se) {
            throw new DavException("user not found", HttpServletResponse.SC_NOT_FOUND, null);
        }
    }
    
	/* Returns DavResource in the user's mailbox at the specified path. */
	public static DavResource getResourceAt(DavContext ctxt, String user, String path) throws DavException {
        ZimbraLog.dav.debug("getResource at "+user+" "+path);
		if (path == null)
			throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, null);
		
		java.util.Collection<DavResource> rss = getResources(ctxt, user, path, false);
		if (rss.size() > 0)
			return rss.iterator().next();
		return null;
	}

	public static java.util.Collection<DavResource> getResources(DavContext ctxt, String user, String path, boolean includeChildren) throws DavException {
		ArrayList<DavResource> rss = new ArrayList<DavResource>();
		if (user.equals(""))
			try {
				rss.add(new Principal(ctxt.getAuthAccount(), DavServlet.DAV_PATH));
				return rss;
			} catch (ServiceException e) {
			}
		
		String target = path.toLowerCase();
		DavResource resource = null;
		
		if (target.startsWith(ATTACHMENTS_PREFIX)) {
			resource = getPhantomResource(ctxt, user);
		} else {
		    try {
		        resource = getMailItemResource(ctxt, user, path);
		    } catch (ServiceException se) {
		    	if (path.length() == 1 && path.charAt(0) == '/' && se.getCode().equals(ServiceException.PERM_DENIED)) {
		    		// return the list of folders the authUser has access to
		            ctxt.setCollectionPath("/");
		    		try {
						return getFolders(ctxt, user);
					} catch (ServiceException e) {
				        ZimbraLog.dav.warn("can't get folders for "+user, e);
					}
		    	} else {
			        ZimbraLog.dav.warn("can't get mail item resource for "+user+", "+path, se);
		    	}
		    }
		}
		
		if (resource != null)
			rss.add(resource);
		if (resource != null && includeChildren)
			rss.addAll(resource.getChildren(ctxt));

		return rss;
	}
	
	/* Returns DavResource identified by MailItem id .*/
	public static DavResource getResourceByItemId(DavContext ctxt, String user, int id) throws ServiceException, DavException {
		MailItem item = getMailItemById(ctxt, user, id);
		return getResourceFromMailItem(ctxt, item);
	}
	
    public static final String PRINCIPALS      = "principals";
    public static final String PRINCIPAL_USERS = "users";
    public static final String PRINCIPALS_PATH = "/" + PRINCIPALS + "/" + PRINCIPAL_USERS + "/";
	
	public static final String ACL_USER   = PRINCIPALS_PATH;
	public static final String ACL_GUEST  = "/" + PRINCIPALS + "/" + "guests" + "/";
	public static final String ACL_GROUP  = "/" + PRINCIPALS + "/" + "groups" + "/";
	public static final String ACL_COS    = "/" + PRINCIPALS + "/" + "cos" + "/";
	public static final String ACL_DOMAIN = "/" + PRINCIPALS + "/" + "domain" + "/";
    
	/* RFC 3744 */
	public static String getAclUrl(String principal, String type) throws DavException {
		Account account = null;
		Provisioning prov = Provisioning.getInstance();
		try {
			account = prov.get(AccountBy.id, principal);
			StringBuilder buf = new StringBuilder();
			buf.append(type);
			if (account != null)
				buf.append(account.getName());
			else
				buf.append(principal);
			return getAbsoluteUrl(null, buf.toString());
		} catch (ServiceException e) {
			throw new DavException("cannot create ACL URL for principal "+principal, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
		}
	}

	public static String getRawResourceUrl(DavResource rs) {
		return DavServlet.DAV_PATH + "/" + rs.getOwner() + rs.getUri();
	}
	
	/* Returns URL to the resource. */
	public static String getResourceUrl(DavResource rs) {
	    //return urlEscape(DavServlet.getDavUrl(user) + resourcePath);
        return URLUtil.urlEscape(getRawResourceUrl(rs));
	}
    
	public static String getPrincipalUrl(Account account) {
		return getPrincipalUrl(account, account);
	}
	
	private static boolean onSameServer(Account thisOne, Account thatOne) {
		if (thisOne.getId().equals(thatOne.getId()))
			return true;
		try {
			Provisioning prov = Provisioning.getInstance();
	        Server mine = prov.getServer(thisOne);
	        Server theirs = prov.getServer(thatOne);
	        if (mine != null && theirs != null)
	        	return mine.getId().equals(theirs.getId());
		} catch (Exception e) {
	        ZimbraLog.dav.warn("can't get domain or server for %s %s", thisOne.getId(), thatOne.getId(), e);
		}
		return true;
	}
	public static String getPrincipalUrl(Account authAccount, Account targetAccount) {
        String url = getPrincipalUrl(targetAccount.getName());
        if (!onSameServer(authAccount, targetAccount)) {
        	try {
            	url = getAbsoluteUrl(targetAccount, url);
    		} catch (ServiceException se) {
    	        ZimbraLog.dav.warn("can't generate absolute url for "+targetAccount.getName(), se);
    		}
        }
        return url;
	}
    public static String getPrincipalUrl(String user) {
        return URLUtil.urlEscape(PRINCIPALS_PATH + user + "/");
    }
	
    public static String getPrincipalCollectionUrl(Account acct) throws ServiceException {
    	return URLUtil.urlEscape(PRINCIPALS_PATH);
    }
    
    public static String getResourceUrl(Account user, String path) throws ServiceException {
    	return getAbsoluteUrl(user, DavServlet.DAV_PATH + "/" + user.getName() + path);
    }
    
    private static String getAbsoluteUrl(Account user, String path) throws ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Domain domain = null;
		Server server = prov.getLocalServer();
		if (user != null) {
			domain = prov.getDomain(user);
			server = prov.getServer(user);
		}
		return DavServlet.getServiceUrl(server, domain, path);
    }
    
	private static Map sRenamedResourceMap = MapUtil.newLruMap(100);
	
	public static void addToRenamedResource(String user, String path, DavResource rsc) {
		synchronized (sRenamedResourceMap) {
			sRenamedResourceMap.put(new Pair<String,String>(user, path.toLowerCase()),
			                        new Pair<DavResource,Long>(rsc, System.currentTimeMillis()));
		}
	}
	public static DavResource checkRenamedResource(String user, String path) {
	    Pair<String,String> key = new Pair<String,String>(user, path.toLowerCase());
	    DavResource rsc = null;
	    synchronized (sRenamedResourceMap) {
	        @SuppressWarnings("unchecked")
	        Pair<DavResource,Long> item = (Pair<DavResource,Long>)sRenamedResourceMap.get(key);
	        if (item != null) {
	            long age = System.currentTimeMillis() - item.getSecond();
	            // keep a short TTL of 15 minutes.
	            if (age > 15 * Constants.MILLIS_PER_MINUTE)
	                sRenamedResourceMap.remove(key);
	            else
	                rsc = item.getFirst();
	        }
	    }
	    return rsc;
	}
    private static DavResource getMailItemResource(DavContext ctxt, String user, String path) throws ServiceException, DavException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.name, user);
        if (account == null)
            throw new DavException("no such accout "+user, HttpServletResponse.SC_NOT_FOUND, null);

        if (ctxt.getUser().compareTo(user) != 0 || !Provisioning.onLocalServer(account)) {
            try {
                return new RemoteCollection(ctxt, path, account);
            } catch (MailServiceException.NoSuchItemException e) {
                return null;
            }
        }
        
        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        String id = null;
        int index = path.indexOf('?');
        if (index > 0) {
            Map<String, String> params = HttpUtil.getURIParams(path.substring(index+1));
            path = path.substring(0, index);
            id = params.get("id");
        }
        OperationContext octxt = ctxt.getOperationContext();
        MailItem item = null;
        
        // simple case.  root folder or if id is specified.
        if (path.equals("/"))
            item = mbox.getFolderByPath(octxt, "/");
        else if (id != null)
            item = mbox.getItemById(octxt, Integer.parseInt(id), MailItem.TYPE_UNKNOWN);

        if (item != null)
            return getResourceFromMailItem(ctxt, item);
        
        // check for named items (folders, documents)
        try {
            return getResourceFromMailItem(ctxt, mbox.getItemByPath(octxt, path));
        } catch (MailServiceException.NoSuchItemException e) {
        }

        // check if the this is renamed folder.
        DavResource rs = checkRenamedResource(user, path);
        if (rs != null)
            return rs;
        
        // look up the item from path
        if (path.endsWith("/"))
        	path = path.substring(0, path.length()-1);
        index = path.lastIndexOf('/');
        String folderPath = path.substring(0, index);
        Folder f = null;
        if (index != -1) {
            try {
                f = mbox.getFolderByPath(octxt, folderPath);
            } catch (MailServiceException.NoSuchItemException e) {
            }
        }
        if (f != null) {
        	if (path.toLowerCase().endsWith(CalendarObject.CAL_EXTENSION)) {
        		String uid = path.substring(index + 1, path.length() - CalendarObject.CAL_EXTENSION.length());
        		index = uid.indexOf(',');
        		if (index > 0) {
        			id = uid.substring(index+1);
        			item = mbox.getItemById(octxt, Integer.parseInt(id), MailItem.TYPE_UNKNOWN);

        		} else {
        			item = mbox.getCalendarItemByUid(octxt, uid);
        		}
            } else if (path.toLowerCase().endsWith(AddressObject.VCARD_EXTENSION)) {
                try {
                    String uid = URLDecoder.decode(path.substring(index + 1, path.length() - AddressObject.VCARD_EXTENSION.length()), "UTF-8");
                    index = uid.indexOf(':');
                    if (index > 0) {
                        item = mbox.getContactById(octxt, Integer.parseInt(uid.substring(index+1)));
                    } else {
                        ZimbraQueryResults zqr = null;
                        StringBuilder query = new StringBuilder();
                        query.append("#").append(ContactConstants.A_vCardUID).append(":");
                        query.append(uid);
                        query.append(" OR ").append("#").append(ContactConstants.A_vCardURL).append(":");
                        query.append(uid);
                        ZimbraLog.dav.debug("query %s", query.toString());
                        try {
                            zqr = mbox.search(ctxt.getOperationContext(), query.toString(), new byte[] { MailItem.TYPE_CONTACT }, SortBy.NAME_ASCENDING, 10);
                            if (zqr.hasNext()) {
                                ZimbraHit hit = zqr.getNext();
                                if (hit instanceof ContactHit) {
                                    item = ((ContactHit)hit).getContact();
                                }
                            }
                        } catch (Exception e) {
                            ZimbraLog.dav.error("can't search for: uid="+uid, e);
                        } finally {
                            if (zqr != null)
                                try {
                                    zqr.doneWithSearchResults();
                                } catch (ServiceException e) {}
                        }
                    }
                } catch (UnsupportedEncodingException e) {
                    ZimbraLog.dav.warn("Can't decode URL %s", path);
                }
        	} else if (f.getId() == Mailbox.ID_FOLDER_INBOX || f.getId() == Mailbox.ID_FOLDER_SENT) {
        		ctxt.setPathInfo(path.substring(index+1));
        		// delegated scheduling and notification handling
        		return getResourceFromMailItem(ctxt, f);
        	}
        }

        return getResourceFromMailItem(ctxt, item);
    }
    
    private static java.util.Collection<DavResource> getFolders(DavContext ctxt, String user) throws ServiceException, DavException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.name, user);
        if (account == null)
            throw new DavException("no such accout "+user, HttpServletResponse.SC_NOT_FOUND, null);

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        OperationContext octxt = ctxt.getOperationContext();
        ArrayList<DavResource> rss = new ArrayList<DavResource>();
        for (Folder f : mbox.getVisibleFolders(octxt))
        	rss.add(getResourceFromMailItem(ctxt, f));
        return rss;
    }
    
    private static DavResource getCalendarItemForMessage(DavContext ctxt, Message msg) throws ServiceException {
    	Mailbox mbox = msg.getMailbox();
    	if (msg.isInvite() && msg.hasCalendarItemInfos()) {
    		Message.CalendarItemInfo calItemInfo = msg.getCalendarItemInfo(0);
    		try {
    			Invite invite = calItemInfo.getInvite();
    			if (invite == null && calItemInfo.getCalendarItemId() != Message.CalendarItemInfo.CALITEM_ID_NONE) {
    			    // Pre-6.0 data
        			CalendarItem item = mbox.getCalendarItemById(ctxt.getOperationContext(), calItemInfo.getCalendarItemId());
        			invite = calItemInfo.getInvite();
                    int compNum = calItemInfo.getComponentNo();
                    invite = item.getInvite(msg.getId(), compNum);        			    
    			}
    			if (invite != null) {
    				String path = CalendarObject.CalendarPath.generate(ctxt, msg.getPath(), invite.getUid(), msg.getId());
    				return new CalendarObject.ScheduleMessage(ctxt, path, ctxt.getUser(), invite, msg);
    			}
            } catch (MailServiceException.NoSuchItemException e) {
            	// the appt must have been cancelled or deleted.
            	// bug 26315
            }
    	}
    	return null;
    }
    
	/* Returns DavResource for the MailItem. */
	public static DavResource getResourceFromMailItem(DavContext ctxt, MailItem item) throws DavException {
		DavResource resource = null;
		if (item == null)
			return resource;
		byte itemType = item.getType();
		
		try {
			byte viewType;
			switch (itemType) {
            case MailItem.TYPE_MOUNTPOINT :
				Mountpoint mp = (Mountpoint) item;
            	viewType = mp.getDefaultView();
            	// don't expose mounted calendars when using iCal style delegation model.
            	if (!ctxt.useIcalDelegation() && viewType == MailItem.TYPE_APPOINTMENT)
            		resource = new RemoteCalendarCollection(ctxt, mp);
            	else
            		resource = new RemoteCollection(ctxt, mp);
                break;
			case MailItem.TYPE_FOLDER :
				Folder f = (Folder) item;
				viewType = f.getDefaultView();
				if (f.getId() == Mailbox.ID_FOLDER_INBOX && ctxt.isSchedulingEnabled())
					resource = new ScheduleInbox(ctxt, f);
				else if (f.getId() == Mailbox.ID_FOLDER_SENT && ctxt.isSchedulingEnabled())
					resource = new ScheduleOutbox(ctxt, f);
				else if (viewType == MailItem.TYPE_APPOINTMENT ||
						viewType == MailItem.TYPE_TASK)
					resource = getCalendarCollection(ctxt, f);
                else if (viewType == MailItem.TYPE_CONTACT)
                    resource = new AddressbookCollection(ctxt, f);
				else
					resource = new Collection(ctxt, f);
				break;
			case MailItem.TYPE_WIKI :
			case MailItem.TYPE_DOCUMENT :
				resource = new Notebook(ctxt, (Document)item);
				break;
			case MailItem.TYPE_APPOINTMENT :
			case MailItem.TYPE_TASK :
				resource = new CalendarObject.LocalCalendarObject(ctxt, (CalendarItem)item);
				break;
			case MailItem.TYPE_MESSAGE :
				resource = getCalendarItemForMessage(ctxt, (Message)item);
				break;
			case MailItem.TYPE_CONTACT :
			    resource = new AddressObject(ctxt, (Contact)item);
			    break;
			}
		} catch (ServiceException e) {
			resource = null;
			ZimbraLog.dav.info("cannot create DavResource", e);
		}
		return resource;
	}
	
	private static MailItemResource getCalendarCollection(DavContext ctxt, Folder f) throws ServiceException, DavException {
		String[] homeSets = Provisioning.getInstance().getConfig().getMultiAttr(Provisioning.A_zimbraCalendarCalDavAlternateCalendarHomeSet);
		// if alternate homeSet is set then default Calendar and Tasks folders 
		// are no longer being used to store appointments and tasks.
		if (homeSets.length > 0 && 
				(f.getId() == Mailbox.ID_FOLDER_CALENDAR ||
				 f.getId() == Mailbox.ID_FOLDER_TASKS))
			return new Collection(ctxt, f);
		if (f.getDefaultView() == MailItem.TYPE_APPOINTMENT && !ctxt.getAuthAccount().isFeatureCalendarEnabled())
			return new Collection(ctxt, f);
		if (f.getDefaultView() == MailItem.TYPE_TASK && !ctxt.getAuthAccount().isFeatureTasksEnabled())
			return new Collection(ctxt, f);
		return new CalendarCollection(ctxt, f);
	}
	
	private static DavResource getPhantomResource(DavContext ctxt, String user) throws DavException {
		DavResource resource;
		String target = ctxt.getPath();
		
		ArrayList<String> tokens = new ArrayList<String>();
		StringTokenizer tok = new StringTokenizer(target, "/");
		int numTokens = tok.countTokens();
		while (tok.hasMoreTokens()) {
			tokens.add(tok.nextToken());
		}
		
		//
		// return BrowseWrapper
		//
		// /attachments/
		// /attachments/by-date/
		// /attachments/by-type/
		// /attachments/by-type/image/
		// /attachments/by-sender/
		// /attachments/by-sender/zimbra.com/

		//
		// return SearchWrapper
		//
		// /attachments/by-date/today/
		// /attachments/by-type/image/last-month/
		// /attachments/by-sender/zimbra.com/last-week/
		
		//
		// return AttachmentWrapper
		//
		// /attachments/by-date/today/image.gif
		// /attachments/by-type/image/last-month/image.gif
		// /attachments/by-sender/zimbra.com/last-week/image.gif

		switch (numTokens) {
		case 1:
		case 2:
			resource = new BrowseWrapper(target, user, tokens);
			break;
		case 3:
			if (tokens.get(1).equals(PhantomResource.BY_DATE))
				resource = new SearchWrapper(target, user, tokens);
			else
				resource = new BrowseWrapper(target, user, tokens);
			break;
		case 4:
			if (tokens.get(1).equals(PhantomResource.BY_DATE))
				resource = new Attachment(target, user, tokens, ctxt);
			else
				resource = new SearchWrapper(target, user, tokens);
			break;
		case 5:
			resource = new Attachment(target, user, tokens, ctxt);
			break;
		default:
			resource = null;
		}
		
		return resource;
	}
	
	private static MailItem getMailItemById(DavContext ctxt, String user, int id) throws DavException, ServiceException {
		Provisioning prov = Provisioning.getInstance();
		Account account = prov.get(AccountBy.name, user);
		if (account == null)
			throw new DavException("no such accout "+user, HttpServletResponse.SC_NOT_FOUND, null);
		
		Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
		return mbox.getItemById(ctxt.getOperationContext(), id, MailItem.TYPE_UNKNOWN);
	}
	
	public static Account getPrincipal(String principalUrl) throws ServiceException {
		int index = principalUrl.indexOf(PRINCIPALS_PATH);
		if (index == -1)
			return null;
		String acct = principalUrl.substring(index + PRINCIPALS_PATH.length());
		Provisioning prov = Provisioning.getInstance();
		return prov.get(AccountBy.name, acct);
	}
}
