/*
 * Created on 2004. 9. 13.
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
public class SetItemTags extends RedoableOp {

	private int mId;
	private byte mType;
	private int mFlags;
    private long mTags;
    private String mConstraint;

	public SetItemTags() {
		mId = UNKNOWN_ID;
		mType = MailItem.TYPE_UNKNOWN;
        mConstraint = "";
	}

	public SetItemTags(int mailboxId, int itemId, byte itemType, int flags, long tags, TargetConstraint tcon) {
		setMailboxId(mailboxId);
		mId = itemId;
		mType = itemType;
		mFlags = flags;
        mTags = tags;
        mConstraint = (tcon == null ? "" : tcon.toString());
	}

	public int getOpCode() {
		return OP_SET_ITEM_TAGS;
	}

	protected String getPrintableData() {
        StringBuffer sb = new StringBuffer("id=");
        sb.append(mId).append(", type=").append(mType);
        sb.append(", flags=[").append(mFlags);
        sb.append("], tags=[").append(mTags).append("]");
        if (mConstraint.length() > 0)
            sb.append(", constraint=").append(mConstraint);
        return sb.toString();
	}

	protected void serializeData(DataOutput out) throws IOException {
		out.writeInt(mId);
		out.writeByte(mType);
        out.writeInt(mFlags);
        out.writeLong(mTags);
        out.writeUTF(mConstraint);
	}

	protected void deserializeData(DataInput in) throws IOException {
		mId = in.readInt();
		mType = in.readByte();
        mFlags = in.readInt();
        mTags = in.readLong();
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

		mbox.setTags(getOperationContext(), mId, mType, mFlags, mTags, tcon);
	}
}
