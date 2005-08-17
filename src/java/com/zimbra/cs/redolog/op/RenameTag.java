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
public class RenameTag extends RedoableOp {

	private int mTagId;
	private String mName;

	public RenameTag() {
		mTagId = UNKNOWN_ID;
	}

	public RenameTag(int mailboxId, int tagId, String name) {
		setMailboxId(mailboxId);
		mTagId = tagId;
		mName = name != null ? name : "";
	}

	public int getOpCode() {
		return OP_RENAME_TAG;
	}

	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mTagId).append(", name=").append(mName);
        return sb.toString();
	}

	protected void serializeData(DataOutput out) throws IOException {
		out.writeInt(mTagId);
		writeUTF8(out, mName);
	}

	protected void deserializeData(DataInput in) throws IOException {
		mTagId = in.readInt();
		mName = readUTF8(in);
	}

	public void redo() throws Exception {
		int mboxId = getMailboxId();
		Mailbox mbox = Mailbox.getMailboxById(mboxId);
		mbox.renameTag(getOperationContext(), mTagId, mName);
	}
}
