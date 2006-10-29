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

import org.dom4j.Element;

import com.zimbra.common.util.DateUtil;
import com.zimbra.cs.dav.DavElements;

public class TimeRange {
	private long mStart;
	private long mEnd;
	
	public static long sMIN_DATE;
	public static long sMAX_DATE;
	
	static {
		sMIN_DATE = DateUtil.parseGeneralizedTime("19000101000000Z").getTime();
		sMAX_DATE = DateUtil.parseGeneralizedTime("20991231235959Z").getTime();
	}
	
	public TimeRange(Element elem) {
		mStart = sMIN_DATE;
		mEnd = sMAX_DATE;
		if (elem != null && elem.getQName().equals(DavElements.E_TIME_RANGE)) {
			String s = elem.attributeValue(DavElements.P_START);
			if (s != null)
				mStart = Long.parseLong(s);
			
			s = elem.attributeValue(DavElements.P_END);
			if (s != null)
				mEnd = Long.parseLong(s);
		}
	}
	
	public long getStart() {
		return mStart;
	}
	
	public long getEnd() {
		return mEnd;
	}
}
