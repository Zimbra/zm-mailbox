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
import java.util.HashMap;
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

	private IImapGetFolderAttributes mIGetFolderAttributes; 	
	private String mPattern;
	
	private Map<String, String> mSubs;
	
	ImapLSubOperation(ImapSession session, OperationContext oc, Mailbox mbox, 
				      IImapGetFolderAttributes getFolderAttributes, String pattern) {
		super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);

		mIGetFolderAttributes = getFolderAttributes;
		mPattern = pattern;
	}
	
	protected void callback() throws ServiceException {
		synchronized (mMailbox) {
			mSubs = getMatchingSubscriptions(getMailbox(), mPattern);
			for (Map.Entry<String, String> hit : mSubs.entrySet()) {
				Folder folder = null;
				try {
					folder = getMailbox().getFolderByPath(getOpCtxt(), hit.getKey());
				} catch (MailServiceException.NoSuchItemException nsie) { }
				// FIXME: need to determine "name attributes" for mailbox (\Marked, \Unmarked, \Noinferiors, \Noselect)
				boolean visible = hit.getValue() != null && ImapFolder.isFolderVisible(folder);
				String attributes = visible ? mIGetFolderAttributes.doGetFolderAttributes(folder) : "\\Noselect";
				hit.setValue("LSUB (" + attributes + ") \"/\" " + ImapFolder.formatPath(hit.getKey(), (ImapSession) mSession));
			}
		}
	}

    Map<String, String> getMatchingSubscriptions(Mailbox mbox, String pattern) throws ServiceException {
        String childPattern = pattern + "/.*";
        HashMap<String, String> hits = new HashMap<String, String>();
        ArrayList<String> children = new ArrayList<String>();

        // 6.3.9: "A special situation occurs when using LSUB with the % wildcard. Consider 
        //         what happens if "foo/bar" (with a hierarchy delimiter of "/") is subscribed
        //         but "foo" is not.  A "%" wildcard to LSUB must return foo, not foo/bar, in
        //         the LSUB response, and it MUST be flagged with the \Noselect attribute."

        // figure out the set of subscribed mailboxes that match the pattern
        Folder root = mbox.getFolderById(mOpCtxt, Mailbox.ID_FOLDER_USER_ROOT);
        for (Folder folder : root.getSubfolderHierarchy()) {
            if (!folder.isTagged(mbox.mSubscribeFlag))
                continue;
            String path = ImapFolder.exportPath(folder.getPath(), (ImapSession) mSession);
            if (path.toUpperCase().matches(pattern))
                hits.put(path, path);
            else if (path.toUpperCase().matches(childPattern))
                children.add(path);
        }
        if (children.isEmpty())
            return hits;

        // figure out the set of unsubscribed mailboxes that match the pattern and are parents of subscribed mailboxes
        for (String partName : children) {
            int delimiter = partName.lastIndexOf('/');
            while (delimiter > 0) {
                partName = partName.substring(0, delimiter);
                if (!hits.containsKey(partName) && partName.toUpperCase().matches(pattern))
                    hits.put(partName, null);
                delimiter = partName.lastIndexOf('/');
            }
        }
        return hits;
    }

	Map<String, String> getSubs() { return mSubs; }
}
