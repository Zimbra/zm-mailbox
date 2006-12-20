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
 * Portions created by Zimbra are Copyright (C) 2006 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.dav.caldav;

import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.dom4j.Element;

import com.zimbra.common.util.Constants;
import com.zimbra.cs.dav.DavElements;

/**
 * draft-dusseault-caldav section 9.9.
 * 
 * @author jylee
 *
 */
public class TimeRange {
	private long mStart;
	private long mEnd;

	public TimeRange(Element elem) {
		mStart = mEnd = 0;
		if (elem != null && elem.getQName().equals(DavElements.E_TIME_RANGE)) {
			String s = elem.attributeValue(DavElements.P_START);
			if (s != null)
				mStart = parseDateWithUTCTime(s);
			
			s = elem.attributeValue(DavElements.P_END);
			if (s != null)
				mEnd = parseDateWithUTCTime(s);
		}
		if (mStart == 0)
			mStart = getMinDate();
		if (mEnd == 0)
			mEnd = getMaxDate();
	}
	
	private static long getMinDate() {
		return System.currentTimeMillis() - Constants.MILLIS_PER_MONTH;
	}
	
	private static long getMaxDate() {
		return System.currentTimeMillis() + Constants.MILLIS_PER_MONTH * 12;
	}
	
    private static long parseDateWithUTCTime(String time) {
    	if (time.length() != 8 && time.length() != 16)
    		return 0;
    	if (!time.endsWith("Z"))
    		return 0;
    	TimeZone tz = TimeZone.getTimeZone("GMT");
    	int year, month, date, hour, min, sec;
    	int index = 0;
    	year = Integer.parseInt(time.substring(index, index+4)); index+=4;
    	month = Integer.parseInt(time.substring(index, index+2))-1; index+=2;
    	date = Integer.parseInt(time.substring(index, index+2)); index+=2;
    	hour = min = sec = 0;
    	if (time.length() == 16) {
    		if (time.charAt(index) == 'T') index++;
    		hour = Integer.parseInt(time.substring(index, index+2)); index+=2;
    		min = Integer.parseInt(time.substring(index, index+2)); index+=2;
    		sec = Integer.parseInt(time.substring(index, index+2)); index+=2;
    	}
    	GregorianCalendar calendar = new GregorianCalendar(tz);
    	calendar.set(year, month, date, hour, min, sec);
    	return calendar.getTimeInMillis();
    }
    
	public long getStart() {
		return mStart;
	}
	
	public long getEnd() {
		return mEnd;
	}
}
