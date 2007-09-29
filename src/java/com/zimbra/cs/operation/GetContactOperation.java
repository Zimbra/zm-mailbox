/*
 * ***** BEGIN LICENSE BLOCK *****
 * Zimbra Collaboration Suite Server
 * Copyright (C) 2006 Zimbra, Inc.
 * 
 * The contents of this file are subject to the Yahoo! Public License
 * Version 1.0 ("License"); you may not use this file except in
 * compliance with the License.  You may obtain a copy of the License at
 * http://www.zimbra.com/license.
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied.
 * ***** END LICENSE BLOCK *****
 */
package com.zimbra.cs.operation;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Contact;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.session.Session;

public class GetContactOperation extends Operation {
	
	private static int LOAD = 1;
	private static int MAX = 20;
	private static int SCALE = 100;
	static {
		Operation.Config c = loadConfig(GetContactListOperation.class);
		if (c != null) {
			LOAD = c.mLoad;
			if (c.mScale > 0)
				SCALE = c.mScale;
			if (c.mMaxLoad > 0)
				MAX = c.mMaxLoad;
		}
	}
	
	static void init(int load, int scale, int maxload) {
		LOAD = load;
		if (scale > 0)
			SCALE = scale;
		if (maxload > 0)
			MAX = maxload;
	}
	
	private List<Integer> mIds;
	private List<Contact> mContacts;

	public GetContactOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
				List<Integer> ids) {
		super(session, oc, mbox, req, Math.min(1 + (LOAD * (ids.size() / SCALE)), MAX));
		
		mIds = ids;
	}
	
	public String toString() {
		StringBuilder toRet = new StringBuilder("GetContactOperation(");
		for (int id : mIds) {
			toRet.append(id).append(",");
		}
		toRet.append(")");
		return toRet.toString();
	}

	protected void callback() throws ServiceException {
		Mailbox mbox = getMailbox();
		mContacts = new ArrayList<Contact>();
		synchronized(mbox) {
			for (int id : mIds) {
				Contact con = mbox.getContactById(getOpCtxt(), id);
				mContacts.add(con);
			}
		}
	}
	
	public List<Contact> getResults() { return mContacts; } 
}
