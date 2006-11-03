/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * 
 * The contents of this file are subject to the Mozilla Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite Server.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005, 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.CalendarResource;
import com.zimbra.cs.db.DbMailItem;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Flag;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Appointment.Instance;
import com.zimbra.cs.service.*;

/**
 * @author tim
 *
 * Calculates the FreeBusy list for a mailbox.  
 * 
 * The FreeBusy list is a list of Intervals each from (start, end] with a type 
 * of "Free" or "Busy" or eventually "Out of Office".
 * 
 * Calculating from the mailbox is relatively simple: start with everything free, then
 * expand the mailbox's list of appointments and add busy intervals to the list...then
 * combine intervals that are right next to each other (ie 9-10busy, 10-11busy becomes
 * 9-11busy)
 */
public class FreeBusy implements Iterable<FreeBusy.Interval> {

    private static Log sLog = LogFactory.getLog(FreeBusy.class);
    
    private FreeBusy(IntervalList list) {
        mList = list;
    }
    IntervalList mList; 
    
    private static class IntervalIterator implements Iterator<Interval> {
        private Interval mCur;
        private IntervalIterator(IntervalList list) {
            mCur = list.getHead();
        }
        public boolean hasNext() { return (mCur != null); }
        public Interval next() { 
            Interval toRet = mCur;
            mCur = mCur.getNext();
            return toRet;
        }
        public void remove() {
            throw new IllegalArgumentException("Unsupported");
        }
    }
    
    public Iterator<Interval> iterator() {
        return new IntervalIterator(mList);
    }
    
    private static class IntervalList {
        
        IntervalList(long start, long end) {
            mStart = start;
            mEnd = end;
            mHead = new Interval(start, end, IcalXmlStrMap.FBTYPE_FREE);
        }

        void addInterval(Interval toAdd) {
            assert(toAdd.mStart <= toAdd.mEnd);
            // we only care about intervals within our window!  
            if (toAdd.mStart < mStart) {
                toAdd.mStart = mStart;
            }
            if (toAdd.mEnd > mEnd) {
                toAdd.mEnd = mEnd;
            }

            // step 1: Of the intervals already in the list, find the one that
            //         contains the start of toAdd.
            //
            //         <---> <---> <-- uber-start --> <---> <---> ...
            //                            <------- toAdd -------->
            //
            //         Remember this "uber-start" interval, we'll need it below
            //
            Interval uberStart;
            for (uberStart = mHead; uberStart.hasNext(); uberStart = uberStart.getNext())
            {
                if (uberStart.getNext().mStart > toAdd.mStart) {
                    break;
                }
            }
            assert(uberStart.mStart <= toAdd.mStart);
            assert(uberStart.overlapsOrAbuts(toAdd));
            
            Interval cur = uberStart;
            
            while (toAdd.mStart < toAdd.mEnd) {
                assert(cur.mEnd >= cur.mStart);
                assert(cur.mStart <= toAdd.mStart);

                // if some of cur is before toAdd, then split it off
                //
                // before:
                // <-------------------- cur ------------>
                //                 <------------ toAdd ------------>
                //
                // via:
                // <-------------------- cur ------------>
                //                 <---- newInt --------->
                //                 <------------ toAdd ------------>
                //
                // after:
                // <--------------><---- cur ------------>
                //                 <------------ toAdd ------------>
                //
                if (toAdd.mStart > cur.mStart) {
                    Interval newInt = new Interval(toAdd.mStart,
                                                   cur.mEnd, cur.mStatus,
                                                   cur.getInstances());
                    cur.insertAfter(newInt);
                    cur.mEnd = newInt.mStart;
                    cur = newInt;
                }
                
                // if some of cur is AFTER toAdd then split it off
                //
                // before:
                // <------------- cur ---------------------------->
                // <--------- toAdd --------->
                //
                // via:
                // <------------- cur ---------------------------->
                //                            <----- afterUs ----->
                // <--------- toAdd --------->
                //
                // after:
                // <------------- cur -------><----- afterUs ----->
                // <--------- toAdd --------->
                //
                if (toAdd.mEnd < cur.mEnd) {
                    Interval afterUs = new Interval(toAdd.mEnd,
                                                    cur.mEnd, cur.mStatus,
                                                    cur.getInstances());
                    cur.insertAfter(afterUs);
                    cur.mEnd = toAdd.mEnd;
                }
                
                // OK -- so now cur is some chunk of toAdd which we can use!
                //
                // Either they are identical intervals:
                // <------------- cur ---------------->
                // <------------ toAdd --------------->
                //
                // Or cur is part of toAdd with same start time:
                // <------------- cur ---------------->
                // <------------ toAdd ---------------------------->
                cur.combineStatus(toAdd.mStatus);
                cur.addInstances(toAdd.getInstances());

                // Now let's look at the rest of toAdd not covered by cur.
                //
                // before:
                // <------------- cur ---------------->
                // <------------ toAdd ------------------------------>
                //
                // after:
                // <------------- cur ---------------->
                // <----------------------------------><--- toAdd --->
                toAdd.mStart = cur.mEnd;
                
                if (cur.mNext != null) {
                    cur = cur.mNext;
                } else {
                    assert(toAdd.mStart == toAdd.mEnd);
                }
            }
            
            
            // FINISH: iterate from uber-start until past toAdd's end, join intervals next to each other
            
//            System.out.print("BEFORE combining: "+toString());
            
            cur = uberStart;
            if (cur.hasPrev()) {
                cur = cur.getPrev();
            }
            
            while (cur.getNext() != null) {
                assert(cur.getNext().mStart == cur.mEnd);
                if (cur.mStatus.equals(cur.getNext().mStatus)) {
                    cur.mEnd = cur.getNext().mEnd;
                    cur.removeNext();
                } else {
                    cur = cur.getNext();                    
                }
            }
            
//            System.out.println("AFTER combining: "+toString());
            
        }

        public String toString() {
            StringBuffer toRet = new StringBuffer("\n");
            for (Interval cur = mHead; cur != null; cur = cur.getNext()) {
                toRet.append("\t").append(cur.toString()).append("\n");
            }
            return toRet.toString();
        }
        
        public Interval getHead() { return mHead; }
        
        
        long mStart;
        long mEnd;
        
        Interval mHead;
    }
    
    public static class Interval {
        public Interval(long start, long end, String status) {
            mStart = start;
            mEnd = end;
            assert(end >= start);
            if (status != null) {
                mStatus = status;
            } else {
                mStatus = IcalXmlStrMap.FBTYPE_FREE;
            }
            mInstances = new LinkedHashSet<Instance>();
        }

        public Interval(long start, long end, String status, Instance instance) {
            this(start, end, status);
            if (instance != null)
                mInstances.add(instance);
        }

        public Interval(long start, long end, String status, LinkedHashSet<Instance> instances) {
            this(start, end, status);
            addInstances(instances);
        }

        public String toString() {
            StringBuilder toRet = new StringBuilder();
            toRet.append("start=").append(mStart);
            toRet.append(", end=").append(mEnd);
            toRet.append(", status=").append(mStatus);
            toRet.append(", invites=[");
            int i = 0;
            for (Instance instance : mInstances) {
                if (i > 0)
                    toRet.append(", ");
                i++;
                toRet.append(instance.toString());
            }
            toRet.append("]");
            return toRet.toString();
        }
        
        long mStart;
        long mEnd;
        Interval mNext = null;
        Interval mPrev = null;
        String mStatus;
        LinkedHashSet<Instance> mInstances;  // invites relevant to this interval
                                             // LinkedHashSet rather than generic
                                             // set to preserve insertion order

        void insertAfter(Interval other) {
            other.mNext = mNext;
            other.mPrev = this;
            if (mNext != null) {
                mNext.mPrev = other;
            }
            mNext = other;
        }
        
        void removeNext() {
            mNext = mNext.getNext();
            if (mNext != null) {
                mNext.mPrev = this;
            }
        }

        void addInstances(LinkedHashSet<Instance> instances) {
            if (instances != null)
                mInstances.addAll(instances);
        }

        // Set this.mStatus to the "busier" of this.mStatus and otherStatus.
        void combineStatus(String otherStatus) {
            mStatus = chooseBusier(mStatus, otherStatus);
        }
        
        public long getStart() { return mStart; }
        public long getEnd() { return mEnd; }
        public String getStatus() { return mStatus; }
        public LinkedHashSet<Instance> getInstances() { return mInstances; }

        public boolean overlapsOrAbuts(Interval other) {
//            return (other.mEnd > mStart && other.mStart < mEnd);  
            return (other.mEnd >= mStart && other.mStart < mEnd);  
        }
        public boolean hasPrev() { return mPrev!= null; }
        public Interval getPrev() { return mPrev; }
        public boolean hasNext() { return mNext != null; }
        public Interval getNext() { return mNext; }
    }

    public String getBusiest() {
        String val = IcalXmlStrMap.FBTYPE_FREE;
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Interval interval = (Interval) iter.next();
            val = chooseBusier(val, interval.getStatus());
        }
        return val;
    }

    /**
     * Returns all invites (and therefore appointments) that caused non-free
     * times.
     * @return
     */
    public LinkedHashSet<Instance> getAllInstances() {
        LinkedHashSet<Instance> instances = new LinkedHashSet<Instance>();
        for (Iterator iter = iterator(); iter.hasNext(); ) {
            Interval interval = (Interval) iter.next();
            instances.addAll(interval.getInstances());
        }
        return instances;
    }


    public static FreeBusy getFreeBusyList(Mailbox mbox,
                                          long start, long end)
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
        // Check if this account is an always-free calendar resource.
        Account acct = mbox.getAccount();
        if (acct instanceof CalendarResource) {
            CalendarResource resource = (CalendarResource) acct;
            if (resource.autoAcceptDecline() && !resource.autoDeclineIfBusy()) {
                IntervalList intervals = new IntervalList(start, end);
                return new FreeBusy(intervals);
            }
        }

        int exApptId = exAppt == null ? -1 : exAppt.getId();

        List<Folder> folders = mbox.getFolderList(null, DbMailItem.SORT_NONE);
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
        
        Collection<Appointment> appts = mbox.getAppointmentsForRange(null, start, end, Mailbox.ID_AUTO_INCREMENT, exFolders);

        IntervalList intervals = new IntervalList(start, end);

        for (Appointment cur : appts) {
            if (cur.getId() == exApptId)
                continue;

            // Move start time of expansion up by default instance's duration
            // to catch instances whose tail end overlap the F/B time window.
            Invite defInv = cur.getDefaultInviteOrNull();
            if (defInv == null)
                continue;
            long defInvStart = defInv.getStartTime().getDate().getTime();
            long defInvEnd = defInv.getEffectiveEndTime().getDate().getTime();
            long startAdjusted = start - (defInvEnd - defInvStart) + 1;

            Collection instances = cur.expandInstances(startAdjusted, end); 
            for (Iterator instIter = instances.iterator(); instIter.hasNext();) {
                Appointment.Instance inst = (Appointment.Instance)(instIter.next());
                assert(inst.getStart() < end && inst.getEnd() > start);
                InviteInfo invId = inst.getInviteInfo();
                try {
                    Appointment appt = inst.getAppointment();
                    Invite inv = appt.getInvite(invId);
                    if (!inv.isTransparent()) {
                        String freeBusy = appt.getEffectiveFreeBusyActual(inv, inst);
                        if (!IcalXmlStrMap.FBTYPE_FREE.equals(freeBusy)) {
                            Interval ival = new Interval(inst.getStart(), inst.getEnd(), freeBusy, inst);
                            intervals.addInterval(ival);
                        }
                    }
                } catch (MailServiceException.NoSuchItemException e) {
                    sLog.debug("Could not load invite "+invId.toString() + " for appt "+mbox.getId());
                }
            }
        }
        return new FreeBusy(intervals);
    }


    // FBTYPE values defined in iCalendar
    public static final String FBTYPE_FREE = "FREE";
    public static final String FBTYPE_BUSY = "BUSY";
    public static final String FBTYPE_BUSY_TENTATIVE = "BUSY-TENTATIVE";
    public static final String FBTYPE_BUSY_UNAVAILABLE = "BUSY-UNAVAILABLE";

    // FBTYPE values used by Microsoft Outlook
    public static final String FBTYPE_OUTLOOK_FREE = "FREE";
    public static final String FBTYPE_OUTLOOK_BUSY = "BUSY";
    public static final String FBTYPE_OUTLOOK_TENTATIVE = "TENTATIVE";
    public static final String FBTYPE_OUTLOOK_OUTOFOFFICE = "OOF";

    private static String sBusyOrder[] = new String[4];
    
    static {
        // The lower index, the busier. 
        sBusyOrder[0] = IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE;
        sBusyOrder[1] = IcalXmlStrMap.FBTYPE_BUSY;
        sBusyOrder[2] = IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE;
        sBusyOrder[3] = IcalXmlStrMap.FBTYPE_FREE;
    }

    public static String chooseBusier(String freeBusy1, String freeBusy2) {
        for (int i = 0; i < sBusyOrder.length; i++) {
            String busy = sBusyOrder[i];
            if (busy.equals(freeBusy1))
                return freeBusy1;
            if (busy.equals(freeBusy2))
                return freeBusy2;
        }
        if (freeBusy1 != null)
            return freeBusy1;
        else
            return freeBusy2;
    }


    public String toString() { return mList.toString(); };

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
            Mailbox mbox = Mailbox.getMailboxById(1);
            FreeBusy fb = getFreeBusyList(mbox, 0, Long.MAX_VALUE);
            System.out.println(fb.toString());
        } catch (ServiceException e){
            System.out.println("EXCEPTION: "+e);
            e.printStackTrace();
        }
            
    }
}
