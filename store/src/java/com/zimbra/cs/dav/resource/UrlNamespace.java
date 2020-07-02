/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016 Synacor, Inc.
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software Foundation,
 * version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.resource;

import java.util.ArrayList;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletResponse;

import com.zimbra.common.account.Key;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.Constants;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.MapUtil;
import com.zimbra.common.util.Pair;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.DavNames;
import com.zimbra.cs.mailbox.Document;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.calendar.Invite;

/**
 * UrlNamespace provides a mapping from a URL to a DavResource.
 *
 * @author jylee
 *
 */
public class UrlNamespace {
    public static final String ATTACHMENTS_PREFIX = "/attachments";
    public static final String PRINCIPALS      = "principals";
    public static final String PRINCIPAL_USERS = "users";
    public static final String PRINCIPALS_PATH = "/" + PRINCIPALS + "/" + PRINCIPAL_USERS + "/";

    public static final String ACL_USER   = PRINCIPALS_PATH;
    public static final String ACL_GUEST  = "/" + PRINCIPALS + "/" + "guests" + "/";
    public static final String ACL_GROUP  = "/" + PRINCIPALS + "/" + "groups" + "/";
    public static final String ACL_COS    = "/" + PRINCIPALS + "/" + "cos" + "/";
    public static final String ACL_DOMAIN = "/" + PRINCIPALS + "/" + "domain" + "/";

    private static Map <Pair<String,String>,Pair<DavResource,Long>>sRenamedResourceMap =
            MapUtil.newLruMap(100);

    public static class UrlComponents {
        public String user;
        public String path;
    }

    /**
     * Parse the given url into user and path information.
     * @param url must be passed as decoded
     * @return user and path info as UrlComponents
     */

    public static UrlComponents parseUrl(String urlToParse) {
        String url = urlToParse;
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
        if (rs != null)
            rs.mUri = uc.path;
        return rs;
    }

    public static DavResource getPrincipalAtUrl(DavContext ctxt, String url) throws DavException {
        String name = ctxt.getAuthAccount().getName();
        String proxyPrincipal = "";
        if (url != null) {
            int index = url.indexOf(PRINCIPALS_PATH);
            if (index == -1 || url.endsWith(PRINCIPALS_PATH)) {
                try {
                    return new Principal(ctxt.getAuthAccount(), url);
                } catch (ServiceException se) {
                    throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, se);
                }
            }
            index += PRINCIPALS_PATH.length();
            name = url.substring(index);
            if (name.indexOf('/') > 0) {
                proxyPrincipal = name.substring(name.indexOf('/') + 1);
                if (proxyPrincipal.endsWith("/")) {
                    proxyPrincipal = proxyPrincipal.substring(0, proxyPrincipal.indexOf('/'));
                }
                name = name.substring(0, name.indexOf('/'));
            }
            name = name.replaceAll("%40", "@");
        } else {
            url = "/";
        }
        ZimbraLog.dav.debug("getPrincipalAtUrl name='%s' url='%s' proxyPrincipal='%s'", name, url, proxyPrincipal);

        try {
            Account acct = Provisioning.getInstance().get(Key.AccountBy.name, name);
            if (acct == null) {
                throw new DavException("user not found", HttpServletResponse.SC_NOT_FOUND, null);
            }
            if (CalendarProxyRead.CALENDAR_PROXY_READ.equals(proxyPrincipal)) {
                if (!ctxt.useIcalDelegation()) {
                    throw new DavException("Not available because zimbraPrefAppleIcalDelegationEnabled=FALSE",
                            HttpServletResponse.SC_NOT_FOUND, null);
                }
                return new CalendarProxyRead(acct, url);
            }
            if (CalendarProxyWrite.CALENDAR_PROXY_WRITE.equals(proxyPrincipal)) {
                if (!ctxt.useIcalDelegation()) {
                    throw new DavException("Not available because zimbraPrefAppleIcalDelegationEnabled=FALSE",
                            HttpServletResponse.SC_NOT_FOUND, null);
                }
                return new CalendarProxyWrite(acct, url);
            }
            return new User(ctxt, acct, url);
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
        ZimbraLog.dav.debug("getResource at user='%s' path='%s'", user, path);
        if (path == null) {
            throw new DavException("invalid uri", HttpServletResponse.SC_NOT_FOUND, null);
        }

        java.util.Collection<DavResource> rss = getResources(ctxt, user, path, false);
        if (rss.size() > 0) {
            return rss.iterator().next();
        }
        return null;
    }

    public static java.util.Collection<DavResource> getResources(
            DavContext ctxt, String user, String path, boolean includeChildren)
    throws DavException {
        ArrayList<DavResource> rss = new ArrayList<DavResource>();
        if ("".equals(user)) {
            try {
                rss.add(new Principal(ctxt.getAuthAccount(), DavServlet.DAV_PATH));
                return rss;
            } catch (ServiceException e) {
            }
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
                    // iCal makes this request for delegated calendars. iCal5 doesn't display the contents of
                    // delegated calendars if the delegator's home collection is not present in the PROPFIND response.
                    // Since the user does not have permissions on delegator's home collection, return an empty
                    // collection and the list of folders the authUser has access to.
                    rss.add(new Collection("/", user));
                    ctxt.setCollectionPath("/");
                    if (includeChildren) {
                        try {
                            rss.addAll(getFolders(ctxt, user));
                        } catch (ServiceException e) {
                            ZimbraLog.dav.warn("can't get folders for user='%s'",user, e);
                        }
                    }
                    return rss;
                } else {
                    ZimbraLog.dav.warn("can't get mail item resource for user='%s' path='%s'", user, path, se);
                }
            }
        }

        if (resource != null) {
            rss.add(resource);
        }
        if (resource != null && includeChildren) {
            rss.addAll(resource.getChildren(ctxt));
        }

        return rss;
    }

    /* Returns DavResource identified by MailItem id .*/
    public static DavResource getResourceByItemId(DavContext ctxt, String user, int id) throws ServiceException, DavException {
        MailItem item = getMailItemById(ctxt, user, id);
        return getResourceFromMailItem(ctxt, item);
    }

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
            return getAbsoluteUrl(null, buf.toString().replaceAll("@", "%40"));
        } catch (ServiceException e) {
            throw new DavException("cannot create ACL URL for principal "+principal, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }
    }

    public static String getRawResourceUrl(DavResource rs) {
        return DavServlet.DAV_PATH + "/" + rs.getOwner() + rs.getUri();
    }

    /* Returns URL to the resource. */
    public static String getResourceUrl(DavResource rs) {
        String str = HttpUtil.urlEscape(getRawResourceUrl(rs));
        // A lot of clients don't like naked "@" signs
        // e.g. Mac Contacts created 2 contacts every time a new contact is created via rfc5995 POST
        // if the response Location: header contained "@"
        return str.replaceAll("//", "%2F").replaceAll("@", "%40");
    }

    public static String getPrincipalUrl(Account account) {
        return getPrincipalUrl(account, account);
    }

    private static boolean onSameServer(Account thisOne, Account thatOne) {
        if (thisOne.getId().equals(thatOne.getId()))
            return true;
        try {
            Provisioning prov = Provisioning.getInstance();
            String mine = Provisioning.affinityServer(thisOne);
            String theirs = Provisioning.affinityServer(thatOne);
            if (mine != null && theirs != null)
                return mine.equals(theirs);
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

    public static String getCalendarProxyReadUrl(Account authAccount, Account targetAccount) {
        return UrlNamespace.getPrincipalUrl(authAccount, targetAccount)
                 + CalendarProxyRead.CALENDAR_PROXY_READ + "/";
    }

    public static String getCalendarProxyWriteUrl(Account authAccount, Account targetAccount) {
        return UrlNamespace.getPrincipalUrl(authAccount, targetAccount)
                 + CalendarProxyWrite.CALENDAR_PROXY_WRITE+ "/";
    }

    public static String getPrincipalUrl(String user) {
        return HttpUtil.urlEscape(PRINCIPALS_PATH + user + "/").replaceAll("@", "%40");
    }

    public static String getPrincipalCollectionUrl(Account acct) throws ServiceException {
        return HttpUtil.urlEscape(PRINCIPALS_PATH);
    }

    public static String getCalendarHomeSetUrl(String authUser) {
        return DavServlet.DAV_PATH + "/" + authUser.replaceAll("@", "%40");
    }

    public static String getAddressbookHomeSetUrl(String authUser) {
        return getCalendarHomeSetUrl(authUser);
    }

    public static String getSchedulingInboxUrl(String authUser, String user) {
        StringBuilder url = new StringBuilder();
        // always use authenticated user's inbox.
        url.append(DavServlet.DAV_PATH).append("/").append(authUser).append("/Inbox/");
        if (!authUser.equals(user)) {
            url.append(user).append("/");
        }
        return url.toString();
    }

    public static String getSchedulingOutboxUrl(String authUser, String user) {
        StringBuilder url = new StringBuilder();
        // always use authenticated user's outbox.
        url.append(DavServlet.DAV_PATH).append("/").append(authUser).append("/Sent/");
        if (!authUser.equals(user)) {
            url.append(user).append("/");
        }
        return url.toString();
    }

    /**
     * @param folder  - name of folder e.g. "Calendar"
     */
    public static String getFolderUrl(String authUser, String folder) {
        StringBuilder url = new StringBuilder();
        url.append(DavServlet.DAV_PATH).append("/").append(authUser).append("/").append(folder).append("/");
        return url.toString();
    }

    public static String getResourceUrl(Account user, String path) throws ServiceException {
        return getAbsoluteUrl(user, DavServlet.DAV_PATH + "/" + user.getName() + path);
    }

	private static String getAbsoluteUrl(Account user, String path) throws ServiceException {
		String affinityIp = null;
		if (user != null) {
			affinityIp = Provisioning.affinityServer(user);
		}
		return DavServlet.getServiceUrl(affinityIp, path);
	}

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
            Pair<DavResource,Long> item = sRenamedResourceMap.get(key);
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

    /**
     *
     * @param ctxt
     * @param user
     * @param path - May contain parameters
     * @return
     */
    private static DavResource getMailItemResource(DavContext ctxt, String user, String path)
    throws ServiceException, DavException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.name, user);
        if (account == null) {
            // Anti-account name harvesting.
            ZimbraLog.dav.info("Failing GET of mail item resource - no such account '%s' path '%s'", user, path);
            throw new DavException("Request denied", HttpServletResponse.SC_NOT_FOUND, null);
        }

        if (ctxt.getUser().compareTo(user) != 0 || !Provisioning.onLocalServer(account)) {
            try {
                return new RemoteCollection(ctxt, path, account);
            } catch (MailServiceException.NoSuchItemException e) {
                return null;
            }
        }

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        int id = 0;
        int index = path.indexOf('?');
        if (index > 0) {
            Map<String, String> params = HttpUtil.getURIParams(path.substring(index+1));
            path = path.substring(0, index);
            if (params.containsKey("id")) {
                try {
                    id = Integer.parseInt(params.get("id"));
                } catch (NumberFormatException e) {
                }
            }
        }
        // At this point, path will have had any parameters stripped from it
        OperationContext octxt = ctxt.getOperationContext();
        MailItem item = null;

        // simple case.  root folder or if id is specified.
        if ("/".equals(path)) {
            item = mbox.getFolderByPath(octxt, "/");
        } else if (id > 0) {
            item = mbox.getItemById(octxt, id, MailItem.Type.UNKNOWN);
        }
        if (item != null) {
            return getResourceFromMailItem(ctxt, item);
        }
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
        if (path.endsWith("/")) {
            path = path.substring(0, path.length()-1);
        }
        index = path.lastIndexOf('/');
        String folderPath = path.substring(0, index);
        String baseName = path.substring(index + 1);
        Folder f = null;
        if (index != -1) {
            try {
                f = mbox.getFolderByPath(octxt, folderPath);
            } catch (MailServiceException.NoSuchItemException e) {
            }
        }
        if (f != null) {
            /* First check whether the default name has been over-ridden - perhaps because the name was
             * chosen by a DAV client via PUT to a URL when creating the item. */
            DavNames.DavName davName = null;
            if (DebugConfig.enableDAVclientCanChooseResourceBaseName) {
                davName = DavNames.DavName.create(mbox.getId(), f.getId(), baseName);
            }
            if (davName != null) {
                Integer itemId = DavNames.get(davName);
                if (itemId != null) {
                    item = mbox.getItemById(octxt, itemId, MailItem.Type.UNKNOWN);
                    if ((item != null) && (f.getId() == item.getFolderId())) {
                        return getResourceFromMailItem(ctxt, item);
                    }
                    item = null;
                }
            }
            if (baseName.toLowerCase().endsWith(CalendarObject.CAL_EXTENSION)) {
                String uid = baseName.substring(0, baseName.length() - CalendarObject.CAL_EXTENSION.length());
                // Unescape the name (It was encoded in DavContext intentionally)
                uid = HttpUtil.urlUnescape(uid);
                index = uid.indexOf(',');
                if (index > 0) {
                    try {
                        id = Integer.parseInt(uid.substring(index+1));
                    } catch (NumberFormatException e) {
                    }
                }
                if (id > 0) {
                    item = mbox.getItemById(octxt, id, MailItem.Type.UNKNOWN);
                } else {
                    item = mbox.getCalendarItemByUid(octxt, uid);
                }
                if ((item != null) && (f.getId() != item.getFolderId())) {
                    item = null;
                }
            } else if (baseName.toLowerCase().endsWith(AddressObject.VCARD_EXTENSION)) {
                rs = AddressObject.getAddressObjectByUID(ctxt, baseName, account, f);
                if (rs != null) {
                    return rs;
                }
            } else if (f.getId() == Mailbox.ID_FOLDER_INBOX || f.getId() == Mailbox.ID_FOLDER_SENT) {
                ctxt.setActingAsDelegateFor(baseName);
                // delegated scheduling and notification handling
                return getResourceFromMailItem(ctxt, f);
            }
        }

        return getResourceFromMailItem(ctxt, item);
    }

    private static java.util.Collection<DavResource> getFolders(DavContext ctxt, String user) throws ServiceException, DavException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.name, user);
        if (account == null) {
            // Anti-account name harvesting.
            ZimbraLog.dav.info("Failing GET of folders - no such account '%s'", user);
            throw new DavException("Request denied", HttpServletResponse.SC_NOT_FOUND, null);
        }

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
                if (invite == null && calItemInfo.calItemCreated()) {
                    // Pre-6.0 data
                    CalendarItem item = mbox.getCalendarItemById(ctxt.getOperationContext(),
                            calItemInfo.getCalendarItemId());
                    invite = calItemInfo.getInvite();
                    int compNum = calItemInfo.getComponentNo();
                    invite = item.getInvite(msg.getId(), compNum);
                }
                if (invite != null) {
                    String path = CalendarObject.CalendarPath.generate(ctxt, msg.getPath(), invite.getUid(),
                            mbox.getId(), msg.getId(), msg.getId());
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
        if (item == null) {
            return resource;
        }
        MailItem.Type itemType = item.getType();

        try {
            MailItem.Type viewType;
            switch (itemType) {
            case MOUNTPOINT :
                Mountpoint mp = (Mountpoint) item;
                viewType = mp.getDefaultView();
                // don't expose mounted calendars when using iCal style delegation model.
                if (!ctxt.useIcalDelegation() &&
                        (viewType == MailItem.Type.APPOINTMENT || viewType == MailItem.Type.TASK)) {
                    resource = new RemoteCalendarCollection(ctxt, mp);
                } else if (viewType == MailItem.Type.CONTACT) {
                    resource = new RemoteAddressbookCollection(ctxt, mp);
                } else {
                    resource = new RemoteCollection(ctxt, mp);
                }
                break;
            case FOLDER :
                Folder f = (Folder) item;
                viewType = f.getDefaultView();
                if (f.getId() == Mailbox.ID_FOLDER_INBOX && DavResource.isSchedulingEnabled()) {
                    resource = new ScheduleInbox(ctxt, f);
                } else if (f.getId() == Mailbox.ID_FOLDER_SENT && DavResource.isSchedulingEnabled()) {
                    resource = new ScheduleOutbox(ctxt, f);
                } else if (viewType == MailItem.Type.APPOINTMENT || viewType == MailItem.Type.TASK) {
                    resource = getCalendarCollection(ctxt, f);
                } else if (viewType == MailItem.Type.CONTACT) {
                    resource = new AddressbookCollection(ctxt, f);
                } else {
                    resource = new Collection(ctxt, f);
                }
                break;
            case DOCUMENT :
                resource = new Notebook(ctxt, (Document)item);
                break;
            case APPOINTMENT :
            case TASK :
                resource = new CalendarObject.LocalCalendarObject(ctxt, (CalendarItem)item);
                break;
            case MESSAGE :
                resource = getCalendarItemForMessage(ctxt, (Message)item);
                break;
            case CONTACT :
                resource = new AddressObject(ctxt, (Contact)item);
                break;
            default:
                break;
            }
        } catch (ServiceException e) {
            ZimbraLog.dav.info("cannot create DavResource", e);
        }
        return resource;
    }

    private static MailItemResource getCalendarCollection(DavContext ctxt, Folder f) throws ServiceException, DavException {
        String[] homeSets = Provisioning.getInstance().getConfig().getMultiAttr(Provisioning.A_zimbraCalendarCalDavAlternateCalendarHomeSet);
        // if alternate homeSet is set then default Calendar and Tasks folders
        // are no longer being used to store appointments and tasks.
        if (homeSets.length > 0 && (f.getId() == Mailbox.ID_FOLDER_CALENDAR || f.getId() == Mailbox.ID_FOLDER_TASKS)) {
            return new Collection(ctxt, f);
        }
        if (f.getDefaultView() == MailItem.Type.APPOINTMENT && !ctxt.getAuthAccount().isFeatureCalendarEnabled()) {
            return new Collection(ctxt, f);
        }
        if (f.getDefaultView() == MailItem.Type.TASK && !ctxt.getAuthAccount().isFeatureTasksEnabled()) {
            return new Collection(ctxt, f);
        }
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
            break;
        }

        return resource;
    }

    private static MailItem getMailItemById(DavContext ctxt, String user, int id) throws DavException, ServiceException {
        Provisioning prov = Provisioning.getInstance();
        Account account = prov.get(AccountBy.name, user);
        if (account == null) {
            // Anti-account name harvesting.
            ZimbraLog.dav.info("Failing GET of mail item - no such account '%s' id=%d", user, id);
            throw new DavException("Request denied", HttpServletResponse.SC_NOT_FOUND, null);
        }

        Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
        return mbox.getItemById(ctxt.getOperationContext(), id, MailItem.Type.UNKNOWN);
    }

    public static Account getPrincipal(String principalUrl) throws ServiceException {
        int index = principalUrl.indexOf(PRINCIPALS_PATH);
        if (index == -1)
            return null;
        String acct = principalUrl.substring(index + PRINCIPALS_PATH.length());
        acct = acct.replaceAll("%40", "@");
        Provisioning prov = Provisioning.getInstance();
        return prov.get(AccountBy.name, acct);
    }
}
