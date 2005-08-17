/*
 * Created on 2004. 12. 14.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author jhahm
 */
public class ColorNote extends RedoableOp {

    private int mId;
    private byte mColor;

    public ColorNote() {
        mId = UNKNOWN_ID;
    }

    public ColorNote(int mailboxId, int id, byte color) {
        setMailboxId(mailboxId);
        mId = id;
        mColor = color;
    }

    public int getOpCode() {
        return OP_COLOR_NOTE;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", color=").append(mColor);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mId);
        out.writeByte(mColor);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mId = in.readInt();
        mColor = in.readByte();
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = Mailbox.getMailboxById(mboxId);
        mailbox.colorNote(getOperationContext(), mId, mColor);
    }
}
