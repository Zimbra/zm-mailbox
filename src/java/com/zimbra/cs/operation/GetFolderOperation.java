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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.session.Session;

public class GetFolderOperation extends Operation {
	private static int LOAD = 1;
	static {
		Operation.Config c = loadConfig(GetFolderOperation.class);
		if (c != null)
			LOAD = c.mLoad;
	}
	
	private String mFolderName;
	
	private Folder mFolder;
	
	public GetFolderOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
				String folderName) throws ServiceException {
		super(session, oc, mbox, req, LOAD);
		
		mFolderName = folderName;
	}
	
	protected void callback() throws ServiceException {
		mFolder = mMailbox.getFolderByPath(getOpCtxt(), mFolderName);
	}
	
	public Folder getFolder() { return mFolder; }
}
