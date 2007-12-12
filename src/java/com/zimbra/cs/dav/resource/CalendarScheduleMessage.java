/*
 * ***** BEGIN LICENSE BLOCK *****
 * 
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2007 Zimbra, Inc.
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
package com.zimbra.cs.dav.resource;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;

public class CalendarScheduleMessage extends CalendarObject.LocalCalendarObject {

	public CalendarScheduleMessage(DavContext ctxt, Message msg) throws ServiceException, DavException {
		super(ctxt, getPath(ctxt, msg), getAssociatedCalendarItem(ctxt, msg));
	}

	private static CalendarItem getAssociatedCalendarItem(DavContext ctxt, Message msg) throws ServiceException, DavException {
		Mailbox mbox = getMailbox(ctxt);
		Message.CalendarItemInfo calItemInfo = msg.getCalendarItemInfo(0);
		return mbox.getCalendarItemById(null, calItemInfo.getCalendarItemId());
	}
	
	private static String getPath(DavContext ctxt, Message msg) throws ServiceException, DavException {
		CalendarItem calItem = getAssociatedCalendarItem(ctxt, msg);
		return CalendarPath.generate(msg.getPath(), calItem.getUid());
	}
}
