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

import java.io.IOException;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.BrowseResult;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.session.Session;

public class BrowseOperation extends Operation {
	private static int LOAD = 10;
	static {
		Operation.Config c = loadConfig(BrowseOperation.class);
		if (c != null)
			LOAD = c.mLoad;
	}
	
	private String mBrowseBy;
	private BrowseResult mResult;
	
	public BrowseOperation(Session session, OperationContext oc, Mailbox mbox, 
				Requester req, String browseBy) throws ServiceException {
		super(session, oc, mbox, req, req.getPriority(), LOAD);
		mBrowseBy = browseBy;
	}
	
	protected void callback() throws ServiceException {
		try {
			mResult = mMailbox.browse(getOpCtxt(), mBrowseBy);
		} catch (IOException e) {
			throw ServiceException.FAILURE("IO error", e);
		}
	}
	
	public BrowseResult getResult() { return mResult; }
}
