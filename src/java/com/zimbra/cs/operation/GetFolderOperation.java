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
package com.zimbra.cs.operation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.service.ServiceException;
import com.zimbra.cs.service.util.ItemId;
import com.zimbra.cs.session.Session;

public class GetFolderOperation extends Operation {
	
	private static final int LOAD = 1;
	
	private ItemId mIid;
	
	private FolderNode mResult;
	
	public static class FolderNode {
		public Folder mFolder;
		public List<FolderNode> mSubFolders = new ArrayList<FolderNode>();
	}

	public GetFolderOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
				ItemId iid) {
		super(session, oc, mbox, req, LOAD);
		
		mIid = iid;
	}
	
	public String toString() {
		return "GetFolderOperation("+mIid != null ? mIid.toString() : Mailbox.ID_FOLDER_USER_ROOT+")";
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
