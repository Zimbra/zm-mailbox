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
import java.util.Iterator;
import java.util.List;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;

public class GetFolderTreeOperation extends Operation {
	
	private static int LOAD = 1;
	static {
		Operation.Config c = loadConfig(GetFolderTreeOperation.class);
		if (c != null)
			LOAD = c.mLoad;
	}
	
	private ItemId mIid;
	
	private FolderNode mResult;
	
	public static class FolderNode {
		public Folder mFolder;
		public List<FolderNode> mSubFolders = new ArrayList<FolderNode>();
	}

	public GetFolderTreeOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
				ItemId iid) {
		super(session, oc, mbox, req, LOAD);
		
		mIid = iid;
	}
	
	public String toString() {
		return "GetFolderTreeOperation("+mIid != null ? mIid.toString() : Mailbox.ID_FOLDER_USER_ROOT+")";
	}

	protected void callback() throws ServiceException {
		Mailbox mbox = getMailbox();
		
		synchronized(mbox) {
			// get the root node...
			int folderId = mIid != null ? mIid.getId() : Mailbox.ID_FOLDER_USER_ROOT;
			Folder folder = mbox.getFolderById(getOpCtxt(), folderId);
			if (folder == null)
				throw MailServiceException.NO_SUCH_FOLDER(folderId);
			
			mResult = new FolderNode();
			mResult.mFolder = folder;
			
			// for each subNode...
			handleFolder(mResult);
		}
			
	}
	
	private void handleFolder(FolderNode parent) throws ServiceException {
		List subfolders = parent.mFolder.getSubfolders(getOpCtxt());
		if (subfolders != null)
			for (Iterator it = subfolders.iterator(); it.hasNext(); ) {
				Folder subfolder = (Folder) it.next();
				if (subfolder != null) {
					FolderNode fn = new FolderNode();
					parent.mSubFolders.add(fn);
					fn.mFolder = subfolder;
					handleFolder(fn);
				}					
			}
	}

	public FolderNode getResult() { return mResult; }
}
