/*
 * Created on 2004. 7. 21.
 */
package com.zimbra.cs.redolog.op;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.zimbra.cs.mailbox.MailItem;
import com.zimbra.cs.mailbox.MailItem.TargetConstraint;
import com.zimbra.cs.mailbox.Mailbox;
import com.zimbra.cs.service.ServiceException;


/**
 * @author jhahm
 */
public class AlterItemTag extends RedoableOp {

	private int mId;
    private byte mType;
	private int mTagId;
	private boolean mTagged;
    private String mConstraint;

	public AlterItemTag() {
		mId = UNKNOWN_ID;
        mType = MailItem.TYPE_UNKNOWN;
		mTagId = UNKNOWN_ID;
		mTagged = false;
        mConstraint = "";
	}

	public AlterItemTag(int mailboxId, int id, byte type, int tagId, boolean tagged, TargetConstraint tcon) {
		setMailboxId(mailboxId);
		mId = id;
        mType = type;
		mTagId = tagId;
		mTagged = tagged;
        mConstraint = (tcon == null ? "" : tcon.toString());
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.Redoable#getOperationCode()
	 */
	public int getOpCode() {
		return OP_ALTER_ITEM_TAG;
	}

	/* (non-Javadoc)
	 * @see com.zimbra.cs.redolog.Redoable#getRedoContent()
	 */
	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", type=").append(mType);
        sb.append(", tag=").append(mTagId).append(", tagged=").append(mTagged);
        if (mConstraint.length() > 0)
            sb.append(", constraint=").append(mConstraint);
		return sb.toString();
	}

	protected void serializeData(DataOutput out) throws IOException {
		out.writeInt(mId);
        out.writeByte(mType);
		out.writeInt(mTagId);
		out.writeBoolean(mTagged);
        out.writeUTF(mConstraint);
	}

	protected void deserializeData(DataInput in) throws IOException {
		mId = in.readInt();
        mType = in.readByte();
		mTagId = in.readInt();
		mTagged = in.readBoolean();
        mConstraint = in.readUTF();
	}

	public void redo() throws Exception {
		int mboxId = getMailboxId();
		Mailbox mbox = Mailbox.getMailboxById(mboxId);

        TargetConstraint tcon = null;
        if (mConstraint != null && mConstraint.length() > 0)
            try {
                tcon = TargetConstraint.parseConstraint(mbox, mConstraint);
            } catch (ServiceException e) {
                mLog.warn(e);
            }

		mbox.alterTag(getOperationContext(), mId, mType, mTagId, mTagged, tcon);
	}
}
