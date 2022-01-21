/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2021 Synacor, Inc.
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

package com.zimbra.cs.mailbox.calendar;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;

import org.apache.commons.io.IOUtils;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.zimbra.common.account.Key.AccountBy;
import com.zimbra.common.calendar.Attach;
import com.zimbra.common.calendar.Geo;
import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ParsedDuration;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZCalendarBuilder;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.calendar.ZCalendar.ZVCalendar;
import com.zimbra.common.localconfig.DebugConfig;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.mime.MimeConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ByteUtil;
import com.zimbra.common.util.Log;
import com.zimbra.common.util.LogFactory;
import com.zimbra.common.util.StringUtil;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Identity;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.index.Fragment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.CalendarItem.Instance;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.mailbox.calendar.Alarm.Action;
import com.zimbra.cs.mailbox.calendar.Alarm.TriggerRelated;
import com.zimbra.cs.mailbox.calendar.Alarm.TriggerType;
import com.zimbra.cs.mailbox.calendar.Recurrence.IRecurrence;
import com.zimbra.cs.mime.MPartInfo;
import com.zimbra.cs.mime.Mime;
import com.zimbra.cs.util.AccountUtil.AccountAddressMatcher;

/**
 * Invite
 *
 * Invite isn't really the right name for this class, it should be called CalendarComponent
 * or something...
 *
 * An Invite represents a single component entry of an CalendarItem -- ie a single VEvent or a VTodo or whatever
 * This is our smallest "chunk" of raw iCal data -- it has a single UUID, etc etc
 */
public class Invite {

    private static final boolean OUTLOOK_COMPAT_ALLDAY =
        LC.calendar_outlook_compatible_allday_events.booleanValue();

    public static final String HEADER_SEPARATOR = "*~*~*~*~*~*~*~*~*~*";

    static Log sLog = LogFactory.getLog(Invite.class);

    /**
     * Constructs an Invite object. This is called when an invite
     * is being retrieved from the database.
     * @param end
     * @param start
     */
    Invite(MailItem.Type type, String methodStr, TimeZoneMap tzmap, CalendarItem calItem, String uid, String status,
            String priority, String pctComplete, long completed, String freebusy, String transp, String classProp,
            ParsedDateTime start, ParsedDateTime end, ParsedDuration duration, Recurrence.IRecurrence recurrence,
            boolean isOrganizer, ZOrganizer org, List<ZAttendee> attendees, String name, String loc, int flags,
            String partStat, Boolean rsvp, RecurId recurrenceId, long dtstamp, long lastModified,
            int seqno, int lastFullSeqno, int mailboxId, int mailItemId,
            int componentNum, boolean sentByMe, String description, String descHtml, String fragment,
            List<String> comments, List<String> categories, List<String> contacts, Geo geo, String url) {
        setItemType(type);
        mMethod = lookupMethod(methodStr);
        mTzMap = tzmap;
        mCalItem = calItem;
        mUid = uid;
        mStatus = status;
        mPriority = priority;
        mPercentComplete = pctComplete;
        mCompleted = completed;
        mFreeBusy = freebusy;
        mTransparency = transp;
        mClass = classProp;
        mClassSetByMe = sentByMe;
        mStart = start;
        mEnd = end;
        mDuration = duration;
        mIsOrganizer = isOrganizer;
        mOrganizer = org;
        mAttendees = attendees;
        mName = name != null ? name : "";
        mLocation = loc != null ? loc : "";
        mFlags = flags;
        mPartStat = partStat;
        mRsvp = rsvp;
        mSeqNo = seqno;
        // bug 74117 : mLastSeqNo contains the sequence number for which invitation is sent to all the attendees.
        // This will be used when accepting the replies.
        mLastFullSeqNo = lastFullSeqno;
        setDtStamp(dtstamp);
        setLastModified(lastModified);

        mMailboxId = mailboxId;
        mMailItemId = mailItemId;
        mComponentNum = componentNum;
        mSentByMe = sentByMe;
        setDescription(description, descHtml);
        mFragment = fragment != null ? fragment : "";
        mComments = comments != null ? comments : new ArrayList<String>();
        mCategories = categories != null ? categories : new ArrayList<String>();
        mContacts = contacts != null ? contacts : new ArrayList<String>();
        mGeo = geo;
        setUrl(url);

        setRecurrence(recurrence);
        setRecurId(recurrenceId);
    }

    private Recurrence.IRecurrence mRecurrence;
    public Recurrence.IRecurrence getRecurrence() { return mRecurrence; }
    public void setRecurrence(Recurrence.IRecurrence recur) {
        // Set RRULE only when RECURRENCE-ID is not set.
        if (mRecurrenceId == null) {
            mRecurrence = recur;
            setIsRecurrence(mRecurrence != null);
        }
    }
    private void clearRecurrence() {
        mRecurrence = null;
        setIsRecurrence(false);
    }
    protected RecurId mRecurrenceId = null; // RECURRENCE_ID
    public RecurId getRecurId() { return mRecurrenceId; }
    public void setRecurId(RecurId rid) {
        mRecurrenceId = rid;
        // Clear any RRULE if we're setting RECURRENCE-ID to a non-null value.
        if (mRecurrenceId != null)
            clearRecurrence();
    }
    public boolean hasRecurId() { return mRecurrenceId != null; }

    private boolean mSentByMe;
    private String mFragment;
    public String getFragment() { return mFragment; }
    public void setFragment(String fragment) { mFragment = fragment; }

    /**
     * Create an Invite object which will then be added to a mailbox Mailbox.addInvite()
     *
     * @param method usually "REQUEST" or else CANCEL/REPLY/PUBLISH
     * @param tzMap TimeZoneMap which contains every timezone referenced in DtStart, DtEnd, Duration or Recurrence
     * @param uid UID of this calendar item
     * @param status IcalXmlStrMap.STATUS_* RFC2445 status: eg TENTATIVE/CONFIRMED/CANCELLED
     * @param freeBusy IcalXmlStrMap.FB* (F/B/T/U -- show time as Free/Busy/Tentative/Unavailable)
     * @param transparency IcalXmlStrMap.TRANSP_* RFC2445 Transparency
     * @param classProp IcalXmlStrMap.CLASS_*
     * @param allDayEvent TRUE if this is an all-day-event, FALSE otherwise.  This will override the Time part of DtStart and DtEnd, and will throw an ServiceException.FAILURE if the Duration is not Days or Weeks
     * @param dtStart Start time
     * @param dtEndOrNull End time OR NULL (duration must be specified if this is null)
     * @param durationOrNull Duration (may not be specified if dtEnd is specified)
     * @param recurID If this invite is an EXCEPTION, the ID of the instance being excepted
     * @param recurrenceOrNull IRecurrence rule tree
     * @param organizer RFC2445 Organizer: see Invite.createOrganizer
     * @param attendees list of RFC2445 Attendees: see Invite.createAttendee
     * @param name Name of this calendar item
     * @param location Location of this calendar item
     * @param description Description of this calendar item
     * @param dtStamp RFC2445 sequencing
     * @param lastModified
     * @param seqNo RFC2445 sequencing
     * @param partStat IcalXMLStrMap.PARTSTAT_* RFC2445 Participant Status of this mailbox
     * @param rsvp RFC2445 RSVP
     * @param sentByMe TRUE if this mailbox sent this invite
     */
    public static Invite createInvite(int mailboxId, MailItem.Type type, String method, TimeZoneMap tzMap,
            String uidOrNull, String status, String priority, String pctComplete, long completed, String freeBusy,
            String transparency, String classProp, boolean allDayEvent, ParsedDateTime dtStart,
            ParsedDateTime dtEndOrNull, ParsedDuration durationOrNull, RecurId recurId,
            Recurrence.IRecurrence recurrenceOrNull, boolean isOrganizer, ZOrganizer organizer,
            List<ZAttendee> attendees, String name, String location, String description, String descHtml,
            List<String> comments, List<String> categories, List<String> contacts, Geo geo, String url,
            long dtStamp, long lastModified, int seqNo, int lastFullSeqNo, String partStat, boolean rsvp, boolean sentByMe) {
        return new Invite(type, method, tzMap, null, // no calendar item yet
                uidOrNull, status, priority, pctComplete, completed, freeBusy, transparency, classProp, dtStart,
                dtEndOrNull, durationOrNull, recurrenceOrNull, isOrganizer, organizer, attendees, name, location,
                Invite.APPT_FLAG_EVENT | (allDayEvent ? Invite.APPT_FLAG_ALLDAY : 0),
                partStat, rsvp, recurId, dtStamp, lastModified, seqNo, lastFullSeqNo, mailboxId, 0, // mailItemId MUST BE SET
                0, // component num
                sentByMe, description, descHtml, Fragment.getFragment(description, true), comments, categories,
                contacts, geo, url);
    }

    /**
     * Called by Mailbox.addInvite once it has an ID for this invite
     *
     * @param invId
     */
    public void setInviteId(int invId) {
        this.mMailItemId = invId;
        if (mRecurrence != null) {
            mRecurrence.setInviteId(new InviteInfo(this));
        }
    }


    //private static final String FN_ADDRESS         = "a";
    private static final String FN_ITEMTYPE        = "it";
    private static final String FN_APPT_FLAGS      = "af";
    private static final String FN_ATTENDEE        = "at";
    private static final String FN_SENTBYME        = "byme";
    private static final String FN_CATEGORY        = "cat";
    private static final String FN_CLASS           = "cl";
    private static final String FN_CLASS_SETBYME   = "clSetByMe";
    private static final String FN_COMPLETED       = "completed";
    private static final String FN_COMPNUM         = "comp";
    private static final String FN_COMMENT         = "cmt";
    private static final String FN_CONTACT         = "contact";
    private static final String FN_DESC            = "desc";
    private static final String FN_DESC_HTML       = "descH";
    private static final String FN_X_ZIMBRA_DESC_HTML = "xDescH";
    private static final String FN_DESC_IN_META    = "dinM";  // whether description is stored in metadata
    private static final String FN_FRAGMENT        = "frag";
    private static final String FN_DTSTAMP         = "dts";
    private static final String FN_DURATION        = "duration";
    private static final String FN_END             = "et";
    private static final String FN_APPT_FREEBUSY   = "fb";
    private static final String FN_GEO             = "geo";
    private static final String FN_LOCATION        = "l";
    private static final String FN_LOCAL_ONLY      = "lo";
    private static final String FN_INVMSGID        = "mid";
    private static final String FN_LAST_MODIFIED   = "lastMod";
    private static final String FN_METHOD          = "mthd";
    private static final String FN_NAME            = "n";
    private static final String FN_NUM_ATTENDEES   = "numAt";
    private static final String FN_NUM_CATEGORIES  = "numCat";
    private static final String FN_NUM_COMMENTS    = "numCmt";
    private static final String FN_NUM_CONTACTS    = "numContacts";
    private static final String FN_ORGANIZER       = "org";
    private static final String FN_IS_ORGANIZER    = "isOrg";
    private static final String FN_PARTSTAT        = "ptst";
    private static final String FN_RSVP            = "rsvp";
    private static final String FN_RECURRENCE = "recurrence";
    private static final String FN_RECUR_ID        = "rid";
    private static final String FN_SEQ_NO          = "seq";
    private static final String FN_LAST_FULL_SEQ_NO     = "lfseq";
    private static final String FN_STATUS          = "status";  // calendar: event/todo/journal status
    private static final String FN_START           = "st";
    private static final String FN_TRANSP          = "tr";
    private static final String FN_TZMAP           = "tzm"; // calendaring: timezone map
    private static final String FN_UID             = "u";
    private static final String FN_PRIORITY        = "prio";
    private static final String FN_PCT_COMPLETE    = "pctcompl";
    private static final String FN_NUM_ALARMS      = "numAl";
    private static final String FN_ALARM           = "al";
    private static final String FN_DONT_INDEX_MM   = "noidxmm";
    private static final String FN_URL             = "url";

    public static int getMaxDescInMeta() {
        return LC.calendar_max_desc_in_metadata.intValueWithinRange(0, 1048576);
    }

    /**
     * This is only really public to support serializing RedoOps -- you
     * really don't want to call this API from anywhere else
     *
     * @param inv
     * @return
     */
    public static Metadata encodeMetadata(Invite inv) {
        Metadata meta = new Metadata();

        // Add local-only to metadata only when it's true.  This is consistent with the way metadata
        // looked before local-only flag was introduced.
        if (inv.isLocalOnly())
            meta.put(FN_LOCAL_ONLY, true);

        meta.put(FN_ITEMTYPE, inv.getItemType().toByte());
        meta.put(FN_UID, inv.getUid());
        meta.put(FN_INVMSGID, inv.getMailItemId());
        meta.put(FN_COMPNUM, inv.getComponentNum());
        meta.put(FN_SENTBYME, inv.mSentByMe);
        if (!inv.isPublic())
            meta.put(FN_CLASS, inv.getClassProp());
        meta.put(FN_CLASS_SETBYME, inv.classPropSetByMe());
        meta.put(FN_STATUS, inv.getStatus());
        if (inv.hasFreeBusy())
            meta.put(FN_APPT_FREEBUSY, inv.getFreeBusy());
        meta.put(FN_TRANSP, inv.getTransparency());
        meta.put(FN_START, inv.mStart);
        meta.put(FN_END, inv.mEnd);
        if (inv.mCompleted != 0)
            meta.put(FN_COMPLETED, inv.mCompleted);
        meta.put(FN_DURATION, inv.mDuration);
        meta.put(FN_METHOD, inv.mMethod.toString());
        meta.put(FN_FRAGMENT, inv.mFragment);

        // Put mDescription in metadata if it's short enough.
        if (inv.mDescInMeta) {
            meta.put(FN_DESC_IN_META, inv.mDescInMeta);
            if (inv.mDescription != null)
                meta.put(FN_DESC, inv.mDescription);
            if (inv.mDescHtml != null)
                meta.put(FN_DESC_HTML, inv.mDescHtml);
            if (inv.xZimbraDescHtml != null)
                meta.put(FN_X_ZIMBRA_DESC_HTML, inv.xZimbraDescHtml);
        }

        if (inv.mRecurrence != null) {
            meta.put(FN_RECURRENCE, inv.mRecurrence.encodeMetadata());
        }

        meta.put(FN_NAME, inv.getName());

        meta.put(FN_LOCATION, inv.mLocation);
        meta.put(FN_APPT_FLAGS, inv.getFlags());
        meta.put(FN_PARTSTAT, inv.getPartStat());
        if (inv.hasRsvp()) {
            meta.put(FN_RSVP, inv.getRsvp());
        }

        meta.put(FN_TZMAP, Util.encodeAsMetadata(inv.mTzMap));

        if (inv.hasRecurId()) {
            meta.put(FN_RECUR_ID, inv.getRecurId().encodeMetadata());
        }
        meta.put(FN_DTSTAMP, inv.getDTStamp());
        if (inv.getLastModified() != 0)
            meta.put(FN_LAST_MODIFIED, inv.getLastModified());
        meta.put(FN_SEQ_NO, inv.getSeqNo());
        meta.put(FN_LAST_FULL_SEQ_NO, inv.getLastFullSeqNo());

        if (inv.hasOrganizer()) {
            meta.put(FN_ORGANIZER, inv.getOrganizer().encodeMetadata());
        }
        meta.put(FN_IS_ORGANIZER, inv.isOrganizer());

        List<ZAttendee> ats = inv.getAttendees();
        meta.put(FN_NUM_ATTENDEES, String.valueOf(ats.size()));
        int i = 0;
        for (Iterator<ZAttendee> iter = ats.iterator(); iter.hasNext(); i++) {
            ZAttendee at = iter.next();
            meta.put(FN_ATTENDEE + i, at.encodeAsMetadata());
        }

        meta.put(FN_PRIORITY, inv.getPriority());
        meta.put(FN_PCT_COMPLETE, inv.getPercentComplete());

        List<String> comments = inv.getComments();
        if (comments != null) {
            int numComm = comments.size();
            if (numComm > 0) {
                meta.put(FN_NUM_COMMENTS, numComm);
                int idx = 0;
                for (String comm : comments) {
                    meta.put(FN_COMMENT + idx, comm);
                    idx++;
                }
            }
        }

        List<String> contacts = inv.getContacts();
        if (contacts != null) {
            int numContacts = contacts.size();
            if (numContacts > 0) {
                meta.put(FN_NUM_CONTACTS, numContacts);
                int idx = 0;
                for (String contact : contacts) {
                    meta.put(FN_CONTACT + idx, contact);
                    idx++;
                }
            }
        }

        List<String> categories = inv.getCategories();
        if (categories != null) {
            int numCat = categories.size();
            if (numCat > 0) {
                meta.put(FN_NUM_CATEGORIES, numCat);
                int idx = 0;
                for (String cat : categories) {
                    meta.put(FN_CATEGORY + idx, cat);
                    idx++;
                }
            }
        }

        Geo geo = inv.getGeo();
        if (geo != null) {
            meta.put(FN_GEO, Util.encodeMetadata(geo));
        }

        String url = inv.getUrl();
        if (url != null && url.length() > 0)
            meta.put(FN_URL, url);

        if (!inv.mAlarms.isEmpty()) {
            meta.put(FN_NUM_ALARMS, inv.mAlarms.size());
            i = 0;
            for (Iterator<Alarm> iter = inv.mAlarms.iterator(); iter.hasNext(); i++) {
                Alarm alarm = iter.next();
                meta.put(FN_ALARM + i, alarm.encodeMetadata());
            }
        }

        if (inv.mXProps.size() > 0)
            Util.encodeXPropsAsMetadata(meta, inv.xpropsIterator());

        if (inv.mDontIndexMimeMessage)
            meta.put(FN_DONT_INDEX_MM, true);
        return meta;
    }

    public static ICalTok lookupMethod(String methodName) {
        ICalTok toRet;
        String methodNameUpper = methodName.toUpperCase();  // work around livemeeting.com bug
        try {
            toRet = ICalTok.valueOf(methodNameUpper);
        } catch (IllegalArgumentException e) {
            toRet = ICalTok.PUBLISH;
            // Apple iCal generates non-standard "METHOD:EXPORT".
            if (methodNameUpper.compareToIgnoreCase("EXPORT") != 0)
                sLog.warn("Invalid METHOD " + methodName +
                          "; assuming PUBLISH", e);
        }
        switch (toRet) {
        case REQUEST:
        case PUBLISH:
        case REPLY:
        case ADD:
        case CANCEL:
        case REFRESH:
        case COUNTER:
        case DECLINECOUNTER:
            return toRet;
        default:
            return ICalTok.PUBLISH;
        }
    }

    /**
     * This API is public for RedoLogging to call into it -- you probably don't want to call it from
     * anywhere else!
     *
     * @param mailboxId
     * @param meta
     * @param calItem
     * @param accountTZ
     * @return
     * @throws ServiceException
     */
    public static Invite decodeMetadata(int mailboxId, Metadata meta, CalendarItem calItem, ICalTimeZone accountTZ)
    throws ServiceException {
        byte btype = (byte) meta.getLong(FN_ITEMTYPE, -1);
        MailItem.Type type = btype >= 0 ? MailItem.Type.of(btype) : MailItem.Type.APPOINTMENT;
        String uid = meta.get(FN_UID, null);
        int mailItemId = (int)meta.getLong(FN_INVMSGID);
        int componentNum = (int)meta.getLong(FN_COMPNUM);
        String classProp = meta.get(FN_CLASS, IcalXmlStrMap.CLASS_PUBLIC);
        boolean classPropSetByMe = meta.getBool(FN_CLASS_SETBYME, false);
        String status = meta.get(FN_STATUS, IcalXmlStrMap.STATUS_CONFIRMED);
        String freebusy = meta.get(FN_APPT_FREEBUSY, null);
        String transp = meta.get(FN_TRANSP, IcalXmlStrMap.TRANSP_OPAQUE);
        boolean sentByMe = meta.getBool(FN_SENTBYME);
        String fragment = meta.get(FN_FRAGMENT, "");

        boolean descInMeta = meta.getBool(FN_DESC_IN_META, false);  // default to false for backward compat
        String desc = descInMeta ? meta.get(FN_DESC, null) : null;
        String descHtml = descInMeta ? meta.get(FN_DESC_HTML, null) : null;
        String xDescHtml = descInMeta ? meta.get(FN_X_ZIMBRA_DESC_HTML, null) : null;

        boolean hasXMicrosoftHeader = false;
        for (Map.Entry<String, ?> entry : meta.asMap().entrySet()) {
            if (entry.getValue().toString().contains("X-MICROSOFT-SKYPETEAMSMEETINGURL")) {
                hasXMicrosoftHeader = true;
                break;
            }
        }

        // update HTML description if invite_ignore_x_alt_description is true
        // and contains X-MICROSOFT headers
        if (hasXMicrosoftHeader && !StringUtil.isNullOrEmpty(xDescHtml) && LC.invite_ignore_x_alt_description.booleanValue()) {
            descHtml = xDescHtml;
        }

        long completed = meta.getLong(FN_COMPLETED, 0);

        ParsedDateTime dtStart = null;
        ParsedDateTime dtEnd = null;
        ParsedDuration duration = null;

        RecurId recurrenceId = null;

        TimeZoneMap tzMap = Util.decodeFromMetadata(meta.getMap(FN_TZMAP), accountTZ);

        Metadata metaRecur = meta.getMap(FN_RECURRENCE, true);
        Recurrence.IRecurrence recurrence = null;
        if (metaRecur != null) {
            recurrence = Recurrence.decodeMetadata(metaRecur, tzMap);
        }

        String methodStr = meta.get(FN_METHOD, ICalTok.PUBLISH.toString());
        if (ICalTok.CANCEL.toString().equals(methodStr))
            status = IcalXmlStrMap.STATUS_CANCELLED;

        int flags = (int) meta.getLong(FN_APPT_FLAGS, 0);
        try {
            // DtStart
            dtStart = ParsedDateTime.parse(meta.get(FN_START, null), tzMap);
            // DtEnd
            dtEnd = ParsedDateTime.parse(meta.get(FN_END, null), tzMap);
            if ((flags & APPT_FLAG_ALLDAY)!=0) {
                // Fixup historic data with incorrect all-day start/end format.
                if (dtStart != null)
                    dtStart.forceDateOnly();
                if (dtEnd != null)
                    dtEnd.forceDateOnly();
            }
            // Duration
            duration = ParsedDuration.parse(meta.get(FN_DURATION, null));

            if (meta.containsKey(FN_RECUR_ID)) {
                Metadata rdata = meta.getMap(FN_RECUR_ID);

                recurrenceId = RecurId.decodeMetadata(rdata, tzMap);
            }

        } catch (ParseException e) {
            throw ServiceException.FAILURE(String.format("Error parsing metadata for invite %s-%s in calItem %s",
                    mailItemId, componentNum, (calItem != null) ? Integer.toString(calItem.getId()) : "(null)"), e);
        }

        String name = meta.get(FN_NAME, "");
        String loc = meta.get(FN_LOCATION, null);

        // For existing invites with no partstat, default to ACCEPTED status.
        String partStat = meta.get(FN_PARTSTAT, IcalXmlStrMap.PARTSTAT_ACCEPTED);
        // For existing invites with no RSVP, default to true.
        boolean rsvp = meta.getBool(FN_RSVP, true);
        long dtstamp = meta.getLong(FN_DTSTAMP, 0);
        long lastModified = meta.getLong(FN_LAST_MODIFIED, 0);
        int seqno = (int) meta.getLong(FN_SEQ_NO, 0);
        int lastFullSeqno = (int) meta.getLong(FN_LAST_FULL_SEQ_NO, seqno);

        ZOrganizer org = null;
        try {
            Metadata metaOrg = meta.getMap(FN_ORGANIZER, true);
            org = metaOrg != null ? new ZOrganizer(metaOrg) : null;
        } catch (ServiceException e) {
            sLog.warn("Problem decoding organizer for calItem %s invite %s-%s",
                    (calItem != null) ? Integer.toString(calItem.getId()) : "(null)", mailItemId, componentNum);
        }

        long numAts = meta.getLong(FN_NUM_ATTENDEES, 0);
        ArrayList<ZAttendee> attendees = new ArrayList<ZAttendee>((int) numAts);
        for (int i = 0; i < numAts; i++) {
            try {
                Metadata metaAttendee = meta.getMap(FN_ATTENDEE + i, true);
                if (metaAttendee != null)
                    attendees.add(new ZAttendee(metaAttendee));
            } catch (ServiceException e) {
                ZimbraLog.calendar.warn("Problem decoding attendee %s for calendar item %s invite %s-%s",
                        i, (calItem != null) ? Integer.toString(calItem.getId()) : "(null)", mailItemId, componentNum);
            }
        }

        boolean isOrganizer = false;
        if (meta.containsKey(FN_IS_ORGANIZER)) {
            isOrganizer = meta.getBool(FN_IS_ORGANIZER);
        } else {
            // backward compat for invites created before FN_IS_ORGANIZER was introduced
            if (org != null) {
                String orgAddr = org.getAddress();
                Account account = MailboxManager.getInstance().getMailboxById(mailboxId).getAccount();
                AccountAddressMatcher acctMatcher = new AccountAddressMatcher(account);
                isOrganizer = acctMatcher.matches(orgAddr);
            } else {
                // If there are other attendees, it's an Outlook POP/IMAP bug.  If not,
                // it's a properly formatted single-user event.  See isOrganizer()
                // method for more info.
                isOrganizer = numAts < 1;
            }
        }

        String priority = meta.get(FN_PRIORITY, null);
        String pctComplete = meta.get(FN_PCT_COMPLETE, null);

        List<String> comments = null;
        int numComm = (int) meta.getLong(FN_NUM_COMMENTS, 0);
        if (numComm > 0) {
            comments = new ArrayList<String>(numComm);
            for (int i = 0; i < numComm; i++) {
                String comm = meta.get(FN_COMMENT + i, null);
                if (comm != null)
                    comments.add(comm);
            }
        }

        List<String> contacts = null;
        int numContacts = (int) meta.getLong(FN_NUM_CONTACTS, 0);
        if (numContacts > 0) {
            contacts = new ArrayList<String>(numContacts);
            for (int i = 0; i < numContacts; i++) {
                String contact = meta.get(FN_CONTACT + i, null);
                if (contact != null)
                    contacts.add(contact);
            }
        }

        List<String> categories = null;
        int numCat = (int) meta.getLong(FN_NUM_CATEGORIES, 0);
        if (numCat > 0) {
            categories = new ArrayList<String>(numCat);
            for (int i = 0; i < numCat; i++) {
                String cat = meta.get(FN_CATEGORY + i, null);
                if (cat != null)
                    categories.add(cat);
            }
        }

        Geo geo = null;
        Metadata metaGeo = meta.getMap(FN_GEO, true);
        if (metaGeo != null)
            geo = Util.decodeGeoFromMetadata(metaGeo);

        String url = meta.get(FN_URL, null);

        Invite invite = new Invite(type, methodStr, tzMap, calItem, uid, status, priority, pctComplete, completed,
                freebusy, transp, classProp, dtStart, dtEnd, duration, recurrence, isOrganizer, org, attendees, name,
                loc, flags, partStat, rsvp, recurrenceId, dtstamp, lastModified,
                seqno, lastFullSeqno, mailboxId, mailItemId, componentNum, sentByMe,
                desc, descHtml, fragment, comments, categories, contacts, geo, url);
        invite.mDescInMeta = descInMeta;  // a little hacky, but necessary
        invite.setXZimbraDescriptionHtml(xDescHtml);
        invite.setClassPropSetByMe(classPropSetByMe);

        long numAlarms = meta.getLong(FN_NUM_ALARMS, 0);
        for (int i = 0; i < numAlarms; i++) {
            try {
                Metadata metaAlarm = meta.getMap(FN_ALARM + i, true);
                if (metaAlarm != null) {
                    Alarm alarm = Alarm.decodeMetadata(metaAlarm);
                    if (alarm != null)
                        invite.addAlarm(alarm);
                }
            } catch (ServiceException e) {
                ZimbraLog.calendar.warn("Problem decoding alarm %s for calendar item %s invite %s-%s",
                        i, (calItem != null) ? Integer.toString(calItem.getId()) : "(null)",
                        mailItemId, componentNum, e);
            }
        }

        List<ZProperty> xprops = Util.decodeXPropsFromMetadata(meta);
        if (xprops != null) {
            for (ZProperty xprop : xprops) {
                boolean isHtmlDesc = false;
                if (ICalTok.X_ALT_DESC.equals(xprop.getToken())) {
                    // Backward compat.  We used to save X-ALT-DESC property as an x-prop.  Now we use it
                    // for HTML description, when FMTTYPE=text/html.
                    ZParameter fmttype = xprop.getParameter(ICalTok.FMTTYPE);
                    if (fmttype != null && MimeConstants.CT_TEXT_HTML.equalsIgnoreCase(fmttype.getValue())) {
                        isHtmlDesc = true;
                        invite.mDescHtml = xprop.getValue();
                    }
                }
                if (!isHtmlDesc)
                    invite.addXProp(xprop);
            }
        }

        invite.setDontIndexMimeMessage(meta.getBool(FN_DONT_INDEX_MM, false));

        boolean localOnly = meta.getBool(FN_LOCAL_ONLY, false);
        invite.setLocalOnly(localOnly);

        invite.sanitize(false);
        return invite;
    }


    private String mDescription;
    private String mDescHtml;
    private String xZimbraDescHtml;
    private boolean mDescInMeta = true;  // assume description is in metadata unless someone sets a large value

    /**
     * An optimization for indexing, if this is set then we don't need to try to fetch
     * this Invite's MimeMessage in order to reindex it.
     *
     * This is TRUE if the Invite had no accompanying MimeMessage when it was stored,
     * e.g. if it came from a REST-imported ICS file.
     */
    private boolean mDontIndexMimeMessage = false;

    public synchronized void setDontIndexMimeMessage(boolean truthiness) { mDontIndexMimeMessage = truthiness; }
    public synchronized boolean getDontIndexMimeMessage() { return mDontIndexMimeMessage; }

    /**
     * Returns if this Invite object has DESCRIPTION that is stored in metadata.  If this method returns
     * false, it means getting the description requires the expensive parsing of the MIME part in
     * calendar item blob.
     * @return
     */
    public boolean descInMeta() { return mDescInMeta; }

    /**
     * Returns whether this Invite has a MIME part in calendar item blob.
     * @return
     */
    public boolean hasBlobPart() { return !descInMeta() || hasAttachment(); }

    /**
     * Returns the meeting notes.  Meeting notes is the text/plain part in an
     * invite.  It typically includes CUA-generated meeting summary as well as
     * text entered by the user.
     *
     * This method can be called for existing invites only.
     * Null is returned if this method is called on an incoming invite that
     * hasn't been committed to the backend yet.
     *
     * @return null if notes is not found
     * @throws ServiceException
     */
    public synchronized String getDescription() throws ServiceException {
        if (!mDescInMeta && mDescription == null)
            loadDescFromBlob();
        return mDescription;
    }

    /**
     * Returns the invite email text/html part.
     *
     * @return null if notes is not found
     * @throws ServiceException
     */
    public synchronized String getXZimbraDescriptionHtml() throws ServiceException {
        return xZimbraDescHtml;
    }

    public synchronized void setXZimbraDescriptionHtml(String html) throws ServiceException {
        if (html != null) {
            xZimbraDescHtml = html;
        }
    }

    public synchronized String getDescriptionHtml() throws ServiceException {
        if (!mDescInMeta && mDescHtml == null)
            loadDescFromBlob();
        return mDescHtml;
    }

    private synchronized void loadDescFromBlob() throws ServiceException {
        MimeMessage mmInv = mCalItem != null ? mCalItem.getSubpartMessage(mMailItemId) : null;
        if (mmInv != null) {
            mDescription = getDescription(mmInv, MimeConstants.CT_TEXT_PLAIN);
            mDescHtml = getDescription(mmInv, MimeConstants.CT_TEXT_HTML);
        }
    }

    public synchronized void setDescription(String desc, String html) {
        int maxInMeta = getMaxDescInMeta();
        boolean shortDesc = desc == null || desc.length() <= maxInMeta;
        boolean shortHtml = html == null || html.length() <= maxInMeta * 3;  // markups are bloated
        mDescInMeta = shortDesc && shortHtml;
        mDescription = desc;
        mDescHtml = html;
    }

    /**
     * Returns the meeting notes.  Meeting notes is the text/plain part in an
     * invite.  It typically includes CUA-generated meeting summary as well as
     * text entered by the user.
     *
     * @return null if notes is not found
     * @throws ServiceException
     */
    public static String getDescription(Part mmInv, String mimeType) throws ServiceException {
        if (mmInv == null) return null;
        try {
            // If top-level is text/calendar, parse the iCalendar object and return
            // the DESCRIPTION of the first VEVENT/VTODO encountered.
            String mmCtStr = mmInv.getContentType();
            if (mmCtStr != null) {
                ContentType mmCt = new ContentType(mmCtStr);
                if (mmCt.match(MimeConstants.CT_TEXT_CALENDAR)) {
                    boolean wantHtml = MimeConstants.CT_TEXT_HTML.equalsIgnoreCase(mimeType);
                    Object mmInvContent = mmInv.getContent();
                    InputStream is = null;
                    try {
                        String charset = MimeConstants.P_CHARSET_UTF8;
                        if (mmInvContent instanceof InputStream) {
                            charset = mmCt.getParameter(MimeConstants.P_CHARSET);
                            if (charset == null)
                                charset = MimeConstants.P_CHARSET_UTF8;
                            is = (InputStream) mmInvContent;
                        } else if (mmInvContent instanceof String) {
                            String str = (String) mmInvContent;
                            charset = MimeConstants.P_CHARSET_UTF8;
                            is = new ByteArrayInputStream(str.getBytes(charset));
                        }
                        if (is != null) {
                            ZVCalendar iCal = ZCalendarBuilder.build(is, charset);
                            for (Iterator<ZComponent> compIter = iCal.getComponentIterator(); compIter.hasNext(); ) {
                                ZComponent component = compIter.next();
                                ICalTok compTypeTok = component.getTok();
                                if (compTypeTok == ICalTok.VEVENT || compTypeTok == ICalTok.VTODO) {
                                    if (!wantHtml)
                                        return component.getPropVal(ICalTok.DESCRIPTION, null);
                                    else
                                        return component.getDescriptionHtml();
                                }
                            }
                        }
                    } finally {
                        ByteUtil.closeStream(is);
                    }
                }
            }

            Object mmInvContent = mmInv.getContent();
            if (!(mmInvContent instanceof MimeMultipart)) {
                if (mmInvContent instanceof InputStream) {
                    ByteUtil.closeStream((InputStream) mmInvContent);
                }
                return null;
            }
            MimeMultipart mm = (MimeMultipart) mmInvContent;

            // If top-level is multipart, get description from text/* part.
            int numParts = mm.getCount();
            String charset = null;
            for (int i  = 0; i < numParts; i++) {
                BodyPart part = mm.getBodyPart(i);
                String ctStr = part.getContentType();
                try {
                    ContentType ct = new ContentType(ctStr);
                    if (ct.match(mimeType)) {
                        charset = ct.getParameter(MimeConstants.P_CHARSET);
                        if (charset == null) charset = MimeConstants.P_CHARSET_DEFAULT;
                        byte[] descBytes = ByteUtil.getContent(part.getInputStream(), part.getSize());
                        return new String(descBytes, charset);
                    }
                    // If part is a multipart, recurse.
                    if (ct.getBaseType().matches(MimeConstants.CT_MULTIPART_WILD)) {
                        String str = getDescription(part, mimeType);
                        if (str != null) {
                            return str;
                        }
                    }
                } catch (javax.mail.internet.ParseException e) {
                    ZimbraLog.calendar.warn("Invalid Content-Type found: \"" + ctStr + "\"; skipping part", e);
                }
            }
        } catch (IOException e) {
            throw ServiceException.FAILURE("Unable to get calendar item notes MIME part", e);
        } catch (MessagingException e) {
            throw ServiceException.FAILURE("Unable to get calendar item notes MIME part", e);
        }
        return null;
    }

    /**
     * Returns the MimeMessage that corresponds to this invite.  This method
     * should be called only for invites that have been created on the server
     * already.
     * @return can return null
     */
    public MimeMessage getMimeMessage() throws ServiceException {
        return  getMimeMessage(!DebugConfig.disableMimeConvertersForCalendarBlobs);
    }

    public MimeMessage getMimeMessage(boolean runConverters) throws ServiceException {
        if (mCalItem == null || mMailItemId <= 0) return null;
        return mCalItem.getSubpartMessage(mMailItemId, runConverters);
    }

    public void setPartStat(String partStat) { mPartStat = partStat; }

    /**
     * The Invite datastructure caches "my" partstat so that I don't have to search through all
     * of the Attendee records every time I want to know my status....this function updates the
     * catched PartStat data when I receive a new Invite
     */
    public void updateMyPartStat(Account acct, String partStat)
    throws ServiceException {
        if (mIsOrganizer) {
            setPartStat(IcalXmlStrMap.PARTSTAT_ACCEPTED);
            setRsvp(false);
        } else {
            ZAttendee at = getMatchingAttendee(acct);
            if (at != null) {
                if (at.getRsvp() != null) {
                    setRsvp(at.getRsvp().booleanValue());
                }
                //
                // for BUG 4866 -- basically, if the incoming invite doesn't have a
                // PARTSTAT for us, assume it is "NEEDS-ACTION" iff this Invite supports
                // NEEDS_ACTION (ie is a Request or a Counter)
                //
                if (mMethod == ICalTok.REQUEST || mMethod == ICalTok.COUNTER) {
                    setPartStat(partStat);
                    at.setPartStat(partStat);
                }
            } else {
                // if this is the first time we're parsing this, and we can't find ourself on the
                // attendee list, then allow a reply...
                setRsvp(true);
            }
        }
    }

    /**
     * @return the CalendarItem object, or null if one could not be found
     */
    public CalendarItem getCalendarItem() {
        return mCalItem;
    }

    public void setCalendarItem(CalendarItem calItem) {
        mCalItem = calItem;
    }

    public void setIsAllDayEvent(boolean allDayEvent) {
        if (allDayEvent) {
            mFlags |= APPT_FLAG_ALLDAY;
        } else {
            mFlags &= ~APPT_FLAG_ALLDAY;
        }
    }
    public int getComponentNum() { return mComponentNum; }
    public void setComponentNum(int num) { mComponentNum = num; }
    public int getMailboxId() { return mMailboxId; }
    void setMailboxId(int id) { mMailboxId = id; }
    public int getMailItemId() { return mMailItemId; }
    public void setMailItemId(int id) { mMailItemId = id; }
    public int getFlags() { return mFlags; }
    public void setFlags(int flags) { mFlags = flags; }
    public String getPartStat() { return mPartStat; }
    public boolean hasRsvp() { return mRsvp != null; }
    public Boolean getRsvp() { return mRsvp; }
    public void setRsvp(Boolean rsvp) { mRsvp = rsvp; }
    public String getUid() { return mUid; };
    public void setUid(String uid) { mUid = uid; }
    public String getName() { return mName; };
    public void setName(String name) { mName = name; }
    public String getStatus() { return mStatus; }
    public void setStatus(String status) { mStatus = status; }
    public boolean hasFreeBusy() { return mFreeBusy != null; }
    public String getFreeBusy() { return mFreeBusy != null ? mFreeBusy : IcalXmlStrMap.FBTYPE_BUSY; }
    public void setFreeBusy(String fb) { mFreeBusy = fb; }
    public String getTransparency() { return mTransparency; }
    public boolean isTransparent() { return IcalXmlStrMap.TRANSP_TRANSPARENT.equals(mTransparency); }
    public void setTransparency(String transparency) { mTransparency = transparency; }
    public String getClassProp() { return mClass; }
    public void setClassProp(String classProp) { mClass = classProp; }
    public boolean classPropSetByMe() { return mClassSetByMe; }
    public void setClassPropSetByMe(boolean b) { mClassSetByMe = b; }
    public long getCompleted() { return mCompleted; }
    public void setCompleted(long completed) { mCompleted = completed; }
    public int getSeqNo() { return mSeqNo; }
    public int getLastFullSeqNo() { return mLastFullSeqNo; }
    public void setSeqNo(int seqNo) { mSeqNo = seqNo; }
    public void setLastFullSeqNo(int seqNo) { mLastFullSeqNo = seqNo; }
    public ParsedDateTime getStartTime() { return mStart; }
    public void setDtStart(ParsedDateTime dtStart) { mStart = dtStart; }
    public ParsedDateTime getEndTime() { return mEnd; }
    public void setDtEnd(ParsedDateTime dtend) { mEnd = dtend; }
    public ParsedDuration getDuration() { return mDuration; }
    public void setDuration(ParsedDuration dur) { mDuration = dur; }
    public String getPriority() { return mPriority; }
    public void setPriority(String prio) { mPriority = prio; }
    public String getPercentComplete() { return mPercentComplete; }
    public void setPercentComplete(String pct) { mPercentComplete = pct; }
    public List<String> getCategories() { return mCategories; }
    public void addCategory(String category) { mCategories.add(category); }
    public List<String> getContacts() { return mContacts; }
    public void addContact(String contact) { mContacts.add(contact); }
    public List<String> getComments() { return mComments; }
    public void addComment(String comment) { mComments.add(comment); }
    public Geo getGeo() { return mGeo; }
    public void setGeo(Geo geo) { mGeo = geo; }
    public String getUrl() { return mUrl; }
    public void setUrl(String url) { mUrl = url != null ? url : ""; }

    public long getDTStamp() { return mDTStamp; }
    public void setDtStamp(long stamp) {
        mDTStamp = stamp / 1000 * 1000;  // IMPORTANT: Remove millis resolution. (bug 20641)
        if (mLastModified == 0)
            setLastModified(mDTStamp);
    }
    public long getLastModified() { return mLastModified; }
    public void setLastModified(long lastModified) {
        mLastModified = lastModified / 1000 * 1000;  // drop millis resolution
    }

    public boolean isPublic() {
        return IcalXmlStrMap.CLASS_PUBLIC.equals(mClass);
    }

    public boolean isCancel() {
        return ICalTok.CANCEL.toString().equals(mMethod) ||
               IcalXmlStrMap.STATUS_CANCELLED.equals(mStatus);
    }

    public String getFreeBusyActual() {
        return partStatToFreeBusyActual(mPartStat);
    }

    /**
     * Returns actual free-busy status taking into account the free-busy
     * setting of the event, the user's participation status, and the
     * scheduling status of the event.
     *
     * The getFreeBusy() method simply returns the event's free-busy
     * setting.
     * @return
     */
    public String partStatToFreeBusyActual(String partStat) {
        String fb = getFreeBusy();

        // If event itself is FBTYPE_FREE, it doesn't matter whether
        // invite was accepted or declined.  It shows up as free time.
        if (IcalXmlStrMap.FBTYPE_FREE.equals(fb))
            return IcalXmlStrMap.FBTYPE_FREE;

        // If invite was accepted, use event's free-busy status.
        if (IcalXmlStrMap.PARTSTAT_ACCEPTED.equals(partStat))
            return fb;

        // If invite was received but user hasn't acted on it yet
        // (NEEDS_ACTION), or if the user tentatively accepted it,
        // or if the event was only tentatively scheduled rather
        // than confirmed, then he/she is tentatively busy regardless
        // of the free-busy status of the event.  (Unless event specified
        // FBTYPE_FREE, but that case was already taken care of above.
        if (IcalXmlStrMap.PARTSTAT_NEEDS_ACTION.equals(partStat) ||
                IcalXmlStrMap.PARTSTAT_TENTATIVE.equals(partStat) ||
                IcalXmlStrMap.STATUS_TENTATIVE.equals(mStatus))
            return IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE;

        // If invite was declined or delegated to someone else, or if
        // this is a cancelled event, the user is free.
        if (IcalXmlStrMap.PARTSTAT_DECLINED.equals(partStat) ||
                IcalXmlStrMap.PARTSTAT_DELEGATED.equals(partStat) ||
                IcalXmlStrMap.STATUS_CANCELLED.equals(mStatus))
            return IcalXmlStrMap.FBTYPE_FREE;

        return fb;
    }

    /**
     * Calculate the "Effective End" of this event: that is, the value of DtEnd if set,
     * or the value of DtStart+Duration if that is set.  If neither is set, add 1 day to DtStart
     * if all-day event.  If not all-day, return DtStart. (0 duration)
     *
     * @return
     */
    public ParsedDateTime getEffectiveEndTime() {
        if (mEnd != null)
            return mEnd;
        if (mStart == null)
            return null;
        ParsedDuration dur = mDuration;
        if (dur == null) {
            if (isTodo())
                return null;
            if (!mStart.hasTime())
                dur = ParsedDuration.ONE_DAY;
            else
                dur = ParsedDuration.ONE_SECOND;
        }
        return mStart.add(dur);
    }

    /**
     *
     * Try to calculate the effective "default duration" of this event..this is either the DURATION
     * that was specified, or it is the DtEnd-DtStart of the first instance if they exist -- or, if they
     * don't exist, then this isn't an Event per se and you don't need the answer anyway: so we'll return NULL
     *
     * @return
     */
    public ParsedDuration getEffectiveDuration() {
        if (mDuration != null)
            return mDuration;
        if (mStart == null)
            return null;
        if (mEnd != null)
            return mEnd.difference(mStart);
        // DTSTART is there, but neither DTEND nor DURATION is set.
        if (isTodo())
            return null;
        if (!mStart.hasTime())
            return ParsedDuration.ONE_DAY;
        else
            return ParsedDuration.ONE_SECOND;
    }

    public String getEffectivePartStat() throws ServiceException {
        if (mCalItem == null) return getPartStat();
        Instance inst = isRecurrence() ? null : Instance.fromInvite(mCalItem.getId(), this);
        return mCalItem.getEffectivePartStat(this, inst);
    }

    public String getLocation() { return mLocation; }
    public void setLocation(String location) { mLocation = location; }
    public boolean isAllDayEvent() { return ((mFlags & APPT_FLAG_ALLDAY)!=0); }
    public boolean hasOrganizer() { return mOrganizer != null; }
    public boolean hasOtherAttendees() {
        return ((mAttendees != null) && (mAttendees.size() > 0));
    }
    void setIsRecurrence(boolean isRecurrence) {
        if (isRecurrence) {
            mFlags |= APPT_FLAG_ISRECUR;
        } else {
            mFlags &= ~APPT_FLAG_ISRECUR;
        }
    }
    public boolean isRecurrence() { return ((mFlags & APPT_FLAG_ISRECUR)!=0); }

    public boolean hasAlarm() {
        return !mAlarms.isEmpty();
    }

    private List<Attach> attaches = null;
    /**
     * Warning! Not always populated!
     * @return
     */
    public List<Attach> getIcalendarAttaches() {
        return attaches != null ? attaches : new ArrayList<Attach>();
    }

    public void addIcalendarAttach(Attach attach) {
        if (attaches == null) {
            attaches = Lists.newArrayList();
            setHasAttachment(true);
        }
        attaches.add(attach);
    }

    public boolean hasAttachment() { return ((mFlags & APPT_FLAG_HAS_ATTACHMENT)!=0); }
    public void setHasAttachment(boolean hasAttachment) {
        if (hasAttachment) {
            mFlags |= APPT_FLAG_HAS_ATTACHMENT;
        } else {
            mFlags &= ~APPT_FLAG_HAS_ATTACHMENT;
        }
    }

    public boolean isDraft() { return ((mFlags & APPT_FLAG_DRAFT)!=0); }
    public void setDraft(boolean draft) {
        if (draft) {
            mFlags |= APPT_FLAG_DRAFT;
        } else {
            mFlags &= ~APPT_FLAG_DRAFT;
        }
    }

    public boolean isNeverSent() { return ((mFlags & APPT_FLAG_NEVER_SENT)!=0); }
    public void setNeverSent(boolean neverSent) {
        if (neverSent) {
            mFlags |= APPT_FLAG_NEVER_SENT;
        } else {
            mFlags &= ~APPT_FLAG_NEVER_SENT;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{ ");
        sb.append("mboxid: ").append(this.mMailboxId);
        sb.append(", mailitem: ").append(this.mMailItemId);
        sb.append(", compnum: ").append(this.mComponentNum);
        sb.append(", uid: ").append(this.mUid);
        sb.append(", status: ").append(getStatus());
        sb.append(", partStat: ").append(getPartStat());
        sb.append(", rsvp: ");
        if (hasRsvp()) {
            sb.append(getRsvp());
        } else {
            sb.append("(not specified)");
        }
        sb.append(", freeBusy: ").append(mFreeBusy);
        sb.append(", transp: ").append(getTransparency());
        sb.append(", class: ").append(getClassProp());
        sb.append(", classSetByMe: ").append(classPropSetByMe());
        sb.append(", sentByMe: ").append(sentByMe());
        sb.append(", start: ").append(this.mStart);
        sb.append(", end: ").append(this.mEnd);
        sb.append(", duration: ").append(this.mDuration);
        sb.append(", organizer: ");
        if (hasOrganizer())
            sb.append(getOrganizer().getAddress());
        else
            sb.append("(not specified)");
        sb.append(", name: ").append(this.mName);
        sb.append(", location: ").append(this.mLocation);
        sb.append(", allDay: ").append(isAllDayEvent());
        sb.append(", otherAts: ").append(hasOtherAttendees());
        sb.append(", hasAlarm: ").append(hasAlarm());
        sb.append(", isRecur: ").append(isRecurrence());
        sb.append(", recurId: ").append(getRecurId());
        sb.append(", DTStamp: ").append(mDTStamp);
        sb.append(", lastMod: ").append(mLastModified);
        sb.append(", mSeqNo ").append(mSeqNo);
        sb.append(", mLastFullSeqNo ").append(mLastFullSeqNo);
        if (isDraft())
            sb.append(", draft: ").append(true);
        if (isNeverSent())
            sb.append(", neverSent: ").append(true);

        for (Alarm alarm : mAlarms) {
            sb.append(", alarm: ").append(alarm.toString());
        }

        for (ZProperty xprop : mXProps) {
            sb.append(", ").append(xprop.toString());
        }

        sb.append("}");
        return sb.toString();
    }

    public static final int APPT_FLAG_TODO            = 0x01;
    public static final int APPT_FLAG_EVENT           = 0x02;
    public static final int APPT_FLAG_ALLDAY          = 0x04;

    // TIM: removed this, wasn't being reliably set, and isn't necessary
    //  [instead we just check for mAttendees.size()>0 ]
    //public static final int APPT_FLAG_OTHER_ATTENDEES = 0x08;

    public static final int APPT_FLAG_HASALARM        = 0x10;  // obsolete
    public static final int APPT_FLAG_ISRECUR         = 0x20;
    public static final int APPT_FLAG_NEEDS_REPLY     = 0x40;  // obsolete
    public static final int APPT_FLAG_HAS_ATTACHMENT  = 0x80;
    public static final int APPT_FLAG_DRAFT           = 0x100;
    public static final int APPT_FLAG_NEVER_SENT      = 0x200;  // true means attendees have never been notified

    protected CalendarItem mCalItem = null;

    // all of these are loaded from / stored in the meta
    protected String mUid;
    protected String mStatus = IcalXmlStrMap.STATUS_CONFIRMED;
    protected String mFreeBusy = null;
    protected String mTransparency = IcalXmlStrMap.TRANSP_OPAQUE;  // transparent or opaque
    protected String mClass = IcalXmlStrMap.CLASS_PUBLIC;  // public, private, confidential
    protected boolean mClassSetByMe;
    protected ParsedDateTime mStart = null;
    protected ParsedDateTime mEnd = null;
    protected ParsedDuration mDuration = null;
    protected long mCompleted = 0;  // COMPLETED DATE-TIME of VTODO

    protected String mName; /* name of the invite, aka "subject" */
    protected String mLocation;
    protected int mFlags = APPT_FLAG_EVENT;
    protected long mDTStamp = 0;
    protected long mLastModified = 0;
    protected int mSeqNo = 0;
    protected int mLastFullSeqNo = 0;

    // Participation status for this calendar user.  Values are the
    // 2-character strings in ICalXmlStrMap.sPartStatMap, not the longer
    // iCalendar PARTSTAT values.
    // For meeting organizer, this should always be "AC".  (accepted)
    protected String mPartStat = IcalXmlStrMap.PARTSTAT_NEEDS_ACTION;

    protected Boolean mRsvp = null;

    // not in metadata:
    protected int mMailboxId = 0;
    protected int mMailItemId = 0;
    protected int mComponentNum = 0;

    private List<ZAttendee> mAttendees = new ArrayList<ZAttendee>();
    private ZOrganizer mOrganizer;
    private boolean mIsOrganizer;

    // (bug 27645)
    // An exception is marked as local-only if it exists only in an attendee's appointment/task.
    // An exception that was created by the organizer and sent to attendees are not marked
    // local-only in the attendees' appointments.
    //
    // The difference between a local-only exception and one that isn't local-only is what happens
    // to it when the recurrence series is updated.  A local-only exception is effectively removed,
    // by replacing its content with the series content except the reminders.  Thus the exception
    // "snaps" back to the series.  If the exception was a cancellation, the canceled instance will
    // reappear.  In contrast, a non-local-only exception remains unmodified when the series is
    // updated.  The end result is consistent overall appointment state for organizer and attendees,
    // while preserving the exceptions published by the organizer and also preserving any local
    // reminders the attendees set for themselves.
    //
    // By default local-only is set to true because most call sites of Invite deal with local
    // changes within a mailbox.  It should be changed to false by calling setLocalOnly(false) where
    // appropriate, such as during delivery of invite email.
    //
    // For the organizer mailbox local-only is never true.  (See isLocalOnly method.)
    //
    // When appointment exported to ics format a local-only invite sets X-ZIMBRA-LOCAL-ONLY:TRUE
    // property.  During ics parse/import only VEVENTs/VTODOs with X-ZIMBRA-LOCAL-ONLY:TRUE are
    // initialized as local-only invite.  This is the opposite behavior from the default value of
    // mLocalOnly.  This is done for backward compatibility.
    //
    // Local-only flag is set in metadata as "lo" field.  Only local-only invites will write this flag.
    // During metadata decoding, missing "lo" field means not local-only.  Again this is the opposite
    // behavior of mLocalOnly default, and it's done this way for backward compatibility.
    //
    // An exception instance that was both modified by the attendee and organizer is not considered
    // local-only.  As soon as organizer publishes an exception that instance is forever non-local-only,
    // regardless of how many times the attendee makes local changes before or after the organizer's.
    private boolean mLocalOnly = true;

    private String mPriority;         // 0 .. 9
    private String mPercentComplete;  // 0 .. 100

    private List<String> mCategories = new ArrayList<String>();
    private List<String> mContacts = new ArrayList<String>();
    private List<String> mComments = new ArrayList<String>();
    private Geo mGeo;

    private String mUrl;

    // MailItem type of calendar item containing this invite
    private MailItem.Type type = MailItem.Type.APPOINTMENT;

    private ICalTok mMethod;

    private final List<Alarm> mAlarms = new ArrayList<Alarm>();

    private final List<ZProperty> mXProps = new ArrayList<ZProperty>();

    public Invite(String method, TimeZoneMap tzMap, boolean isOrganizer) {
        setItemType(MailItem.Type.APPOINTMENT);
        mMethod = lookupMethod(method);
        if (ICalTok.CANCEL.equals(mMethod))
            mStatus = IcalXmlStrMap.STATUS_CANCELLED;
        mTzMap = tzMap;
        mIsOrganizer = isOrganizer;
        mFragment = "";
    }

    public Invite(MailItem.Type type, String method, TimeZoneMap tzMap, boolean isOrganizer) {
        setItemType(type);
        mMethod = lookupMethod(method);
        if (ICalTok.CANCEL.equals(mMethod)) {
            mStatus = IcalXmlStrMap.STATUS_CANCELLED;
        }
        mTzMap = tzMap;
        mIsOrganizer = isOrganizer;
        mFragment = "";
    }


    public String getMethod() { return mMethod.toString(); }
    public void setMethod(String methodStr) { mMethod = lookupMethod(methodStr); }

    public boolean sentByMe() { return mSentByMe; }
    public void setSentByMe(boolean sentByMe) { mSentByMe = sentByMe; }

    /**
     * @param acct
     * @return TRUE if this account is the "organizer" of the Event
     * @throws ServiceException
     */
    private boolean thisAcctIsOrganizer(Account acct) throws ServiceException {
        if (hasOrganizer()) {
            AccountAddressMatcher acctMatcher = new AccountAddressMatcher(acct);
            String addr = getOrganizer().getAddress();
            boolean isOrg = acctMatcher.matches(addr);
            if (!isOrg && acct != null) {
                // bug 41638: Let's also check if address matches zimbraPrefFromAddress.
                String prefFromAddr = acct.getPrefFromAddress();
                if (prefFromAddr != null && prefFromAddr.equalsIgnoreCase(addr))
                    isOrg = true;
            }
            return isOrg;
        } else {
            // If there are other attendees, it's an Outlook POP/IMAP bug.  If not,
            // it's a properly formatted single-user event.  See isOrganizer()
            // method for more info.
            return !hasOtherAttendees();
        }
    }

    /**
     * Find the (first) Attendee in our list that matches the passed-in account.  If multiple attendees
     * match this account (because an account can have multiple addresses), the address that matches
     * the given identity (persona) id is returned.  Account's default identity is used if identityId
     * is null or invalid.
     *
     * @param acct
     * @param identityId
     * @return The first matching attendee
     * @throws ServiceException
     */
    public ZAttendee getMatchingAttendee(Account acct, String identityId) throws ServiceException {
        Identity identity;
        if (identityId != null) {
            identity = acct.getIdentityById(identityId);
            if (identity == null) {
                ZimbraLog.calendar.warn("No such identity " + identityId + " for account " + acct.getName());
                identity = acct.getDefaultIdentity();
            }
        } else {
            identity = acct.getDefaultIdentity();
        }

        String identityEmail = identity.getAttr(Provisioning.A_zimbraPrefFromAddress);
        ZAttendee acctMatch = null;
        List<ZAttendee> attendees = getAttendees();
        AccountAddressMatcher acctMatcher = new AccountAddressMatcher(acct);
        for (ZAttendee at : attendees) {
            String thisAtEmail = at.getAddress();
            // Does this attendee match our identity?
            if (identityEmail != null && identityEmail.equalsIgnoreCase(thisAtEmail))
                return at;
            if (acctMatch == null && acctMatcher.matches(thisAtEmail)) {
                acctMatch = at;
                // If we didn't have identity email for some reason, we have our best match.
                if (identityEmail == null)
                    return at;
            }
        }
        return acctMatch;
    }

    /**
     * Find the (first) Attendee in our list that matches the passed-in account
     *
     * @param acct
     * @return The first matching attendee
     * @throws ServiceException
     */
    public ZAttendee getMatchingAttendee(Account acct) throws ServiceException {
        return getMatchingAttendee(acct, null);
    }

    /**
     * Find the (first) Attendee in our list that matches the passed-in name
     *
     * @param acct
     * @return The first matching attendee
     * @throws ServiceException
     */
    public ZAttendee getMatchingAttendee(String atName) throws ServiceException {
        // Find my ATTENDEE record in the Invite, it must be in our response
        List<ZAttendee> attendees = getAttendees();
        for (ZAttendee at : attendees) {
            String thisAtEmail = at.getAddress();
            if (thisAtEmail != null && thisAtEmail.equalsIgnoreCase(atName)) {
                return at;
            }
        }
        return null;
    }

    public ZAttendee getMatchingAttendee(ZAttendee matchAttendee) throws ServiceException {
        // Look up internal account for the attendee.  For internal users we want to match
        // on all email addresses of the account.
        AccountAddressMatcher acctMatcher = null;
        String matchAddress = matchAttendee.getAddress();
        if (matchAddress != null) {
            Account matchAcct = Provisioning.getInstance().get(AccountBy.name, matchAddress);
            if (matchAcct != null) {
                acctMatcher = new AccountAddressMatcher(matchAcct);
            }
        }
        for (ZAttendee at : getAttendees()) {
            if (matchAttendee.addressesMatch(at) || (acctMatcher != null && acctMatcher.matches(at.getAddress()))) {
                return at;
            }
        }
        return null;
    }

    /**
     * Updates the ATTENDEE entries in this invite which match entries in the other one -- presumably
     * because the attendee has sent us a reply to change his status.  The Caller is responsible
     * for ensuring the changed MetaData is written through to SQL and sending a notification of
     * MailItem change.
     *
     * @param reply
     * @return
     * @throws ServiceException
     */
    public boolean updateMatchingAttendeesFromReply(Invite reply) throws ServiceException {
        // Find my ATTENDEE record in the Invite, it must be in our response
        List<ZAttendee> attendees = getAttendees();

        ArrayList<ZAttendee> toAdd = new ArrayList<ZAttendee>();

        boolean modified = false;

        for (ZAttendee replyAt : reply.getAttendees()) {  // should almost always have only one element
            ZAttendee at = getMatchingAttendee(replyAt);
            if (at != null) {
                // BUG:4911  When an invitee responds they include an ATTENDEE record, but
                // it doesn't have to have all fields.  In particular, we don't want to let them
                // change their ROLE...
                //     if (replyAt.hasRole() && !replyAt.getRole().equals(at.getRole())) {
                //         at.setRole(replyAt.getRole());
                //         modified = true;
                //     }
                // bug 21848: Similar to above comment on bug 4911, we don't want the reply to
                // update the RSVP.  It seems most CUAs will send ATTENDEE record without setting
                // RSVP.  Because RSVP=FALSE by default, transferring the RSVP value to the invite
                // would end up inadvertently clearing the original RSVP value.
                //     if (replyAt.hasRsvp() && !replyAt.getRsvp().equals(at.getRsvp())) {
                //         at.setRsvp(replyAt.getRsvp());
                //         modified = true;
                //     }

                if (replyAt.hasPartStat() && !replyAt.getPartStat().equals(at.getPartStat())) {
                    at.setPartStat(replyAt.getPartStat());
                    modified = true;
                }
                continue;
            }
            toAdd.add(replyAt);
        }

        if (toAdd.size() > 0) {
            for (ZAttendee add : toAdd) {
                modified = true;
                attendees.add(add);
            }
        }
        return modified;
    }

    public List<ZAttendee> getAttendees() {
        return mAttendees;
    }

    public void clearAttendees() {
        mAttendees.clear();
    }

    public void addAttendee(ZAttendee at) {
        mAttendees.add(at);
    }

    public void setOrganizer(ZOrganizer org) {
        mOrganizer = org;
    }


    public ZOrganizer getOrganizer() {
        // Be careful!  Don't assume this is non-null.
        return mOrganizer;
    }

    /**
     * Returns Account object for the invite's organizer.
     * @return null if organizer info is missing or organizer is not an
     *         internal user
     * @throws ServiceException
     */
    public Account getOrganizerAccount() throws ServiceException {
        Account account = null;
        if (mIsOrganizer && mCalItem != null)
            return mCalItem.getAccount();
        if (hasOrganizer()) {
            String address = getOrganizer().getAddress();
            if (address != null) {
                try {
                    account = Provisioning.getInstance().get(AccountBy.name, address);
                } catch (ServiceException e) {
                    if (ServiceException.INVALID_REQUEST.equals(e.getCode()))
                        ZimbraLog.calendar.warn("Ignoring invalid organizer address: " + address);
                    else
                        throw e;
                }
            }
        } else if (mCalItem != null) {
            account = mCalItem.getAccount();
        }
        return account;
    }

    /**
     * Returns whether the invite was created for the organizer account.
     * @return
     */
    public boolean isOrganizer() {
        return mIsOrganizer;
    }
    public void setIsOrganizer(Account acct) throws ServiceException {
        mIsOrganizer = thisAcctIsOrganizer(acct);
    }
    public void setIsOrganizer(boolean isOrganizer) {
        mIsOrganizer = isOrganizer;
    }

    public boolean isLocalOnly() {
        // Local-only is never true for the organizer user.
        return mLocalOnly && !mIsOrganizer;
    }

    public void setLocalOnly(boolean localOnly) {
        mLocalOnly = localOnly;
    }

    public boolean isEvent() {
        return type == MailItem.Type.APPOINTMENT;
    }

    public boolean isTodo() {
        return type == MailItem.Type.TASK;
    }

    public MailItem.Type getItemType() {
        return type;
    }

    public void setItemType(MailItem.Type type) {
        this.type = type;
        // If mStatus is set to default appointment status but we have a task
        // invite, change to default task status.
        if (type == MailItem.Type.TASK && IcalXmlStrMap.STATUS_CONFIRMED.equals(mStatus)) {
            mStatus = IcalXmlStrMap.STATUS_NEEDS_ACTION;
        }
    }

    private final TimeZoneMap mTzMap;

    public TimeZoneMap getTimeZoneMap() { return mTzMap; }

    public ZVCalendar newToICalendar(boolean includePrivateData) throws ServiceException {
        return newToICalendar(OUTLOOK_COMPAT_ALLDAY, includePrivateData);
    }

    public ZVCalendar newToICalendar(boolean useOutlookCompatMode, boolean includePrivateData)
    throws ServiceException {
        ZVCalendar vcal = new ZVCalendar();
        vcal.addVersionAndProdId();

        vcal.addProperty(new ZProperty(ICalTok.METHOD, mMethod.toString()));

        // timezones
        if (!isAllDayEvent() || useOutlookCompatMode) {  // Don't write any VTIMEZONE for all-day appointments.
            for (Iterator<ICalTimeZone> iter = mTzMap.tzIterator(); iter.hasNext();) {
                ICalTimeZone cur = iter.next();
                vcal.addComponent(cur.newToVTimeZone());
            }
        }

        vcal.addComponent(newToVComponent(useOutlookCompatMode, includePrivateData));
        return vcal;
    }

    public static interface InviteVisitor {
        public void visit(Invite inv) throws ServiceException;
    }

    public static List<Invite> createFromCalendar(Account account, String fragment, ZVCalendar cal, boolean sentByMe)
    throws ServiceException {
        return createFromCalendar(account, fragment, cal, sentByMe, null, 0);
    }

    public static List<Invite> createFromCalendar(
            Account account, String fragment, ZVCalendar cal, boolean sentByMe, Mailbox mbx, int mailItemId)
    throws ServiceException {
        List<Invite> list = new ArrayList<Invite>();
        createFromCalendar(list, account, fragment, cal, sentByMe, mbx, mailItemId, false, null);
        return list;
    }

    public static List<Invite> createFromCalendar(
            Account account, String fragment, List<ZVCalendar> cals, boolean sentByMe)
    throws ServiceException {
        return createFromCalendar(account, fragment, cals, sentByMe, false, null);
    }

    public static void createFromCalendar(
            Account account, String fragment,
            String method, TimeZoneMap tzmap, Iterator<ZComponent> compIter,
            boolean sentByMe, boolean continueOnError, InviteVisitor visitor)
    throws ServiceException {
        createFromCalendar(null, account, fragment, method, tzmap, compIter, sentByMe, null, 0,
                           continueOnError, visitor);
    }

    public static List<Invite> createFromCalendar(
            Account account, String fragment, List<ZVCalendar> cals, boolean sentByMe,
            boolean continueOnError, InviteVisitor visitor)
    throws ServiceException {
        List<Invite> list = new ArrayList<Invite>();
        for (ZVCalendar cal : cals) {
            createFromCalendar(list, account, fragment, cal, sentByMe, (Mailbox)null, 0,
                               continueOnError, visitor);
        }
        return list;
    }

    public static void adjustTZIDnames(List<ZComponent> components, Map<String,String> tzidRenames) {
        if (tzidRenames.isEmpty() || (null == components) || components.isEmpty()) {
            return;
        }
        for (ZComponent comp : components) {
            if (!ICalTok.VTIMEZONE.equals(comp.getTok())) {
                for (Entry<String, String> entry: tzidRenames.entrySet()) {
                    for (ZProperty prop : comp.getProperties()) {
                        ZParameter param = prop.getParameter(ICalTok.TZID);
                        if (null != param) {
                            if (entry.getKey().equals(param.getValue())) {
                                param.setValue(entry.getValue());
                            }
                        }
                    }
                }
                adjustTZIDnames(ImmutableList.copyOf(comp.getComponents()), tzidRenames);
            }
        }
    }

    private static void createFromCalendar(
            List<Invite> toAdd, Account account, String fragment, ZVCalendar cal, boolean sentByMe,
            Mailbox mbx, int mailItemId,
            boolean continueOnError, InviteVisitor visitor)
    throws ServiceException {
        String method = cal.getPropVal(ICalTok.METHOD, ICalTok.PUBLISH.toString());

        // process the TIMEZONE's first: everything depends on them being there...
        TimeZoneMap tzmap = new TimeZoneMap(Util.getAccountTimeZone(account));
        Map<String,String> tzidRenames = Maps.newHashMap();
        List<ZComponent> components = Lists.newArrayList(cal.getComponentIterator());
        for (ZComponent comp : components) {
            if (ICalTok.VTIMEZONE.equals(comp.getTok())) {
                ICalTimeZone tz = ICalTimeZone.fromVTimeZone(comp, false /* skipLookup */,
                                ICalTimeZone.TZID_NAME_ASSIGNMENT_BEHAVIOR.KEEP_IF_DOESNT_CLASH);
                tzmap.add(tz);
                String origTZID = comp.getPropVal(ICalTok.TZID, null);
                if ((null != origTZID) && (origTZID != tz.getID())) {
                    tzidRenames.put(origTZID, tz.getID());
                }
            }
        }
        adjustTZIDnames(components, tzidRenames);
        createFromCalendar(toAdd, account, fragment, method, tzmap, cal.getComponentIterator(),
                           sentByMe, mbx, mailItemId, continueOnError, visitor);
    }

    private static void createFromCalendar(List<Invite> toAdd, Account account, String fragment, String method,
            TimeZoneMap tzmap, Iterator<ZComponent> compIter, boolean sentByMe, Mailbox mbx, int mailItemId,
            boolean continueOnError, InviteVisitor visitor) throws ServiceException {
        int compNum = 0;
        while (compIter.hasNext()) {
            ZComponent comp = compIter.next();
            Invite newInv = null;
            try {
                MailItem.Type type;
                ICalTok compTypeTok = comp.getTok();
                if (compTypeTok == null) continue;
                if (ICalTok.VTODO.equals(compTypeTok)) {
                    type = MailItem.Type.TASK;
                } else {
                    type = MailItem.Type.APPOINTMENT;
                }
                switch (compTypeTok) {
                case VEVENT:
                case VTODO:
                    boolean isEvent = ICalTok.VEVENT.equals(compTypeTok);
                    boolean isTodo = ICalTok.VTODO.equals(compTypeTok);
                    try {
                        newInv = new Invite(type, method, tzmap, false);
                        newInv.setLocalOnly(false);  // set to true later if X-ZIMBRA-LOCAL-ONLY is present
                        if (toAdd != null)
                            toAdd.add(newInv);

                        List<Object> addRecurs = new ArrayList<Object>();
                        List<Object> subRecurs = new ArrayList<Object>();

                        newInv.setComponentNum(compNum);
                        if (mbx != null)
                            newInv.setMailboxId(mbx.getId());
                        newInv.setMailItemId(mailItemId);
                        newInv.setSentByMe(sentByMe);
                        compNum++;

                        List<ZComponent> subcomponents = Lists.newArrayList(comp.getComponentIterator());
                        for (ZComponent subcomp : subcomponents) {
                            ICalTok subCompTypeTok = subcomp.getTok();
                            switch (subCompTypeTok) {
                            case VALARM:
                                Alarm alarm = Alarm.parse(subcomp);
                                if (alarm != null)
                                    newInv.addAlarm(alarm);
                                break;
                            default:
                                // ignore all other sub components
                            }
                        }
                        boolean isTodoCompleted = false;
                        boolean sawIntendedFreeBusy = false;
                        List<ZProperty> properties = Lists.newArrayList(comp.getPropertyIterator());
                        for (ZProperty prop : properties) {
                            String propVal = prop.getValue();
                            ICalTok propToken = prop.getToken();
                            if (propToken == null) {
                                // Skip properties with missing value.  There may be parameters specified, but
                                // it's still wrong to send a property without value.  They can only lead to
                                // parse errors later, so ignore them.
                                if (propVal == null || propVal.length() < 1)
                                    continue;
                                String name = prop.getName();
                                if (name.startsWith("X-") || name.startsWith("x-"))
                                    newInv.addXProp(prop);
                            } else if (propToken.equals(ICalTok.CATEGORIES)) {
                                List<String> categories = prop.getValueList();
                                if (categories != null && !categories.isEmpty()) {
                                    for (String cat : categories) {
                                        newInv.addCategory(cat);
                                    }
                                }
                            } else {
                                // Skip properties with missing value.  There may be parameters specified, but
                                // it's still wrong to send a property without value.  They can only lead to
                                // parse errors later, so ignore them.
                                if (propVal == null || propVal.length() < 1)
                                    continue;
                                switch (propToken) {
                                case ORGANIZER:
                                    newInv.setOrganizer(new ZOrganizer(prop));
                                    break;
                                case ATTENDEE:
                                    newInv.addAttendee(new ZAttendee(prop));
                                    break;
                                case DTSTAMP:
                                    ParsedDateTime dtstamp = ParsedDateTime.parse(prop, tzmap);
                                    newInv.setDtStamp(dtstamp.getUtcTime());
                                    break;
                                case LAST_MODIFIED:
                                    ParsedDateTime lastModified = ParsedDateTime.parse(prop, tzmap);
                                    newInv.setLastModified(lastModified.getUtcTime());
                                    break;
                                case RECURRENCE_ID:
                                    ParsedDateTime rid = ParsedDateTime.parse(prop, tzmap);
                                    if (DebugConfig.enableThisAndFuture) {
                                        newInv.setRecurId(new RecurId(rid, prop.paramVal(ICalTok.RANGE, null)));
                                    } else {
                                        newInv.setRecurId(new RecurId(rid, RecurId.RANGE_NONE));
                                    }
                                    break;
                                case SEQUENCE:
                                    newInv.setSeqNo(prop.getIntValue());
                                    break;
                                case DTSTART:
                                    ParsedDateTime dtstart = ParsedDateTime.parse(prop, tzmap);
                                    newInv.setDtStart(dtstart);
                                    if (!dtstart.hasTime())
                                        newInv.setIsAllDayEvent(true);
                                    break;
                                case DTEND:
                                    if (isEvent) {
                                        ParsedDateTime dtend = ParsedDateTime.parse(prop, tzmap);
                                        newInv.setDtEnd(dtend);
                                        if (!dtend.hasTime())
                                            newInv.setIsAllDayEvent(true);
                                    }
                                    break;
                                case DUE:
                                    if (isTodo) {
                                        ParsedDateTime due = ParsedDateTime.parse(prop, tzmap);
                                        // DUE is for VTODO what DTEND is for VEVENT.
                                        newInv.setDtEnd(due);
                                        if (!due.hasTime())
                                            newInv.setIsAllDayEvent(true);
                                    }
                                    break;
                                case DURATION:
                                    ParsedDuration dur = ParsedDuration.parse(propVal);
                                    newInv.setDuration(dur);
                                    break;
                                case LOCATION:
                                    newInv.setLocation(propVal);
                                    break;
                                case SUMMARY:
                                    String summary = propVal;
                                    if (summary != null) {
                                        // Make sure SUMMARY is a single line.
                                        summary = summary.replaceAll("[\\\r\\\n]+", " ");
                                    }
                                    prop.setValue(summary);
                                    newInv.setName(summary);
                                    break;
                                case DESCRIPTION:
                                    newInv.setDescription(propVal, newInv.mDescHtml);
                                    newInv.setFragment(Fragment.getFragment(propVal, true));
                                    break;
                                case X_ALT_DESC:
                                    ZParameter fmttype = prop.getParameter(ICalTok.FMTTYPE);
                                    if (fmttype != null && MimeConstants.CT_TEXT_HTML.equalsIgnoreCase(fmttype.getValue())) {
                                        String html = propVal;
                                        newInv.setDescription(newInv.mDescription, html);
                                    } else {
                                        // Unknown format.  Just add as an x-prop.
                                        newInv.addXProp(prop);
                                    }
                                    break;
                                case COMMENT:
                                    newInv.addComment(propVal);
                                    break;
                                case UID:
                                    newInv.setUid(propVal);
                                    break;
                                case RRULE:
                                    ZRecur recur = new ZRecur(propVal, tzmap);
                                    addRecurs.add(recur);
                                    newInv.setIsRecurrence(true);
                                    break;
                                case RDATE:
                                    if (DebugConfig.enableRdate) {
                                        RdateExdate rdate = RdateExdate.parse(prop, tzmap);
                                        addRecurs.add(rdate);
                                        newInv.setIsRecurrence(true);
                                    }
                                    break;
                                case EXRULE:
                                    ZRecur exrecur = new ZRecur(propVal, tzmap);
                                    subRecurs.add(exrecur);
                                    newInv.setIsRecurrence(true);
                                    break;
                                case EXDATE:
                                    RdateExdate exdate = RdateExdate.parse(prop, tzmap);
                                    subRecurs.add(exdate);
                                    newInv.setIsRecurrence(true);
                                    break;
                                case STATUS:
                                    String status = IcalXmlStrMap.sStatusMap.toXml(propVal);
                                    if (status != null) {
                                        if (IcalXmlStrMap.STATUS_IN_PROCESS.equals(status)) {
                                            String zstatus = prop.getParameterVal(ICalTok.X_ZIMBRA_STATUS, null);
                                            if (ICalTok.X_ZIMBRA_STATUS_WAITING.toString().equals(zstatus) ||
                                                ICalTok.X_ZIMBRA_STATUS_DEFERRED.toString().equals(zstatus)) {
                                                newInv.setStatus(IcalXmlStrMap.sStatusMap.toXml(zstatus));
                                            } else {
                                                newInv.setStatus(status);
                                            }
                                        } else {
                                            newInv.setStatus(status);
                                            if (isTodo && IcalXmlStrMap.STATUS_COMPLETED.equals(status))
                                                isTodoCompleted = true;
                                        }
                                    }
                                    break;
                                case TRANSP:
                                    // TRANSP is examined only when intended F/B is not supplied.
                                    if (isEvent && !sawIntendedFreeBusy) {
                                        String transp = IcalXmlStrMap.sTranspMap.toXml(propVal);
                                        if (transp != null) {
                                            newInv.setTransparency(transp);
                                            // If transparent, set intended f/b to free.
                                            // If opaque, don't set intended f/b because there are multiple possibilities.
                                            if (newInv.isTransparent())
                                                newInv.setFreeBusy(IcalXmlStrMap.FBTYPE_FREE);
                                        }
                                    }
                                    break;
                                case CLASS:
                                    String classProp = IcalXmlStrMap.sClassMap.toXml(propVal);
                                    if (classProp != null)
                                        newInv.setClassProp(classProp);
                                    break;
                                case X_MICROSOFT_CDO_ALLDAYEVENT:
                                    if (isEvent) {
                                        if (prop.getBoolValue())
                                            newInv.setIsAllDayEvent(true);
                                    }
                                    break;
                                case X_MICROSOFT_CDO_INTENDEDSTATUS:
                                    sawIntendedFreeBusy = true;
                                    if (isEvent) {
                                        String fb = IcalXmlStrMap.sOutlookFreeBusyMap.toXml(propVal);
                                        if (fb != null) {
                                            newInv.setFreeBusy(fb);
                                            // Intended F/B takes precedence over TRANSP.
                                            if (IcalXmlStrMap.FBTYPE_FREE.equals(fb))
                                                newInv.setTransparency(IcalXmlStrMap.TRANSP_TRANSPARENT);
                                            else
                                                newInv.setTransparency(IcalXmlStrMap.TRANSP_OPAQUE);
                                        }
                                    }
                                    break;
                                case PRIORITY:
                                    String prio = propVal;
                                    if (prio != null)
                                        newInv.setPriority(prio);
                                    break;
                                case PERCENT_COMPLETE:
                                    if (isTodo) {
                                        String pctComplete = propVal;
                                        if (pctComplete != null) {
                                            newInv.setPercentComplete(pctComplete);
                                            if (prop.getIntValue() == 100)
                                                isTodoCompleted = true;
                                        }
                                    }
                                    break;
                                case COMPLETED:
                                    if (isTodo) {
                                        ParsedDateTime completed = ParsedDateTime.parseUtcOnly(propVal);
                                        newInv.setCompleted(completed.getUtcTime());
                                        isTodoCompleted = true;
                                    }
                                    break;
                                case CONTACT:
                                    newInv.addContact(propVal);
                                    break;
                                case GEO:
                                    Geo geo = Geo.parse(prop);
                                    newInv.setGeo(geo);
                                    break;
                                case URL:
                                    newInv.setUrl(propVal);
                                    break;
                                case X_ZIMBRA_LOCAL_ONLY:
                                    if (prop.getBoolValue())
                                        newInv.setLocalOnly(true);
                                    break;
                                case X_ZIMBRA_DISCARD_EXCEPTIONS:
                                    newInv.addXProp(prop);
                                    break;
                                case X_ZIMBRA_CHANGES:
                                    newInv.addXProp(prop);
                                    break;
                                case ATTACH:
                                    Attach attach = Attach.parse(prop);
                                    if (attach.getBinaryB64Data() != null) {
                                        newInv.addIcalendarAttach(attach);
                                    }
                                    break;
                                }
                            }
                        }

                        if (isTodoCompleted) {
                            // set the status to Completed.
                            newInv.setStatus(IcalXmlStrMap.STATUS_COMPLETED);
                            // set percent-complete to 100
                            newInv.setPercentComplete(Integer.toString(100));
                            if (newInv.getCompleted() == 0) // set COMPLETED property to now if not already set.
                                newInv.setCompleted(System.currentTimeMillis());
                        }

                        newInv.setIsOrganizer(account);

                        newInv.validateDuration();

                        ParsedDuration duration = newInv.getDuration();
                        boolean durationCalculated = false;
                        if (duration == null) {
                            ParsedDateTime end = newInv.getEndTime();
                            if (end != null && newInv.getStartTime() != null) {
                                duration = end.difference(newInv.getStartTime());
                                durationCalculated = true;
                            }
                        }

                        if (!addRecurs.isEmpty() || !subRecurs.isEmpty()) {
                            // We have a recurrence.  Make sure DTSTART is not null.
                            ParsedDateTime st = newInv.getStartTime();
                            if (st == null) {
                                ParsedDateTime et = newInv.getEndTime();
                                if (et != null) {
                                    if (et.hasTime())
                                        st = et.add(ParsedDuration.NEGATIVE_ONE_SECOND);
                                    else
                                        st = et.add(ParsedDuration.NEGATIVE_ONE_DAY);
                                    newInv.setDtStart(st);
                                } else {
                                    // Both DTSTART and DTEND are unspecified.  Recurrence makes no sense!
                                    throw ServiceException.INVALID_REQUEST("recurrence used without DTSTART", null);
                                }
                            }
                            if (durationCalculated && newInv.getItemType() == MailItem.Type.TASK) {
                                if (newInv.getStartTime() != null && !newInv.getStartTime().hasTime()) {
                                    duration = ParsedDuration.ONE_DAY;
                                } else {
                                    duration = ParsedDuration.ONE_SECOND;
                                }
                            }
                        }

                        InviteInfo inviteInfo = new InviteInfo(newInv);
                        List<IRecurrence> addRules = new ArrayList<IRecurrence>();
                        if (addRecurs.size() > 0) {
                            for (Iterator<Object> iter = addRecurs.iterator(); iter.hasNext();) {
                                Object next = iter.next();
                                if (next instanceof ZRecur) {
                                    ZRecur cur = (ZRecur) next;
                                    addRules.add(new Recurrence.SimpleRepeatingRule(newInv.getStartTime(), duration, cur, inviteInfo));
                                } else if (next instanceof RdateExdate) {
                                    RdateExdate rdate = (RdateExdate) next;
                                    addRules.add(new Recurrence.SingleDates(rdate, duration, inviteInfo));
                                }
                            }
                        }
                        List<IRecurrence> subRules = new ArrayList<IRecurrence>();
                        if (subRecurs.size() > 0) {
                            for (Iterator<Object> iter = subRecurs.iterator(); iter.hasNext();) {
                                Object next = iter.next();
                                if (next instanceof ZRecur) {
                                    ZRecur cur = (ZRecur) iter.next();
                                    subRules.add(new Recurrence.SimpleRepeatingRule(newInv.getStartTime(), duration, cur, inviteInfo));
                                } else if (next instanceof RdateExdate) {
                                    RdateExdate exdate = (RdateExdate) next;
                                    subRules.add(new Recurrence.SingleDates(exdate, duration, inviteInfo));
                                }
                            }
                        }

                        if (newInv.hasRecurId()) {
                            if (addRules.size() > 0) {
                                newInv.setRecurrence(new Recurrence.ExceptionRule(newInv.getRecurId(),
                                        newInv.getStartTime(), duration, new InviteInfo(newInv), addRules, subRules));
                            }
                        } else {
                            if (addRules.size() > 0) { // since exclusions can't affect DtStart, just ignore them if there are no add rules
                                newInv.setRecurrence(new Recurrence.RecurrenceRule(newInv.getStartTime(), duration, new InviteInfo(newInv), addRules, subRules));
                            }
                        }

                        String location = newInv.getLocation();
                        if (location == null)
                            newInv.setLocation("");

                        // Process callback.
                        if (visitor != null)
                            visitor.visit(newInv);
                    } catch (ParseException e) {
                        throw ServiceException.PARSE_ERROR(
                            "Unable to parse iCalendar data: " + e.getMessage(), e);

                    }

                    break;
                }
            } catch (ServiceException e) {
                if (!continueOnError) throw e;
                if (newInv != null)
                    logIcsParseImportError(newInv, e);
                else
                    ZimbraLog.calendar.warn("Skipping error during ics parse/import", e);
            } catch (RuntimeException e) {
                if (!continueOnError) throw e;
                if (newInv != null)
                    logIcsParseImportError(newInv, e);
                else
                    ZimbraLog.calendar.warn("Skipping error during ics parse/import", e);
            }
        }
    }

    public static void logIcsParseImportError(Invite inv, Exception e) {
        String uid = inv.getUid();
        String recurrenceId = inv.hasRecurId() ? inv.getRecurId().toString() : null;
        int seq = inv.getSeqNo();
        String dtStart = inv.getStartTime() != null ? inv.getStartTime().toString() : null;
        String summary = inv.getName();
        ZimbraLog.calendar.warn(
                "Skipping error during ics parse/import: UID:" + uid +
                (recurrenceId != null ? ", RECURRENCE-ID:" + recurrenceId : "") +
                ", SEQUENCE:" + seq +
                (dtStart != null ? ", DTSTART:" + dtStart : "") +
                (summary != null ? ", SUMMARY:" + summary : ""),
                e);
    }

    protected ZComponent addInlineATTACHes(ZComponent comp) {
        MimeMessage mimeMsg = null;
        try {
            mimeMsg = getMimeMessage();
        } catch (ServiceException e1) {
            return comp;
        }
        if (mimeMsg == null) {
            return comp;
        }
        try {
            List<MPartInfo> parts = Mime.getParts(mimeMsg, MimeConstants.P_CHARSET_UTF8);
            if (parts != null && !parts.isEmpty()) {
                for (MPartInfo body : parts.get(0).getChildren()) {
                    if (body.isMultipart()) {
                        continue;
                    }
                    MimePart mp = body.getMimePart();
                    String ctype = StringUtil.stripControlCharacters(body.getContentType());
                    if (MimeConstants.CT_TEXT_CALENDAR.equalsIgnoreCase(ctype)) {
                        // A true calendar part has "method" parameter in the content type.
                        // Otherwise it's just an attachment that happens to be a .ics file.
                        try {
                            ContentType ct = new ContentType(body.getMimePart().getContentType());
                            if (ct.getParameter("method") != null) {
                                continue;
                            }
                        } catch (MessagingException e) {
                        }
                    }
                    String contentType = StringUtil.stripControlCharacters(body.getContentType());
                    String fileName = Mime.getFilename(mp);
                    try (InputStream in = mp.getInputStream()) {
                        byte[] rawBytes = IOUtils.toByteArray(in);
                        Attach attachment = Attach.fromUnencodedAndContentType(rawBytes, contentType);
                        if (!Strings.isNullOrEmpty(fileName)) {
                            attachment.setFileName(fileName);
                        }
                        comp.addProperty(attachment.toZProperty());
                    }
                }
            }
        } catch (MessagingException | IOException e) {
            ZimbraLog.calendar.warn("Problem adding inline ATTACHes", e);
        }
        return comp;
    }

    public ZComponent newToVComponent(boolean useOutlookCompatAllDayEvents, boolean includePrivateData)
    throws ServiceException {
        return newToVComponent(useOutlookCompatAllDayEvents, includePrivateData, false);
    }

    public ZComponent newToVComponent(boolean useOutlookCompatAllDayEvents, boolean includePrivateData,
                                             boolean includeAttaches)
    throws ServiceException {
        boolean isRequestPublishCancel =
            ICalTok.REQUEST.equals(mMethod) || ICalTok.PUBLISH.equals(mMethod) || ICalTok.CANCEL.equals(mMethod);
        ICalTok compTok;
        if (type == MailItem.Type.TASK) {
            compTok = ICalTok.VTODO;
            useOutlookCompatAllDayEvents = false;
        } else {
            compTok = ICalTok.VEVENT;
        }
        ZComponent component = new ZComponent(compTok);

        component.addProperty(new ZProperty(ICalTok.UID, getUid()));

        IRecurrence recur = getRecurrence();
        if (recur != null) {
            for (Iterator iter = recur.addRulesIterator(); iter!=null && iter.hasNext();) {
                IRecurrence cur = (IRecurrence)iter.next();

                switch (cur.getType()) {
                case Recurrence.TYPE_SINGLE_DATES:
                    if (DebugConfig.enableRdate) {
                        Recurrence.SingleDates sd = (Recurrence.SingleDates) cur;
                        RdateExdate rdate = sd.getRdateExdate();
                        rdate.addAsSeparateProperties(component);
                    }
                    break;
                case Recurrence.TYPE_REPEATING:
                    Recurrence.SimpleRepeatingRule srr = (Recurrence.SimpleRepeatingRule)cur;
                    component.addProperty(new ZProperty(ICalTok.RRULE, srr.getRule().toString()));
                    break;
                }

            }
            for (Iterator iter = recur.subRulesIterator(); iter!=null && iter.hasNext();) {
                IRecurrence cur = (IRecurrence)iter.next();

                switch (cur.getType()) {
                case Recurrence.TYPE_SINGLE_DATES:
                    Recurrence.SingleDates sd = (Recurrence.SingleDates) cur;
                    RdateExdate exdate = sd.getRdateExdate();
                    exdate.addAsSeparateProperties(component);
                    break;
                case Recurrence.TYPE_REPEATING:
                    Recurrence.SimpleRepeatingRule srr = (Recurrence.SimpleRepeatingRule)cur;
                    component.addProperty(new ZProperty(ICalTok.EXRULE, srr.getRule().toString()));
                    break;
                }
            }
        }

        if (includePrivateData || isPublic()) {
            // SUMMARY (aka Name or Subject)
            String name = getName();
            if (name != null && name.length()>0)
                component.addProperty(new ZProperty(ICalTok.SUMMARY, name));

            // DESCRIPTION and X-ALT-DESC;FMTTYPE=text/html
            String desc = getDescription();
            if (desc != null) {
                // Remove Outlook-style *~*~*~ header block.  Remove separator plus two newlines.
                int delim = desc.indexOf(HEADER_SEPARATOR);
                if (delim >= 0) {
                    desc = desc.substring(delim + HEADER_SEPARATOR.length());
                    desc = desc.replaceFirst("^\\r?\\n\\r?\\n", "");
                }
                if (desc.length() > 0)
                    component.addProperty(new ZProperty(ICalTok.DESCRIPTION, desc));
            }
            String descHtml = getDescriptionHtml();
            if (descHtml != null && descHtml.length() > 0) {
                ZProperty altDesc = new ZProperty(ICalTok.X_ALT_DESC, descHtml);
                altDesc.addParameter(new ZParameter(ICalTok.FMTTYPE, MimeConstants.CT_TEXT_HTML));
                component.addProperty(altDesc);
            }

            // COMMENT
            List<String> comments = getComments();
            if (comments != null && !comments.isEmpty()) {
                for (String comment : comments) {
                    component.addProperty(new ZProperty(ICalTok.COMMENT, comment));
                }
            }

            // LOCATION
            String location = getLocation();
            if (location != null && location.length() > 0)
                component.addProperty(new ZProperty(ICalTok.LOCATION, location.toString()));

            // ATTENDEES
            for (ZAttendee at : getAttendees()) {
                component.addProperty(at.toProperty());
            }

            // PRIORITY
            if (mPriority != null)
                component.addProperty(new ZProperty(ICalTok.PRIORITY, mPriority));

            // PERCENT-COMPLETE
            if (isTodo() && mPercentComplete != null)
                component.addProperty(new ZProperty(ICalTok.PERCENT_COMPLETE, mPercentComplete));

            // COMPLETED
            if (isTodo() && mCompleted != 0) {
                ParsedDateTime completed = ParsedDateTime.fromUTCTime(mCompleted);
                component.addProperty(completed.toProperty(ICalTok.COMPLETED, false));
            }

            // CATEGORIES
            List<String> categories = getCategories();
            if (categories != null && !categories.isEmpty()) {
                ZProperty catsProp = new ZProperty(ICalTok.CATEGORIES);
                catsProp.setValueList(categories);
                component.addProperty(catsProp);
            }

            // CONTACT
            List<String> contacts = getContacts();
            if (contacts != null && !contacts.isEmpty()) {
                for (String contact : contacts) {
                    component.addProperty(new ZProperty(ICalTok.CONTACT, contact));
                }
            }

            // GEO
            if (mGeo != null)
                component.addProperty(mGeo.toZProperty());

            // VALARMs
            for (Alarm alarm : mAlarms) {
                ZComponent alarmComp = alarm.toZComponent();
                component.addComponent(alarmComp);
            }

            // x-prop
            for (ZProperty xprop : mXProps) {
                component.addProperty(xprop);
            }

            // ORGANIZER
            if (hasOrganizer()) {
                ZOrganizer organizer = getOrganizer();
                ZProperty orgProp = organizer.toProperty();
                component.addProperty(orgProp);
                // Hack for Outlook 2007 (bug 25777)
                if (organizer.hasSentBy() && !ICalTok.REPLY.equals(mMethod) && !ICalTok.COUNTER.equals(mMethod)) {
                    String sentByParam = orgProp.paramVal(ICalTok.SENT_BY, null);
                    if (sentByParam != null) {
                        ZProperty xMsOlkSender = new ZProperty("X-MS-OLK-SENDER");
                        xMsOlkSender.setValue(sentByParam);
                        component.addProperty(xMsOlkSender);
                    }
                }
            }
        }

        // DTSTART
        ParsedDateTime dtstart = getStartTime();
        if (dtstart != null)
            component.addProperty(dtstart.toProperty(ICalTok.DTSTART, useOutlookCompatAllDayEvents));

        // DTEND or DUE
        ParsedDateTime dtend = getEndTime();
        if (dtend != null) {
            ICalTok prop = ICalTok.DTEND;
            if (isTodo())
                prop = ICalTok.DUE;
            component.addProperty(dtend.toProperty(prop, useOutlookCompatAllDayEvents));
        }

        // DURATION
        ParsedDuration dur = getDuration();
        if (dur != null)
            component.addProperty(new ZProperty(ICalTok.DURATION, dur.toString()));

        // STATUS
        String status = getStatus();
        String statusIcal = IcalXmlStrMap.sStatusMap.toIcal(status);
        if (IcalXmlStrMap.STATUS_ZCO_WAITING.equals(status) ||
            IcalXmlStrMap.STATUS_ZCO_DEFERRED.equals(status)) {
            ZParameter param = new ZParameter(ICalTok.X_ZIMBRA_STATUS, statusIcal);
            ZProperty prop = new ZProperty(ICalTok.STATUS, ICalTok.IN_PROCESS.toString());
            prop.addParameter(param);
            component.addProperty(prop);
        } else {
            component.addProperty(new ZProperty(ICalTok.STATUS, statusIcal));
        }

        // CLASS
        component.addProperty(new ZProperty(ICalTok.CLASS, IcalXmlStrMap.sClassMap.toIcal(getClassProp())));

        if (isEvent()) {
            // allDay
            if (isAllDayEvent())
                component.addProperty(new ZProperty(ICalTok.X_MICROSOFT_CDO_ALLDAYEVENT, true));

            // Microsoft Outlook compatibility for free-busy status
            if (isRequestPublishCancel) {
                String outlookFreeBusy = IcalXmlStrMap.sOutlookFreeBusyMap.toIcal(getFreeBusy());
                component.addProperty(new ZProperty(ICalTok.X_MICROSOFT_CDO_INTENDEDSTATUS, outlookFreeBusy));
            }

            // TRANSPARENCY
            component.addProperty(new ZProperty(ICalTok.TRANSP, IcalXmlStrMap.sTranspMap.toIcal(getTransparency())));
        }


        // RECURRENCE-ID
        RecurId recurId = getRecurId();
        if (recurId != null)
            component.addProperty(recurId.toProperty(useOutlookCompatAllDayEvents));

        // LAST-MODIFIED
        long lastModified = getLastModified();
        if (lastModified != 0) {
            ParsedDateTime dtLastModified = ParsedDateTime.fromUTCTime(lastModified);
            component.addProperty(dtLastModified.toProperty(ICalTok.LAST_MODIFIED, false));
        }

        // DTSTAMP
        ParsedDateTime dtStamp = ParsedDateTime.fromUTCTime(getDTStamp());
        component.addProperty(dtStamp.toProperty(ICalTok.DTSTAMP, false));

        // SEQUENCE
        component.addProperty(new ZProperty(ICalTok.SEQUENCE, getSeqNo()));

        // URL
        String url = getUrl();
        if (url != null && url.length() > 0)
            component.addProperty(new ZProperty(ICalTok.URL, url));

        if (isLocalOnly())
            component.addProperty(new ZProperty(ICalTok.X_ZIMBRA_LOCAL_ONLY, true));

        if (includeAttaches) {
            addInlineATTACHes(component);
        }
        return component;
    }

    /**
     * Assumption UID already matches.
     * @param recurId - if this is null, it means match Series or instance if it is a single instance
     */
    public static Invite matchingInvite(Iterable<Invite>invites, RecurId aRecurId) {
        if (invites == null) {
            return null;
        }
        for (Invite cur : invites) {
            // FIXME should check for cur.recurID WITHIN_RANGE (THISANDFUTURE-type support)
            if ((cur.getRecurId() != null && cur.getRecurId().equals(aRecurId)) ||
                    (cur.getRecurId() == null && aRecurId == null)) {
                return cur;
            }
        }
        return null;
    }

    /**
     * Assumption UID already matches.
     * @param recurId - if this is null, it means match Series or instance if it is a single instance
     */
    public static Invite matchingInvite(Invite invites[], RecurId aRecurId) {
        return matchingInvite(Arrays.asList(invites), aRecurId);
    }

    public List<Instance> getExdates() {
        List<Instance> exdates = Lists.newArrayList();
        IRecurrence recurrence = getRecurrence();
        if (recurrence == null) {
            return exdates;
        }

        for (Iterator<?> iter = recurrence.subRulesIterator(); iter!=null && iter.hasNext();) {
            IRecurrence cur = (IRecurrence)iter.next();
            switch (cur.getType()) {
            case Recurrence.TYPE_SINGLE_DATES:
                Recurrence.SingleDates sd = (Recurrence.SingleDates) cur;
                RdateExdate exdate = sd.getRdateExdate();
                if (exdate.isEXDATE()) {
                    exdates.addAll(sd.expandInstances(getMailItemId()));
                } else {
                    ZimbraLog.calendar.debug("RDATE '%s' ignored", exdate);
                }
                break;
            case Recurrence.TYPE_REPEATING:
                // See http://tools.ietf.org/html/rfc5545 A.3. Deprecated Features
                // 1.  The "EXRULE" property can no longer be specified in a component.
                ZimbraLog.calendar.debug("EXRULE ignored - no longer supported in rfc5545");
                break;
            }
        }
        return exdates;
    }

    public static List<Instance> getExdates(Invite inv) {
        List<Instance> exdates = Lists.newArrayList();
        if (inv == null) {
            return exdates;
        }
        return inv.getExdates();
    }

    public static ZComponent[] toVComponents(Invite[] invites,
                                             boolean includePrivateData,
                                             boolean useOutlookCompatAllDayEvents,
                                             boolean convertCanceledInstancesToExdates)
    throws ServiceException {
        return toVComponents(invites, includePrivateData, useOutlookCompatAllDayEvents,
                convertCanceledInstancesToExdates, false /* includeAttaches */);
    }

    public static ZComponent[] toVComponents(Invite[] invites,
                                             boolean includePrivateData,
                                             boolean useOutlookCompatAllDayEvents,
                                             boolean convertCanceledInstancesToExdates,
                                             boolean includeAttaches)
    throws ServiceException {
        List<ZComponent> comps = new ArrayList<ZComponent>(invites.length);
        if (!convertCanceledInstancesToExdates || invites.length <= 1) {
            for (Invite inv : invites) {
                ZComponent comp = inv.newToVComponent(useOutlookCompatAllDayEvents, includePrivateData,
                        includeAttaches);
                comps.add(comp);
            }
        } else {
            // Activate the hack that converts standalone VEVENT/VTODO components with STATUS:CANCELLED
            // into EXDATEs on the series component. (bug 36434)
            Invite seriesInv = null;
            ZComponent seriesComp = null;
            // Find the series invite.
            for (Invite inv : invites) {
                if (inv.isRecurrence()) {
                    ZComponent comp = inv.newToVComponent(useOutlookCompatAllDayEvents, includePrivateData,
                            includeAttaches);
                    seriesComp = comp;
                    comps.add(seriesComp);
                    seriesInv = inv;
                    break;
                }
            }
            for (Invite inv : invites) {
                if (inv != seriesInv) {  // We already handled the series invite in the previous loop.
                    if (inv.hasRecurId() && inv.isCancel()) {
                        // Canceled instance is added as an EXDATE to the series, instead of being treated
                        // as a standalone component.
                        if (seriesComp != null) {
                            RecurId rid = inv.getRecurId();
                            ZProperty ridProp = rid.toProperty(false);
                            // EXDATE and RECURRENCE-ID have same value types and parameter list.  Just copy over.
                            ZProperty exdateProp = new ZProperty(ICalTok.EXDATE, ridProp.getValue());
                            for (Iterator<ZParameter> paramsIter = ridProp.parameterIterator(); paramsIter.hasNext(); ) {
                                ZParameter param = paramsIter.next();
                                exdateProp.addParameter(param);
                            }
                            seriesComp.addProperty(exdateProp);
                        } else {
                            // But if there is no series component, let the canceled instance be a component.
                            ZComponent comp = inv.newToVComponent(useOutlookCompatAllDayEvents, includePrivateData,
                                                includeAttaches);
                                if (comp != null)
                                comps.add(comp);
                        }
                    } else {
                        // Modified instances are added as standalone components.
                        ZComponent comp = inv.newToVComponent(useOutlookCompatAllDayEvents, includePrivateData,
                                includeAttaches);
                        if (comp != null)
                            comps.add(comp);
                    }
                }
            }
        }
        return comps.toArray(new ZComponent[0]);
    }

    public Iterator<Alarm> alarmsIterator() { return mAlarms.iterator(); }
    public void addAlarm(Alarm alarm) {
        mAlarms.add(alarm);
    }
    public List<Alarm> getAlarms() { return mAlarms; }

    /**
     * Clear this Invite's alarms
     */
    public void clearAlarms() {
        mAlarms.clear();
    }

    public Iterator<ZProperty> xpropsIterator() { return mXProps.iterator(); }
    public void addXProp(ZProperty prop) {
        mXProps.add(prop);
    }
    public void removeXProp(String xpropName) {
        for (Iterator<ZProperty> iter = mXProps.iterator(); iter.hasNext(); ) {
            ZProperty prop = iter.next();
            if (prop.getName().equalsIgnoreCase(xpropName))
                iter.remove();
        }
    }
    public ZProperty getXProperty(String xpropName) {
        for (ZProperty prop : mXProps) {
            if (prop.getName().equalsIgnoreCase(xpropName))
                return prop;
        }
        return null;
    }

    /**
     * RFC2445 requires end date/time to be later than start date/time but
     * some calendar clients don't honor that.  Make sure the interval between
     * start and end are at least 1 second (if date/time) or 1 day (if date-only).
     * @throws ServiceException
     */
    public void validateDuration() throws ServiceException {
        if (mStart == null)
            return;
        if (!isTodo()) {
            ParsedDuration durMinimum =
                mStart.hasTime() ? ParsedDuration.parse(false, 0, 0, 0, 0, 1)
                                 : ParsedDuration.parse(false, 0, 1, 0, 0, 0);
            if (mEnd != null && mEnd.compareTo(mStart) <= 0) {
                mEnd = mStart.add(durMinimum);
            } else if (mDuration != null) {
                ParsedDateTime et = mStart.add(mDuration);
                long durMillis = et.getUtcTime() - mStart.getUtcTime();
                if (durMillis <= 0)
                    mDuration = durMinimum;
            }
        }
    }

    /**
     * Returns true if this invite's sequence is same or greater than other invite's sequence.
     * @param other
     * @return
     */
    public boolean isSameOrNewerVersion(Invite other) {
        if (other == null) return false;
        int thisSeq = getSeqNo();
        int otherSeq = other.getSeqNo();
        return thisSeq >= otherSeq;
    }

    public boolean isNewerVersion(Invite other) {
        if (other == null) return false;
        int thisSeq = getSeqNo();
        int otherSeq = other.getSeqNo();
        return thisSeq > otherSeq;
    }

    public Invite newCopy() {
        List<ZAttendee> attendees = new ArrayList<ZAttendee>(mAttendees.size());
        for (ZAttendee at : mAttendees) {
            attendees.add(new ZAttendee(at));  // add a copy of attendee
        }
        ZOrganizer org = mOrganizer != null ? new ZOrganizer(mOrganizer) : null;
        Invite inv = new Invite(type, mMethod != null ? mMethod.toString() : null, (mTzMap != null) ? mTzMap.clone() : null,
                mCalItem, mUid, mStatus, mPriority, mPercentComplete, mCompleted, mFreeBusy, mTransparency, mClass, mStart,
                mEnd, mDuration, mRecurrence, mIsOrganizer, org, attendees, mName, mLocation,
                mFlags, mPartStat, mRsvp, mRecurrenceId, mDTStamp, mLastModified, mSeqNo, mLastFullSeqNo,
                0, // mMailboxId
                0, // mMailItemId
                0, // mComponentNum
                mSentByMe, mDescription, mDescHtml, mFragment, new ArrayList<String>(mComments),
                new ArrayList<String>(mCategories), new ArrayList<String>(mContacts),
                mGeo != null ? new Geo(mGeo.getLatitude(), mGeo.getLongitude()) : null, mUrl);
        inv.setClassPropSetByMe(classPropSetByMe());
        inv.setDontIndexMimeMessage(getDontIndexMimeMessage());
        inv.mLocalOnly = mLocalOnly;
        inv.mDescInMeta = mDescInMeta;

        inv.clearAlarms();
        for (Alarm alarm : mAlarms) {
            inv.addAlarm(alarm.newCopy());
        }
        return inv;
    }

    private static String limitIntegerRange(String value, int min, int max, String defaultValue) {
        String retval = defaultValue;
        if (value != null) {
            try {
                int num = Integer.parseInt(value);
                if (num < min)
                    retval = Integer.toString(min);
                else if (num > max)
                    retval = Integer.toString(max);
                else
                    retval = value;
            } catch (NumberFormatException e) {}
        }
        return retval;
    }

    /**
     * Returns true if method is organizer-originated method, namely
     * PUBLISH, REQUEST, ADD, CANCEL or DECLINECOUNTER.
     * @return
     */
    public static boolean isOrganizerMethod(String method) {
        ICalTok methodTok = ICalTok.lookup(method);
        return isOrganizerMethod(methodTok);
    }

    public static boolean isOrganizerMethod(ICalTok method) {
        boolean isRequesting;
        if (method != null) {
            switch (method) {
            case REQUEST:
            case PUBLISH:
            case CANCEL:
            case ADD:
            case DECLINECOUNTER:
                isRequesting = true;
                break;
            default:
                isRequesting = false;
            }
        } else {
            isRequesting = true;
        }
        return isRequesting;
    }

    public void sanitize(boolean throwException) throws ServiceException {
        if ((mUid == null || mUid.length() == 0)) {
            if (throwException)
                throw ServiceException.INVALID_REQUEST("missing UID; subject=" + mName, null);
            else
                ZimbraLog.calendar.warn("UID missing; subject=" + mName);
        }
        mUid = fixupIfOutlookUid(mUid);

        // Don't let a task have DTSTART without DUE.
        if (isTodo() && mStart != null && mEnd == null)
            mStart = null;

        // Keep all-day flag and DTSTART/DTEND/DUE in sync.
        ParsedDateTime dt = mStart != null ? mStart : mEnd;  // Use DTSTART if given.  Fall back to DTEND/DUE.
        if (dt == null) {
            // No DTSTART.  Force non-all-day.
            setIsAllDayEvent(false);
        } else if (!dt.hasTime()) {
            // DTSTART has no time part.  Force all-day.
            setIsAllDayEvent(true);
        } else if (!dt.hasZeroTime()) {
            // Time part is not T000000.  Force non-all-day.
            setIsAllDayEvent(false);
        } else {
            // Time part is T000000.  Strictly speaking presence of any time part implies non-all-day,
            // but Outlook compatibility dictates we allow T000000 in an all-day appointment.
            // Leave current all-day flag as is.
        }

        // ORGANIZER is required if there is at least one ATTENDEE.
        if (isOrganizerMethod(mMethod) && hasOtherAttendees() && !hasOrganizer()) {
            if (throwException) {
                throw ServiceException.INVALID_REQUEST(
                        "ORGANIZER missing when ATTENDEEs are present; UID=" + mUid + ", subject=" + mName,
                        null);
            } else {
                // If we don't know who the organizer is, remove attendees.  Some clients will assume missing
                // organizer means current user is the organizer.  If attendees were kept, these clients will
                // send cancel notice to the attendees when appointment is deleted.  The attendees will get
                // confused because the cancel notice came from someone other than the organizer.
                ZimbraLog.calendar.warn(
                        "ORGANIZER missing; clearing ATTENDEEs to avoid confusing clients; UID=" + mUid + ", subject=" + mName);
                clearAttendees();
            }
        }

        // DTEND or DUE, if specified, can't be earlier than DTSTART.
        if (mStart != null && mEnd != null && mEnd.compareTo(mStart) < 0)
            mEnd = (ParsedDateTime) mStart.clone();

        // Recurrence rule can't be set without DTSTART.
        if (mRecurrence != null && mStart == null) {
            if (throwException) {
                throw ServiceException.INVALID_REQUEST("recurrence used without DTSTART; UID=" + mUid + ", subject=" + mName, null);
            } else {
                ZimbraLog.calendar.warn("recurrence used without DTSTART; removing recurrence; UID=" + mUid + ", subject=" + mName);
                mRecurrence = null;
            }
        }

        // Don't allow using different time zones in DTSTART and DTEND for a recurrence. (prevents future problems)
        if (isRecurrence() && mStart != null && mEnd != null &&  !mStart.getTimeZone().equals(mEnd.getTimeZone())) {
            ZimbraLog.calendar.warn(
                    "recurrence uses different time zones in DTSTART and DTEND; forcing DTEND to DTSTART time zone; UID=" +
                    mUid + ", subject=" + mName);
            mEnd.toTimeZone(mStart.getTimeZone());
        }

        mPercentComplete = limitIntegerRange(mPercentComplete, 0, 100, null);
        mPriority = limitIntegerRange(mPriority, 0, 9, null);

        // Clean up the time zone map to remove unreferenced TZs.
        Set<String> tzids = getReferencedTZIDs();
        mTzMap.reduceTo(tzids);

        // Set LAST-MODIFIED to DTSTAMP if unset.
        if (mLastModified == 0)
            mLastModified = mDTStamp;
    }

    /**
     * Add default alarm to an invite using the account's preferences.
     * @param inv
     * @param acct
     * @throws ServiceException
     */
    public static void setDefaultAlarm(Invite inv, Account acct) throws ServiceException {
        inv.clearAlarms();
        int prefNonAllDayMinutesBefore = (int) acct.getLongAttr(
                Provisioning.A_zimbraPrefCalendarApptReminderWarningTime, 0);
        int hoursBefore = 0;
        int minutesBefore = -1;
        if (!inv.isAllDayEvent()) {
            hoursBefore = 0;
            minutesBefore = prefNonAllDayMinutesBefore;
        } else if (prefNonAllDayMinutesBefore > 0 || prefNonAllDayMinutesBefore == -1) {
            // If preference says reminder is enabled, use 18-hours for all-day appointments,
            // regardless of preference value for non-all-day appointments.
            hoursBefore = 18;
            minutesBefore = 0;
        }
        if (minutesBefore > 0 || minutesBefore == -1 || hoursBefore > 0) {
            String summary = inv.getName();
            if (minutesBefore == -1) {
                minutesBefore = 0;
            }
            Alarm newAlarm = new Alarm(
                    Action.DISPLAY, TriggerType.RELATIVE, TriggerRelated.START,
                    ParsedDuration.parse(true, 0, 0, hoursBefore, minutesBefore, 0),
                    null, null, 0, null, summary, null, null, null);
            inv.addAlarm(newAlarm);
        }
    }

    public Set<String> getReferencedTZIDs() {
        Set<String> tzids = new HashSet<String>();
        // DTSTART
        if (mStart != null && mStart.hasTime()) {
            ICalTimeZone tz = mStart.getTimeZone();
            if (tz != null)
                tzids.add(tz.getID());
        }
        // DTEND/DUE
        if (mEnd != null && mEnd.hasTime()) {
            ICalTimeZone tz = mEnd.getTimeZone();
            if (tz != null)
                tzids.add(tz.getID());
        }
        // RECURRENCE-ID
        if (mRecurrenceId != null) {
            ParsedDateTime dt = mRecurrenceId.getDt();
            if (dt.hasTime()) {
                ICalTimeZone tz = dt.getTimeZone();
                if (tz != null)
                    tzids.add(tz.getID());
            }
        }
        // RDATE/EXDATE
        IRecurrence recur = getRecurrence();
        if (recur != null)
            tzids.addAll(Recurrence.getReferencedTZIDs(recur));
        return tzids;
    }

    // remove all data considered private
    public void clearPrivateInfo() {
        mName = null;
        mDescription = null;
        mDescHtml = null;
        mComments.clear();
        mLocation = null;
        mAttendees.clear();
        mPriority = null;
        mPercentComplete = null;
        mCompleted = 0;
        mCategories.clear();
        mContacts.clear();
        mGeo = null;
        mAlarms.clear();
        mXProps.clear();
    }

    /**
     * If this Invite is a series invite, create a new Invite object for the instance denoted by recurIdDt.
     * Returns null if this Invite is not a series Invite.
     * @param recurIdDt
     * @return
     */
    public Invite makeInstanceInvite(ParsedDateTime recurIdDt) throws ServiceException {
        if (!isRecurrence())
            return null;
        Invite instInv = newCopy();
        instInv.setLocalOnly(true);
        instInv.setRecurrence(null);
        RecurId rid = new RecurId(recurIdDt, RecurId.RANGE_NONE);
        instInv.setRecurId(rid);
        ParsedDateTime dtEnd = recurIdDt.add(instInv.getEffectiveDuration());
        instInv.setDtStart(recurIdDt);
        instInv.setDtEnd(dtEnd);
        return instInv;
    }

    // iCalendar PRIORITY to hi/med/low mapping according to RFC5545 Section 3.8.1.9
    public boolean isHighPriority() {
        if (mPriority != null) {
            int prio = 0;
            try {
                prio = Integer.parseInt(mPriority);
            } catch (NumberFormatException e) {}
            return prio >= 1 && prio <= 4;
        }
        return false;
    }
    public boolean isLowPriority() {
        if (mPriority != null) {
            int prio = 0;
            try {
                prio = Integer.parseInt(mPriority);
            } catch (NumberFormatException e) {}
            return prio >= 6 && prio <= 9;
        }
        return false;
    }

    private static boolean isHexDigits(String str) {
        int len = str.length();
        for (int i = 0; i < len; ++i) {
            char ch = str.charAt(i);
            if ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F') || (ch >= 'a' &&  ch <= 'f'))
                continue;
            return false;
        }
        return true;
    }

    private static final String OUTLOOK_GLOBAL_ID_PREFIX = "040000008200E00074C5B7101A82E008";

    public static boolean isOutlookUid(String uid) {
        if (uid == null)
            return false;
        int len = uid.length();
        if (len >= 82 && len % 2 == 0 && isHexDigits(uid)) {
            String upper = uid.toUpperCase();
            return upper.startsWith(OUTLOOK_GLOBAL_ID_PREFIX);
        }
        return false;
    }

    // Outlook-generated UIDs are supposed to be uppercase.  (bug 57727)
    public static String fixupIfOutlookUid(String uid) {
        return isOutlookUid(uid) ? uid.toUpperCase() : uid;
    }
}
