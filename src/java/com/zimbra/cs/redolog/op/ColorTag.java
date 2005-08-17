/*
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.Mailbox;


/**
 * @author jhahm
 */
public class ColorTag extends RedoableOp {

	private int mTagId;
	private byte mColor;

	public ColorTag() {
		mTagId = UNKNOWN_ID;
		mColor = 0;
	}

	public ColorTag(int mailboxId, int tagId, byte color) {
		setMailboxId(mailboxId);
		mTagId = tagId;
		mColor = color;
	}

	public int getOpCode() {
		return OP_COLOR_TAG;
	}

	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("tag=");
        sb.append(mTagId).append(", color=").append(mColor);
        return sb.toString();
	}

	protected void serializeData(DataOutput out) throws IOException {
		out.writeInt(mTagId);
		out.writeByte(mColor);
	}

	protected void deserializeData(DataInput in) throws IOException {
		mTagId = in.readInt();
		mColor = in.readByte();
	}

	public void redo() throws Exception {
		int mboxId = getMailboxId();
		Mailbox mbox = Mailbox.getMailboxById(mboxId);
        mbox.colorTag(getOperationContext(), mTagId, mColor);
	}
}
