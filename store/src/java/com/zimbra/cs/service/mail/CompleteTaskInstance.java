/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.service.mail;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.ParsedDuration;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.mailbox.MailboxLock;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.CalendarItem.Instance;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.OperationContext;
import com.zimbra.cs.mailbox.Task;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.ZAttendee;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.soap.ZimbraSoapContext;


public class CompleteTaskInstance extends CalendarRequest {

    private static final String[] TARGET_PATH = new String[] { MailConstants.A_ID };

    @Override
    protected String[] getProxiedIdPath(Element request)     { return TARGET_PATH; }

    @Override
    protected boolean checkMountpointProxy(Element request)  { return false; }

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Mailbox mbox = getRequestedMailbox(zsc);
        OperationContext octxt = getOperationContext(zsc, context);

        ItemId iid = new ItemId(request.getAttribute(MailConstants.A_ID), zsc);
        Element exceptElem = request.getElement(MailConstants.E_CAL_EXCEPTION_ID);

        try (final MailboxLock l = mbox.getWriteLockAndLockIt()) {
            CalendarItem calItem = mbox.getCalendarItemById(octxt, iid.getId());
            if (calItem == null) {
                throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Could not find calendar item");
            }
            if (!(calItem instanceof Task)) {
                throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "Calendar item is not a task");
            }
            Invite inv = calItem.getDefaultInviteOrNull();
            if (inv == null) {
                throw MailServiceException.NO_SUCH_CALITEM(iid.toString(), "No default invite found");
            }
            if (!inv.isRecurrence()) {
                throw ServiceException.INVALID_REQUEST("Task is not a recurring task", null);
            }

            ParsedDateTime recurStart = inv.getStartTime();
            if (recurStart == null) {
                throw ServiceException.INVALID_REQUEST("Recurring task is missing start time", null);
            }

            // the instance being marked complete
            TimeZoneMap tzmap = inv.getTimeZoneMap();
            Element tzElem = request.getOptionalElement(MailConstants.E_CAL_TZ);
            ICalTimeZone tz = null;
            if (tzElem != null) {
                tz = CalendarUtils.parseTzElement(tzElem);
                tzmap.add(tz);
            }
            ParsedDateTime exceptDt = CalendarUtils.parseDateTime(exceptElem, tzmap);

            if (exceptDt.getUtcTime() != recurStart.getUtcTime()) {
                throw MailServiceException.INVITE_OUT_OF_DATE(iid.toString());
            }

            // Create a new single-instance task for completed date.
            Invite completed = createCompletedInstanceInvite(inv, exceptDt);
            mbox.addInvite(octxt, completed, calItem.getFolderId());

            // Update recurrence's start date to the next instance start date.
            long oldStart = recurStart.getUtcTime();
            long newStart = -1;
            Collection<Instance> instances = calItem.expandInstances(oldStart, Long.MAX_VALUE, false);
            for (Instance inst : instances) {
                if (inst.getStart() > oldStart) {
                    newStart = inst.getStart();
                    break;
                }
            }
            if (newStart != -1) {
                // Update DTSTART to newStart.
                ParsedDateTime newStartDt = ParsedDateTime.fromUTCTime(newStart);
                newStartDt.toTimeZone(inv.getStartTime().getTimeZone());
                newStartDt.setHasTime(recurStart.hasTime());
                // Update DUE.
                ParsedDuration dur = inv.getEffectiveDuration();
                if (dur != null) {
                    ParsedDateTime due = newStartDt.add(dur);
                    inv.setDtEnd(due);
                }
                inv.setDtStart(newStartDt);
                inv.setSeqNo(inv.getSeqNo() + 1);
                inv.setDtStamp(System.currentTimeMillis());
                mbox.addInvite(octxt, inv, calItem.getFolderId());
            } else {
                // No more instance left.  Delete the recurring task.
                mbox.delete(octxt, calItem.getId(), calItem.getType());
            }
        }

        // response
        Element response = getResponseElement(zsc);
        return response;
    }

    private Invite createCompletedInstanceInvite(Invite recur, ParsedDateTime dtStart) throws ServiceException {
        Invite inst = new Invite(MailItem.Type.TASK, recur.getMethod(), (recur.getTimeZoneMap() != null) ?
                recur.getTimeZoneMap().clone() : null, recur.isOrganizer());

        long now = System.currentTimeMillis();

        // Assign a new UID.
        String uid = LdapUtil.generateUUID();
        inst.setUid(uid);
        inst.setSeqNo(0);

        // Set completed status/pct/time.
        inst.setStatus(IcalXmlStrMap.STATUS_COMPLETED);
        inst.setPercentComplete("100");
        inst.setCompleted(now);

        // Set time fields.
        inst.setDtStart(dtStart);
        ParsedDuration dur = recur.getEffectiveDuration();
        if (dur != null) {
            ParsedDateTime due = dtStart.add(dur);
            inst.setDtEnd(due);
        }
        inst.setDtStamp(now);

        // Recurrence-related fields should be unset.
        inst.setRecurrence(null);
        inst.setRecurId(null);

        // Copy the rest of the fields.
        inst.setPriority(recur.getPriority());
        inst.setOrganizer(recur.getOrganizer());
        List<ZAttendee> attendees = recur.getAttendees();
        for (ZAttendee at : attendees)
            inst.addAttendee(at);
        inst.setName(recur.getName());
        inst.setLocation(recur.getLocation());
        inst.setFlags(recur.getFlags());
        inst.setDescription(recur.getDescription(), recur.getDescriptionHtml());
        inst.setFragment(recur.getFragment());

        return inst;
    }
}
