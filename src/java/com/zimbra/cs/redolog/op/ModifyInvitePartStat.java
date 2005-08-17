/*
 * Created on Jul 24, 2005
 */
package com.liquidsys.coco.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.liquidsys.coco.mailbox.Mailbox;

/**
 * @author jhahm
 */
public class ModifyInvitePartStat extends RedoableOp {

    private int mApptId = UNKNOWN_ID; // appointment which contains invite
    private int mInvId = UNKNOWN_ID;  // invite ID
    int mCompNum;                  // component number
    private String mPartStat;      // "AC" (accepted), "TE" (tentative), etc.

    public ModifyInvitePartStat() {
    }

    public ModifyInvitePartStat(int mailboxId, int apptId, int inviteId, int compNum, String partStat) {
        setMailboxId(mailboxId);
        mApptId = apptId;
        mInvId= inviteId;
        mCompNum = compNum;
        mPartStat = partStat;
    }

    public int getOpCode() {
        return OP_MODIFY_INVITE_PARTSTAT;
    }

    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());
        mbox.modifyInvitePartStat(getOperationContext(), mApptId, mInvId, mCompNum, mPartStat);
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("apptId=").append(mApptId);
        sb.append(", invId=").append(mInvId);
        sb.append(", comp=").append(mCompNum);
        sb.append(", partStat=").append(mPartStat);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mApptId);
        out.writeInt(mInvId);
        out.writeInt(mCompNum);
        writeUTF8(out, mPartStat);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mApptId = in.readInt();
        mInvId = in.readInt();
        mCompNum = in.readInt();
        mPartStat = readUTF8(in);
    }
}
