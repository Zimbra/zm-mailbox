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

import java.util.Iterator;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.fb.FreeBusy.FBInstance;
import com.zimbra.cs.fb.FreeBusy.Interval;
import com.zimbra.cs.fb.FreeBusy.IntervalList;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.cache.CalendarData;
import com.zimbra.cs.mailbox.calendar.cache.CalendarItemData;
import com.zimbra.cs.mailbox.calendar.cache.FullInstanceData;
import com.zimbra.cs.mailbox.calendar.cache.InstanceData;

public class LocalFreeBusyProvider {

    public static FreeBusy getFreeBusy(Account acct, String name, long start, long end) 
    	throws ServiceException {
    	return getFreeBusyList(MailboxManager.getInstance().getMailboxByAccount(acct), name, start, end);
    }
	public static FreeBusy getFreeBusyList(Mailbox mbox, String name, long start, long end)
		throws ServiceException {
		return getFreeBusyList(mbox, name, start, end, null);
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
		return getFreeBusyList(mbox, mbox.getAccount().getName(), start, end, exAppt);
	}

	public static FreeBusy getFreeBusyList(Mailbox mbox, String name, long start, long end, Appointment exAppt)
    throws ServiceException {
	    // Check if this account is an always-free calendar resource.
        Account acct = mbox.getAccount();
        if (acct instanceof CalendarResource) {
            CalendarResource resource = (CalendarResource) acct;
            if (resource.autoAcceptDecline() && !resource.autoDeclineIfBusy()) {
                IntervalList intervals = new IntervalList(start, end);
                return new FreeBusy(acct.getName(), intervals, start, end);
            }
        }

        int exApptId = exAppt == null ? -1 : exAppt.getId();

        IntervalList intervals = new IntervalList(start, end);

        List<CalendarData> calDataList =
            mbox.getAllCalendarsSummaryForRange(null, MailItem.TYPE_APPOINTMENT, start, end);
        for (CalendarData calData : calDataList) {
            int folderId = calData.getFolderId();
            Folder f = mbox.getFolderById(null, folderId);
            if ((f.getFlagBitmask() & Flag.BITMASK_EXCLUDE_FREEBUSY) != 0)
                continue;
            for (Iterator<CalendarItemData> iter = calData.calendarItemIterator(); iter.hasNext(); ) {
                CalendarItemData appt = iter.next();
                int apptId = appt.getCalItemId();
                if (apptId == exApptId)
                    continue;
                FullInstanceData defaultInstance = appt.getDefaultData();
                if (defaultInstance == null)
                    continue;
                boolean isTransparent = false;
                String transp = defaultInstance.getTransparency();
                isTransparent = IcalXmlStrMap.TRANSP_TRANSPARENT.equals(transp);
                long defaultDuration = 0;
                if (defaultInstance.getDuration() != null)
                    defaultDuration = defaultInstance.getDuration().longValue();
                String defaultFreeBusy = defaultInstance.getFreeBusyActual();
                for (Iterator<InstanceData> instIter = appt.instanceIterator(); instIter.hasNext(); ) {
                    InstanceData instance = instIter.next();
                    long instStart = instance.getDtStart() != null ? instance.getDtStart().longValue() : 0;
                    // Skip instances that are outside the time range but were returned due to alarm being in range.
                    if (instStart >= end)
                        continue;
                    long dur = defaultDuration;
                    if (instance.getDuration() != null)
                        dur = instance.getDuration().longValue();
                    long instEnd = instStart + dur;

                    long recurIdDt = 0;
                    // Skip if instance is TRANSPARENT to free/busy searches.
                    if (instance instanceof FullInstanceData) {
                        FullInstanceData fullInst = (FullInstanceData) instance;
                        String transpInst = fullInst.getTransparency();
                        recurIdDt = fullInst.getRecurrenceId();
                        if (IcalXmlStrMap.TRANSP_TRANSPARENT.equals(transpInst))
                            continue;
                    } else if (isTransparent) {
                        continue;
                    }

                    String freeBusy = instance.getFreeBusyActual();
                    if (freeBusy == null)
                        freeBusy = defaultFreeBusy;
                    if (!IcalXmlStrMap.FBTYPE_FREE.equals(freeBusy)) {
                        FBInstance fbInst = new FBInstance(instStart, instEnd, apptId, recurIdDt);
                        Interval ival = new Interval(instStart, instEnd, freeBusy, fbInst);
                        intervals.addInterval(ival);
                    }
                }
            }
        }
        return new FreeBusy(name, intervals, start, end);
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
