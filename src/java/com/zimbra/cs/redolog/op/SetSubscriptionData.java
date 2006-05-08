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
 * Created on Nov 12, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author dkarp
 */
public class SetSubscriptionData extends RedoableOp {

    private int mFolderId;
    private long mLastItemDate;
    private String mLastItemGuid;

    public SetSubscriptionData() {
        mFolderId = Mailbox.ID_AUTO_INCREMENT;
        mLastItemGuid = "";
    }

    public SetSubscriptionData(int mailboxId, int folderId, long date, String guid) {
        setMailboxId(mailboxId);
        mFolderId = folderId;
        mLastItemDate = date;
        mLastItemGuid = guid == null ? "" : guid;
    }

    public int getOpCode() {
        return OP_SET_SUBSCRIPTION_DATA;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mFolderId);
        sb.append(", date=").append(mLastItemDate);
        sb.append(", guid=").append(mLastItemGuid);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mFolderId);
        out.writeLong(mLastItemDate);
        writeUTF8(out, mLastItemGuid);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mFolderId = in.readInt();
        mLastItemDate = in.readLong();
        mLastItemGuid = readUTF8(in);
    }

    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        mbox.setSubscriptionData(getOperationContext(), mFolderId, mLastItemDate, mLastItemGuid);
    }
}
