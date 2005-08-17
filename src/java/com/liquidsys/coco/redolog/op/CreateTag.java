/*
 * Created on 2004. 7. 21.
 */
package com.liquidsys.coco.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.liquidsys.coco.mailbox.MailServiceException;
import com.liquidsys.coco.mailbox.Mailbox;


/**
 * @author jhahm
 */
public class CreateTag extends RedoableOp {

	private int mTagId;
	private String mName;
	private byte mColor;

	public CreateTag() {
		mTagId = UNKNOWN_ID;
		mColor = 0;
	}

	public CreateTag(int mailboxId, String name, byte color) {
		setMailboxId(mailboxId);
		mTagId = UNKNOWN_ID;
		mName = name != null ? name : "";
		mColor = color;
	}

	public int getTagId() {
		return mTagId;
	}

	public void setTagId(int tagId) {
		mTagId = tagId;
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.Redoable#getOperationCode()
	 */
	public int getOpCode() {
		return OP_CREATE_TAG;
	}

	/* (non-Javadoc)
	 * @see com.liquidsys.coco.redolog.Redoable#getRedoContent()
	 */
	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=").append(mTagId);
        sb.append(", name=").append(mName).append(", color=").append(mColor);
        return sb.toString();
	}

	protected void serializeData(DataOutput out) throws IOException {
		out.writeInt(mTagId);
		writeUTF8(out, mName);
		out.writeByte(mColor);
	}

	protected void deserializeData(DataInput in) throws IOException {
		mTagId = in.readInt();
		mName = readUTF8(in);
		mColor = in.readByte();
	}

	public void redo() throws Exception {
		int mboxId = getMailboxId();
		Mailbox mbox = Mailbox.getMailboxById(mboxId);
        try {
            mbox.createTag(getOperationContext(), mName, mColor);
        } catch (MailServiceException e) {
            String code = e.getCode();
            if (code.equals(MailServiceException.ALREADY_EXISTS)) {
                if (mLog.isInfoEnabled())
                    mLog.info("Tag " + mTagId + " already exists in mailbox " + mboxId);
            } else
                throw e;
        }
	}
}
