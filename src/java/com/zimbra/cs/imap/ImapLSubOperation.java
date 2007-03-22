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
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.mailbox.Folder;
import com.zimbra.cs.mailbox.MailServiceException;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.Mailbox.OperationContext;
import com.zimbra.cs.operation.Operation;
import com.zimbra.cs.session.Session;

public class ImapLSubOperation extends Operation {
	private static int LOAD = 25;
    	static {
    		Operation.Config c = loadConfig(ImapLSubOperation.class);
    		if (c != null)
    			LOAD = c.mLoad;
    	}

	private ImapPath mPattern;
    private ImapCredentials mCredentials;
    private boolean mOutputChildInfo;

	private List<String> mSubs = new ArrayList<String>();

	ImapLSubOperation(Session session, OperationContext oc, Mailbox mbox, ImapPath pattern, ImapCredentials creds, boolean children) {
        super(session, oc, mbox, Requester.IMAP, Requester.IMAP.getPriority(), LOAD);

        mPattern = pattern;
        mCredentials = creds;
        mOutputChildInfo = children;
	}


    protected void callback() throws ServiceException {
        Account acct = mMailbox.getAccount();

        String pattern = mPattern.asImapPath().toUpperCase();
        Map<ImapPath, Boolean> hits = new HashMap<ImapPath, Boolean>();

        synchronized (mMailbox) {
            List<Folder> folders = mMailbox.getFolderById(getOpCtxt(), Mailbox.ID_FOLDER_USER_ROOT).getSubfolderHierarchy();
            for (Folder folder : folders) {
                if (folder.isTagged(mMailbox.mSubscribeFlag)) {
                    if (!checkSubscription(new ImapPath(null, folder.getPath(), mCredentials), pattern, hits))
                        checkSubscription(new ImapPath(acct.getName(), folder.getPath(), mCredentials), pattern, hits);
                }
            }

            Set<String> remoteSubscriptions = mCredentials.listSubscriptions();
            if (remoteSubscriptions != null && !remoteSubscriptions.isEmpty()) {
                for (String sub : remoteSubscriptions)
                    checkSubscription(new ImapPath(sub, mCredentials), pattern, hits);
            }

            for (Map.Entry<ImapPath, Boolean> hit : hits.entrySet()) {
                String attrs = getFolderAttributes(hit.getKey(), hit.getValue());
				mSubs.add("LSUB (" + attrs + ") \"/\" " + hit.getKey().asUtf7String());
			}
		}
	}

    private boolean checkSubscription(ImapPath path, String pattern, Map<ImapPath, Boolean> hits) {
        if (path.asImapPath().toUpperCase().matches(pattern)) {
            hits.put(path, Boolean.TRUE);  return true;
        } else if (!path.asImapPath().toUpperCase().matches(pattern + "/.*")) {
            return false;
        }

        // 6.3.9: "A special situation occurs when using LSUB with the % wildcard. Consider 
        //         what happens if "foo/bar" (with a hierarchy delimiter of "/") is subscribed
        //         but "foo" is not.  A "%" wildcard to LSUB must return foo, not foo/bar, in
        //         the LSUB response, and it MUST be flagged with the \Noselect attribute."

        // figure out the set of unsubscribed mailboxes that match the pattern and are parents of subscribed mailboxes
        boolean matched = false;
        int delimiter = path.asZimbraPath().lastIndexOf('/');
        while (delimiter > 0) {
            path = new ImapPath(path.asZimbraPath().substring(0, delimiter), mCredentials);
            if (!hits.containsKey(path) && path.asImapPath().toUpperCase().matches(pattern)) {
                hits.put(path, Boolean.FALSE);  matched = true;
            }
            delimiter = path.asZimbraPath().lastIndexOf('/');
        }
        return matched;
    }

    private String getFolderAttributes(ImapPath path, boolean matched) throws ServiceException {
        if (!matched)
            return "\\Noselect";
        if (!path.belongsTo(mMailbox))
            return "";
        try {
            if (path.isVisible()) {
                Folder folder = getMailbox().getFolderByPath(getOpCtxt(), path.asZimbraPath());
                return ImapListOperation.getFolderAttributes(mCredentials, folder, mOutputChildInfo);
            }
        } catch (MailServiceException.NoSuchItemException nsie) { }
        return "\\Noselect";
    }

	List<String> getSubs()  { return mSubs; }
}
