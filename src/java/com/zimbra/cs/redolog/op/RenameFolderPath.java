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
 * Portions created by Zimbra are Copyright (C) 2005 Zimbra, Inc.
 * All Rights Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on 2004. 12. 13.
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author jhahm
 */
public class RenameFolderPath extends RedoableOp {

    private int mId;
    private String mName;
    private int mParentIds[];

    public RenameFolderPath() {
        mId = UNKNOWN_ID;
    }

    public RenameFolderPath(int mailboxId, int id, String name) {
        setMailboxId(mailboxId);
        mId = id;
        mName = name != null ? name : "";
    }

    public int[] getParentIds() {
        return mParentIds;
    }

    public void setParentIds(int parentIds[]) {
        mParentIds = parentIds;
    }

    public int getOpCode() {
        return OP_RENAME_FOLDER_PATH;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", name=").append(mName);
        if (mParentIds != null) {
        	sb.append(", destParentIds=[");
            for (int i = 0; i < mParentIds.length; i++) {
            	sb.append(mParentIds[i]);
                if (i < mParentIds.length - 1)
                    sb.append(", ");
            }
            sb.append("]");
        }
        return sb.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeUTF(mName);
        if (mParentIds != null) {
            out.writeInt(mParentIds.length);
            for (int i = 0; i < mParentIds.length; i++)
                out.writeInt(mParentIds[i]);
        } else {
            out.writeInt(0);
        }
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mName = in.readUTF();
        int numParentIds = in.readInt();
        if (numParentIds > 0) {
        	mParentIds = new int[numParentIds];
            for (int i = 0; i < numParentIds; i++)
            	mParentIds[i] = in.readInt();
        }
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);
        mailbox.renameFolder(getOperationContext(), mId, mName);
    }
}
