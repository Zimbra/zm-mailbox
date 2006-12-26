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
 * Portions created by Zimbra are Copyright (C) 2004, 2005, 2006 Zimbra, Inc.
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

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.mailbox.MailboxManager;
import com.zimbra.cs.redolog.RedoLogInput;
import com.zimbra.cs.redolog.RedoLogOutput;

/**
 * @author jhahm
 */
public class RenameItem extends RedoableOp {

    int mId;
    byte mType;
    int mFolderId;
    String mName;

    public RenameItem() {
        mId = mFolderId = UNKNOWN_ID;
        mType = MailItem.TYPE_UNKNOWN;
    }

    public RenameItem(int mailboxId, int id, byte type, String name, int folderId) {
        setMailboxId(mailboxId);
        mId = id;
        mType = type;
        mFolderId = folderId;
        mName = name != null ? name : "";
    }

    public int getOpCode() {
        return OP_RENAME_ITEM;
    }

    protected String getPrintableData() {
        return "id=" + mId + ", type=" + mType + ", name=" + mName + ",parent=" + mFolderId;
    }

    protected void serializeData(RedoLogOutput out) throws IOException {
        out.writeInt(mId);
        out.writeInt(mFolderId);
        out.writeUTF(mName);
        out.writeByte(mType);
    }

    protected void deserializeData(RedoLogInput in) throws IOException {
        mId = in.readInt();
        mFolderId = in.readInt();
        mName = in.readUTF();
        mType = in.readByte();
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = MailboxManager.getInstance().getMailboxById(mboxId);
        mailbox.rename(getOperationContext(), mId, mType, mName, mFolderId);
    }
}
