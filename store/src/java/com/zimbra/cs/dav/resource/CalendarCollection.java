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

import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.QName;

import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.calendar.ZCalendar;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.HttpUtil;
import com.zimbra.common.util.L10nUtil;
import com.zimbra.common.util.L10nUtil.MsgKey;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.dav.DavProtocol;
import com.zimbra.cs.dav.caldav.AutoScheduler;
import com.zimbra.cs.dav.caldav.CalDavUtils;
import com.zimbra.cs.dav.caldav.Range.TimeRange;
import com.zimbra.cs.dav.property.CalDavProperty;
import com.zimbra.cs.dav.property.ResourceProperty;
import com.zimbra.cs.dav.service.method.Delete;
import com.zimbra.cs.dav.service.method.Get;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.cs.fb.FreeBusyQuery;
import com.zimbra.cs.mailbox.BadOrganizerException;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.CalendarItem.ReplyInfo;
import com.zimbra.cs.mailbox.DavNames;
import com.zimbra.cs.mailbox.DavNames.DavName;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.SetCalendarItemData;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.RecurId;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;
import com.zimbra.cs.mailbox.calendar.cache.CtagInfo;
import com.zimbra.cs.mime.ParsedMessage;
import com.zimbra.cs.service.FileUploadServlet.Upload;
import com.zimbra.cs.servlet.ETagHeaderFilter;
import com.zimbra.cs.util.AccountUtil.AccountAddressMatcher;

/**
 * draft-dusseault-caldav-15 section 4.2
 *
 * @author jylee
 *
 */
public class CalendarCollection extends Collection {

    public CalendarCollection(DavContext ctxt, Folder f) throws DavException, ServiceException {
        super(ctxt, f);
        Account acct = f.getAccount();
        addProperty(ResourceProperty.AddMember.create(UrlNamespace.getFolderUrl(ctxt.getUser(), f.getName())));

        if (f.getDefaultView() == MailItem.Type.APPOINTMENT || f.getDefaultView() == MailItem.Type.TASK) {
            addResourceType(DavElements.E_CALENDAR);
        }
        // the display name can be a user friendly string like "John Smith's Calendar".
        // but the problem is the name may be too long to fit into the field in UI.
        Locale lc = acct.getLocale();
        String description = L10nUtil.getMessage(MsgKey.caldavCalendarDescription, lc, acct.getAttr(Provisioning.A_displayName), f.getName());
        ResourceProperty desc = new ResourceProperty(DavElements.E_CALENDAR_DESCRIPTION);
        desc.setMessageLocale(lc);
        desc.setStringValue(description);
        desc.setVisible(false);
        addProperty(desc);
        addProperty(CalDavProperty.getSupportedCalendarComponentSet(f.getDefaultView()));
        addProperty(CalDavProperty.getSupportedCalendarData());
        addProperty(CalDavProperty.getSupportedCollationSet());
        addProperty(CalDavProperty.getCalendarTimezone(acct));

        mCtag = CtagInfo.makeCtag(f);
        setProperty(DavElements.E_GETCTAG, mCtag);

        addProperty(getIcalColorProperty());
        setProperty(DavElements.E_ALTERNATE_URI_SET, null, true);
        setProperty(DavElements.E_GROUP_MEMBER_SET, null, true);
        setProperty(DavElements.E_GROUP_MEMBERSHIP, null, true);

        // remaining recommended attributes: calendar-timezone, max-resource-size,
        // min-date-time, max-date-time, max-instances, max-attendees-per-instance,
        //
    }

    private static final QName[] SUPPORTED_REPORTS = {
            DavElements.E_CALENDAR_QUERY,
            DavElements.E_CALENDAR_MULTIGET,
            DavElements.E_FREE_BUSY_QUERY,
            DavElements.E_ACL_PRINCIPAL_PROP_SET,
            DavElements.E_PRINCIPAL_MATCH,
            DavElements.E_PRINCIPAL_PROPERTY_SEARCH,
            DavElements.E_PRINCIPAL_SEARCH_PROPERTY_SET,
            DavElements.E_EXPAND_PROPERTY
    };

    @Override
    protected QName[] getSupportedReports() {
        return SUPPORTED_REPORTS;
    }

    /* Returns all the appoinments stored in the calendar as DavResource. */
    @Override
    public java.util.Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
        return getChildren(ctxt, null);
    }

    protected Map<String,DavResource> mAppts;
    protected boolean mMetadataOnly;
    protected String mCtag;

    /* Returns all the appointments specified in hrefs */
    public java.util.Collection<DavResource> getChildren(DavContext ctxt, TimeRange range) throws DavException {
        Map<String,DavResource> requestedAppts = null;
        boolean fetchAppts =
            range != null || // ranged request
            mAppts == null; // hasn't fetched before
        if (fetchAppts) {
            try {
                requestedAppts = getAppointmentMap(ctxt, range);
                // cache the full appt list
                if (range == null && needCalendarData(ctxt))
                    mAppts = requestedAppts;
            } catch (ServiceException se) {
                ZimbraLog.dav.error("can't get calendar items", se);
                return Collections.emptyList();
            }
        } else {
            requestedAppts = mAppts;
        }
        return requestedAppts.values();
    }

    protected Map<String,String> getUidToHrefMap(java.util.Collection<String> hrefs) {
        HashMap<String,String> uidmap = new HashMap<String,String>();
        for (String href : hrefs) {
            try {
                int start = href.lastIndexOf('/') + 1;
                int end = href.lastIndexOf(".ics");
                if ((start >= 0) && (end > start)) {
                    String uid = href.substring(start, end);
                    uid = URLDecoder.decode(uid, "UTF-8");
                    if (start > 0 && end > 0 && end > start) {
                        uidmap.put(uid, href);
                    }
                } else {
                    ZimbraLog.dav.warn("Unexpected href '%s' for a calendar item - ignoring it.", href);
                }
            } catch (Exception e) {
                ZimbraLog.dav.warn("can't decode href %s", href, e);
            }
        }
        return uidmap;
    }

    // see if the request is only for the metadata, for which case we have a shortcut
    // to prevent scanning and improve performance.
    private static final HashSet<QName> sMetaProps;
    static {
        sMetaProps = new HashSet<QName>();
        sMetaProps.add(DavElements.E_GETETAG);
        sMetaProps.add(DavElements.E_RESOURCETYPE);
        sMetaProps.add(DavElements.E_DISPLAYNAME);
    }
    protected boolean needCalendarData(DavContext ctxt) throws DavException {
        String method = ctxt.getRequest().getMethod();
        if (method.equals(Get.GET) || method.equals(Delete.DELETE))
            return true;
        for (QName prop : ctxt.getRequestProp().getProps())
            if (!sMetaProps.contains(prop))
                return true;
        return false;
    }
    protected Mailbox getCalendarMailbox(DavContext ctxt) throws ServiceException, DavException {
        return getMailbox(ctxt);
    }
    protected Map<String,DavResource> getAppointmentMap(DavContext ctxt, TimeRange range) throws ServiceException, DavException {
        Mailbox mbox = getCalendarMailbox(ctxt);

        HashMap<String,DavResource> appts = new HashMap<String,DavResource>();
        ctxt.setCollectionPath(getUri());
        if (range == null)
            range = new TimeRange(getOwner());
        long start = range.getStart();
        long end = range.getEnd();
        start = start == Long.MIN_VALUE ? -1 : start;
        end = end == Long.MAX_VALUE ? -1 : end;
        if (!needCalendarData(ctxt)) {
            ZimbraLog.dav.debug("METADATA only");
            mMetadataOnly = true;
            for (CalendarItem.CalendarMetadata item : mbox.getCalendarItemMetadata(getId(), start, end)) {
                appts.put(item.uid, new CalendarObject.LightWeightCalendarObject(getUri(), getOwner(), item));
            }
        } else {
            for (CalendarItem calItem : mbox.getCalendarItemsForRange(ctxt.getOperationContext(), start, end, getId(), null))
                appts.put(calItem.getUid(), new CalendarObject.LocalCalendarObject(ctxt, calItem));
        }
        return appts;
    }

    public java.util.Collection<DavResource> getAppointmentsByUids(DavContext ctxt, List<String> hrefs) throws ServiceException, DavException {
        Map<String,String> uidmap = getUidToHrefMap(hrefs);
        Mailbox mbox = getCalendarMailbox(ctxt);

        ArrayList<DavResource> appts = new ArrayList<DavResource>();
        ctxt.setCollectionPath(getUri());
        Map<String,CalendarItem> calItems = mbox.getCalendarItemsByUid(ctxt.getOperationContext(), new ArrayList<String>(uidmap.keySet()));
        for (String uid : calItems.keySet()) {
            CalendarItem calItem = calItems.get(uid);
            if (calItem == null)
                appts.add(new DavResource.InvalidResource(uidmap.get(uid), getOwner()));
            else
                appts.add(new CalendarObject.LocalCalendarObject(ctxt, calItem));
        }
        return appts;
    }

    private String findSummary(ZVCalendar cal) {
        Iterator<ZComponent> iter = cal.getComponentIterator();
        while (iter.hasNext()) {
            ZComponent comp = iter.next();
            String summary = comp.getPropVal(ICalTok.SUMMARY, null);
            if (summary != null)
                return summary;
        }
        return "calendar event";
    }

    private String findEventUid(List<Invite> invites) throws DavException {
        String uid = null;
        MailItem.Type itemType = null;
        LinkedList<Invite> inviteList = new LinkedList<Invite>();
        for (Invite i : invites) {
            MailItem.Type mItemType = i.getItemType();
            if (mItemType == MailItem.Type.APPOINTMENT || mItemType == MailItem.Type.TASK) {
                if (uid != null && uid.compareTo(i.getUid()) != 0)
                    throw new DavException.InvalidData(DavElements.E_VALID_CALENDAR_OBJECT_RESOURCE, "too many components");
                uid = i.getUid();
            }

            if (itemType != null && itemType != mItemType)
                throw new DavException.InvalidData(DavElements.E_VALID_CALENDAR_OBJECT_RESOURCE, "different types of components in the same resource");
            else
                itemType = mItemType;

            if (i.isRecurrence())
                inviteList.addFirst(i);
            else
                inviteList.addLast(i);
        }
        if (uid == null)
            throw new DavException.InvalidData(DavElements.E_SUPPORTED_CALENDAR_COMPONENT, "no event in the request");

        if ((getDefaultView() == MailItem.Type.APPOINTMENT || getDefaultView() == MailItem.Type.TASK) && (itemType != getDefaultView()))
            throw new DavException.InvalidData(DavElements.E_SUPPORTED_CALENDAR_COMPONENT, "resource type not supported in this collection");

        invites.clear();
        invites.addAll(inviteList);
        return uid;
    }

    /**
     * Check that we have a valid organizer field and if not, make adjustments if appropriate.
     * For Vanilla CalDAV access where Apple style delegation has not been enabled, attempts by the delegate
     * to use a shared calendar acting as themselves are translated to appear as if acting as a delegate,
     * otherwise the experience can be very poor.
     */
    private void adjustOrganizer(DavContext ctxt, Invite invite) throws ServiceException {
        if (!invite.hasOrganizer() && !invite.hasOtherAttendees()) {
            return;
        }
        ZOrganizer org = invite.getOrganizer();
        String origOrganizerAddr = null;
        // if ORGANIZER field is unset, set the field value with authUser's email addr.
        if (org == null) {
            org = new ZOrganizer(ctxt.getAuthAccount().getName(), null);
            invite.setOrganizer(org);
            invite.setIsOrganizer(true);
        } else {
            // iCal may use alias for organizer address. Rewrite that to primary address
            origOrganizerAddr = org.getAddress();
            Account acct = Provisioning.getInstance().get(AccountBy.name, origOrganizerAddr);
            if (acct != null) {
                String newAddr = acct.getName();
                if (!origOrganizerAddr.equals(newAddr)) {
                    org.setAddress(newAddr);
                    invite.setIsOrganizer(acct);
                }
            }
        }
        if (ctxt.useIcalDelegation()) {
            return;
        }
        String owner = getOwner();
        if (owner == null) {
            return;
        }
        Account ownerAcct = Provisioning.getInstance().get(AccountBy.name, owner);
        if (ownerAcct == null) {
            return;
        }
        AccountAddressMatcher acctMatcher = new AccountAddressMatcher(ctxt.getAuthAccount());
        origOrganizerAddr = org.getAddress();
        if (acctMatcher.matches(origOrganizerAddr) && !owner.equalsIgnoreCase(origOrganizerAddr)) {
            /**
             * Client probably doesn't realize user should be acting as a delegate for this calendar.
             * Adjust ATTENDEE and ORGANIZER if applicable.
             */
            org.setAddress(ownerAcct.getName());
            org.setCn(ownerAcct.getDisplayName());
            org.setSentBy(origOrganizerAddr);
            invite.setIsOrganizer(ownerAcct);
            boolean hasOwnerAttendee = false;
            for (ZAttendee attendee : invite.getAttendees()) {
                if (attendee.addressMatches(owner)) {
                    hasOwnerAttendee = true;
                }
            }
            if (!hasOwnerAttendee) {
                // If have an ATTENDEE record for who client thought was the ORGANIZER with
                // PARTSTAT:ACCEPTED - probably meant the real ORGANIZER
                for (ZAttendee attendee : invite.getAttendees()) {
                    if (attendee.addressMatches(origOrganizerAddr) &&
                            (IcalXmlStrMap.PARTSTAT_ACCEPTED.equalsIgnoreCase(attendee.getPartStat()))) {
                        attendee.setAddress(ownerAcct.getName());
                        attendee.setCn(ownerAcct.getDisplayName());
                        attendee.setSentBy(origOrganizerAddr);
                    }
                }
            }
        }
    }

    @Override
    public void handlePost(DavContext ctxt) throws DavException, IOException, ServiceException {
        Provisioning prov = Provisioning.getInstance();
        DavResource rs = null;
        try {
            String user = ctxt.getUser();
            Account account = prov.get(AccountBy.name, user);
            if (account == null) {
                // Anti-account name harvesting.
                ZimbraLog.dav.info("Failing POST to Calendar - no such account '%s'", user);
                throw new DavException("Request denied", HttpServletResponse.SC_NOT_FOUND, null);
            }

            List<Invite> invites = uploadToInvites(ctxt, account);
            String uid = findEventUid(invites);
            rs =  createItemFromInvites(ctxt, account, uid + ".ics", invites, false);
            if (rs.isNewlyCreated()) {
                ctxt.getResponse().setHeader("Location", rs.getHref());
                ctxt.setStatus(HttpServletResponse.SC_CREATED);
            } else {
                ctxt.setStatus(HttpServletResponse.SC_NO_CONTENT);
            }
            if (rs.hasEtag()) {
                ctxt.getResponse().setHeader(DavProtocol.HEADER_ETAG, rs.getEtag());
                ctxt.getResponse().setHeader(ETagHeaderFilter.ZIMBRA_ETAG_HEADER, rs.getEtag());
            }
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.FORBIDDEN)) {
                throw new DavException(e.getMessage(), HttpServletResponse.SC_FORBIDDEN, e);
            } else {
                throw new DavException("cannot create icalendar item", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            }
        }
    }

    /**
     * Creates an appointment sent in PUT request in this calendar.
     * @param name - basename (last component) of the path - i.e. the name within this collection
     */
    @Override
    public DavResource createItem(DavContext ctxt, String name) throws DavException, IOException {
        Provisioning prov = Provisioning.getInstance();
        try {
            String user = ctxt.getUser();
            Account account = prov.get(AccountBy.name, user);
            if (account == null) {
                // Anti-account name harvesting.
                ZimbraLog.dav.info("Failing POST to Calendar - no such account '%s'", user);
                throw new DavException("Request denied", HttpServletResponse.SC_NOT_FOUND, null);
            }

            List<Invite> invites = uploadToInvites(ctxt, account);
            return createItemFromInvites(ctxt, account, name, invites, true);
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.FORBIDDEN)) {
                throw new DavException(e.getMessage(), HttpServletResponse.SC_FORBIDDEN, e);
            } else {
                throw new DavException("cannot create icalendar item", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            }
        }
    }

    /**
     * @param name Preferred DAV basename for the new item - including ".ics" if appropriate.
     * @param allowUpdate - PUTs are allowed to update a pre-existing item.  POSTs to the containing collection are not.
     */
    public DavResource createItemFromInvites(DavContext ctxt, Account account, String name, List<Invite> invites,
            boolean allowUpdate)
    throws DavException, IOException {

        boolean useEtag = allowUpdate;
        try {
            String user = account.getName();
            /*
             * Some of the CalDAV clients do not behave very well when it comes to etags.
             *     chandler doesn't set User-Agent header, doesn't understand If-None-Match or If-Match headers.
             *     evolution 2.8 always sets If-None-Match although we return etag in REPORT.
             *     ical correctly understands etag and sets If-Match for existing etags, but does not use If-None-Match
             *     for new resource creation.
             */
            HttpServletRequest req = ctxt.getRequest();

            String etag = null;
            if (useEtag) {
                etag = req.getHeader(DavProtocol.HEADER_IF_MATCH);
                useEtag = (etag != null);
            }

            String baseName = HttpUtil.urlUnescape(name);
            boolean acceptableClientChosenBasename =
                    DebugConfig.enableDAVclientCanChooseResourceBaseName && baseName.equals(name);
            if (name.endsWith(CalendarObject.CAL_EXTENSION)) {
                name = name.substring(0, name.length()-CalendarObject.CAL_EXTENSION.length());
                // Unescape the name (It was encoded in DavContext intentionally)
                name = HttpUtil.urlUnescape(name);
            }

            String uid = findEventUid(invites);
            Mailbox mbox = MailboxManager.getInstance().getMailboxByAccount(account);
            CalendarItem origCalItem = null;
            // Is the basename of the path client assigned rather than following the standard pattern?
            Integer itemId = null;
            if (acceptableClientChosenBasename) {
                itemId =  DavNames.get(this.mMailboxId, this.mId, baseName);
            }
            if (itemId != null) {
                try {
                    MailItem mailItem = mbox.getItemById(ctxt.getOperationContext(), itemId, MailItem.Type.UNKNOWN);
                    if (mailItem != null && mailItem instanceof CalendarItem) {
                        origCalItem = (CalendarItem) mailItem;
                    }
                } catch (ServiceException se) {
                }
            }
            if (origCalItem == null) {
                if (uid.equals(name)) {
                    origCalItem = mbox.getCalendarItemByUid(ctxt.getOperationContext(), name);
                } else {
                    /* the basename of the path doesn't fit our preferred naming convention. */
                    origCalItem = mbox.getCalendarItemByUid(ctxt.getOperationContext(), uid);
                    String redirectUrl = null;
                    if (origCalItem != null) {
                        if (this.mId != origCalItem.getFolderId()) {
                            // In another folder, ignore
                            origCalItem = null;
                        } else {
                            // The item exists, but doesn't have this name - UID conflict.
                            if (acceptableClientChosenBasename) {
                                redirectUrl = hrefForCalendarItem(origCalItem, user, uid);
                            } else {
                                redirectUrl = defaultUrlForCalendarItem(user, uid);
                            }
                            throw new DavException.UidConflict(
                                    "An item with the same UID already exists in the calendar", redirectUrl);
                        }
                    }
                    if ((origCalItem == null) && (!DebugConfig.enableDAVclientCanChooseResourceBaseName)) {
                        redirectUrl = defaultUrlForCalendarItem(user, uid);
                    }

                    if (allowUpdate && (redirectUrl != null)) {
                        /* SC_FOUND - Status code (302) indicating that the resource reside temporarily under a
                         * different URI. Since the redirection might be altered on occasion, the client should
                         * continue to use the Request-URI for future requests.(HTTP/1.1) To represent the status code
                         * (302), it is recommended to use this variable.  Used to be called SC_MOVED_TEMPORARILY
                         */
                        ctxt.getResponse().sendRedirect(redirectUrl);  // sets status to SC_FOUND
                        StringBuilder wrongUrlMsg = new StringBuilder();
                        wrongUrlMsg.append("wrong url - redirecting to:\n").append(redirectUrl);
                        throw new DavException(wrongUrlMsg.toString(), HttpServletResponse.SC_FOUND, null);
                    }
                }
            }
            if (origCalItem == null && useEtag) {
                throw new DavException("event not found", HttpServletResponse.SC_NOT_FOUND, null);
            }

            if (origCalItem != null && !allowUpdate) {
                throw new DavException.UidConflict("An item with the same UID already exists in the calendar",
                        hrefForCalendarItem(origCalItem, user, uid));
            }

            boolean isNewItem = true;
            if (useEtag) {
                String itemEtag = MailItemResource.getEtag(origCalItem);
                if (!itemEtag.equals(etag)) {
                    throw new DavException(
                            String.format("CalDAV client has stale event: event has different etag (%s) vs %s",
                                    itemEtag, etag), HttpServletResponse.SC_PRECONDITION_FAILED);
                }
                isNewItem = false;
            }

            // prepare to call Mailbox.setCalendarItem()
            int flags = 0; String[] tags = null; List<ReplyInfo> replies = null;
            Invite origInvites[] = null;
            if (origCalItem != null) {
                flags = origCalItem.getFlagBitmask();
                tags = origCalItem.getTags();
                replies = origCalItem.getAllReplies();
                origInvites = origCalItem.getInvites();
            }
            SetCalendarItemData scidDefault = new SetCalendarItemData();
            SetCalendarItemData scidExceptions[] = null;

            int idxExceptions = 0;
            boolean first = true;
            for (Invite i : invites) {
                // check for valid uid.
                if (i.getUid() == null)
                    i.setUid(uid);
                adjustOrganizer(ctxt, i);
                // Carry over the MimeMessage/ParsedMessage to preserve any attachments.
                // CalDAV clients don't support attachments, and on edit we have to either
                // retain existing attachments or drop them.  Retaining is better.
                ParsedMessage oldPm = null;
                if (origCalItem != null) {
                    Invite oldInv = origCalItem.getInvite(i.getRecurId());
                    if (oldInv == null && i.hasRecurId()) {
                        // It's a new exception instance.  Inherit from series.
                        oldInv = origCalItem.getInvite((RecurId) null);
                    }
                    if (oldInv != null) {
                        MimeMessage mmInv = origCalItem.getSubpartMessage(oldInv.getMailItemId());
                        oldPm = mmInv != null ? new ParsedMessage(mmInv, false) : null;
                    }
                }
                if (first) {
                    scidDefault.invite = i;
                    scidDefault.message = oldPm;
                    first = false;
                } else {
                    SetCalendarItemData scid = new SetCalendarItemData();
                    scid.invite = i;
                    scid.message = oldPm;
                    if (scidExceptions == null) {
                        scidExceptions = new SetCalendarItemData[invites.size() - 1];
                    }
                    scidExceptions[idxExceptions++] = scid;
                }

                // For attendee case, update replies list with matching ATTENDEE from the invite.
                if (!i.isOrganizer() && replies != null) {
                    ZAttendee at = i.getMatchingAttendee(account);
                    if (at != null) {
                        AccountAddressMatcher acctMatcher = new AccountAddressMatcher(account);
                        ReplyInfo newReply = null;
                        for (Iterator<ReplyInfo> replyIter = replies.iterator(); replyIter.hasNext(); ) {
                            ReplyInfo reply = replyIter.next();
                            if (acctMatcher.matches(reply.getAttendee().getAddress())) {
                                RecurId ridR = reply.getRecurId(), ridI = i.getRecurId();
                                if ((ridR == null && ridI == null) || (ridR != null && ridR.equals(ridI))) {
                                    // matching RECURRENCE-ID
                                    // No need to compare SEQUENCE and DTSTAMP of existing reply and new invite.
                                    // We're just going to take what the caldav client sent, even if it's older
                                    // than the existing reply.
                                    replyIter.remove();
                                    if (!IcalXmlStrMap.PARTSTAT_NEEDS_ACTION.equalsIgnoreCase(at.getPartStat())) {
                                        newReply = new ReplyInfo(at, i.getSeqNo(), i.getDTStamp(), ridI);
                                    }
                                    break;
                                }
                            }
                        }
                        if (newReply != null) {
                            replies.add(newReply);
                        }
                    }
                }
            }
            CalendarItem newCalItem = null;
            AutoScheduler autoScheduler = AutoScheduler.getAutoScheduler(mbox, this.getCalendarMailbox(ctxt),
                    origInvites, mId, flags, tags, scidDefault, scidExceptions, replies, ctxt);
            if (autoScheduler == null) {
                newCalItem = mbox.setCalendarItem(ctxt.getOperationContext(), mId, flags, tags,
                        scidDefault, scidExceptions, replies, CalendarItem.NEXT_ALARM_KEEP_CURRENT);
            } else {
                // Note: This also sets the calendar item
                newCalItem = autoScheduler.doSchedulingActions();
            }
            if (newCalItem == null) {
                throw new DavException("cannot create icalendar item - corrupt ICAL?",
                        HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            if (!uid.equals(name)) {
                if (acceptableClientChosenBasename) {
                    DavNames.put(DavNames.DavName.create(
                            this.mMailboxId, newCalItem.getFolderId(), baseName), newCalItem.getId());
                }
            }
            return new CalendarObject.LocalCalendarObject(ctxt, newCalItem, isNewItem);
        } catch (BadOrganizerException.DiffOrganizerInComponentsException e) {
            throw new DavException.NeedSameOrganizerInAllComponents(e.getMessage());
        } catch (BadOrganizerException e) {
            // Some clients will keep trying with the same data if get INTERNAL_SERVER_ERROR.  Better to use
            // FORBIDDEN if we aren't going to be able to cope with the data
            throw new DavException(e.getMessage(), HttpServletResponse.SC_FORBIDDEN, e);
        } catch (ServiceException e) {
            if (e.getCode().equals(ServiceException.FORBIDDEN)) {
                throw new DavException(e.getMessage(), HttpServletResponse.SC_FORBIDDEN, e);
            } else {
                throw new DavException("cannot create icalendar item", HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
            }
        }
    }

    private String hrefForCalendarItem(CalendarItem calItem, String user, String uid)
    throws DavException, ServiceException {
        String preexistingHref = null;
        DavName davName = null;
        if (DebugConfig.enableDAVclientCanChooseResourceBaseName) {
            davName = DavNames.get(this.mMailboxId, calItem.getId());
        }
        if (davName != null) {
            preexistingHref = fullUrlForChild(user, davName.davBaseName);
        } else {
            preexistingHref = defaultUrlForCalendarItem(user, uid);
        }
        return preexistingHref;
    }

    private List<Invite> uploadToInvites(DavContext ctxt, Account account)
    throws DavException, IOException {
        Upload upload = ctxt.getUpload();
        String contentType = upload.getContentType();
        if (!contentType.startsWith(MimeConstants.CT_TEXT_CALENDAR)) {
            throw new DavException.InvalidData(DavElements.E_SUPPORTED_CALENDAR_DATA,
                    String.format("Incorrect Content-Type '%s', expected '%s'",
                            contentType, MimeConstants.CT_TEXT_CALENDAR));
        }
        if (upload.getSize() <= 0) {
            throw new DavException.InvalidData(DavElements.E_VALID_CALENDAR_DATA,"empty request");
        }
        List<Invite> invites;
        try (InputStream is = ctxt.getUpload().getInputStream()) {
            ZCalendar.ZVCalendar vcalendar = ZCalendar.ZCalendarBuilder.build(is, MimeConstants.P_CHARSET_UTF8);
            CalDavUtils.removeAttendeeForOrganizer(vcalendar);  // Apple iCal fixup
            if (ctxt.isIcalClient()) { // Apple iCal fixup for todos
                CalDavUtils.adjustPercentCompleteForToDos(vcalendar);
            }
            invites = Invite.createFromCalendar(account, findSummary(vcalendar), vcalendar, true);
        } catch (ServiceException se) {
            throw new DavException.InvalidData(DavElements.E_VALID_CALENDAR_DATA,
                    String.format("Problem parsing %s data - %s", MimeConstants.CT_TEXT_CALENDAR, se.getMessage()));
        }
        return invites;
    }

    private String defaultUrlForCalendarItem(String user, String uid) throws DavException, ServiceException {
        StringBuilder basename = new StringBuilder(uid).append(CalendarObject.CAL_EXTENSION);
        return fullUrlForChild(user, basename.toString());
    }

    /* Returns iCalalendar (RFC 2445) representation of freebusy report for specified time range. */
    public String getFreeBusyReport(DavContext ctxt, TimeRange range) throws ServiceException, DavException {
        Mailbox mbox = getCalendarMailbox(ctxt);
        FreeBusy fb = mbox.getFreeBusy(ctxt.getOperationContext(), range.getStart(), range.getEnd(), FreeBusyQuery.CALENDAR_FOLDER_ALL);
        return fb.toVCalendar(FreeBusy.Method.REPLY, ctxt.getAuthAccount().getName(), mbox.getAccount().getName(), null);
    }
}
