/*
 * Created on Sep 19, 2005
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;

/**
 * @author dkarp
 */
public class ColorItem extends RedoableOp {

    private int mId;
    private byte mType;
    private byte mColor;

    public ColorItem() {
        mId = UNKNOWN_ID;
    }

    public ColorItem(int mailboxId, int id, byte type, byte color) {
        setMailboxId(mailboxId);
        mId = id;
        mType = type;
        mColor = color;
    }

    public int getOpCode() {
        return OP_COLOR_ITEM;
    }

    protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", color=").append(mColor);
        return sb.toString();
    }

    protected void serializeData(DataOutput out) throws IOException {
        out.writeInt(mId);
        out.writeByte(mType);
        out.writeByte(mColor);
    }

    protected void deserializeData(DataInput in) throws IOException {
        mId = in.readInt();
        mType = in.readByte();
        mColor = in.readByte();
    }

    public void redo() throws Exception {
        int mboxId = getMailboxId();
        Mailbox mailbox = Mailbox.getMailboxById(mboxId);
        mailbox.setColor(getOperationContext(), mId, mType, mColor);
    }
}
