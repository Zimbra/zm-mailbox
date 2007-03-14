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
import java.util.List;
import java.util.Set;

import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.mailbox.Folder;
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
    private boolean mVisibleFolders;

    private FolderNode mResult;

    public static class FolderNode {
        public int mId;
        public String mName;
        public Folder mFolder;
        public boolean mVisible;
        public List<FolderNode> mSubfolders = new ArrayList<FolderNode>();
    }

    public GetFolderTreeOperation(Session session, OperationContext oc, Mailbox mbox, Requester req,
                ItemId iid, boolean visible) {
        super(session, oc, mbox, req, LOAD);

        mIid = iid;
        mVisibleFolders = visible;
    }

    public String toString() {
        return "GetFolderTreeOperation(" + (mIid != null ? mIid.toString() : Mailbox.ID_FOLDER_USER_ROOT + "") + ")";
    }

    protected void callback() throws ServiceException {
        Mailbox mbox = getMailbox();

        synchronized (mbox) {
            // get the root node...
            int folderId = mIid != null ? mIid.getId() : Mailbox.ID_FOLDER_USER_ROOT;
            Folder folder = mbox.getFolderById(mVisibleFolders ? null : getOpCtxt(), folderId);

            // for each subNode...
            Set<Folder> visible = mbox.getVisibleFolders(getOpCtxt());
            mResult = handleFolder(folder, visible);
        }
    }
    
    private FolderNode handleFolder(Folder folder, Set<Folder> visible) throws ServiceException {
        boolean isVisible = visible == null || visible.remove(folder);

        // short-circuit if we know that this won't be in the output
        List<Folder> subfolders = folder.getSubfolders(null);
        if (!isVisible && subfolders.isEmpty())
            return null;

        FolderNode node = new FolderNode();
        node.mId = folder.getId();
        node.mName = node.mId == Mailbox.ID_FOLDER_ROOT ? null : folder.getName();
        node.mFolder = folder;
        node.mVisible = isVisible;

        // if this was the last visible folder overall, no need to look at children
        if (isVisible && visible != null && visible.isEmpty())
            return node;

        // write the subfolders' data to the response
        for (Folder subfolder : subfolders) {
            FolderNode child = handleFolder(subfolder, visible);
            if (child != null) {
                node.mSubfolders.add(child);
                isVisible = true;
            }
        }

        return isVisible ? node : null;
    }

    public FolderNode getResult() { return mResult; }
}
