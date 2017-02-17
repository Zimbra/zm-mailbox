/* ***** BEGIN LICENSE BLOCK *****
/* Zimbra Collaboration Suite Server
/* Copyright (C) 2009, 2010, 2011, 2013, 2014, 2016 Synacor, Inc.
/*
/* This program is free software: you can redistribute it and/or modify it under
/* the terms of the GNU General Public License as published by the Free Software Foundation,
/* version 2 of the License.
/*
/* This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
/* without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
/* See the GNU General Public License for more details.
/* You should have received a copy of the GNU General Public License along with this program.
/* If not, see <https://www.gnu.org/licenses/>.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.service.mail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.service.ServiceException;
import com.zimbra.common.soap.Element;
import com.zimbra.common.soap.MailConstants;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.fb.FreeBusy;
import com.zimbra.cs.fb.FreeBusyQuery;
import com.zimbra.cs.fb.FreeBusy.Interval;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.CalendarItem.Instance;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Util;
import com.zimbra.soap.SoapServlet;
import com.zimbra.soap.ZimbraSoapContext;

public class CheckRecurConflicts extends ExpandRecur {

    @Override
    public Element handle(Element request, Map<String, Object> context) throws ServiceException {
        ZimbraSoapContext zsc = getZimbraSoapContext(context);
        Account authAcct = getAuthenticatedAccount(zsc);
        Element response = getResponseElement(zsc);

        long rangeStart = request.getAttributeLong(MailConstants.A_CAL_START_TIME, 0);
        if (rangeStart == 0)
            rangeStart = System.currentTimeMillis();
        long rangeEnd = request.getAttributeLong(MailConstants.A_CAL_END_TIME, 0);
        if (rangeEnd == 0)
            rangeEnd = Long.MAX_VALUE;
        boolean allInstances = request.getAttributeBool(MailConstants.A_CAL_ALL, false);
        String exApptUid = request.getAttribute(MailConstants.A_APPT_FREEBUSY_EXCLUDE_UID, null);

        // Parse and expand the recurrence.
        TimeZoneMap tzmap = new TimeZoneMap(Util.getAccountTimeZone(authAcct));
        ParsedRecurrence parsed = parseRecur(request, tzmap);
        List<Instance> instances = getInstances(parsed, rangeStart, rangeEnd);
        if (instances == null || instances.isEmpty())
            return response;

        // Find the range covered by the instances.
        long rangeStartActual = rangeEnd, rangeEndActual = rangeStart;
        for (Instance inst : instances) {
            if (inst.hasStart() && inst.hasEnd()) {
                rangeStartActual = Math.min(rangeStartActual, inst.getStart());
                rangeEndActual = Math.max(rangeEndActual, inst.getEnd());
            }
        }
        if (rangeStartActual >= rangeEndActual)
            return response;

        // Run free/busy search on the users.
        FreeBusyQuery fbQuery = new FreeBusyQuery(
                (HttpServletRequest) context.get(SoapServlet.SERVLET_REQUEST), zsc, authAcct, rangeStartActual, rangeEndActual, exApptUid);
        for (Iterator<Element> usrIter = request.elementIterator(MailConstants.E_FREEBUSY_USER); usrIter.hasNext(); ) {
            Element usrElem = usrIter.next();
            int folderId = (int) usrElem.getAttributeLong(MailConstants.A_FOLDER, FreeBusyQuery.CALENDAR_FOLDER_ALL);
            if (folderId == Mailbox.ID_FOLDER_USER_ROOT || folderId == 0)
                folderId = FreeBusyQuery.CALENDAR_FOLDER_ALL;
            String id = usrElem.getAttribute(MailConstants.A_ID, null);
            if (id != null)
                fbQuery.addAccountId(id, folderId);
            String name = usrElem.getAttribute(MailConstants.A_NAME, null);
            if (name != null)
                fbQuery.addEmailAddress(name, folderId);
        }
        Collection<FreeBusy> fbResults = fbQuery.getResults();
        List<UserConflicts> conflicts = new ArrayList<UserConflicts>();
        for (FreeBusy fb : fbResults) {
            UserConflicts ucon = getConflicts(fb, instances);
            conflicts.add(ucon);
        }

        // Find conflicts for each instance.
        for (Instance inst : instances) {
            Element instElem = addInstance(response, inst);
            int numConflicts = 0;
            for (UserConflicts ucon : conflicts) {
                String fbStatus = ucon.get(inst);
                if (fbStatus != null) {
                    ++numConflicts;
                    String username = ucon.getUsername();
                    Element usrElem = instElem.addElement(MailConstants.E_FREEBUSY_USER);
                    usrElem.addAttribute(MailConstants.A_NAME, username);
                    usrElem.addAttribute(MailConstants.A_APPT_FREEBUSY, fbStatus);
                }
            }
            if (numConflicts == 0 && !allInstances)
                instElem.detach();
        }
        return response;
    }

    private static class UserConflicts {
        private String mUsername;
        private Map<Instance, String /* f/b status */> mMap;

        public UserConflicts(String username) {
            mUsername = username;
            mMap = new HashMap<Instance, String>();
        }

        public String getUsername() { return mUsername; }

        public void put(Instance inst, String fbStatus) {
            mMap.put(inst, fbStatus);
        }

        public String get(Instance inst) {
            return mMap.get(inst);
        }
    }

    private static UserConflicts getConflicts(FreeBusy fb, List<Instance> instances) throws ServiceException {
        UserConflicts conflicts = new UserConflicts(fb.getName());
        Iterator<Interval> ivalIter = fb.iterator();
        Iterator<Instance> instIter = instances.iterator();
        Interval ival = ivalIter.hasNext() ? ivalIter.next() : null;
        Instance inst = instIter.hasNext() ? instIter.next() : null;
        // f/b status of the previous overlap between instance and interval, if previous and current overlaps
        // are with the same instance
        String prevStatus = null;
        while (ival != null && inst != null) {
            String status = ival.getStatus();
            if (IcalXmlStrMap.FBTYPE_FREE.equals(status)) {
                ival = ivalIter.hasNext() ? ivalIter.next() : null;
                continue;
            }
            if (!inst.hasStart() || !inst.hasEnd()) {
                inst = instIter.hasNext() ? instIter.next() : null;
                prevStatus = null;
                continue;
            }
            long instStart = inst.getStart();
            long instEnd = inst.getEnd();
            long ivalStart = ival.getStart();
            long ivalEnd = ival.getEnd();
            // If instance is before interval, go to next instance.
            if (instEnd <= ivalStart) {
                inst = instIter.hasNext() ? instIter.next() : null;
                prevStatus = null;
                continue;
            }
            // If interval is before instance, go to next interval.
            if (ivalEnd <= instStart) {
                ival = ivalIter.hasNext() ? ivalIter.next() : null;
                prevStatus = null;
                continue;
            }

            // We have an overlap!
            if (prevStatus != null)
                status = FreeBusy.chooseBusier(status, prevStatus);
            conflicts.put(inst, status);
            prevStatus = status;

            // Advance to the next interval or instance.
            if (ivalEnd < instEnd) {
                // Go to the next interval if its end time is earlier than the instance end time, because
                // the next interval may still overlap the same instance.
                ival = ivalIter.hasNext() ? ivalIter.next() : null;
            } else {  // instEnd >= ivalEnd
                // If instance end time is at or earlier than interval end time, go to the next
                // instance.  Clear prevStatus because we're changing instance.
                inst = instIter.hasNext() ? instIter.next() : null;
                prevStatus = null;
            }
        }
        return conflicts;
    }
}
