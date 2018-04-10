/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.dom4j.Element;
import org.dom4j.QName;

import com.google.common.collect.Lists;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.caldav.Range.TimeRange;
import com.zimbra.cs.dav.property.CalDavProperty;
import com.zimbra.cs.dav.service.DavServlet;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.SortBy;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;
import com.zimbra.cs.mailbox.Mountpoint;

public class ScheduleInbox extends CalendarCollection {
    public ScheduleInbox(DavContext ctxt, Folder f) throws DavException, ServiceException {
        super(ctxt, f);
        addResourceType(DavElements.E_SCHEDULE_INBOX);
        String user = getOwner();
        addProperty(CalDavProperty.getCalendarFreeBusySet(user, getCalendarFolders(ctxt)));
    }

    private QName supportedInboxReports[] = null;

    @Override
    protected QName[] getSupportedReports() {
        if (supportedInboxReports == null) {
            List<QName> reportList = Lists.newArrayList(super.getSupportedReports());
            /*
             *  rfc6638 CALDAV:free-busy-query REPORT is NOT supported on scheduling inbox
             *  Note that the expected way to determine freebusy information for CalDAV scheduling is to POST
             *  to the scheduling outbox
             */
            reportList.remove(DavElements.E_FREE_BUSY_QUERY);
            supportedInboxReports = reportList.toArray(new QName[reportList.size()]);
        }
        return supportedInboxReports;
    }

    @Override
    public java.util.Collection<DavResource> getChildren(DavContext ctxt, TimeRange tr) throws DavException {
        try {
            return getAppointmentsByUids(ctxt, null);
        } catch (ServiceException se) {
            ZimbraLog.dav.error("can't get schedule messages in folder "+getId(), se);
            return Collections.emptyList();
        }
    }

    protected static final Set<MailItem.Type> SEARCH_TYPES = EnumSet.of(MailItem.Type.MESSAGE);

    @Override
    public java.util.Collection<DavResource> getAppointmentsByUids(DavContext ctxt, List<String> hrefs) throws ServiceException, DavException {
        List<DavResource> result = new ArrayList<DavResource>();
        if (!DavResource.isSchedulingEnabled()) {
            return result;
        }
        Account target = null;
        Provisioning prov = Provisioning.getInstance();
        if (ctxt.getActingAsDelegateFor() != null) {
            target = prov.getAccountByName(ctxt.getActingAsDelegateFor());
        }
        String query = "is:invite is:unread inid:" + getId() + " after:\"-1month\" ";
        Mailbox mbox = getMailbox(ctxt);
        try (ZimbraQueryResults zqr = mbox.index.search(ctxt.getOperationContext(), query,
            SEARCH_TYPES, SortBy.DATE_ASC, 100)) {
            while (zqr.hasNext()) {
                ZimbraHit hit = zqr.getNext();
                if (hit instanceof MessageHit) {
                    Message msg = ((MessageHit)hit).getMessage();
                    if (target == null && msg.getCalendarIntendedFor() != null) {
                        continue;
                    }
                    if (!msg.isInvite() || !msg.hasCalendarItemInfos()) {
                        continue;
                    }
                    /* Bug 40567.  hide replies to avoid them being deleted by CalDAV clients.
                     * TODO: An alternative approach would be to show them but when they are "deleted", flag them as
                     * absent from the scheduling inbox.
                     */
                    if ("REPLY".equals(msg.getCalendarItemInfo(0).getInvite().getMethod())) {
                        continue;
                    }
                    if (target != null) {
                        if (msg.getCalendarIntendedFor() == null) {
                            continue;
                        }
                        Account apptRcpt = prov.getAccountByName(msg.getCalendarIntendedFor());
                        if (apptRcpt == null || !apptRcpt.getId().equals(target.getId())) {
                            continue;
                        }
                    }
                    DavResource rs = UrlNamespace.getResourceFromMailItem(ctxt, msg);
                    if (rs != null) {
                        String href = UrlNamespace.getRawResourceUrl(rs);
                        if (hrefs == null)
                            result.add(rs);
                        else {
                            boolean found = false;
                            for (String ref : hrefs) {
                                if (HttpUtil.urlUnescape(ref).equals(href)) {
                                    result.add(rs);
                                    found = true;
                                    break;
                                }
                            }
                            if (!found)
                                result.add(new DavResource.InvalidResource(href, getOwner()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            ZimbraLog.dav.error("can't search: uri="+getUri(), e);
        } 
        return result;
    }

    @Override
    public void patchProperties(DavContext ctxt, java.util.Collection<Element> set, java.util.Collection<QName> remove) throws DavException, IOException {
        ArrayList<Element> newSet = null;
        for (Element el : set) {
            if (el.getQName().equals(DavElements.E_CALENDAR_FREE_BUSY_SET)) {
                Iterator<?> hrefs = el.elementIterator(DavElements.E_HREF);
                ArrayList<String> urls = new ArrayList<String>();
                while (hrefs.hasNext())
                    urls.add(((Element)hrefs.next()).getText());
                try {
                    updateCalendarFreeBusySet(ctxt, urls);
                } catch (ServiceException e) {
                    ctxt.getResponseProp().addPropError(DavElements.E_CALENDAR_FREE_BUSY_SET, new DavException("error", DavProtocol.STATUS_FAILED_DEPENDENCY));
                } catch (DavException e) {
                    ctxt.getResponseProp().addPropError(DavElements.E_CALENDAR_FREE_BUSY_SET, e);
                }
                if (newSet == null) {
                    newSet = new ArrayList<Element>(set);
                }
                newSet.remove(el);
            }
        }
        if (newSet != null)
            set = newSet;
        super.patchProperties(ctxt, set, remove);
    }

    private void updateCalendarFreeBusySet(DavContext ctxt, ArrayList<String> urls) throws ServiceException, DavException {
        String prefix = DavServlet.DAV_PATH + "/" + getOwner();
        Mailbox mbox = getMailbox(ctxt);
        HashMap<String,Folder> folders = new HashMap<String,Folder>();
        for (Folder f : getCalendarFolders(ctxt)) {
            folders.put(f.getPath(), f);
        }
        for (String origurl : urls) {
            // Clients often encode the "@" we use in our URLs
            String url = HttpUtil.urlUnescape(origurl);
            if (!url.startsWith(prefix)) {
                continue;
            }
            String path = url.substring(prefix.length());
            if (path.endsWith("/"))
                path = path.substring(0, path.length()-1);
            Folder f = folders.remove(path);
            if (f == null) {
                // check for recently renamed folders
                DavResource rs = UrlNamespace.checkRenamedResource(getOwner(), path);
                if (rs == null || !rs.isCollection())
                    throw new DavException("folder not found "+url, DavProtocol.STATUS_FAILED_DEPENDENCY);
                f = mbox.getFolderById(ctxt.getOperationContext(), ((MailItemResource)rs).getId());
            }
            if ((f.getFlagBitmask() & Flag.BITMASK_EXCLUDE_FREEBUSY) == 0)
                continue;
            ZimbraLog.dav.debug("clearing EXCLUDE_FREEBUSY for "+path);
            mbox.alterTag(ctxt.getOperationContext(), f.getId(), MailItem.Type.FOLDER, Flag.FlagInfo.EXCLUDE_FREEBUSY, false, null);
        }
        if (!folders.isEmpty()) {
            for (Folder f : folders.values()) {
                ZimbraLog.dav.debug("setting EXCLUDE_FREEBUSY for "+f.getPath());
                mbox.alterTag(ctxt.getOperationContext(), f.getId(), MailItem.Type.FOLDER, Flag.FlagInfo.EXCLUDE_FREEBUSY, true, null);
            }
        }
    }

    private java.util.Collection<Folder> getCalendarFolders(DavContext ctxt) throws ServiceException, DavException {
        ArrayList<Folder> calendars = new ArrayList<Folder>();
        Mailbox mbox = getMailbox(ctxt);
        for (Folder f : mbox.getFolderList(ctxt.getOperationContext(), SortBy.NONE)) {
            if (!(f instanceof Mountpoint) &&
                    (f.getDefaultView() == MailItem.Type.APPOINTMENT || f.getDefaultView() == MailItem.Type.TASK)) {
                calendars.add(f);
            }
        }
        return calendars;
    }
}
