/*
***** BEGIN LICENSE BLOCK *****
Version: ZPL 1.1

The contents of this file are subject to the Zimbra Public License
Version 1.1 ("License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.zimbra.com/license

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
the License for the specific language governing rights and limitations
under the License.

The Original Code is: Zimbra Collaboration Suite.

The Initial Developer of the Original Code is Zimbra, Inc.  Portions
created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
Reserved.

Contributor(s): 

***** END LICENSE BLOCK *****
*/

package com.zimbra.cs.mailbox.calendar;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Invite;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
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
public class FreeBusy {
    
    private static Log sLog = LogFactory.getLog(FreeBusy.class);
    
    private FreeBusy(IntervalList list) {
        mList = list;
    }
    IntervalList mList; 
    
    private static class IntervalIterator implements Iterator
    {
        private Interval mCur;
        private IntervalIterator(IntervalList list) {
            mCur = list.getHead();
        }
        public boolean hasNext() { return (mCur != null); }
        public Object next() { 
            Object toRet = mCur;
            mCur = mCur.getNext();
            return toRet;
        }
        public void remove() {
            throw new IllegalArgumentException("Unsupported");
        }
    }
    
    public Iterator /* Interval */ iterator() {
        return new IntervalIterator(mList);
    }
    
    private static class IntervalList {
        
        IntervalList(long start, long end) {
            mStart = start;
            mEnd = end;
            mHead = new Interval(start, end, IcalXmlStrMap.FBTYPE_FREE);
        }

        void AddInterval(Interval toAdd) 
        {
            assert(toAdd.mStart <= toAdd.mEnd);
            // we only care about intervals within our window!  
            if (toAdd.mStart < mStart) {
                toAdd.mStart = mStart;
            }
            if (toAdd.mEnd > mEnd) {
                toAdd.mEnd = mEnd;
            }
            
            // step 1: find previous the interval already in the list which overlaps
            //         the start of toAdd 
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
            if (!uberStart.overlapsOrAbuts(toAdd)) {
                assert(uberStart.overlapsOrAbuts(toAdd));
            }
            
            Interval cur = uberStart;
            
            while (toAdd.mStart < toAdd.mEnd) {
                assert(cur.mEnd >= cur.mStart);
                assert(cur.mStart <= toAdd.mStart);

                //      -- if some of cur is before toAdd, then split it off
                if (toAdd.mStart > cur.mStart) {
                    Interval newInt = new Interval(toAdd.mStart, cur.mEnd, cur.mStatus);
                    cur.insertAfter(newInt);
                    cur.mEnd = newInt.mStart;
                    cur = newInt;
                }
                
                //      -- if some of cur is AFTER toAdd then split it off
                if (toAdd.mEnd < cur.mEnd) {
                    Interval afterUs = new Interval(toAdd.mEnd, cur.mEnd, cur.mStatus);
                    cur.insertAfter(afterUs);
                    cur.mEnd = toAdd.mEnd;
                }
                
                // OK -- so now cur is some chunk of toAdd which we can use!
                cur.combineStatus(toAdd.mStatus);
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
        
        public String toString()
        {
            StringBuffer toRet = new StringBuffer("\n");
            for (Interval cur = mHead; cur != null; cur = cur.getNext())
            {
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
//                assert(false);
                mStatus = IcalXmlStrMap.FBTYPE_FREE;
            }
        }
        
        public String toString() 
        {
            StringBuffer toRet = new StringBuffer();
            toRet.append("s=").append(mStart);
            toRet.append(" ");
            toRet.append("e=").append(mEnd);
            toRet.append(" ");
            toRet.append(mStatus);
            return toRet.toString();
        }
        
        long mStart;
        long mEnd;
        Interval mNext = null;
        Interval mPrev = null;
        String mStatus;
        
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

        // Set this.mStatus to the "busier" of this.mStatus and otherStatus.
        void combineStatus(String otherStatus) {
            mStatus = chooseBusier(mStatus, otherStatus);
        }
        
        public long getStart() { return mStart; }
        public long getEnd() { return mEnd; }
        public String getStatus() { return mStatus; }
        
        public boolean overlapsOrAbuts(Interval other) {
//            return (other.mEnd > mStart && other.mStart < mEnd);  
            return (other.mEnd >= mStart && other.mStart < mEnd);  
        }
        public boolean hasPrev() { return mPrev!= null; }
        public Interval getPrev() { return mPrev; }
        public boolean hasNext() { return mNext != null; }
        public Interval getNext() { return mNext; }
    }

    
    public static FreeBusy getFreeBusyList(Mailbox mbx, long start, long end) throws ServiceException {
        Collection appts = mbx.getAppointmentsForRange(start, end);
        
        IntervalList intervals = new IntervalList(start, end);
        
        for (Iterator iter = appts.iterator(); iter.hasNext();) {
            Appointment cur = (Appointment)iter.next();
            
            Collection instances = cur.expandInstances(start, end); 
            for (Iterator instIter = instances.iterator(); instIter.hasNext();) {
                Appointment.Instance inst = (Appointment.Instance)(instIter.next());
                assert(inst.getStart() < end && inst.getEnd() > start);
                InviteInfo invId = inst.getInviteInfo();
                try {
                    Appointment appt = mbx.getAppointmentById(inst.getAppointmentId());
                    Invite inv = appt.getInvite(invId);
                    if (!inv.isTransparent()) {
                        String freeBusy = inv.getFreeBusyActual();
                        Interval ival = new Interval(inst.getStart(), inst.getEnd(), freeBusy);
                        intervals.AddInterval(ival);
                    }
                } catch (MailServiceException.NoSuchItemException e) {
                    sLog.debug("Could not load invite "+invId.toString() + " for appt "+mbx.getId());
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
        l.AddInterval(toAdd);
        System.out.println("Added: "+toAdd+l.toString());
        toAdd = new Interval(10, 20, IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE);
        l.AddInterval(toAdd);
        System.out.println("Added: "+toAdd+l.toString());
        toAdd = new Interval(20, 30, IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE);
        l.AddInterval(toAdd);
        System.out.println("Added: "+toAdd+l.toString());
        toAdd = new Interval(15, 35, IcalXmlStrMap.FBTYPE_BUSY);
        l.AddInterval(toAdd);
        System.out.println("Added: "+toAdd+l.toString());

        try {
            Mailbox mbx = Mailbox.getMailboxById(1);
            FreeBusy fb = getFreeBusyList(mbx, 0, Long.MAX_VALUE);
            System.out.println(fb.toString());
        } catch (ServiceException e){
            System.out.println("EXCEPTION: "+e);
            e.printStackTrace();
        }
            
    }
    
    
}
