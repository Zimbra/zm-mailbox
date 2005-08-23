/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

package com.zimbra.cs.mailbox.calendar;

import java.util.*;

import net.fortuna.ical4j.model.NumberList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.WeekDayList;

import java.text.ParseException;

import com.zimbra.cs.util.ZimbraLog;
import com.zimbra.cs.util.ListUtil;
import com.zimbra.cs.mailbox.Appointment;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Metadata;
import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.mail.MailService;
import com.zimbra.cs.service.ServiceException;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.util.DateTimeFormat;


public class Recurrence 
{
    
    /**
     * @author tim
     *
     * Represents a full iCal recurrence ruleset -- including exceptions, etc.
     * 
     * The concrete subclasses are:
     *    SingleInstanceRule: a 1-time event
     *    
     *    SimpleRepeatingRule: anything that can be expressed in a single rule...RECUR in the iCal RFC
     *    
     *    RecurrenceRule: Rule which fully expresses iCal grammar (support Exceptions, EXRULE, etc)
     *    
     *    ExceptionRule: rule that has a RECURRENCE_ID which is used to determine when it applies
     * 
     */
    public interface IRecurrence extends Cloneable {
        public Metadata encodeMetadata();
        
        abstract List /* Instance */ expandInstances(Appointment appt, long start, long end);
        
        // get the first time for which the rule has instances
        public ParsedDateTime getStartTime();
        // get the last time (-1 means forever) for which the rule has instances 
        public ParsedDateTime getEndTime();

        public Object clone();
        
        abstract public Element toXml(Element parent);
    }
    
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////
    
    
    public static IRecurrence decodeRule(Mailbox mbox, Metadata meta, TimeZoneMap tzmap) 
    throws ServiceException {
        try {
            int ruleType = (int) meta.getLong(FN_RULE_TYPE);
            
            switch (ruleType) {
            case RULE_SIMPLE_REPEATING_RULE:
                return new SimpleRepeatingRule(mbox, meta, tzmap);
            case RULE_EXCEPTION_RULE:
                return new ExceptionRule(mbox, meta, tzmap);
            case RULE_RECURRENCE_RULE:
                return new RecurrenceRule(mbox, meta, tzmap);
            case RULE_SINGLE_INSTANCE:
                return new SingleInstanceRule(mbox, meta, tzmap);
            }
        } catch (ParseException e) {
            throw ServiceException.FAILURE("Parse excetion on metadata: " + meta, e);
        }
        throw new IllegalArgumentException("Unknown IRecur type: " + meta.get(FN_RULE_TYPE));
    } 

    static final String FN_RULE_TYPE = "t";
    static final int RULE_SIMPLE_REPEATING_RULE = 2;
    static final int RULE_EXCEPTION_RULE = 3;
    static final int RULE_RECURRENCE_RULE = 4;
    static final int RULE_SINGLE_INSTANCE = 5;
    
    /**
     * @author tim
     * 
     * Helper class -- basically just an ArrayList of subrules
     *
     */
    public static class MultiRuleSorter
    {
        static private final String FN_NUM_RULES = "nr";
        static private final String FN_RULE = "r";
        
        public Metadata encodeMetadata() {
            Metadata meta = new Metadata();
            meta.put(FN_NUM_RULES, mRules.size());
            for (int i = 0; i < mRules.size(); i++)
                meta.put(FN_RULE + i, ((IRecurrence) mRules.get(i)).encodeMetadata());
            return meta;
        }

        public Object clone() {
            ArrayList newRules = new ArrayList();
            for (Iterator iter = mRules.iterator(); iter.hasNext();) {
                IRecurrence rule = (IRecurrence) iter.next();
                newRules.add(rule.clone());
            }
            return new MultiRuleSorter(newRules);
        }
        
        public MultiRuleSorter(Mailbox mbox, Metadata meta, TimeZoneMap tzmap) throws ServiceException {
            int numRules = (int) meta.getLong(FN_NUM_RULES);
            mRules = new ArrayList(numRules);
            for (int i = 0; i < numRules; i++)
                mRules.add(Recurrence.decodeRule(mbox, meta.getMap(FN_RULE + i), tzmap));
        }
        
        public MultiRuleSorter(ArrayList /* IRecur */ rules) {
            mRules = rules;
        }

        public Element toXml(Element parent) {
            for (Iterator iter = mRules.iterator(); iter.hasNext();) {
                IRecurrence cur = (IRecurrence)iter.next();
                parent.addElement(cur.toXml(parent));
            }
            return parent;
        }

        public List /* Instance */ expandInstances(Appointment appt, long start, long end) {
            List /* Appointment.Instance */ lists[] = new ArrayList[mRules.size()];
            int num = 0;
            for (Iterator iter = mRules.iterator(); iter.hasNext();) {
                IRecurrence cur = (IRecurrence)iter.next();
                lists[num] = cur.expandInstances(appt, start, end);
                num++;
            }
            
            List toRet = new LinkedList();
            ListUtil.mergeSortedLists(toRet, lists, true);
            return toRet;
        }
        
        public String toString() {
            StringBuffer toRet = new StringBuffer();
            toRet.append("(");
            for (Iterator iter = mRules.iterator();iter.hasNext();) {
                IRecurrence rule  = (IRecurrence)iter.next();
                toRet.append(rule.toString());
            } 
            toRet.append(")");
            return toRet.toString();
        }
        
        public ParsedDateTime getStartTime() {
            ParsedDateTime earliestStart = null;
            for (Iterator iter = mRules.iterator(); iter.hasNext();) {
                IRecurrence cur = (IRecurrence)iter.next();
                ParsedDateTime start = cur.getStartTime();
                if (earliestStart == null || (start.compareTo(earliestStart) < 0)) {
                    earliestStart = start;
                }
            }
            return earliestStart;
        }
        
        public ParsedDateTime getEndTime() {
            ParsedDateTime latestEnd = null;
            for (Iterator iter = mRules.iterator(); iter.hasNext();) {
                IRecurrence cur = (IRecurrence)iter.next();
                ParsedDateTime end = cur.getEndTime();
                if (latestEnd == null || (end.compareTo(latestEnd)>0)) {
                    latestEnd = end;
                }
            }
            return latestEnd;
        }
        
        private ArrayList /* IRecur */ mRules;
        
    }
    
    public static class SingleInstanceRule implements IRecurrence {
        private static final String FN_DTSTART = "dts";
        private static final String FN_DURATION = "dur";
        private static final String FN_DTEND = "dte";
        private static final String FN_INVID = "inv";
        
        private ParsedDateTime getEnd()
        {
            if (mDtEnd == null) {
                return mDtStart.add(mDuration);
            }
            return mDtEnd;
        }

        public String toString() {
            StringBuffer toRet = new StringBuffer("[DtStart=").append(mDtStart.toString());
            if (mDuration != null) {
                toRet.append(" Dur=").append(mDuration.toString());
            }
            if (mDtEnd != null) {
                toRet.append(" DtEnd=").append(mDtEnd.toString());
            }
            toRet.append(" InvId=").append(mInvId.toString()).append("]");
            
            return toRet.toString();
        }
        
        public List /* Instance */ expandInstances(Appointment appt, long start, long end) {
            List toRet = new ArrayList();
            ParsedDateTime dtEnd = getEnd();
            toRet.add(new Appointment.Instance(appt, mInvId, mDtStart.getUtcTime(), dtEnd.getUtcTime(), false));
            return toRet;
        }
        
        public Metadata encodeMetadata() {
            Metadata meta = new Metadata();
         
            meta.put(FN_RULE_TYPE, RULE_SINGLE_INSTANCE);
//            meta.addAttribute(FN_DTSTART, mDtStart); 
//            meta.addAttribute(FN_DURATION, mDuration);
            meta.put(FN_DTSTART, mDtStart);
            meta.put(FN_DURATION, mDuration);
            meta.put(FN_DTEND, mDtEnd);
            meta.put(FN_INVID, mInvId.encodeMetadata());

            return meta;
        }
        
        public SingleInstanceRule(Mailbox mbox, Metadata meta, TimeZoneMap tzmap) 
        throws ServiceException, ParseException {
            mDtStart = ParsedDateTime.parse(meta.get(FN_DTSTART), tzmap);
            mDuration = ParsedDuration.parse(meta.get(FN_DURATION, null));
            mDtEnd = ParsedDateTime.parse(meta.get(FN_DTEND, null), tzmap);
            mInvId = InviteInfo.fromMetadata(meta.getMap(FN_INVID), tzmap);
        }
        
        public SingleInstanceRule(ParsedDateTime start, ParsedDuration duration, InviteInfo invId) {
            mDtStart = start;
            mDuration = duration;
            mInvId = invId;
            assert(mDtStart != null);
        }
        
        public SingleInstanceRule(ParsedDateTime start, ParsedDateTime end, InviteInfo invId) {
            mDtStart = start;
            mDtEnd = end;
            mInvId = invId;
            assert(mDtStart != null);
        }
        
        public SingleInstanceRule(ParsedDateTime start, ParsedDateTime end, ParsedDuration duration, InviteInfo invId) {
            mDtStart = start;
            mDtEnd = end;
            mDuration = duration;
            mInvId = invId;
            assert(mDtStart != null);
        }
        
        
        public Element toXml(Element parent) {
            Element elt = parent.addElement(MailService.E_APPT_DATE);
            elt.addAttribute(MailService.A_APPT_START_TIME, mDtStart.toString());
            return elt;
        }
        
        public ParsedDateTime getStartTime() {
            return mDtStart;
        }
        public ParsedDateTime getEndTime() {
            return mDtEnd;
        }
        
        public Object clone() {
            assert(mDtStart != null);
            return new SingleInstanceRule(mDtStart, mDtEnd, mDuration, mInvId);
        }
        
        private ParsedDateTime mDtStart;
        private ParsedDateTime mDtEnd;
        private ParsedDuration mDuration;
        private InviteInfo mInvId;

    }
    
    /**
     * @author tim
     *
     * The output of an RRULE or EXRULE rule -- corresponds to a RECUR value in an iCal specification
     *
     */
    public static class SimpleRepeatingRule implements IRecurrence {
        public SimpleRepeatingRule(ParsedDateTime dtstart, ParsedDuration duration, 
                Recur recur, InviteInfo invId)
        {
            mDtStart = dtstart;
            mRecur = recur;
            mInvId = invId;
            mDuration = duration;
        }
        
        public List /* Instance */ expandInstances(Appointment appt, long start, long end) {
            // net.fortuna.ical4j.model.DateTime(toString());
            net.fortuna.ical4j.model.DateTime dateStart = new net.fortuna.ical4j.model.DateTime(start);
            net.fortuna.ical4j.model.DateTime endDate = new net.fortuna.ical4j.model.DateTime(end);

            ArrayList toRet = null;

            try {
                List dateList = mRecur.getDates(mDtStart.iCal4jDate(), dateStart, endDate, Value.DATE_TIME);
                toRet = new ArrayList(dateList.size());
                
                int num = 0;
                for (Iterator iter = dateList.iterator(); iter.hasNext();) {
                    Date cur = (Date)iter.next();
                    long instStart = cur.getTime();
                    long instEnd = mDuration.addToDate(cur).getTime();
                    if (instStart < end && instEnd > start) {
                        toRet.add(num++, new Appointment.Instance(appt, mInvId, instStart, instEnd, false));
                    }
                }
            } catch (ServiceException se) {
                // Bugs 3172 and 3240.  Ignore recurrence rules with bad data.
                ZimbraLog.calendar.warn("ServiceException expanding recurrence rule: " + mRecur.toString(), se);
                toRet = new ArrayList();
            } catch (IllegalArgumentException iae) {
                // Bugs 3172 and 3240.  Ignore recurrence rules with bad data.
            	ZimbraLog.calendar.warn("Invalid recurrence rule: " + mRecur.toString(), iae);
                toRet = new ArrayList();
            }
            return toRet;
        }

        public Element toXml(Element parent) {
            Element rule = parent.addElement(MailService.E_APPT_RULE);

            // FREQ
            String freq = IcalXmlStrMap.sFreqMap.toXml(mRecur.getFrequency());
            rule.addAttribute(MailService.A_APPT_RULE_FREQ, freq);

            // UNTIL or COUNT
            Date untilDate = mRecur.getUntil();
            if (untilDate != null) {
                String d = DateTimeFormat.getInstance().format(untilDate);
                rule.addElement(MailService.E_APPT_RULE_UNTIL).
                    addAttribute(MailService.A_APPT_DATETIME, d);
            } else {
                int count = mRecur.getCount();
                if (count > 0) {
                    rule.addElement(MailService.E_APPT_RULE_COUNT).
                        addAttribute(MailService.A_APPT_RULE_COUNT_NUM, count);
                }
            }

            // INTERVAL
            int ival = mRecur.getInterval();
            if (ival > 0) {
                rule.addElement(MailService.E_APPT_RULE_INTERVAL).
                    addAttribute(MailService.A_APPT_RULE_INTERVAL_IVAL, ival);
            }

            // BYSECOND
            NumberList bySecond = mRecur.getSecondList();
            if (!bySecond.isEmpty()) {
            	rule.addElement(MailService.E_APPT_RULE_BYSECOND).
                    addAttribute(MailService.A_APPT_RULE_BYSECOND_SECLIST, bySecond.toString());
            }

            // BYMINUTE
            NumberList byMinute = mRecur.getMinuteList();
            if (!byMinute.isEmpty()) {
                rule.addElement(MailService.E_APPT_RULE_BYMINUTE).
                    addAttribute(MailService.A_APPT_RULE_BYMINUTE_MINLIST, byMinute.toString());
            }

            // BYHOUR
            NumberList byHour = mRecur.getHourList();
            if (!byHour.isEmpty()) {
                rule.addElement(MailService.E_APPT_RULE_BYHOUR).
                    addAttribute(MailService.A_APPT_RULE_BYHOUR_HRLIST, byHour.toString());
            }

            // BYDAY
            WeekDayList byDay = mRecur.getDayList();
            if (!byDay.isEmpty()) {
                Element bydayElt = rule.addElement(MailService.E_APPT_RULE_BYDAY);
                for (int i = 0; i < byDay.size(); i++) {
                	WeekDay wkday = (WeekDay) byDay.get(i);
                    Element wkdayElt = bydayElt.addElement(MailService.E_APPT_RULE_BYDAY_WKDAY);
                    int offset = wkday.getOffset();
                    if (offset != 0)
                        wkdayElt.addAttribute(MailService.A_APPT_RULE_BYDAY_WKDAY_ORDWK, offset);
                    wkdayElt.addAttribute(MailService.A_APPT_RULE_DAY, wkday.getDay());
                }
            }

            // BYMONTHDAY
            NumberList byMonthDay = mRecur.getMonthDayList();
            if (!byMonthDay.isEmpty()) {
                rule.addElement(MailService.E_APPT_RULE_BYMONTHDAY).
                    addAttribute(MailService.A_APPT_RULE_BYMONTHDAY_MODAYLIST, byMonthDay.toString());
            }

            // BYYEARDAY
            NumberList byYearDay = mRecur.getYearDayList();
            if (!byYearDay.isEmpty()) {
                rule.addElement(MailService.E_APPT_RULE_BYYEARDAY).
                    addAttribute(MailService.A_APPT_RULE_BYYEARDAY_YRDAYLIST, byYearDay.toString());
            }

            // BYWEEKNO
            NumberList byWeekNo = mRecur.getWeekNoList();
            if (!byWeekNo.isEmpty()) {
                rule.addElement(MailService.E_APPT_RULE_BYWEEKNO).
                    addAttribute(MailService.A_APPT_RULE_BYWEEKNO_WKLIST, byWeekNo.toString());
            }

            // BYMONTH
            NumberList byMonth = mRecur.getMonthList();
            if (!byMonth.isEmpty()) {
                rule.addElement(MailService.E_APPT_RULE_BYMONTH).
                    addAttribute(MailService.A_APPT_RULE_BYMONTH_MOLIST, byMonth.toString());
            }

            // BYSETPOS
            NumberList bySetPos = mRecur.getSetPosList();
            if (!bySetPos.isEmpty()) {
                rule.addElement(MailService.E_APPT_RULE_BYSETPOS).
                    addAttribute(MailService.A_APPT_RULE_BYSETPOS_POSLIST, bySetPos.toString());
            }

            // WKST
            String wkst = mRecur.getWeekStartDay();
            if (wkst != null) {
            	rule.addElement(MailService.E_APPT_RULE_WKST).
                    addAttribute(MailService.A_APPT_RULE_DAY, wkst);
            }

            // x-name
            Map xNames = mRecur.getExperimentalValues();
            for (Iterator iter = mRecur.getExperimentalValues().entrySet().iterator();
                 iter.hasNext(); ) {
            	Map.Entry entry = (Map.Entry) iter.next();
                Element xElt = rule.addElement(MailService.E_APPT_RULE_XNAME);
                xElt.addAttribute(MailService.A_APPT_RULE_XNAME_NAME, (String) entry.getKey());
                xElt.addAttribute(MailService.A_APPT_RULE_XNAME_VALUE, (String) entry.getValue());
            }

            return rule;
        }


        public ParsedDateTime getStartTime() {
            return mDtStart;
        }
        public ParsedDateTime getEndTime() {
            // FIXME: 
            
//            if (mCount > 0) {
//
//                resetIterator();
//                long latestTime = peekNextInstance().getEnd();
//                for (int i = 0; i < mCount && hasNextInstance(); i++) {
//                    getNextInstance();
//                }
//                return peekNextInstance().getEnd();
//            }
            return ParsedDateTime.MAX_DATETIME;
        }
        
//      /**
//      * @param rule
//      */
//     private void computeEndTime(RRule rule) {
//         // TODO Auto-generated method stub
//         Recur recur = rule.getRecur();
//         Date until = recur.getUntil();
//         if (until != null) {
//             mEnd = until.getTime();
//             return;
//         }
//         String freq = recur.getFrequency();
//         int interval = recur.getInterval();
//         if (interval == -1) {
//             interval = 1;
//         }
//         int count = recur.getCount();
//         if (Recur.DAILY.equals(freq)) {
//             java.util.Calendar cal = java.util.Calendar.getInstance();
//             cal.setTimeInMillis(mStart);
//             cal.add(java.util.Calendar.DAY_OF_YEAR, interval * (count - 1));
//             mEnd = cal.getTimeInMillis();
//             return;
//         }
//         // FIXME!
//         mEnd = mStart+1;
//     }
     
     
        
        
        public String toString() {
            StringBuffer toRet = new StringBuffer();
            toRet.append("RULE(FIRST=").append(mDtStart.getDate());
            toRet.append(",DUR=").append(mDuration);
            toRet.append(",RECUR=").append(mRecur.toString());
            return toRet.toString(); 
        }
        
        private static final String FN_DTSTART = "dts";
        private static final String FN_DURATION = "dur";
        private static final String FN_RECUR = "recur";
        private static final String FN_INVID = "inv";

        public Metadata encodeMetadata() {
            Metadata meta = new Metadata();
            meta.put(FN_RULE_TYPE, RULE_SIMPLE_REPEATING_RULE);
            meta.put(FN_DTSTART, mDtStart);
            meta.put(FN_DURATION, mDuration);
            meta.put(FN_RECUR, mRecur);
            meta.put(FN_INVID, mInvId.encodeMetadata());
            
            return meta;
        }
        
        public Object clone()
        {
            return new SimpleRepeatingRule(mDtStart, mDuration, mRecur, mInvId);
        }
        
        public SimpleRepeatingRule(Mailbox mbox, Metadata meta, TimeZoneMap tzmap) throws ServiceException {
//          mDtStart = meta.getAttributeLong(FN_DTSTART);
//          mDuration = meta.getAttributeLong(FN_DURATION);
            try {
                mDtStart = ParsedDateTime.parse(meta.get(FN_DTSTART), tzmap);
                mDuration = ParsedDuration.parse(meta.get(FN_DURATION));
            } catch (ParseException e) {
                throw ServiceException.FAILURE("ParseException ", e);
            }
            try {
                mRecur = new Recur(meta.get(FN_RECUR));
            } catch (ParseException e) {
                throw ServiceException.FAILURE("Error parsing RECUR for appointment: " + meta.get(FN_RECUR), e);
            }
            
            mInvId = InviteInfo.fromMetadata(meta.getMap(FN_INVID), tzmap);
        }

        // define the value
        private ParsedDateTime mDtStart;
        private Recur mRecur;
        private ParsedDuration mDuration;
        private InviteInfo mInvId;
    }    
    
    /**
     * @author tim
     * 
     * Base class: models a set of rules as
     *    DTSTART + (RRULEs + RDATEs - EXRULEs - EXDATEs)
     *    
     *    
     *  Not instantiated directly -- used to build either a Rule (which can have Exceptions) or 
     *  an ExceptionRule (ie component with a RECURRENCE-ID property)
     *
     */
    public static abstract class CompoundRuleBase implements IRecurrence {
        protected CompoundRuleBase(ParsedDateTime dtstart, ParsedDuration duration, InviteInfo invId, 
                ArrayList /*IRecur*/ addRules, 
                ArrayList /* IRecur */ subtractRules)
        {
            mDtStart = dtstart;
            mDuration = duration;
            mInvId = invId;
            mAddRules = new MultiRuleSorter(addRules);
            if (subtractRules.size() > 0) {
                mSubtractRules = new MultiRuleSorter(subtractRules);
            } else {
                mSubtractRules = null;
            }
        }
        
        protected CompoundRuleBase(ParsedDateTime dtstart, ParsedDuration duration, 
                InviteInfo invId)
        {
            mDtStart = dtstart;
            mDuration = duration;
            mInvId = invId;
            mAddRules = null;
            mSubtractRules = null;
        }
        
        public List /* Instance */ expandInstances(Appointment appt, long start, long end) 
        {
            if (mAddRules == null) {
                // trivial, just DtStart!
                List toRet = new ArrayList(1);
                long instStart = mDtStart.getUtcTime();
                long instEnd = mDtStart.add(mDuration).getUtcTime();
                if (instStart < end && instEnd > start) {
                    toRet.add(new Appointment.Instance(appt, mInvId, instStart, instEnd, false));
                }
                return toRet;
            }
            
            // start with the addrules
            List /* Instance */ addRules = mAddRules.expandInstances(appt, start, end);
            
            // subtract the SubRules
            List /* Instance */ subRules;
            if (mSubtractRules != null) {
                subRules = mSubtractRules.expandInstances(appt, start, end);
            } else {
                subRules = new ArrayList();
            }
            
            
            List toRet = ListUtil.subtractSortedLists(addRules, subRules);
            
            
            // ALWAYS include DTSTART -- by spec
            long firstStart = mDtStart.getUtcTime();
            long firstEnd = mDtStart.add(mDuration).getUtcTime();
            if (firstStart < end && firstEnd > start) {
                Appointment.Instance first = null;
                if (toRet.size() >0) {
                    first = (Appointment.Instance)toRet.get(0);
                }
                
                Appointment.Instance dtstartInst = new Appointment.Instance(appt, mInvId, mDtStart.getUtcTime(), mDtStart.add(mDuration).getUtcTime(), false); 
                if (first == null || first.compareTo(dtstartInst) != 0) {
                    assert(first == null || first.compareTo(dtstartInst) > 0); // first MUST be after dtstart!
                    toRet.add(0,dtstartInst);
                }
            }
                
            return toRet;
        }
        
        public Element toXml(Element parent) {
            // subclass should have already created <recur> or <except> element
            
//            parent.addAttribute(MailService.A_APPT_START_TIME, mDtStart.toString());
//            parent.addAttribute(MailService.A_APPT_DURATION, mDuration.toString());
//            parent.addAttribute(MailService.A_APPT_INV_ID, mInvId.mMsgId);
//            parent.addAttribute(MailService.A_APPT_COMPONENT_NUM, mInvId.mComponentId);
            
            if (mAddRules != null) {
                Element addElt = parent.addElement(MailService.E_APPT_ADD);
                mAddRules.toXml(addElt);
            }
            if (mSubtractRules != null) {
                Element excludeElt = parent.addElement(MailService.E_APPT_EXCLUDE);
                mSubtractRules.toXml(excludeElt);
            }
            return parent;
        }
        
        
        public ParsedDateTime getStartTime() {
            return mDtStart;
        }
        
        public ParsedDateTime getEndTime() {
            if (mAddRules != null) {
                return mAddRules.getEndTime(); // FIXME should take into account EXCEPTIONS?
            } else {
                return mDtStart.add(mDuration);
            }
        }
        
        static final String FN_DTSTART = "dts";
        static final String FN_DURATION = "duration";
        static final String FN_ADDRULES = "add";
        static final String FN_SUBRULES = "sub";
        static final String FN_INVID = "invid";
        
        public Metadata encodeMetadata() {
            Metadata meta = new Metadata();
            meta.put(FN_DTSTART, mDtStart);
            meta.put(FN_DURATION, mDuration);
            if (mAddRules != null)
                meta.put(FN_ADDRULES, mAddRules.encodeMetadata());
            if (mSubtractRules != null)
                meta.put(FN_SUBRULES, mSubtractRules.encodeMetadata());
            meta.put(FN_INVID, mInvId.encodeMetadata());

            return meta;
        }
        
        protected CompoundRuleBase(Mailbox mbox, Metadata meta, TimeZoneMap tzmap) 
        throws ServiceException, ParseException {
            mDtStart = ParsedDateTime.parse(meta.get(FN_DTSTART), tzmap);
            mDuration = ParsedDuration.parse(meta.get(FN_DURATION));

            Metadata metaRules = meta.getMap(FN_ADDRULES, true);
            if (metaRules != null) {
                mAddRules = new MultiRuleSorter(mbox, metaRules, tzmap);
            }
            Metadata metaSubrules = meta.getMap(FN_SUBRULES, true);
            if (metaSubrules != null) {
                mSubtractRules = new MultiRuleSorter(mbox, metaSubrules, tzmap);
            }

            mInvId = InviteInfo.fromMetadata(meta.getMap(FN_INVID), tzmap);
        }
        
        protected CompoundRuleBase(ParsedDateTime dtstart, ParsedDuration duration, 
                MultiRuleSorter addRules, MultiRuleSorter subtractRules, 
                InviteInfo invID) 
        {
            mDtStart = dtstart;
            mDuration = duration;
            mAddRules = addRules;
            mSubtractRules = subtractRules;
            mInvId = invID;
        }
        
        public String toString() {
            StringBuffer toRet = new StringBuffer();
            toRet.append("FIRST=").append(mDtStart.getDate());
            toRet.append(",DUR=").append(mDuration);
            if (mAddRules != null) {
                toRet.append("\n\t\tADD[").append(mAddRules.toString()).append("]");
            }
            if (mSubtractRules != null) {
                toRet.append("\n\t\tSUBTRACT[").append(mSubtractRules.toString()).append("]");
            }
            return toRet.toString(); 
        }

        public abstract Object clone();
        
        protected ParsedDateTime mDtStart;
        protected ParsedDuration mDuration;
        
        protected MultiRuleSorter mAddRules; // RRULE, RDATE
        protected MultiRuleSorter mSubtractRules; // RRULE, RDATE
        protected InviteInfo mInvId;
    }
    
    public static interface IException extends IRecurrence
    {
        boolean matches(long date);
    }
    
//    /**
//     * @author tim
//     * 
//     * Represents the range of instances specified by a RECURRENCE_ID iCalendar component
//     */
//    public static class RecurrenceRange
//    {
//        public static final int NOT_RANGE = 1;
//        public static final int THIS_AND_FUTURE = 2;
//        public static final int THIS_AND_PRIOR = 3;
//        
//        static final String FN_RANGE_TYPE = "rgtyp";
//        static final String FN_RECURRENCE_ID = "recurId";
//        
//        private ParsedDateTime mRecurrenceId;
//        private int mRangeType = NOT_RANGE;
//        
//        RecurrenceRange(ParsedDateTime recurrenceId, int rangeType) {
//            mRecurrenceId = recurrenceId;
//            assert(rangeType == NOT_RANGE || rangeType == THIS_AND_FUTURE || rangeType == THIS_AND_PRIOR);
//            mRangeType = rangeType;
//        }
//        
//        public Metadata encodeMetadata()
//        {
//            Metadata meta = new Metadata();
//            
//            meta.put(FN_RECURRENCE_ID, mRecurrenceId);
//            meta.put(FN_RANGE_TYPE, Integer.toString(mRangeType));
//            
//            return meta;
//        }
//        
//        protected RecurrenceRange(Metadata meta, TimeZoneMap tzMap) throws ServiceException, ParseException
//        {
//            mRecurrenceId = ParsedDateTime.parse(meta.get(FN_RECURRENCE_ID), tzMap);
//            mRangeType = (int)meta.getLong(FN_RANGE_TYPE);
//        }
//        
//        public boolean matches(long date) {
//            // TODO we don't currently handle ranges!
//            return (mRecurrenceId.compareTo(date) == 0);
//        }
//        
//        
//        public String toString() {
//            StringBuffer toRet = new StringBuffer();
//            switch (mRangeType) {
//            case NOT_RANGE:
//                toRet.append("(RECURRENCE-ID=").append(mRecurrenceId.toString()).append(")");
//                break;
//            case THIS_AND_FUTURE:
//                toRet.append("(RECURRENCE-ID=RANGE:THISANDFUTURE;").append(mRecurrenceId.toString()).append(")");
//                break;
//            case THIS_AND_PRIOR:
//                toRet.append("(RECURRENCE-ID=RANGE:THISANDPRIOR;").append(mRecurrenceId.toString()).append(")");
//                break;
//            }
//            return toRet.toString(); 
//        }
//    }
    
    /**
     * @author tim
     * 
     * an RFC2446 CANCEL request to cancel an instance or range of instances
     * 
     * basically, this intercepts one or more Recurrence instances, and replaces them with nothing
     */
    public static class CancellationRule implements IException
    {
        static private final String FN_RECURRENCE_ID = "recurId";
        
        private RecurId mRecurRange;
        
        public CancellationRule(RecurId recurId) {
            mRecurRange = recurId;
        }
        
        CancellationRule(Mailbox mbx, Metadata meta, TimeZoneMap tzmap) 
        throws ServiceException, ParseException
        {
            mRecurRange = RecurId.decodeMetadata(meta.getMap(FN_RECURRENCE_ID), tzmap);
        }
        
        public Metadata encodeMetadata() {
            Metadata meta = new Metadata();
            
            meta.put(FN_RECURRENCE_ID, mRecurRange.encodeMetadata());
            return meta;
        }
        
        public List /* Instance */ expandInstances(Appointment appt, long start, long end) {
            return new ArrayList(); // NONE!
        }
        
        public ParsedDateTime getStartTime() {
            return null;
        }
        
        public ParsedDateTime getEndTime() {
            return null;
        }

        public Object clone() {
            return new CancellationRule(mRecurRange);
        }
        
        public Element toXml(Element parent) {
            Element elt = parent.addElement(MailService.E_APPT_CANCELLATION);
            mRecurRange.toXml(elt);
            
            return elt;
        }
        
        public boolean matches(long date) {
            return mRecurRange.withinRange(date);
        }
    }
    
    
    /**
     * @author tim
     *
     * A recurrence-rule that has a RECURRENCE-ID and can tell you if it applies
     * or not
     */
    public static class ExceptionRule extends CompoundRuleBase implements IException
    {
        public ExceptionRule(RecurId recurrenceId, 
                ParsedDateTime dtstart, ParsedDuration duration, 
                InviteInfo invId, 
                ArrayList /*IRecur*/ addRules, ArrayList /* IRecur */ subtractRules)
        {
            super(dtstart, duration, invId, addRules, subtractRules);
            mRecurRange = recurrenceId;
        }

        public ExceptionRule(RecurId recurrenceId,
                ParsedDateTime dtstart, ParsedDuration duration, 
                InviteInfo invId)  
        {
            super(dtstart, duration, invId);
            mRecurRange = recurrenceId;
        }
        
        protected ExceptionRule(Mailbox mbx, Metadata meta, TimeZoneMap tzmap) throws ParseException, ServiceException
        {
            super(mbx, meta, tzmap);
            mRecurRange = RecurId.decodeMetadata(meta.getMap(FN_RECURRENCE_ID), tzmap);
        }

        public List /* Instance */ expandInstances(Appointment appt, long start, long end) 
        {
            List toRet = super.expandInstances(appt, start, end);
            
            for (Iterator iter = toRet.iterator(); iter.hasNext();) {
                Appointment.Instance cur = (Appointment.Instance)iter.next();
                cur.setIsException(true);
            }
            return toRet;
        }
        

        public Element toXml(Element parent) {
            Element elt = parent.addElement(MailService.E_APPT_EXCEPTION_RULE);
            mRecurRange.toXml(elt);
            
            // now put the 
            super.toXml(elt);
            
            return elt;
        }
        
        static final String FN_RECURRENCE_ID = "recurId";
        static final String FN_RANGE_TYPE = "rgtyp";
        public Metadata encodeMetadata()
        {
            Metadata meta = super.encodeMetadata();
            meta.put(FN_RULE_TYPE, RULE_EXCEPTION_RULE);
            meta.put(FN_RECURRENCE_ID, mRecurRange.encodeMetadata());
            return meta;
        }

        public boolean matches(long date) {
            return mRecurRange.withinRange(date);
        }
        
        public String toString() {
            StringBuffer toRet = new StringBuffer("EXCEPTION(");
            toRet.append(mRecurRange.toString());
            toRet.append(" ");
            toRet.append(super.toString());
            return toRet.toString();
        }
        
        private ExceptionRule(ExceptionRule other) {
            super(other.mDtStart, other.mDuration, 
                    other.mAddRules == null ? null : (MultiRuleSorter)other.mAddRules.clone(), 
                            other.mSubtractRules == null ? null : (MultiRuleSorter)other.mSubtractRules.clone(), 
                                    other.mInvId);
            mRecurRange = other.mRecurRange;
        }
        
        public Object clone() {
            return new ExceptionRule(this);
        }
        
        private RecurId mRecurRange; 
    }
    
    /**
     * @author tim
     *
     * Models the initial "master" rule in a ruleset -- ie the one and only component for a 
     * particular UID that has no RECURRENCE-ID setting
     * 
     * To get the output of this rule you basically:
     *     For each Instance:   ( DTSTART + (RRULEs+RDATES-EXRULEs-EXDATEs) )
     *        Check the RECURRENCE-ID of each Exception 
     *           if it matches, then use that Exceptions Instances
     *           else return the Instance
     *        
     */
    public static class RecurrenceRule extends CompoundRuleBase 
    {
        public RecurrenceRule(ParsedDateTime dtstart, ParsedDuration duration, 
                InviteInfo invId, 
                ArrayList /*IRecur*/ addRules, ArrayList /* IRecur */ subtractRules)
        {
            super(dtstart, duration, invId, addRules, subtractRules);
            mExceptions = new ArrayList();
        }
        
        public RecurrenceRule(ParsedDateTime dtstart, ParsedDuration duration,
                InviteInfo invId)
        {
            super(dtstart, duration, invId);
            mExceptions = new ArrayList();
        }

        public RecurrenceRule(Mailbox mbox, Metadata meta, TimeZoneMap tzmap) 
        throws ServiceException, ParseException {
            super(mbox, meta, tzmap);
            
            int numEx = (int) meta.getLong(FN_NUM_EXCEPTIONS);
            mExceptions = new ArrayList(numEx);

            for (int i = 0; i < numEx; i++) {
                if (meta.containsKey(FN_EXCEPTION+i)) {
                    mExceptions.add(i, new ExceptionRule(mbox, meta.getMap(FN_EXCEPTION+i), tzmap));
                } else if (meta.containsKey(FN_CANCELLATION+i)) {
                    mExceptions.add(i, new CancellationRule(mbox, meta.getMap(FN_CANCELLATION+i), tzmap));
                }
            }
        }

        public Element toXml(Element parent) {
            for (Iterator iter = mExceptions.iterator(); iter.hasNext();) {
                IException cur = (IException)iter.next();
                cur.toXml(parent);
            }
            super.toXml(parent);
            
            return parent;
        }
        
        
        public void addException(IException rule) {
            mExceptions.add(rule);
        }
        
        public List /* Instance */ expandInstances(Appointment appt, long start, long end) {
            // get the list of instances that THIS rule expands into
            List stdInstances = super.expandInstances(appt, start, end);

            List exceptInstances[] = new List[mExceptions.size()]; // as big as we might need
            int numActiveExceptions = 0;
            
            // now, iterate through the instances in THIS rule and for each one,
            // check all the EXCEPTION rules to see if they have a RECURRENCE_ID that
            // matches.  If they do, then this instance is REMOVED from *our* set,
            // and the EXCEPTION's output is added into the set of lists to add. 
            for (Iterator stdIter = stdInstances.iterator(); stdIter.hasNext();) {
                Appointment.Instance cur = (Appointment.Instance)stdIter.next();
                Appointment.Instance origCur = cur;
            
                int exceptNum = 0;
                for (Iterator exceptIter = mExceptions.iterator(); exceptIter.hasNext();exceptNum++) {
                    IException except = (IException)exceptIter.next();
                    
                    if (except != null) {
                
                        if (cur == null && except.matches(origCur.getStart())) {
                            // hmm - there must be two different exceptions that both match this
                            // original instance!  What do we do here?  TODO read more to see
                            // what the RFC says -- for now we'll log and add the extra instances...
                            System.out.println("WARNING: multiple exceptions for same instance.  Using BOTH right now!");
                            if (exceptInstances[exceptNum] == null) {
                                numActiveExceptions++;                            
                                exceptInstances[exceptNum] = except.expandInstances(appt, start, end);
                            }
                        }
                        if (cur != null && except.matches(cur.getStart())) {
                            stdIter.remove(); // matched!  remove the current instance from our list
                            
                            cur = null;
                            if (exceptInstances[exceptNum] == null) {
                                numActiveExceptions++;                            
                                exceptInstances[exceptNum] = except.expandInstances(appt, start, end);
                            }
                        }
                    }
                }
            }

            List toRet;
            if (numActiveExceptions == 0){
                toRet = stdInstances;
            } else {
                toRet = new ArrayList();
                List toAdd[] = new List[numActiveExceptions + 1];
                toAdd[0] = stdInstances;
                int off = 1;
                for (int i = 0; i < exceptInstances.length; i++) {
                    if (exceptInstances[i] != null) {
                        toAdd[off++] = exceptInstances[i];
                    }
                }
                
                // WTF does this mean?
                 assert(off == numActiveExceptions+1);
                
                ListUtil.mergeSortedLists(toRet, toAdd, true);
            }

            return toRet;
        }
        
        public String toString() {
            StringBuffer toRet = new StringBuffer();
            
            toRet.append("RECUR(").append(super.toString());
            for (Iterator iter = mExceptions.iterator(); iter.hasNext();) {
                IException ex = (IException)iter.next();
                toRet.append("\n\t\t").append(ex.toString());
            }
            toRet.append(")");
            
            return toRet.toString();
        }
        
        
        static final String FN_NUM_EXCEPTIONS = "numEx";
        static final String FN_EXCEPTION = "ex";
        static final String FN_CANCELLATION = "ca";
        
        public Metadata encodeMetadata()
        {
            Metadata meta = super.encodeMetadata();
            
            meta.put(FN_RULE_TYPE, Integer.toString(RULE_RECURRENCE_RULE));
            meta.put(FN_NUM_EXCEPTIONS, Integer.toString(mExceptions.size()));
            for (int i = 0; i < mExceptions.size(); i++) {
                IException cur = (IException)mExceptions.get(i);
                if (cur instanceof ExceptionRule) {
                    meta.put(FN_EXCEPTION+i, cur.encodeMetadata());
                } else {
                    meta.put(FN_CANCELLATION+i, cur.encodeMetadata());
                }
            }
            return meta;
        }
        
        private RecurrenceRule(RecurrenceRule other) {
            super(other.mDtStart, other.mDuration, 
                    other.mAddRules == null ? null : (MultiRuleSorter)other.mAddRules.clone(), 
                            other.mSubtractRules == null ? null : (MultiRuleSorter)other.mSubtractRules.clone(), 
                                    other.mInvId);
            mExceptions = new ArrayList();
            for (Iterator iter = other.mExceptions.iterator(); iter.hasNext();) {
                IException cur = (IException)iter.next();
                mExceptions.add(cur.clone());
            }
        }
        
        public Object clone() {
            return new RecurrenceRule(this);
        }
        
        
        protected ArrayList /* IException */ mExceptions;
    }
    
}
