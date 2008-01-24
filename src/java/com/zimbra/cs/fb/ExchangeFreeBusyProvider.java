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

import com.zimbra.cs.fb.FreeBusy.IntervalList;

public class ExchangeFreeBusyProvider {
	public FreeBusy getFreeBusy(String emailAddr, long start, long end) {
		return new ExchangeUserFreeBusy(new IntervalList(start, end), start, end);
	}
	public static class ExchangeUserFreeBusy extends FreeBusy {
	    protected ExchangeUserFreeBusy(IntervalList list, long start, long end) {
	    	super(list, start, end);
	    }
	}
}
