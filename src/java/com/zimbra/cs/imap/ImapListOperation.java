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
package com.zimbra.cs.imap;

import java.util.ArrayList;
import java.util.List;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;

public class ImapListOperation extends Operation {
	
	private static int LOAD = 25;
    	static {
    		Operation.Config c = loadConfig(ImapListOperation.class);
    		if (c != null)
    			LOAD = c.mLoad;
    	}
	
	private IImapGetFolderAttributes mIGetFolderAttributes;
	private String mPattern;
	
	private List<String> mMatches;

	ImapListOperation(ImapSession session, OperationContext oc, Mailbox mbox, 
				      IImapGetFolderAttributes getFolderAttributes, String pattern) {
		super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);

		mIGetFolderAttributes = getFolderAttributes;
		mPattern = pattern;
	}

	protected void callback() throws ServiceException {
		synchronized (mMailbox) {
			mMatches = new ArrayList<String>();
			
			Folder root = mMailbox.getFolderById(getOpCtxt(), Mailbox.ID_FOLDER_USER_ROOT);
			for (Folder folder : root.getSubfolderHierarchy()) {
				if (!ImapFolder.isFolderVisible(folder, (ImapSession) mSession))
					continue;
				String path = ImapFolder.exportPath(folder.getPath(), (ImapSession) mSession);
				// FIXME: need to determine "name attributes" for mailbox (\Marked, \Unmarked, \Noinferiors, \Noselect)
				if (path.toUpperCase().matches(mPattern))
					mMatches.add("LIST (" + mIGetFolderAttributes.doGetFolderAttributes(folder) + ") \"/\" " + ImapFolder.formatPath(folder.getPath(), (ImapSession) mSession));
			}
		}
	}

	public List<String> getMatches() { return mMatches; }
}
