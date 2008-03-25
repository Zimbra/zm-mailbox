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

import java.util.ArrayList;
import java.util.Collections;

import com.zimbra.common.service.ServiceException;
import com.zimbra.common.util.ZimbraLog;
import com.zimbra.cs.dav.DavContext;
import com.zimbra.cs.dav.DavElements;
import com.zimbra.cs.dav.DavException;
import com.zimbra.cs.index.MailboxIndex;
import com.zimbra.cs.index.MessageHit;
import com.zimbra.cs.index.ZimbraHit;
import com.zimbra.cs.index.ZimbraQueryResults;
import com.zimbra.cs.mailbox.CalendarItem;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;

public class ScheduleInbox extends Collection {
	public ScheduleInbox(DavContext ctxt, Folder f) throws DavException, ServiceException {
		super(ctxt, f);
		addResourceType(DavElements.E_SCHEDULE_INBOX);
	}
	public java.util.Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
		try {
			return getScheduleMessages(ctxt);
		} catch (ServiceException se) {
			ZimbraLog.dav.error("can't get schedule messages in folder "+getId(), se);
			return Collections.emptyList();
		}
	}

	protected static final byte[] SEARCH_TYPES = new byte[] { MailItem.TYPE_MESSAGE };
	
	protected java.util.Collection<DavResource> getScheduleMessages(DavContext ctxt) throws ServiceException, DavException {
		ArrayList<DavResource> result = new ArrayList<DavResource>();
		String query = "after:-1month is:invite inid:" + getId();
		Mailbox mbox = getMailbox(ctxt);
		ZimbraQueryResults zqr = null;
		try {
			zqr = mbox.search(ctxt.getOperationContext(), query, SEARCH_TYPES, MailboxIndex.SortBy.NAME_ASCENDING, 100);
			while (zqr.hasNext()) {
                ZimbraHit hit = zqr.getNext();
                if (hit instanceof MessageHit) {
                	Message msg = ((MessageHit)hit).getMessage();
                	CalendarItem calItem = UrlNamespace.getCalendarItemForMessage(ctxt, msg);
                	if (calItem != null)
                		result.add(new CalendarObject.LocalCalendarObject(ctxt, calItem));
                }
			}
		} catch (Exception e) {
			ZimbraLog.dav.error("can't search: uri="+getUri(), e);
		} finally {
			if (zqr != null)
				try {
					zqr.doneWithSearchResults();
				} catch (ServiceException e) {}
		}
		return result;
	}
}
