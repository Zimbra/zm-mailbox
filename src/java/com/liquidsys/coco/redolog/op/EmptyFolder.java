/*
 * Created on Jun 6, 2005
 */
package com.liquidsys.coco.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.liquidsys.coco.mailbox.Mailbox;

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

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mId);
        out.writeBoolean(mSubfolders);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mId = in.readInt();
        mSubfolders = in.readBoolean();
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mbox = Mailbox.getMailboxById(mboxId);
        mbox.emptyFolder(getOperationContext(), mId, mSubfolders);
    }
}
