/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2016 Synacor, Inc.
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

package com.zimbra.cs.fb;

import java.io.IOException;
import java.io.StringWriter;
import java.text.ParseException;
import java.util.*;

import com.zimbra.common.calendar.ICalTimeZone;
import com.zimbra.common.calendar.ParsedDateTime;
import com.zimbra.common.calendar.TimeZoneMap;
import com.zimbra.common.calendar.ZCalendar;
import com.zimbra.common.calendar.ZCalendar.ICalTok;
import com.zimbra.common.calendar.ZCalendar.ZComponent;
import com.zimbra.common.calendar.ZCalendar.ZParameter;
import com.zimbra.common.calendar.ZCalendar.ZProperty;
import com.zimbra.common.localconfig.LC;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.ldap.LdapUtil;
import com.zimbra.cs.mailbox.calendar.IcalXmlStrMap;
import com.zimbra.cs.mailbox.calendar.Invite;
import com.zimbra.cs.mailbox.calendar.Period;
import com.zimbra.cs.mailbox.calendar.ZOrganizer;

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
	private String mName;
	private long mStart;
    private long mEnd;
    protected IntervalList mList; 

	// free from start to end
	public static FreeBusy emptyFreeBusy(String name, long start, long end) {
		return new FreeBusy(name, start, end);
	}
	// unknown (no data) from start to end
    public static FreeBusy nodataFreeBusy(String name, long start, long end) {
        IntervalList il = new IntervalList(start, end);
        if (!LC.freebusy_disable_nodata_status.booleanValue())
            il.addInterval(new Interval(start, end, IcalXmlStrMap.FBTYPE_NODATA));
        return new FreeBusy(name, il, start, end);
    }
    protected FreeBusy(String name, long start, long end) {
    	this(name, new IntervalList(start, end), start, end);
    }
    protected FreeBusy(String name, long start, long end, String fbStatus) {
        this(name, new IntervalList(start, end, fbStatus), start, end);
    }
    
    protected FreeBusy(String name, IntervalList list, long start, long end) {
    	mName = name;
        mList = list;
        mStart = start;
        mEnd = end;
    }

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
    
    public String getName() {
    	return mName;
    }
    
    public Iterator<Interval> iterator() {
        return new IntervalIterator(mList);
    }
    
    protected static class IntervalList implements Iterable<Interval> {
        
        public Iterator<Interval> iterator() {
        	return new IntervalIterator(this);
        }
        
        IntervalList(long start, long end) {
            mStart = start;
            mEnd = end;
            mHead = new Interval(start, end, IcalXmlStrMap.FBTYPE_FREE);
        }
        
        IntervalList(long start, long end, String fbStatus) {
            mStart = start;
            mEnd = end;
            mHead = new Interval(start, end, fbStatus);
        }

        public void addInterval(Interval toAdd) {
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
                    cur.mInstances.addAll(cur.getNext().mInstances);
                    cur.removeNext();
                } else {
                    cur = cur.getNext();                    
                }
            }
            
//            System.out.println("AFTER combining: "+toString());
            
        }

        public String toString() {
            StringBuilder toRet = new StringBuilder("\n");
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
      //Below parameters are added to support Detailed and FreeBusy view response.
        String id;
        String location;
        String subject;
        boolean isMeeting;
        boolean isRecurring;
        boolean isException;
        boolean isReminderSet;
        boolean isPrivate;
        boolean hasPermission = true;
        boolean detailsExist = false;

        public Interval(long start, long end, String status) {
            mStart = start;
            mEnd = end;
            assert(end >= start);
            if (status != null) {
                mStatus = status;
            } else {
                mStatus = IcalXmlStrMap.FBTYPE_FREE;
            }
            mInstances = new LinkedHashSet<FBInstance>();
        }

        public Interval(long start, long end, String status, FBInstance instance) {
            this(start, end, status);
            if (instance != null)
                mInstances.add(instance);
        }

        public Interval(long start, long end, String status, LinkedHashSet<FBInstance> instances) {
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
            for (FBInstance instance : mInstances) {
                if (i > 0)
                    toRet.append(", ");
                i++;
                toRet.append(instance.toString());
            }
            toRet.append("]");
            return toRet.toString();
        }

        public String getId() {
            return this.id;
        }

        public String getLocation() {
            return this.location;
        }

        public String getSubject() {
            return this.subject;
        }

        public boolean isMeeting() {
            return this.isMeeting;
        }

        public boolean isRecurring() {
            return this.isRecurring;
        }

        public boolean isException() {
            return this.isException;
        }

        public boolean isReminderSet() {
            return this.isReminderSet;
        }

        public boolean isPrivate() {
            return this.isPrivate;
        }

        public boolean isHasPermission() {
            return this.hasPermission;
        }

        public boolean isDetailsExist() {
            return this.detailsExist;
        }

        long mStart;
        long mEnd;
        Interval mNext = null;
        Interval mPrev = null;
        String mStatus;
        LinkedHashSet<FBInstance> mInstances;  // instances relevant to this interval
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

        void addInstances(LinkedHashSet<FBInstance> instances) {
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
        public LinkedHashSet<FBInstance> getInstances() { return mInstances; }

        public boolean overlapsOrAbuts(Interval other) {
//            return (other.mEnd > mStart && other.mStart < mEnd);  
            return (other.mEnd >= mStart && other.mStart <= mEnd);  
        }
        public boolean hasPrev() { return mPrev!= null; }
        public Interval getPrev() { return mPrev; }
        public boolean hasNext() { return mNext != null; }
        public Interval getNext() { return mNext; }

        void setStatus(String status) { mStatus = status; }
    }

    public String getBusiest() {
        String val = IcalXmlStrMap.FBTYPE_FREE;
        for (Iterator<Interval> iter = iterator(); iter.hasNext(); ) {
            Interval interval = iter.next();
            val = chooseBusier(val, interval.getStatus());
        }
        return val;
    }



    // FBTYPE values defined in iCalendar
    public static final String FBTYPE_FREE = "FREE";
    public static final String FBTYPE_BUSY = "BUSY";
    public static final String FBTYPE_BUSY_TENTATIVE = "BUSY-TENTATIVE";
    public static final String FBTYPE_BUSY_UNAVAILABLE = "BUSY-UNAVAILABLE";
    public static final String FBTYPE_NODATA = "X-ZIMBRA-FREEBUSY-NODATA";

    // FBTYPE values used by Microsoft Outlook
    public static final String FBTYPE_OUTLOOK_FREE = "FREE";
    public static final String FBTYPE_OUTLOOK_BUSY = "BUSY";
    public static final String FBTYPE_OUTLOOK_TENTATIVE = "TENTATIVE";
    public static final String FBTYPE_OUTLOOK_OUTOFOFFICE = "OOF";

    private static String sBusyOrder[] = new String[5];
    
    static {
        // The lower index, the busier. 
        sBusyOrder[0] = IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE;
        sBusyOrder[1] = IcalXmlStrMap.FBTYPE_BUSY;
        sBusyOrder[2] = IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE;
        sBusyOrder[3] = IcalXmlStrMap.FBTYPE_NODATA;
        sBusyOrder[4] = IcalXmlStrMap.FBTYPE_FREE;
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

    public enum Method {
    	PUBLISH, REQUEST, REPLY
    }

    private static final String NL = "\r\n";
    private static final String MAILTO = "mailto:";
    private static final String HTTP = "http:";

    /*
     * attendee is required for METHOD == REQUEST || METHOD == REPLY
     * url is required for METHOD == PUBLISH || METHOD == REPLY
     * 
     */
    public String toVCalendar(Method m, String organizer, String attendee, String url) {
    	if (m == null || organizer == null) {
    		throw new IllegalArgumentException("missing method or organizer");
    	}
		ParsedDateTime now = ParsedDateTime.fromUTCTime(System.currentTimeMillis());
		ParsedDateTime startTime = ParsedDateTime.fromUTCTime(mStart);
		ParsedDateTime endTime = ParsedDateTime.fromUTCTime(mEnd);
		
		StringBuilder toRet = new StringBuilder("BEGIN:VCALENDAR").append(NL);
		toRet.append("PRODID:").append(ZCalendar.sZimbraProdID).append(NL);
        toRet.append("VERSION:").append(ZCalendar.sIcalVersion).append(NL);
        toRet.append("METHOD:").append(m.name()).append(NL);
		toRet.append("BEGIN:VFREEBUSY").append(NL);

		toRet.append("ORGANIZER:");
		if (!organizer.toLowerCase().startsWith(MAILTO) && !organizer.toLowerCase().startsWith(HTTP))
			toRet.append(MAILTO);
		toRet.append(organizer).append(NL);
		if (attendee != null) {
			toRet.append("ATTENDEE:");
			if (!attendee.toLowerCase().startsWith(MAILTO) && !attendee.toLowerCase().startsWith(HTTP))
				toRet.append(MAILTO);
			toRet.append(attendee).append(NL);
		}
		toRet.append("DTSTAMP:").append(now.toString()).append(NL);
		toRet.append("DTSTART:").append(startTime.toString()).append(NL);
		toRet.append("DTEND:").append(endTime.toString()).append(NL);
		if (url != null)
			toRet.append("URL:").append(url).append(NL);


//		BEGIN:VFREEBUSY
//		ORGANIZER:jsmith@host.com
//		DTSTART:19980313T141711Z
//		DTEND:19980410T141711Z
//		FREEBUSY:19980314T233000Z/19980315T003000Z
//		FREEBUSY:19980316T153000Z/19980316T163000Z
//		FREEBUSY:19980318T030000Z/19980318T040000Z
//		URL:http://www.host.com/calendar/busytime/jsmith.ifb
//		END:VFREEBUSY


		for (Iterator<Interval> iter = this.iterator(); iter.hasNext(); ) {
			FreeBusy.Interval cur = iter.next();
			String status = cur.getStatus();

			if (status.equals(IcalXmlStrMap.FBTYPE_FREE)) {
				continue;
            } else if (status.equals(IcalXmlStrMap.FBTYPE_NODATA)) {
                // Treat no-data case same as free, because other apps probably won't understand a non-standard fbtype value.
                continue;
			} else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY)) {
				toRet.append("FREEBUSY;FBTYPE=BUSY:");   // default is BUSY, but let's be explicit about it and set FBTYPE to BUSY
			} else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE)) {
				toRet.append("FREEBUSY;FBTYPE=BUSY-TENTATIVE:");
			} else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE)) {
				toRet.append("FREEBUSY;FBTYPE=BUSY-UNAVAILABLE:");
			} else {
				assert(false);
				toRet.append(":");
			}

			ParsedDateTime curStart = ParsedDateTime.fromUTCTime(cur.getStart());
			ParsedDateTime curEnd = ParsedDateTime.fromUTCTime(cur.getEnd());

			toRet.append(curStart.toString()).append('/').append(curEnd.toString()).append(NL);
		}

		toRet.append("END:VFREEBUSY").append(NL);
		toRet.append("END:VCALENDAR").append(NL);
		return toRet.toString();
    }

    public String toVCalendarAsVEvents() throws ServiceException {
        StringWriter writer = new StringWriter();
        String publish = ICalTok.PUBLISH.toString();
        long now = System.currentTimeMillis();

        writer.append("BEGIN:VCALENDAR").append(NL);
        writer.append("PRODID:").append(ZCalendar.sZimbraProdID).append(NL);
        writer.append("VERSION:").append(ZCalendar.sIcalVersion).append(NL);
        writer.append("METHOD:").append(publish).append(NL);

        String uidBase = "tmp_" + LdapUtil.generateUUID() + "_";
        int uidCount = 0;
        TimeZoneMap tzMap = new TimeZoneMap(ICalTimeZone.getUTC());
        for (Iterator<Interval> iter = this.iterator(); iter.hasNext(); ) {
            FreeBusy.Interval cur = iter.next();
            String status = cur.getStatus();

            if (status.equals(IcalXmlStrMap.FBTYPE_FREE)) {
                continue;
            } else if (status.equals(IcalXmlStrMap.FBTYPE_NODATA)) {
                // Treat no-data case same as free, because other apps probably won't understand a non-standard fbtype value.
                continue;
            }

            Invite inv = new Invite(publish, tzMap, true);
            inv.setUid(uidBase + (++uidCount));
            inv.setSeqNo(0);
            inv.setDtStamp(now);
            inv.setDtStart(ParsedDateTime.fromUTCTime(cur.getStart()));
            inv.setDtEnd(ParsedDateTime.fromUTCTime(cur.getEnd()));
            inv.setFreeBusy(status);
            if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_TENTATIVE)) {
                inv.setStatus(IcalXmlStrMap.STATUS_TENTATIVE);
            } else if (status.equals(IcalXmlStrMap.FBTYPE_BUSY_UNAVAILABLE)) {
                inv.setStatus(IcalXmlStrMap.STATUS_CONFIRMED);
            } else {  // busy
                inv.setStatus(IcalXmlStrMap.STATUS_CONFIRMED);
            }

            ZComponent comp = inv.newToVComponent(false, true);
            try {
                comp.toICalendar(writer);
            } catch (IOException e) {
                throw ServiceException.FAILURE("can't write iCalendar object", e);
            }
        }
        writer.append("END:VCALENDAR").append(NL);
        return writer.toString();
    }

    public String toString() { return mList.toString(); };
    
    public long getStartTime() { return mStart; }
    public long getEndTime() { return mEnd; }

    public static class FBInstance implements Comparable<FBInstance> {
        private long mStartTime;
        private long mEndTime;
        private int mApptId;
        private long mRecurIdDt;
        private String mFreeBusy;

        public FBInstance(String fb, long start, long end, int apptId, long recurIdDt) {
            mFreeBusy = fb;
            mStartTime = start;
            mEndTime = end;
            mApptId = apptId;
            mRecurIdDt = recurIdDt;
        }

        public long getStartTime()  { return mStartTime; }
        public long getEndTime()    { return mEndTime; }
        public int getApptId()      { return mApptId; }
        public long getRecurIdDt()  { return mRecurIdDt; }
        public String getFreeBusy() { return mFreeBusy; }

        public int compareTo(FBInstance other) {
            long startDiff = mStartTime - other.mStartTime;
            if (startDiff != 0)
                return (startDiff > 0) ? 1 : -1;
            long endDiff = mEndTime - other.mEndTime;
            if (endDiff != 0)
                return (endDiff > 0) ? 1 : -1;
            int idDiff = mApptId - other.mApptId;
            if (idDiff != 0)
                return idDiff;
            long ridDiff = mRecurIdDt - other.mRecurIdDt;
            if (ridDiff != 0)
                return (ridDiff > 0) ? 1 : -1;
            return mFreeBusy.compareTo(other.mFreeBusy);
        }

        public boolean equals(Object o) {
            if (!(o instanceof FBInstance)) {
                return false;
            }

            FBInstance other = (FBInstance) o;
            return (mStartTime == other.mStartTime) && (mEndTime == other.mEndTime) &&
                   (mApptId == other.mApptId) && (mRecurIdDt == other.mRecurIdDt) &&
                   (mFreeBusy.equals(other.mFreeBusy));
        }
    }

    /**
     * Create a FreeBusy object from VFREEBUSY ZComponent.
     * @param comp
     * @return
     * @throws ServiceException
     */
    public static FreeBusy parse(ZComponent comp) throws ServiceException {
        String name = null;
        ParsedDateTime dtStart = null;
        ParsedDateTime dtEnd = null;
        List<Interval> intervals = new ArrayList<Interval>();
        TimeZoneMap tzmap = new TimeZoneMap(ICalTimeZone.getUTC());
        Iterator<ZProperty> propIter = comp.getPropertyIterator();
        while (propIter.hasNext()) {
            ZProperty prop = propIter.next();
            ICalTok tok = prop.getToken();
            if (tok == null)
                continue;
            switch (tok) {
            case DTSTART:
                try {
                    dtStart = ParsedDateTime.parse(prop, tzmap);
                } catch (ParseException e) {
                    throw ServiceException.INVALID_REQUEST("bad DTSTART: " + prop.toString(), e);
                }
                break;
            case DTEND:
                try {
                    dtEnd = ParsedDateTime.parse(prop, tzmap);
                } catch (ParseException e) {
                    throw ServiceException.INVALID_REQUEST("bad DTEND: " + prop.toString(), e);
                }
                break;
            case ORGANIZER:
                ZOrganizer att = new ZOrganizer(prop);
                name = att.getAddress();
                break;
            case FREEBUSY:
                String fbStatus = IcalXmlStrMap.FBTYPE_FREE;
                ZParameter fbType = prop.getParameter(ICalTok.FBTYPE);
                if (fbType != null)
                    fbStatus = IcalXmlStrMap.sFreeBusyMap.toXml(fbType.getValue());
                List<String> vals = prop.getValueList();
                for (String fbVal : vals) {
                    Period period;
                    try {
                        period = Period.parse(fbVal, ICalTimeZone.getUTC(), tzmap);
                    } catch (ParseException e) {
                        throw ServiceException.INVALID_REQUEST("bad period value: " + fbVal, e);
                    }
                    intervals.add(new Interval(period.getStart().getUtcTime(), period.getEnd().getUtcTime(), fbStatus));
                }
                break;
            }
        }

        if (name == null)
            throw ServiceException.INVALID_REQUEST("VFREEBUSY missing ORGANIZER", null);
        if (dtStart == null || dtEnd == null)
            throw ServiceException.INVALID_REQUEST("VFREEBUSY missing DTSTART/DTEND", null);

        IntervalList ivalList = new IntervalList(dtStart.getUtcTime(), dtEnd.getUtcTime());
        for (Interval ival : intervals) {
            ivalList.addInterval(ival);
        }
        return new FreeBusy(name, ivalList, dtStart.getUtcTime(), dtEnd.getUtcTime());
    }
}
