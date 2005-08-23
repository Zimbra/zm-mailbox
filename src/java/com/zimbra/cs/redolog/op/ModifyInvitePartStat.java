/*
 * ***** BEGIN LICENSE BLOCK *****
 * Version: ZPL 1.1
 * 
 * The contents of this file are subject to the Zimbra Public License
 * Version 1.1 ("License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.zimbra.com/license
 * 
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See
 * the License for the specific language governing rights and limitations
 * under the License.
 * 
 * The Original Code is: Zimbra Collaboration Suite.
 * 
 * The Initial Developer of the Original Code is Zimbra, Inc.  Portions
 * created by Zimbra are Copyright (C) 2005 Zimbra, Inc.  All Rights
 * Reserved.
 * 
 * Contributor(s): 
 * 
 * ***** END LICENSE BLOCK *****
 */

/*
 * Created on Jul 24, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author jhahm
 */
public class ModifyInvitePartStat extends RedoableOp {

    private int mApptId = UNKNOWN_ID; // appointment which contains invite
    private int mInvId = UNKNOWN_ID;  // invite ID
    int mCompNum;                  // component number
    private String mPartStat;      // "AC" (accepted), "TE" (tentative), etc.
    private boolean mNeedsReply;

    public ModifyInvitePartStat() {
    }

    public ModifyInvitePartStat(int mailboxId, int apptId, int inviteId, int compNum, boolean needsReply, String partStat) {
        setMailboxId(mailboxId);
        mApptId = apptId;
        mInvId= inviteId;
        mCompNum = compNum;
        mPartStat = partStat;
        mNeedsReply = needsReply;
    }

    public int getOpCode() {
        return OP_MODIFY_INVITE_PARTSTAT;
    }

    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        mbox.modifyInvitePartStat(getOperationContext(), mApptId, mInvId, mCompNum, mNeedsReply, mPartStat);
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("apptId=").append(mApptId);
        sb.append(", invId=").append(mInvId);
        sb.append(", comp=").append(mCompNum);
        sb.append(", needsReply=").append(mNeedsReply);
        sb.append(", partStat=").append(mPartStat);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mApptId);
        out.writeInt(mInvId);
        out.writeInt(mCompNum);
        if (mNeedsReply) {
            out.writeBoolean(true);
        } else {
            out.writeBoolean(false);
        }
        writeUTF8(out, mPartStat);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mApptId = in.readInt();
        mInvId = in.readInt();
        mCompNum = in.readInt();
        mNeedsReply = in.readBoolean();
        mPartStat = readUTF8(in);
    }
}
