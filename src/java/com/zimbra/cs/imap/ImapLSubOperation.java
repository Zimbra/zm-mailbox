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

import java.util.Map;

import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.service.ServiceException;

public class ImapLSubOperation extends Operation {
	private static int LOAD = 25;
	static {
		Operation.Config c = loadConfig(ImapLSubOperation.class);
		if (c != null)
			LOAD = c.mLoad;
	}
	
	private ImapSession mImapSession;
	private IImapGetFolderAttributes mIGetFolderAttributes; 	
	private String mPattern;
	
	private Map<String, String> mSubs;
	
	ImapLSubOperation(ImapSession session, OperationContext oc, Mailbox mbox, 
				IImapGetFolderAttributes getFolderAttributes, String pattern) throws ServiceException		
	{
		super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);

		mImapSession = session;
		mIGetFolderAttributes = getFolderAttributes;
		mPattern = pattern;
	}
	
	protected void callback() throws ServiceException {
		synchronized (mMailbox) {
			mSubs = mImapSession.getMatchingSubscriptions(mMailbox, mPattern);
			for (Map.Entry<String, String> hit : mSubs.entrySet()) {
				Folder folder = null;
				try {
					folder = mMailbox.getFolderByPath(this.getOpCtxt(), hit.getKey());
				} catch (MailServiceException.NoSuchItemException nsie) { }
				// FIXME: need to determine "name attributes" for mailbox (\Marked, \Unmarked, \Noinferiors, \Noselect)
				boolean visible = hit.getValue() != null && ImapFolder.isFolderVisible(folder);
				String attributes = visible ? mIGetFolderAttributes.doGetFolderAttributes(folder) : "\\Noselect";
				hit.setValue("LSUB (" + attributes + ") \"/\" " + ImapFolder.encodeFolder(hit.getKey()));
			}
		}
	}
	
	Map<String, String> getSubs() { return mSubs; }
}
