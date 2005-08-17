package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;


public class ModifyInviteFlag extends RedoableOp {

    private int mId = UNKNOWN_ID;
    int mCompNum; // component number
    private int mFlag;
    private boolean mAdd; // true to OR the bit in, false to AND it out

    public ModifyInviteFlag() {
    }

    public ModifyInviteFlag(int mailboxId, int id, int compNum, int flag, boolean add) {
        setMailboxId(mailboxId);
        mId = id;
        mCompNum = compNum;
        mFlag = flag;
        mAdd = add;
    }
    
    public int getOpCode() {
        return OP_MODIFY_INVITE_FLAG;
    }

    public void redo() throws Exception {
        Mailbox mbox = Mailbox.getMailboxById(getMailboxId());

//        mbox.modifyInviteFlag(getOperationContext(), mId, mCompNum, mFlag, mAdd);
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", comp=").append(mCompNum);
        sb.append(", flag=").append(mFlag).append(", add=").append(mAdd);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mId);
        out.writeInt(mCompNum);
        out.writeInt(mFlag);
        out.writeBoolean(mAdd);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mId = in.readInt();
        mCompNum = in.readInt();
        mFlag = in.readInt();
        mAdd  = in.readBoolean();
    }
    
}
