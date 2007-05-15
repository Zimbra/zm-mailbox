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
package com.zimbra.cs.dav.resource;

import java.util.ArrayList;
import java.util.Collection;
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
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Message;

public class ScheduleInbox extends CalendarCollection {
	public ScheduleInbox(DavContext ctxt, Folder f) throws DavException, ServiceException {
		super(ctxt, f);
		addResourceType(DavElements.E_SCHEDULE_INBOX);
	}
	public Collection<DavResource> getChildren(DavContext ctxt) throws DavException {
		return Collections.emptyList();
/*
		try {
			return getScheduleMessages(ctxt);
		} catch (ServiceException se) {
			ZimbraLog.dav.error("can't get schedule messages in folder "+getId(), se);
			return Collections.emptyList();
		}
*/
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
                	if (msg.isInvite() && msg.hasCalendarItemInfos())
                		result.add(new CalendarScheduleMessage(ctxt, msg));
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
