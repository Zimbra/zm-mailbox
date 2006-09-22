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
 * Created on Jun 6, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author dkarp
 */
public class EmptyFolder extends RedoableOp {

    private int mId;
    private boolean mSubfolders;


    public EmptyFolder() {
        mId = UNKNOWN_ID;
        mSubfolders = false;
    }

    public EmptyFolder(int mailboxId, int id, boolean subfolders) {
        setMailboxId(mailboxId);
        mId = id;
        mSubfolders = subfolders;
    }

    public int getOpCode() {
        return OP_EMPTY_FOLDER;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", subfolders=").append(mSubfolders);
        return sb.toString();
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeBoolean(mSubfolders);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mSubfolders = in.readBoolean();
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = MailboxManager.getInstance().getMailboxById(mboxId);
        mbox.emptyFolder(getOperationContext(), mId, mSubfolders);
    }
}
