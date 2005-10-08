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

/**
 * 
 */
package com.zimbra.cs.mailbox.calendar;

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.Duration;

import com.zimbra.cs.service.Element;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.mail.MailService;

public final class ParsedDuration
{
    static public final int WEEKS = 1;
    static public final int DAYS = 2;
    static public final int HOURS = 3;
    static public final int MINUTES = 4;
    static public final int SECONDS = 5;
    
    private int mWeeks = 0;
    private int mDays = 0;
    private int mHours = 0;
    private int mMins = 0;
    private int mSecs = 0;
    private boolean mNegative = false;
    
    static Pattern sDurationWeekPattern = Pattern.compile("([+-]?P)(\\d+)W");
    static Pattern sDurationDayTimePattern = Pattern.compile("([+-]?P)(?:(\\d+)D)?(?:T(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?)?");
    
    public static final ParsedDuration ONE_DAY = new ParsedDuration(false, 0, 1, 0, 0, 0);
    
    private ParsedDuration(boolean negative, int weeks, int days, int hours, int mins, int secs) {
        mNegative = negative;
        mWeeks = weeks;
        mDays = days;
        mHours = hours;
        mMins = mins;
        mSecs = secs;
    }
    
    public Object clone() {
        ParsedDuration toRet = new ParsedDuration();
        toRet.mWeeks = mWeeks;
        toRet.mDays = mDays;
        toRet.mHours = mHours;
        toRet.mMins = mMins;
        toRet.mSecs = mSecs;
        toRet.mNegative = mNegative;
        return toRet;
    }
    
    public ParsedDuration add(int field, int amount) throws ServiceException {
        ParsedDuration dur = (ParsedDuration)clone();
        
        switch (field) {
        case WEEKS:
            dur.mWeeks+=amount;
            break;
        case DAYS:
            dur.mDays+=amount;
            break;
        case HOURS:
            dur.mHours+=amount;
            break;
        case MINUTES:
            dur.mMins+=amount;
            break;
        case SECONDS:
            dur.mSecs+=amount;
            break;
        default:
            throw ServiceException.INVALID_REQUEST("Unknown field in ParsedDuration.add("+field+","+amount+")",null);
        }
        return dur;
    }
    
    public String toString() {
        String start = "P";
        if (mNegative) {
            start = "-P";
        }
        
        StringBuffer toRet = new StringBuffer(start);
        if (mWeeks != 0) {
            assert(mDays == 0 && mHours == 0 && mMins == 0 && mSecs == 0);
            toRet.append(mWeeks+"W");
            return toRet.toString();
        }
        if (mDays != 0) {
            toRet.append(mDays+"D");
            if (mHours == 0 && mMins == 0 && mSecs == 0) {
                return toRet.toString();
            }
        }
        toRet.append("T");
        if (mHours!= 0) {
            toRet.append(mHours+"H");
        }
        if (mMins!= 0) {
            toRet.append(mMins+"M");
        }
        if (mSecs!= 0) {
            toRet.append(mSecs+"S");
        }
        return toRet.toString();
    }
    
    public static ParsedDuration parse(Property prop) throws ServiceException
    {
        return parse(((Duration)prop).getValue());
    }
    
    public static ParsedDuration parse(boolean negative, int weeks, int days, int hours, int mins, int secs) {
        ParsedDuration toRet = new ParsedDuration();
        assert(hours < 24);
        assert(mins < 60);
        assert(secs < 60);
        if (negative) {
            toRet.mNegative = true;
        } else {
            toRet.mNegative = false;
        }
        toRet.mWeeks = weeks;
        toRet.mDays = days;
        toRet.mHours = hours;
        toRet.mMins = mins;
        toRet.mSecs = secs;
        return toRet; 
    }
    
    private ParsedDuration()
    {
        mNegative = false;
        mWeeks = 0;
        mDays = 0;
        mHours = 0;
        mMins = 0;
        mSecs = 0;
    }
    
    public Element toXml(Element parent) {
        Element elt = parent.addElement(MailService.E_APPT_DURATION);
        if (mNegative) {
            elt.addAttribute(MailService.A_APPT_DURATION_NEGATIVE, true);
        }
        
        if (mWeeks > 0) {
            elt.addAttribute(MailService.A_APPT_DURATION_WEEKS, mWeeks);
        } else {
            if (mDays > 0) {
                elt.addAttribute(MailService.A_APPT_DURATION_DAYS, mDays);
            }
            if (mHours> 0) {
                elt.addAttribute(MailService.A_APPT_DURATION_HOURS, mHours);
            }
            if (mMins> 0) {
                elt.addAttribute(MailService.A_APPT_DURATION_MINUTES, mMins);
            }
            if (mSecs> 0) {
                elt.addAttribute(MailService.A_APPT_DURATION_SECONDS, mSecs);
            }
        }
        return elt;
    }
    
    public static ParsedDuration parse(Element elt) throws ServiceException {
        ParsedDuration toRet = new ParsedDuration();
        toRet.mNegative = elt.getAttributeBool(MailService.A_APPT_DURATION_NEGATIVE, false);
        toRet.mWeeks = (int)elt.getAttributeLong(MailService.A_APPT_DURATION_WEEKS, 0);
        toRet.mDays= (int)elt.getAttributeLong(MailService.A_APPT_DURATION_DAYS, 0);
        toRet.mHours= (int)elt.getAttributeLong(MailService.A_APPT_DURATION_HOURS, 0);
        toRet.mMins = (int)elt.getAttributeLong(MailService.A_APPT_DURATION_MINUTES, 0);
        toRet.mSecs = (int)elt.getAttributeLong(MailService.A_APPT_DURATION_SECONDS, 0);
        
        if (toRet.mDays!=0 || toRet.mHours!=0 || toRet.mMins!=0 || toRet.mSecs!=0) {
            if (toRet.mWeeks != 0) {
                throw ServiceException.FAILURE("Weeks may not be specified with other granularity in duration element: "+
                        elt.getName(), null);
            }
        }
        
        return toRet;
    }
    
    public Date addToDate(Date date) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        int mult = 1;
        if (mNegative) {
            mult = -1;
        }
        cal.add(java.util.Calendar.WEEK_OF_YEAR, mult*mWeeks);
        cal.add(java.util.Calendar.DAY_OF_YEAR, mult*mDays);
        cal.add(java.util.Calendar.HOUR_OF_DAY, mult*mHours);
        cal.add(java.util.Calendar.MINUTE, mult*mMins);
        cal.add(java.util.Calendar.SECOND, mult*mSecs);
        
        return cal.getTime();
    }
    
    public long addToTime(long utcTime) {
        return addToDate(new Date(utcTime)).getTime();
    }
    
    /**
     * In order to _correctly_ calculate the number of ms in this duration, we actually
     * need to know the STARTING time...this is because things like "1 Day" might change
     * if we cross a Daylight/Standard timezone boundary.  
     * 
     * @param startDate
     * @return
     */
    public long getDurationAsMsecs(Date startDate) {
        Date endDate = addToDate(startDate);
        
        return endDate.getTime() - startDate.getTime();
    }
    
    public static ParsedDuration parse(String durationStr) throws ServiceException {
        if (durationStr == null)
            return null;
        ParsedDuration retVal = new ParsedDuration();
        
        Matcher m = sDurationWeekPattern.matcher(durationStr);
        if (m.matches()) {
            String p = m.group(1);
            String wk = m.group(2);
            
            if (p.charAt(0) == '-') {
                retVal.mNegative = true;
                retVal.mWeeks = Integer.parseInt(wk);
            }
            
        } else {
            m = sDurationDayTimePattern.matcher(durationStr);
            if (m.matches()) {
                // P, Days, Hours, Mins, Secs
                String p = m.group(1);
                if (p.charAt(0) == '-') {
                    retVal.mNegative = true;
                }
                String da = m.group(2);
                if (da != null) {
                    retVal.mDays = Integer.parseInt(da);
                }
                String ho = m.group(3);
                if (ho!= null) {
                    retVal.mHours = Integer.parseInt(ho);
                }
                String mi = m.group(4);
                if (mi != null) {
                    retVal.mMins = Integer.parseInt(mi);
                }
                String se = m.group(5);
                if (se!= null) {
                    retVal.mSecs = Integer.parseInt(se);
                }
            } else {
                throw ServiceException.INVALID_REQUEST("Could not parse DURATION string: "+durationStr, null);
            }
        }
        assert(retVal.mHours < 24);
        assert(retVal.mMins < 60);
        assert(retVal.mSecs < 60);
        
        return retVal;
    }
    int getWeeks() {
        return mWeeks * (mNegative ? -1 : 1);
    }

    int getDays() {
        return mDays * (mNegative ? -1 : 1);
    }

    int getHours() {
        return mHours * (mNegative ? -1 : 1);
    }

    int getMins() {
        return mMins * (mNegative ? -1 : 1);
    }

    int getSecs() {
        return mSecs * (mNegative ? -1 : 1);
    }
}