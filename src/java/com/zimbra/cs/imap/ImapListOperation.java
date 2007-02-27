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

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;

public class ImapListOperation extends Operation {
	
	private static int LOAD = 25;
    	static {
    		Operation.Config c = loadConfig(ImapListOperation.class);
    		if (c != null)
    			LOAD = c.mLoad;
    	}

    private String mPattern;
    private boolean mOutputChildInfo;
	
	private List<String> mMatches;


    ImapListOperation(ImapSession session, OperationContext oc, Mailbox mbox, String pattern, boolean children) {
		super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);

		mPattern = pattern;
        mOutputChildInfo = children;
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
					mMatches.add("LIST (" + getFolderAttributes((ImapSession) mSession, folder, mOutputChildInfo) + ") \"/\" " +
                                       ImapFolder.formatPath(folder.getPath(), (ImapSession) mSession));
			}
		}
	}

    private static final String[] FOLDER_ATTRIBUTES = {
        "\\HasNoChildren",              "\\HasChildren",
        "\\HasNoChildren \\Noselect",   "\\HasChildren \\Noselect",
        "\\HasNoChildren \\Noinferiors"
    };

    private static final String[] NO_CHILDREN_FOLDER_ATTRIBUTES = {
        "",             "",
        "\\Noselect",   "\\Noselect",
        "\\Noinferiors"
    };

    static String getFolderAttributes(ImapSession session, Folder folder, boolean children) {
        int attributes = (folder.hasSubfolders() ? 0x01 : 0x00);
        attributes    |= (!ImapFolder.isFolderSelectable(folder, session) ? 0x02 : 0x00);
        attributes    |= (folder.getId() == Mailbox.ID_FOLDER_SPAM ? 0x04 : 0x00);
        return children ? FOLDER_ATTRIBUTES[attributes] : NO_CHILDREN_FOLDER_ATTRIBUTES[attributes];
    }

	public List<String> getMatches() { return mMatches; }
}
