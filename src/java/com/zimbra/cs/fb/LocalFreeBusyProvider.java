/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2008 Zimbra, Inc.
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
package com.zimbra.cs.fb;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.db.DbSearch;
import com.zimbra.cs.fb.FreeBusy.Interval;
import com.zimbra.cs.fb.FreeBusy.IntervalList;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.InviteInfo;
import com.zimbra.cs.mailbox.calendar.ParsedDateTime;

public class LocalFreeBusyProvider {

    public static FreeBusy createDummyFreeBusy(long start, long end) {
        IntervalList ilist = new IntervalList(start, end);
        return new FreeBusy(ilist, start, end);
    }

    public static FreeBusy getFreeBusy(Account acct, long start, long end) 
    	throws ServiceException {
    	return getFreeBusyList(MailboxManager.getInstance().getMailboxByAccount(acct), start, end);
    }
	public static FreeBusy getFreeBusyList(Mailbox mbox, long start, long end)
		throws ServiceException {
		return getFreeBusyList(mbox, start, end, null);
	}

	/**
	 * 
	 * @param mbox
	 * @param start
	 * @param end
	 * @param exAppt appointment to exclude; calculate free/busy assuming
	 *               the specified appointment wasn't there
	 * @return
	 * @throws ServiceException
	 */
	public static FreeBusy getFreeBusyList(Mailbox mbox, long start, long end, Appointment exAppt)
	throws ServiceException {
//		Check if this account is an always-free calendar resource.
		Account acct = mbox.getAccount();
		if (acct instanceof CalendarResource) {
			CalendarResource resource = (CalendarResource) acct;
			if (resource.autoAcceptDecline() && !resource.autoDeclineIfBusy()) {
				IntervalList intervals = new IntervalList(start, end);
				return new FreeBusy(intervals, start, end);
			}
		}

		int exApptId = exAppt == null ? -1 : exAppt.getId();

		List<Folder> folders = mbox.getFolderList(null, DbSearch.SORT_NONE);
		List<Folder> excludeFolders = new ArrayList<Folder>();
		for (Folder f : folders) {
			if ((f.getFlagBitmask() & Flag.BITMASK_EXCLUDE_FREEBUSY)!= 0)
				excludeFolders.add(f);
		}

		int[] exFolders = null;
		if (excludeFolders.size() > 0) {
			exFolders = new int[excludeFolders.size()];
			int i = 0;
			for (Folder f : excludeFolders)
				exFolders[i++] = f.getId();
		}

		Collection<CalendarItem> appts = mbox.getCalendarItemsForRange(null, start, end, Mailbox.ID_AUTO_INCREMENT, exFolders);

		IntervalList intervals = new IntervalList(start, end);

		for (Iterator<CalendarItem> iter = appts.iterator(); iter.hasNext(); ) {
			CalendarItem calItem = iter.next();
			if (!(calItem instanceof Appointment))
				continue;

			Appointment cur = (Appointment) calItem;
			if (cur.getId() == exApptId)
				continue;

//			Move start time of expansion up by default instance's duration
//			to catch instances whose tail end overlap the F/B time window.
			Invite defInv = cur.getDefaultInviteOrNull();
			if (defInv == null || !defInv.isEvent())
				continue;
			ParsedDateTime dtStart = defInv.getStartTime();
			long defInvStart = dtStart != null ? dtStart.getDate().getTime() : 0;
			ParsedDateTime dtEnd = defInv.getEffectiveEndTime();
			long defInvEnd = dtEnd != null ? dtEnd.getDate().getTime() : 0;
			long startAdjusted = start - (defInvEnd - defInvStart) + 1;

			Collection instances = cur.expandInstances(startAdjusted, end, false);
			for (Iterator instIter = instances.iterator(); instIter.hasNext();) {
				Appointment.Instance inst = (Appointment.Instance)(instIter.next());
				assert(inst.getStart() < end && inst.getEnd() > start);
				InviteInfo invId = inst.getInviteInfo();
				try {
					Appointment appt = (Appointment) inst.getCalendarItem();
					Invite inv = appt.getInvite(invId);
					if (!inv.isTransparent()) {
						String freeBusy = appt.getEffectiveFreeBusyActual(inv, inst);
						if (!IcalXmlStrMap.FBTYPE_FREE.equals(freeBusy)) {
							Interval ival = new Interval(inst.getStart(), inst.getEnd(), freeBusy, inst);
							intervals.addInterval(ival);
						}
					}
				} catch (MailServiceException.NoSuchItemException e) {
					//sLog.debug("Could not load invite "+invId.toString() + " for appt "+mbox.getId());
				}
			}
		}
		return new FreeBusy(intervals, start, end);
	}

    public static void main(String[] args) {
        IntervalList l = new IntervalList(0, 100);
        Interval toAdd;
        
        System.out.println("List: "+ l.toString());
        
        toAdd = new Interval(50, 60, IcalXmlStrMap.FBTYPE_BUSY);
        l.addInterval(toAdd);
        System.out.println("Added: "+toAdd+l.toString());
        toAdd = new Interval(10, 20, IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE);
        l.addInterval(toAdd);
        System.out.println("Added: "+toAdd+l.toString());
        toAdd = new Interval(20, 30, IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE);
        l.addInterval(toAdd);
        System.out.println("Added: "+toAdd+l.toString());
        toAdd = new Interval(15, 35, IcalXmlStrMap.FBTYPE_BUSY);
        l.addInterval(toAdd);
        System.out.println("Added: "+toAdd+l.toString());

        try {
            Mailbox mbox = MailboxManager.getInstance().getMailboxById(1);
            FreeBusy fb = getFreeBusyList(mbox, 0, Long.MAX_VALUE);
            System.out.println(fb.toString());
        } catch (ServiceException e){
            System.out.println("EXCEPTION: "+e);
            e.printStackTrace();
        }
            
    }
}
